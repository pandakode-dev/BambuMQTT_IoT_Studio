package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import kotlin.math.ceil

@Composable
fun SolarOffGridCalculatorCard() {
    var systemVoltage by remember { mutableStateOf("12.0") }     // Tegangan Aki 12V atau LiFePO4 3.7V
    var avgLoadCurrentMa by remember { mutableStateOf("85.0") }  // Rata-rata arus ESP32 + Sensor + Modem 4G (mA)
    var autonomyDays by remember { mutableStateOf("3") }         // Cadangan mendung tanpa matahari (Hari)
    var peakSunHours by remember { mutableStateOf("4.5") }       // Rata-rata PSH Khatulistiwa / Riau (4.5 jam/hari)

    val vSys = (systemVoltage.toDoubleOrNull() ?: 12.0).coerceAtLeast(3.2)
    val iMa = (avgLoadCurrentMa.toDoubleOrNull() ?: 85.0).coerceAtLeast(1.0)
    val days = (autonomyDays.toDoubleOrNull() ?: 3.0).coerceAtLeast(1.0)
    val psh = (peakSunHours.toDoubleOrNull() ?: 4.5).coerceAtLeast(1.0)

    // 1. Konsumsi Energi Harian (Watt-Hour / Hari)
    val dailyEnergyWh = (iMa / 1000.0) * vSys * 24.0
    // 2. Ukuran Panel Surya WP (Faktor rugi panas & efisiensi konversi MPPT = 1.35x)
    val rawSolarWp = (dailyEnergyWh / psh) * 1.35
    val recommendedCommercialWp = when {
        rawSolarWp <= 5.0 -> 5
        rawSolarWp <= 10.0 -> 10
        rawSolarWp <= 20.0 -> 20
        rawSolarWp <= 30.0 -> 30
        rawSolarWp <= 50.0 -> 50
        rawSolarWp <= 100.0 -> 100
        else -> ceil(rawSolarWp / 50.0).toInt() * 50
    }

    // 3. Kapasitas Aki / Baterai (Ah) dengan batas aman Depth of Discharge (DoD 80%)
    val requiredBatteryWh = (dailyEnergyWh * days) / 0.80
    val requiredBatteryAh = requiredBatteryWh / vSys
    // Setara berapa sel baterai Li-Ion 18650 (1 sel = 3.7V x 3.0Ah = 11.1 Wh)
    val cells18650Count = ceil(requiredBatteryWh / 11.1).toInt().coerceAtLeast(1)
    // Arus minimum Solar Charge Controller (SCC)
    val sccCurrentAmp = ceil((recommendedCommercialWp / vSys) * 1.25).toInt().coerceAtLeast(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SolarPower, contentDescription = null, tint = CopperOrange, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOLAR & OFF-GRID IOT ENERGY CALCULATOR PRO",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Rancang ukuran Panel Surya (WP) & Aki untuk stasiun IoT kebun sawit / hutan 24/7",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Preset Cepat Skema Beban Lapangan
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = avgLoadCurrentMa == "25.0",
                    onClick = { systemVoltage = "3.7"; avgLoadCurrentMa = "25.0"; autonomyDays = "4" },
                    label = { Text("Node Sensor (3.7V)", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = avgLoadCurrentMa == "85.0",
                    onClick = { systemVoltage = "12.0"; avgLoadCurrentMa = "85.0"; autonomyDays = "3" },
                    label = { Text("ESP32 + Modem 4G (12V)", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = avgLoadCurrentMa == "350.0",
                    onClick = { systemVoltage = "12.0"; avgLoadCurrentMa = "350.0"; autonomyDays = "3" },
                    label = { Text("ESP-CAM + Router", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = systemVoltage,
                    onValueChange = { systemVoltage = it },
                    label = { Text("Sistem (Volt)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = avgLoadCurrentMa,
                    onValueChange = { avgLoadCurrentMa = it },
                    label = { Text("Beban Rata2 (mA)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = autonomyDays,
                    onValueChange = { autonomyDays = it },
                    label = { Text("Cadangan Mendung (Hari)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = peakSunHours,
                    onValueChange = { peakSunHours = it },
                    label = { Text("Matahari PSH (Jam)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Hasil Perhitungan Rekomendasi Engineering
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlotStatBox(
                    title = "BEBAN ENERGI HARIAN",
                    value = String.format("%.1f Wh/hr", dailyEnergyWh),
                    color = LogicCyan,
                    modifier = Modifier.weight(1f)
                )
                PlotStatBox(
                    title = "REKOMENDASI PANEL SURYA",
                    value = "$recommendedCommercialWp WP (${String.format("%.1f", rawSolarWp)}W)",
                    color = CopperOrange,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlotStatBox(
                    title = "KAPASITAS AKI (DoD 80%)",
                    value = String.format("%.1f Ah (@%.1fV)", requiredBatteryAh, vSys),
                    color = PcbEmeraldDark,
                    modifier = Modifier.weight(1f)
                )
                PlotStatBox(
                    title = "SETARA BATERAI 18650",
                    value = "$cells18650Count Sel (3000mAh)",
                    color = LogicViolet,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💡 Rekomendasi Rakitan: Gunakan Panel Surya Monocrystalline $recommendedCommercialWp WP + Solar Charge Controller MPPT ${sccCurrentAmp}A + Baterai minimal ${String.format("%.1f", requiredBatteryAh)} Ah agar tahan $autonomyDays hari hujan berturut-turut.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}