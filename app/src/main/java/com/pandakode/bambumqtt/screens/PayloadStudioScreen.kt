package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.EmbeddedMathEngine
import com.pandakode.bambumqtt.ui.theme.*

@Composable
fun PayloadStudioScreen() {
    var deviceId by remember { mutableStateOf("ESP32_PKU_01") }
    var sensorTemp by remember { mutableStateOf("34.5") }
    var relayState by remember { mutableStateOf("true") }

    // Default frame Modbus RTU: Slave ID 01, Function 03 (Read Holding Reg), Start 00 00, Length 00 02
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. MQTT JSON PAYLOAD COMPOSER
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
                        fontSize = 14.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = { Text("Device ID") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sensorTemp,
                        onValueChange = { sensorTemp = it },
                        label = { Text("Nilai Sensor") },
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = relayState,
                        onValueChange = { relayState = it },
                        label = { Text("Status Bool") },
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

        // 2. KALKULATOR CHECKSUM MODBUS RTU CRC-16 (RS485 INDUSTRIAL IOT)
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
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Wajib untuk debugging sensor industri RS485 (NPK Tanah, Weather Station, Power Meter).",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = modbusHexInput,
                    onValueChange = { modbusHexInput = it },
                    label = { Text("Frame HEX Tanpa CRC (Contoh: 01 03 00 00 00 02)") },
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