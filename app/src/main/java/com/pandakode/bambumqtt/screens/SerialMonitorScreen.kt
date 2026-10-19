package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.EmbeddedMathEngine
import com.pandakode.bambumqtt.model.SerialLogMessage
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.UsbOtgDriverEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SerialMonitorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedBaud by remember { mutableStateOf("115200") }
    var isHexMode by remember { mutableStateOf(false) }
    // 3 Mode Koneksi: "USB_OTG", "WIFI_OTA", "SIMULATOR"
    var portMode by remember { mutableStateOf("USB_OTG") }
    var otaIpAddress by remember { mutableStateOf("192.168.4.1") }
    var otaPort by remember { mutableStateOf("23") }
    var connectionStatus by remember { mutableStateOf("Siap memindai port USB OTG / Wi-Fi OTA") }
    var isListeningLive by remember { mutableStateOf(false) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    var commandInput by remember { mutableStateOf("") }
    val baudRates = listOf("9600", "57600", "115200")
    val quickMacros = listOf("AT+GMR", "STATUS?", "RELAY_ON", "READ_ADC", "RESET_MCU")

    var logs by remember {
        mutableStateOf(
            listOf(
                SerialLogMessage(1, "18:20:01.120", "RX", "ets Jul 29 2019 12:21:46 - ESP32-WROOM-32 Booting...", "65 74 73 20 4A 75 6C"),
                SerialLogMessage(2, "18:20:01.450", "RX", "[OK] I2C Bus Initialized (SDA=21, SCL=22) @ 400kHz", "5B 4F 4B 5D 20 49 32 43"),
                SerialLogMessage(3, "18:20:02.010", "RX", "{\"temp\":34.6,\"hum\":68.2,\"vbat\":4.12,\"rssi\":-58}", "7B 22 74 65 6D 70 22 3A")
            )
        )
    }

    fun addRxLine(line: String) {
        val now = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logs = (logs + SerialLogMessage(
            timestamp = now,
            direction = "RX",
            asciiText = line,
            hexText = EmbeddedMathEngine.asciiToHex(line)
        )).takeLast(150)
    }

    fun toggleLiveConnection() {
        if (isListeningLive) {
            activeJob?.cancel()
            isListeningLive = false
            connectionStatus = "Koneksi dihentikan oleh pengguna."
            return
        }

        if (portMode == "USB_OTG") {
            val mcus = UsbOtgDriverEngine.scanConnectedMcuDevices(context)
            if (mcus.isEmpty()) {
                connectionStatus = "USB OTG Kosong! Colok kabel ESP32 atau pilih mode Wi-Fi OTA."
                return
            }
            val targetMcu = mcus.first()
            isListeningLive = true
            activeJob = scope.launch {
                UsbOtgDriverEngine.startUsbSerialSession(
                    context = context,
                    device = targetMcu.device,
                    baudRate = selectedBaud.toIntOrNull() ?: 115200,
                    onLineReceived = { line -> addRxLine(line) },
                    onStatusChange = { st -> connectionStatus = st }
                )
                isListeningLive = false
            }
        } else if (portMode == "WIFI_OTA") {
            isListeningLive = true
            activeJob = scope.launch {
                UsbOtgDriverEngine.startWifiOtaSerialSession(
                    espIp = otaIpAddress,
                    espPort = otaPort.toIntOrNull() ?: 23,
                    onLineReceived = { line -> addRxLine(line) },
                    onStatusChange = { st -> connectionStatus = st }
                )
                isListeningLive = false
            }
        }
    }

    fun sendSerialCommand(cmd: String) {
        if (cmd.isBlank()) return
        val now = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val txMsg = SerialLogMessage(
            timestamp = now,
            direction = "TX",
            asciiText = cmd,
            hexText = EmbeddedMathEngine.asciiToHex(cmd)
        )
        logs = logs + txMsg

        // Jika ada perangkat USB OTG terhubung, kirim langsung via kabel!
        val mcus = UsbOtgDriverEngine.scanConnectedMcuDevices(context)
        if (portMode == "USB_OTG" && mcus.isNotEmpty()) {
            val payload = "$cmd\r\n".toByteArray(Charsets.UTF_8)
            val res = UsbOtgDriverEngine.transmitRawUsbData(context, mcus.first().device, payload)
            if (res.isFailure) {
                addRxLine("[USB ERROR] ${res.exceptionOrNull()?.message}")
            }
            return
        }

        // Balasan cerdas Simulator jika sedang tidak mencolok ESP32 fisik
        val replyAscii = when (cmd.uppercase()) {
            "AT+GMR" -> "AT version:2.4.0.0(ESP32-SDK v4.4) - OK"
            "STATUS?" -> "SYS_OK | Heap Free: 248 KB | Uptime: 04:12:55"
            "RELAY_ON" -> "[GPIO 23] HIGH -> Pompa Irigasi Menyala"
            "READ_ADC" -> "ADC1_CH4 (GPIO 32) = 2890 Raw (2.33 Volt)"
            "RESET_MCU" -> "Rebooting ESP32 Dual-Core Xtensa LX6..."
            else -> "ACK_RECV: \"$cmd\" (${cmd.length} bytes processed)"
        }
        addRxLine(replyAscii)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. HEADER KONFIGURASI PORT YANG SUDAH DIRAPIKAN (ANTI TERPOTONG!)
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Baris 1: Judul Port + Tombol Connect Live
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (portMode == "USB_OTG") Icons.Default.Usb else Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = CopperGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (portMode == "USB_OTG") "USB OTG Serial (CH340/CP2102)" else "Wi-Fi OTA Serial Stream (TCP)",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = connectionStatus,
                                color = PcbMintLight,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Connect / Stop Live Reading
                    Button(
                        onClick = { toggleLiveConnection() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListeningLive) CopperOrange else CopperGold
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isListeningLive) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = SiliconDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isListeningLive) "STOP" else "CONNECT",
                            color = SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // Jika Mode Wi-Fi OTA dipilih, tampilkan input IP & Port ESP32
                if (portMode == "WIFI_OTA") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = otaIpAddress,
                            onValueChange = { otaIpAddress = it },
                            label = { Text("IP ESP32 (OTA)", color = PcbMintLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CopperGold,
                                unfocusedBorderColor = PcbMintLight.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.weight(1.6f)
                        )
                        OutlinedTextField(
                            value = otaPort,
                            onValueChange = { otaPort = it },
                            label = { Text("Port TCP", color = PcbMintLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CopperGold,
                                unfocusedBorderColor = PcbMintLight.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }

                // Baris 2: Horizontal Scrollable Chips (Agar 100% Rapi & Tidak Pernah Terpotong di HP Apa Pun!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip Ganti Jalur (Kabel OTG vs Wi-Fi OTA)
                    FilterChip(
                        selected = portMode == "WIFI_OTA",
                        onClick = {
                            portMode = if (portMode == "USB_OTG") "WIFI_OTA" else "USB_OTG"
                        },
                        label = {
                            Text(
                                text = if (portMode == "USB_OTG") "Jalur: Kabel OTG" else "Jalur: Wi-Fi OTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LogicCyan,
                            selectedLabelColor = SiliconDark,
                            labelColor = CopperGold
                        )
                    )

                    // Chip Baud Rate
                    baudRates.forEach { baud ->
                        FilterChip(
                            selected = selectedBaud == baud,
                            onClick = { selectedBaud = baud },
                            label = { Text("$baud bps", fontSize = 11.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CopperGold,
                                selectedLabelColor = SiliconDark,
                                labelColor = Color.White
                            )
                        )
                    }

                    // Chip ASCII / HEX (Satu Baris Utuh!)
                    FilterChip(
                        selected = isHexMode,
                        onClick = { isHexMode = !isHexMode },
                        label = {
                            Text(
                                text = if (isHexMode) "FORMAT: HEX" else "FORMAT: ASCII",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LogicViolet,
                            selectedLabelColor = Color.White,
                            labelColor = PcbMintLight
                        )
                    )
                }
            }
        }

        // 2. LAYAR TERMINAL CONSOLE (SILICON DARK)
        Card(
            colors = CardDefaults.cardColors(containerColor = SiliconDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.5.dp, PcbEmeraldMedium, RoundedCornerShape(14.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { item ->
                    val isTx = item.direction == "TX"
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "[${item.timestamp}] ${item.direction} ➔ ",
                            color = if (isTx) CopperGold else LogicCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (isHexMode) item.hexText else item.asciiText,
                            color = if (isTx) CopperSoftBg else LogicNeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 3. BARIS TOMBOL MACRO CEPAT (SCROLLABLE AGAR RAPI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickMacros.forEach { macro ->
                OutlinedButton(
                    onClick = { sendSerialCommand(macro) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(macro, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PcbEmeraldDark, maxLines = 1)
                }
            }
        }

        // 4. INPUT COMMAND SERIAL + TOMBOL SEND & CLEAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Ketik perintah AT / Serial (CR+LF)...", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    sendSerialCommand(commandInput)
                    commandInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim", tint = CopperGold)
            }
            IconButton(
                onClick = { logs = emptyList() },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Bersihkan", tint = Color(0xFFD32F2F))
            }
        }
    }
}