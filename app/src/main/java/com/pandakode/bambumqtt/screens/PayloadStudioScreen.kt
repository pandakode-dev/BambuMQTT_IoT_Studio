package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.EmbeddedMathEngine
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.MqttLocalBrokerServer

@Composable
fun PayloadStudioScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("🖧 Broker HP", "Voice & CSV", "ESP-CAM/SSL", "JSON/CRC")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PcbEmeraldDark,
            contentColor = CopperGold
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selectedTab == idx) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> LocalPhoneMqttBrokerContent()
            1 -> SmartBridgeVoiceContent()
            2 -> EspCamSecurityContent()
            3 -> JsonAndModbusCrcContent()
        }
    }
}

@Composable
fun LocalPhoneMqttBrokerContent() {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var portInput by remember { mutableStateOf("1883") }
    var testBroadcastTopic by remember { mutableStateOf("pandakode/relay/pump1") }
    var testBroadcastMsg by remember { mutableStateOf("1") }

    val deviceIp = remember(refreshTrigger) { MqttLocalBrokerServer.getDeviceLocalIpAddress() }
    val isBrokerRunning = remember(refreshTrigger) { MqttLocalBrokerServer.isRunning }
    val connectedClients = remember(refreshTrigger) { MqttLocalBrokerServer.clients.toList() }
    val logs = remember(refreshTrigger) { MqttLocalBrokerServer.trafficLogs.toList() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. KARTU HOST SERVER BROKER MQTT DI HP ANDROID
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (isBrokerRunning) Color(0xFF00E676) else CopperGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBrokerRunning) "🟢 SERVER BROKER HP AKTIF" else "⚪ LOCAL MQTT BROKER SERVER",
                                color = if (isBrokerRunning) Color(0xFF00E676) else CopperGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "IP Broker HP Anda: $deviceIp:$portInput",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Arahkan mqtt_server di ESP32 ke IP di atas via Wi-Fi / Hotspot HP!",
                                color = PcbMintLight,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isBrokerRunning) {
                                MqttLocalBrokerServer.stopBroker { refreshTrigger++ }
                            } else {
                                val port = portInput.toIntOrNull() ?: 1883
                                MqttLocalBrokerServer.startBroker(scope, port) { refreshTrigger++ }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBrokerRunning) Color(0xFFD32F2F) else Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isBrokerRunning) "STOP SERVER" else "START BROKER",
                            color = if (isBrokerRunning) Color.White else SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port TCP Broker", color = PcbMintLight, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CopperGold,
                            unfocusedBorderColor = PcbMintLight.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { MqttLocalBrokerServer.simulateEsp32ClientTraffic { refreshTrigger++ } },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulasi ESP32 Masuk", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // 2. BROADCAST PESAN DARI BROKER HP KE SEMUA ESP32 YANG TERHUBUNG
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📢 BROADCAST COMMAND KE KLIEN ESP32 (${connectedClients.size} Terhubung)",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = testBroadcastTopic,
                        onValueChange = { testBroadcastTopic = it },
                        label = { Text("Topic Target", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = testBroadcastMsg,
                        onValueChange = { testBroadcastMsg = it },
                        label = { Text("Payload", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Button(
                    onClick = {
                        MqttLocalBrokerServer.broadcastPublishToSubscribers(testBroadcastTopic, testBroadcastMsg)
                        MqttLocalBrokerServer.trafficLogs.add(
                            0,
                            com.pandakode.bambumqtt.utils.BrokerTrafficLog(
                                time = "NOW",
                                type = "BROADCAST",
                                clientInfo = "HOST HP",
                                detail = "Topic: [$testBroadcastTopic] ➔ \"$testBroadcastMsg\" dikirim ke ${connectedClients.size} klien"
                            )
                        )
                        refreshTrigger++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = CopperGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push Paket PUBLISH dari Server HP", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                // Daftar Klien ESP32 yang Terhubung ke HP
                if (connectedClients.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    connectedClients.forEach { cl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EsdMatBg, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🔌 ${cl.clientId} (${cl.ipAddress})", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = PcbEmeraldDark)
                                Text("Sub: ${cl.subscribedTopics.joinToString().ifBlank { "-" }}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                            }
                            Text(cl.connectedAt, fontSize = 10.sp, color = CopperOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. KONSOL LOG TRAFFIC PAKET BROKER HP
        Card(
            colors = CardDefaults.cardColors(containerColor = SiliconDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "📡 LIVE MQTT BROKER PACKET SNIFFER (${logs.size} Events)",
                    color = CopperGold,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
                if (logs.isEmpty()) {
                    Text(
                        text = "Klik 'START BROKER' di atas untuk menjadikan HP Anda sebagai Server MQTT Port 1883.",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                } else {
                    logs.take(12).forEach { ev ->
                        Text(
                            text = "[${ev.time}] [${ev.type}] ${ev.clientInfo} -> ${ev.detail}",
                            color = when (ev.type) {
                                "CONNECT" -> LogicNeonGreen
                                "PUBLISH", "BROADCAST" -> CopperGold
                                "SUBSCRIBE" -> LogicCyan
                                else -> Color.White
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JsonAndModbusCrcContent() {
    var deviceId by remember { mutableStateOf("ESP32_PKU_01") }
    var sensorTemp by remember { mutableStateOf("34.5") }
    var relayState by remember { mutableStateOf("true") }
    var modbusHexInput by remember { mutableStateOf("01 03 00 00 00 02") }

    val formattedJson = remember(deviceId, sensorTemp, relayState) {
        """
        {
          "device_id": "$deviceId",
          "studio": "PandaKode",
          "telemetry": {
            "temperature_c": ${sensorTemp.toDoubleOrNull() ?: 0.0},
            "relay_active": ${relayState.lowercase() == "true"}
          }
        }
        """.trimIndent()
    }

    val crcResult = remember(modbusHexInput) {
        EmbeddedMathEngine.calculateModbusCrc16(modbusHexInput)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Icon(Icons.Default.DataObject, contentDescription = null, tint = CopperOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MQTT JSON PAYLOAD COMPOSER & VALIDATOR",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("Device ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sensorTemp,
                        onValueChange = { sensorTemp = it },
                        label = { Text("Nilai Sensor", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = relayState,
                        onValueChange = { relayState = it },
                        label = { Text("Status Bool", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedJson,
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = LogicViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INDUSTRIAL MODBUS RTU CRC-16 CALCULATOR",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 13.sp
                    )
                }

                OutlinedTextField(
                    value = modbusHexInput,
                    onValueChange = { modbusHexInput = it },
                    label = { Text("Frame HEX Tanpa CRC (Contoh: 01 03 00 00 00 02)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PlotStatBox(
                    title = "BYTE CHECKSUM CRC-16 (LOW BYTE FIRST)",
                    value = crcResult,
                    color = LogicViolet,
                    modifier = Modifier.fillMaxWidth()
                )
                PlotStatBox(
                    title = "FRAME LENGKAP SIAP KIRIM KE RS485",
                    value = "${modbusHexInput.trim()} ${crcResult.take(5)}",
                    color = PcbEmeraldDark,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}