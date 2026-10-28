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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.EmbeddedMathEngine
import com.pandakode.bambumqtt.ui.theme.*

@Composable
fun HardwareLabScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "🤖 AI & Node-RED",
        "☀️ Solar & Baterai",
        "🖨️ PDF & FFT Lab",
        "📌 Pinout & I2C"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PcbEmeraldDark,
            contentColor = CopperGold,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (selectedTab == index) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> BambuAiAndFlowContent()
            1 -> {
                SolarOffGridCalculatorCard()
                DeepSleepBatteryCard()
                AdcVoltageDividerCard()
            }
            2 -> {
                SchematicBlueprintExporterCard()
                FftSpectrumAnalyzerView()
            }
            3 -> {
                I2cAddressDecoderList()
                Esp32PinoutReferenceList()
            }
        }
    }
}

@Composable
fun DeepSleepBatteryCard() {
    var capacityMah by remember { mutableStateOf("3000") }
    var activeCurrentMa by remember { mutableStateOf("160") }
    var activeSeconds by remember { mutableStateOf("4") }
    var sleepUa by remember { mutableStateOf("150") }
    var intervalMinutes by remember { mutableStateOf("15") }

    val (avgMa, totalDays) = EmbeddedMathEngine.calculateBatteryLifeDays(
        batteryCapacityMah = capacityMah.toDoubleOrNull() ?: 3000.0,
        activeCurrentMa = activeCurrentMa.toDoubleOrNull() ?: 160.0,
        activeTimeSeconds = activeSeconds.toDoubleOrNull() ?: 4.0,
        sleepCurrentUa = sleepUa.toDoubleOrNull() ?: 150.0,
        wakeIntervalMinutes = intervalMinutes.toDoubleOrNull() ?: 15.0
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = CopperOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ESTIMATOR UMUR BATERAI IOT (ESP32 DEEP-SLEEP)",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = capacityMah, onValueChange = { capacityMah = it }, label = { Text("Baterai (mAh)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = intervalMinutes, onValueChange = { intervalMinutes = it }, label = { Text("Interval Kirim (Mnt)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = activeCurrentMa, onValueChange = { activeCurrentMa = it }, label = { Text("Aktif (mA)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = activeSeconds, onValueChange = { activeSeconds = it }, label = { Text("Durasi (Dtk)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = sleepUa, onValueChange = { sleepUa = it }, label = { Text("Sleep (µA)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlotStatBox("ARUS RATA-RATA", String.format("%.2f mA", avgMa), LogicCyan, Modifier.weight(1f))
                PlotStatBox("ESTIMASI HIDUP", String.format("%.0f Hari (%.1f Bln)", totalDays, totalDays / 30.0), PcbEmeraldDark, Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
fun AdcVoltageDividerCard() {
    var vInput by remember { mutableStateOf("12.0") }
    var r1Kohm by remember { mutableStateOf("30.0") }
    var r2Kohm by remember { mutableStateOf("10.0") }

    val vin = vInput.toDoubleOrNull() ?: 0.0
    val r1 = r1Kohm.toDoubleOrNull() ?: 1.0
    val r2 = r2Kohm.toDoubleOrNull() ?: 1.0

    val vOut = if (r1 + r2 > 0) vin * (r2 / (r1 + r2)) else 0.0
    val esp32AdcRaw = ((vOut / 3.3) * 4095.0).toInt().coerceIn(0, 4095)
    val isSafeForEsp32 = vOut <= 3.3

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = CopperOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PEMBAGI TEGANGAN SENSOR KE ADC ESP32 (MAX 3.3V)",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 13.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = vInput, onValueChange = { vInput = it }, label = { Text("Vin Sensor/Aki (V)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = r1Kohm, onValueChange = { r1Kohm = it }, label = { Text("R1 Atas (kΩ)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = r2Kohm, onValueChange = { r2Kohm = it }, label = { Text("R2 GND (kΩ)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlotStatBox(
                    title = "TEGANGAN PIN ADC (Vout)",
                    value = String.format("%.2f Volt", vOut),
                    color = if (isSafeForEsp32) PcbEmeraldDark else Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
                PlotStatBox(
                    title = "NILAI ADC 12-BIT (0-4095)",
                    value = "$esp32AdcRaw Raw",
                    color = LogicCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (isSafeForEsp32) "✅ AMAN: Tegangan di bawah batas 3.3V ESP32." else "⚠️ BAHAYA OVERVOLTAGE! Vout melebihi 3.3V, besarkan nilai R1!",
                color = if (isSafeForEsp32) PcbEmeraldDark else Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun Esp32PinoutReferenceList() {
    val pins = remember { EmbeddedMathEngine.getEsp32PinGuide() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pins.forEach { info ->
            val badgeColor = when (info.statusSafety) {
                "AMAN" -> PcbEmeraldDark
                "PERHATIAN" -> CopperOrange
                else -> Color(0xFFD32F2F)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = badgeColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(info.pinLabel, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 14.sp)
                        }
                        Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = info.statusSafety,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(info.capability, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = LogicCyan, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(info.notes, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun I2cAddressDecoderList() {
    var searchQuery by remember { mutableStateOf("") }
    val allDevices = remember { EmbeddedMathEngine.getI2cAddressDirectory() }
    val filtered = allDevices.filter {
        it.hexAddress.contains(searchQuery, ignoreCase = true) ||
                it.deviceName.contains(searchQuery, ignoreCase = true)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari Alamat HEX I2C (Misal: 0x3C, 0x27, 0x76, BME280)", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        filtered.forEach { dev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dev.hexAddress,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = CopperOrange,
                            fontSize = 16.sp
                        )
                        Surface(color = PcbEmeraldDark, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = dev.category,
                                color = CopperGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(dev.deviceName, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(dev.wiringNotes, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }
    }
}