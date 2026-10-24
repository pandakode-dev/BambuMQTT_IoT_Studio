package com.pandakode.bambumqtt.utils

import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

data class ConnectedMqttClient(
    val clientId: String,
    val ipAddress: String,
    val connectedAt: String,
    val subscribedTopics: MutableList<String> = mutableListOf(),
    @Transient val socket: Socket? = null,
    @Transient val outputStream: OutputStream? = null
)

data class BrokerTrafficLog(
    val id: Long = System.nanoTime(),
    val time: String,
    val type: String, // "CONNECT", "PUBLISH", "SUBSCRIBE", "PING", "SYSTEM"
    val clientInfo: String,
    val detail: String
)

object MqttLocalBrokerServer {
    var isRunning = false
        private set
    var activePort = 1883
        private set

    private var serverSocket: ServerSocket? = null
    private var brokerJob: Job? = null

    val clients = CopyOnWriteArrayList<ConnectedMqttClient>()
    val trafficLogs = CopyOnWriteArrayList<BrokerTrafficLog>()

    // Mendapatkan IP Lokal HP Android (Wi-Fi LAN atau Hotspot Tethering)
    fun getDeviceLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        // Prioritaskan IP LAN / Hotspot (192.168.x.x, 10.x.x.x, 172.x.x.x)
                        if (ip.startsWith("192.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun logEvent(type: String, client: String, detail: String, onUpdate: () -> Unit) {
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        trafficLogs.add(0, BrokerTrafficLog(time = now, type = type, clientInfo = client, detail = detail))
        if (trafficLogs.size > 120) {
            trafficLogs.removeAt(trafficLogs.size - 1)
        }
        onUpdate()
    }

    // Menyalakan Server Broker MQTT di HP Android
    fun startBroker(
        scope: CoroutineScope,
        port: Int = 1883,
        onStateUpdated: () -> Unit
    ) {
        if (isRunning) return
        activePort = port

        brokerJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                val myIp = getDeviceLocalIpAddress()
                logEvent("SYSTEM", "BROKER HOST", "🟢 Server MQTT Aktif di $myIp:$port (Siap menerima koneksi ESP32!)", onStateUpdated)

                while (isActive && isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(clientSocket, onStateUpdated)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    logEvent("SYSTEM", "ERROR", "Gagal membuka port $port: ${e.message}. Coba gunakan Port 1883 atau 1884.", onStateUpdated)
                }
                isRunning = false
                onStateUpdated()
            }
        }
    }

    fun stopBroker(onStateUpdated: () -> Unit) {
        isRunning = false
        try {
            clients.forEach { try { it.socket?.close() } catch (_: Exception) {} }
            clients.clear()
            serverSocket?.close()
            brokerJob?.cancel()
        } catch (_: Exception) {}
        logEvent("SYSTEM", "BROKER HOST", "🛑 Server Broker MQTT dihentikan.", onStateUpdated)
    }

    // Simulasi Klien ESP32 Masuk ke Server HP (Untuk Tes Instan Tanpa Board Fisik)
    fun simulateEsp32ClientTraffic(onStateUpdated: () -> Unit) {
        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val simId = "ESP32_Node_${(10..99).random()}"
        if (clients.none { it.clientId == simId }) {
            clients.add(
                ConnectedMqttClient(
                    clientId = simId,
                    ipAddress = "192.168.43.${(100..220).random()}",
                    connectedAt = now,
                    subscribedTopics = mutableListOf("pandakode/relay/#", "bambu/pwm/#")
                )
            )
        }
        logEvent("CONNECT", simId, "CONNACK 0x20 Sent -> Klien ESP32 Terhubung & Subscribe ke pandakode/relay/#", onStateUpdated)
        logEvent("PUBLISH", simId, "Topic: [pandakode/sensor/temp] ➔ Payload: \"${(29..37).random()}.${(0..9).random()} °C\" (Broadcasted to ${clients.size} clients)", onStateUpdated)
    }

    private fun handleClientConnection(socket: Socket, onStateUpdated: () -> Unit) {
        val remoteIp = socket.inetAddress?.hostAddress ?: "Unknown"
        var currentClient: ConnectedMqttClient? = null
        try {
            val inp: InputStream = socket.getInputStream()
            val out: OutputStream = socket.getOutputStream()

            while (isRunning && !socket.isClosed) {
                val firstByte = inp.read()
                if (firstByte == -1) break

                val packetType = (firstByte and 0xF0) ushr 4
                val remLen = readRemainingLength(inp)
                val payloadBytes = ByteArray(remLen)
                var readCount = 0
                while (readCount < remLen) {
                    val r = inp.read(payloadBytes, readCount, remLen - readCount)
                    if (r == -1) break
                    readCount += r
                }

                when (packetType) {
                    1 -> { // CONNECT (0x10)
                        val clientId = extractClientIdFromConnect(payloadBytes)
                        val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        val newClient = ConnectedMqttClient(clientId, remoteIp, now, mutableListOf(), socket, out)
                        currentClient = newClient
                        clients.removeAll { it.clientId == clientId }
                        clients.add(newClient)

                        // Balas dengan CONNACK (0x20, 0x02, 0x00, 0x00 = Connection Accepted)
                        out.write(byteArrayOf(0x20, 0x02, 0x00, 0x00))
                        out.flush()
                        logEvent("CONNECT", "$clientId ($remoteIp)", "Klien MQTT Terhubung -> CONNACK Accepted", onStateUpdated)
                    }
                    3 -> { // PUBLISH (0x30)
                        if (payloadBytes.size >= 2) {
                            val topicLen = ((payloadBytes[0].toInt() and 0xFF) shl 8) or (payloadBytes[1].toInt() and 0xFF)
                            if (payloadBytes.size >= 2 + topicLen) {
                                val topic = String(payloadBytes, 2, topicLen, Charsets.UTF_8)
                                val msgOffset = 2 + topicLen
                                val message = String(payloadBytes, msgOffset, payloadBytes.size - msgOffset, Charsets.UTF_8)
                                val senderId = currentClient?.clientId ?: remoteIp
                                logEvent("PUBLISH", senderId, "Topic: [$topic] ➔ \"$message\"", onStateUpdated)

                                // Teruskan (Broadcast) paket PUBLISH ini ke semua klien lain yang terhubung ke HP!
                                broadcastPublishToSubscribers(topic, message)
                            }
                        }
                    }
                    8 -> { // SUBSCRIBE (0x82)
                        if (payloadBytes.size >= 4) {
                            val packetIdMsb = payloadBytes[0]
                            val packetIdLsb = payloadBytes[1]
                            val topicLen = ((payloadBytes[2].toInt() and 0xFF) shl 8) or (payloadBytes[3].toInt() and 0xFF)
                            val topic = if (payloadBytes.size >= 4 + topicLen) {
                                String(payloadBytes, 4, topicLen, Charsets.UTF_8)
                            } else "pandakode/#"

                            currentClient?.subscribedTopics?.add(topic)
                            // Balas dengan SUBACK (0x90, 0x03, PacketID_MSB, PacketID_LSB, QoS 0x00)
                            out.write(byteArrayOf(0x90.toByte(), 0x03, packetIdMsb, packetIdLsb, 0x00))
                            out.flush()
                            logEvent("SUBSCRIBE", currentClient?.clientId ?: remoteIp, "Subscribed ke topik: [$topic]", onStateUpdated)
                        }
                    }
                    12 -> { // PINGREQ (0xC0)
                        // Balas dengan PINGRESP (0xD0, 0x00) agar koneksi ESP32 tetap hidup (Keep-Alive)
                        out.write(byteArrayOf(0xD0.toByte(), 0x00))
                        out.flush()
                    }
                    14 -> { // DISCONNECT (0xE0)
                        break
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            currentClient?.let {
                clients.remove(it)
                logEvent("DISCONNECT", it.clientId, "Klien terputus dari broker HP.", onStateUpdated)
            }
            try { socket.close() } catch (_: Exception) {}
        }
    }

    // Meneruskan pesan MQTT dari Broker HP langsung ke semua ESP32 yang sedang terhubung
    fun broadcastPublishToSubscribers(topic: String, message: String) {
        val body = ByteArrayOutputStream()
        val topicBytes = topic.toByteArray(Charsets.UTF_8)
        body.write((topicBytes.size ushr 8) and 0xFF)
        body.write(topicBytes.size and 0xFF)
        body.write(topicBytes)
        body.write(message.toByteArray(Charsets.UTF_8))

        val bodyBytes = body.toByteArray()
        val packet = ByteArrayOutputStream()
        packet.write(0x30) // PUBLISH QoS 0
        var x = bodyBytes.size
        do {
            var enc = x % 128
            x /= 128
            if (x > 0) enc = enc or 128
            packet.write(enc)
        } while (x > 0)
        packet.write(bodyBytes)
        val rawBytes = packet.toByteArray()

        clients.forEach { client ->
            try {
                client.outputStream?.write(rawBytes)
                client.outputStream?.flush()
            } catch (_: Exception) {}
        }
    }

    private fun readRemainingLength(inp: InputStream): Int {
        var multiplier = 1
        var value = 0
        var encodedByte: Int
        do {
            encodedByte = inp.read()
            if (encodedByte == -1) return 0
            value += (encodedByte and 127) * multiplier
            multiplier *= 128
        } while ((encodedByte and 128) != 0)
        return value
    }

    private fun extractClientIdFromConnect(bytes: ByteArray): String {
        return try {
            val protoLen = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
            val idLenIndex = 2 + protoLen + 4 // Lewati Level(1) + Flags(1) + KeepAlive(2)
            val idLen = ((bytes[idLenIndex].toInt() and 0xFF) shl 8) or (bytes[idLenIndex + 1].toInt() and 0xFF)
            String(bytes, idLenIndex + 2, idLen, Charsets.UTF_8).ifBlank { "ESP32_Client" }
        } catch (_: Exception) {
            "ESP32_Node"
        }
    }
}