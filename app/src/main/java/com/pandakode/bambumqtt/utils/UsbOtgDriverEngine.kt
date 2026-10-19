package com.pandakode.bambumqtt.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

data class DetectedUsbMcu(
    val device: UsbDevice,
    val chipVendorName: String,
    val vidHex: String,
    val pidHex: String,
    val productName: String,
    val hasPermission: Boolean
)

object UsbOtgDriverEngine {
    const val ACTION_USB_PERMISSION = "com.pandakode.bambumqtt.USB_PERMISSION"

    fun scanConnectedMcuDevices(context: Context): List<DetectedUsbMcu> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val list = mutableListOf<DetectedUsbMcu>()

        for (device in usbManager.deviceList.values) {
            val vid = device.vendorId
            val pid = device.productId
            val chipName = identifyUsbBridgeChip(vid, pid)
            val vidHex = String.format("0x%04X", vid)
            val pidHex = String.format("0x%04X", pid)
            val prodName = device.productName ?: "USB Serial Microcontroller"
            val permitted = usbManager.hasPermission(device)

            list.add(
                DetectedUsbMcu(
                    device = device,
                    chipVendorName = chipName,
                    vidHex = vidHex,
                    pidHex = pidHex,
                    productName = prodName,
                    hasPermission = permitted
                )
            )
        }
        return list
    }

    fun identifyUsbBridgeChip(vid: Int, pid: Int): String {
        return when (vid) {
            0x1A86 -> if (pid == 0x55D4) "WCH CH9102F (ESP32)" else "WCH CH340G / CH341"
            0x10C4 -> "Silicon Labs CP2102"
            0x0403 -> "FTDI FT232RL UART"
            0x303A -> "Espressif Native USB"
            0x2341 -> "Arduino Official USB"
            else -> "CDC-ACM Serial (VID:$vid)"
        }
    }

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
        usbManager.requestPermission(device, permissionIntent)
    }

    // 1. LIVE CONTINUOUS USB OTG SERIAL READER & WRITER (Membaca Serial.println ESP32 asli!)
    suspend fun startUsbSerialSession(
        context: Context,
        device: UsbDevice,
        baudRate: Int,
        onLineReceived: (String) -> Unit,
        onStatusChange: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            requestUsbPermission(context, device)
            onStatusChange("Meminta izin USB OTG... Klik 'Izinkan/Allow' pada pop-up Android.")
            return@withContext
        }

        val connection = usbManager.openDevice(device)
        if (connection == null) {
            onStatusChange("Gagal membuka port USB OTG.")
            return@withContext
        }

        var targetInterface: UsbInterface? = null
        var epIn: UsbEndpoint? = null

        for (i in 0 until device.interfaceCount) {
            val usbIf = device.getInterface(i)
            for (j in 0 until usbIf.endpointCount) {
                val ep = usbIf.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_IN) {
                    targetInterface = usbIf
                    epIn = ep
                    break
                }
            }
        }

        if (targetInterface == null || epIn == null) {
            connection.close()
            onStatusChange("Endpoint Bulk-IN tidak ditemukan pada perangkat USB ini.")
            return@withContext
        }

        connection.claimInterface(targetInterface, true)
        configureChipBaudRate(connection, device.vendorId, baudRate)
        onStatusChange("TERHUBUNG LIVE KE ${identifyUsbBridgeChip(device.vendorId, device.productId)} @ $baudRate bps")

        val buffer = ByteArray(256)
        val lineBuilder = StringBuilder()

        try {
            while (currentCoroutineContext().isActive) {
                val bytesRead = connection.bulkTransfer(epIn, buffer, buffer.size, 250)
                if (bytesRead > 0) {
                    // Untuk FTDI, 2 byte pertama adalah modem status header
                    val startOffset = if (device.vendorId == 0x0403 && bytesRead >= 2) 2 else 0
                    if (bytesRead > startOffset) {
                        val chunk = String(buffer, startOffset, bytesRead - startOffset, Charsets.UTF_8)
                        lineBuilder.append(chunk)
                        while (lineBuilder.contains("\n")) {
                            val idx = lineBuilder.indexOf("\n")
                            val cleanLine = lineBuilder.substring(0, idx).trim('\r')
                            lineBuilder.delete(0, idx + 1)
                            if (cleanLine.isNotEmpty()) {
                                withContext(Dispatchers.Main) {
                                    onLineReceived(cleanLine)
                                }
                            }
                        }
                    }
                } else {
                    delay(40L)
                }
            }
        } finally {
            connection.releaseInterface(targetInterface)
            connection.close()
            onStatusChange("Sesi USB OTG Ditutup.")
        }
    }

    // 2. LIVE WI-FI OTA SERIAL STREAMER (Membaca Serial Monitor ESP32 via Wi-Fi Telnet/WebSerial TCP)
    suspend fun startWifiOtaSerialSession(
        espIp: String,
        espPort: Int,
        onLineReceived: (String) -> Unit,
        onStatusChange: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            onStatusChange("Menghubungkan ke Wi-Fi OTA Serial $espIp:$espPort...")
            socket = Socket()
            socket.connect(InetSocketAddress(espIp, espPort), 3500)
            onStatusChange("TERHUBUNG WI-FI OTA SERIAL ($espIp:$espPort)")

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                if (line.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        onLineReceived(line)
                    }
                }
            }
        } catch (e: Exception) {
            onStatusChange("Wi-Fi OTA Terputus: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    // 3. Mengirim Paket Data / Perintah ke Kabel USB OTG
    fun transmitRawUsbData(
        context: Context,
        device: UsbDevice,
        payloadBytes: ByteArray,
        enterBootloaderFirst: Boolean = false
    ): Result<String> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            requestUsbPermission(context, device)
            return Result.failure(Exception("Klik 'Allow/Izinkan' pada pop-up USB Android lalu ulangi."))
        }

        val connection: UsbDeviceConnection = usbManager.openDevice(device)
            ?: return Result.failure(Exception("Gagal membuka jalur USB Host."))

        try {
            var targetInterface: UsbInterface? = null
            var epOut: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val usbIf = device.getInterface(i)
                for (j in 0 until usbIf.endpointCount) {
                    val ep = usbIf.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                        targetInterface = usbIf
                        epOut = ep
                        break
                    }
                }
            }

            if (targetInterface == null || epOut == null) {
                connection.close()
                return Result.failure(Exception("Endpoint Bulk-OUT tidak ditemukan."))
            }

            connection.claimInterface(targetInterface, true)
            configureChipBaudRate(connection, device.vendorId, 115200)

            if (enterBootloaderFirst) {
                triggerEsp32BootloaderSequence(connection, device.vendorId)
            }

            val sent = connection.bulkTransfer(epOut, payloadBytes, payloadBytes.size, 2500)
            connection.releaseInterface(targetInterface)
            connection.close()

            return if (sent >= 0) {
                Result.success("Terkirim $sent bytes ke ${identifyUsbBridgeChip(device.vendorId, device.productId)}")
            } else {
                Result.failure(Exception("Transfer gagal (kode: $sent)"))
            }
        } catch (e: Exception) {
            connection.close()
            return Result.failure(e)
        }
    }

    private fun configureChipBaudRate(conn: UsbDeviceConnection, vid: Int, baudRate: Int) {
        val b0 = (baudRate and 0xFF).toByte()
        val b1 = ((baudRate ushr 8) and 0xFF).toByte()
        val b2 = ((baudRate ushr 16) and 0xFF).toByte()
        val b3 = ((baudRate ushr 24) and 0xFF).toByte()

        when (vid) {
            0x10C4 -> { // Silicon Labs CP2102
                conn.controlTransfer(0x41, 0x00, 0x01, 0, null, 0, 400)
                val baudBytes = byteArrayOf(b0, b1, b2, b3)
                conn.controlTransfer(0x41, 0x1E, 0, 0, baudBytes, 4, 400)
            }
            0x1A86 -> { // WCH CH340
                conn.controlTransfer(0x40, 0xA1, 0, 0, null, 0, 400)
                val factor = if (baudRate == 9600) 0xB202 else 0xCC03
                conn.controlTransfer(0x40, 0x9A, 0x1312, factor, null, 0, 400)
            }
            else -> { // Standard CDC-ACM
                val lineCoding = byteArrayOf(b0, b1, b2, b3, 0x00, 0x00, 0x08)
                conn.controlTransfer(0x21, 0x20, 0, 0, lineCoding, lineCoding.size, 400)
            }
        }
    }

    private fun triggerEsp32BootloaderSequence(conn: UsbDeviceConnection, vid: Int) {
        if (vid == 0x10C4) {
            conn.controlTransfer(0x41, 0x07, 0x0302, 0, null, 0, 300)
            Thread.sleep(100)
            conn.controlTransfer(0x41, 0x07, 0x0301, 0, null, 0, 300)
            Thread.sleep(50)
        } else {
            conn.controlTransfer(0x21, 0x22, 0x02, 0, null, 0, 300)
            Thread.sleep(100)
            conn.controlTransfer(0x21, 0x22, 0x01, 0, null, 0, 300)
            Thread.sleep(50)
        }
    }
}