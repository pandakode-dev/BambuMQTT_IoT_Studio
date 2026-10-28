package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.BlueprintExportEngine
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// =========================================================================
// 1. KOMPONEN SPEKTRUM FREKUENSI FFT / DFT (32-BIN SPECTRUM ANALYZER)
// =========================================================================
@Composable
fun FftSpectrumAnalyzerView() {
    var fundamentalHz by remember { mutableFloatStateOf(50f) } // Default 50 Hz (Getaran Motor / Listrik AC)
    var harmonicGain by remember { mutableFloatStateOf(0.45f) }
    var isRunningFft by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }

    val numBins = 32
    val sampleRateHz = 256f // 256 Hz sampling -> Nyquist 128 Hz (Tiap bin = 4 Hz)

    // Hitung Magnitudo Spektrum DFT 32-Bin Secara Real-Time
    val magnitudes = remember(fundamentalHz, harmonicGain, tick) {
        val n = 64
        val signal = FloatArray(n) { i ->
            val t = i / sampleRateHz
            val w1 = (2.0 * PI * fundamentalHz * t).toFloat()
            val w2 = (2.0 * PI * (fundamentalHz * 2.0f) * t).toFloat()
            val noise = (Random.nextFloat() - 0.5f) * 0.25f
            sin(w1) + harmonicGain * sin(w2) + noise
        }

        FloatArray(numBins) { k ->
            var real = 0.0
            var imag = 0.0
            for (i in 0 until n) {
                val angle = 2.0 * PI * k * i / n
                real += signal[i] * cos(angle)
                imag -= signal[i] * sin(angle)
            }
            (hypot(real, imag).toFloat() / (n / 2f)).coerceIn(0.02f, 1.0f)
        }
    }

    LaunchedEffect(isRunningFft) {
        while (isRunningFft) {
            delay(160L)
            tick++
        }
    }

    val peakBinIndex = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: 0
    val detectedPeakHz = peakBinIndex * (sampleRateHz / 2f / numBins)
    val thdPercent = (harmonicGain * 100f).coerceIn(1f, 99f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SiliconDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(255.dp)
                .border(2.dp, PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barWidth = (w / numBins) * 0.72f
                    val gap = (w / numBins) * 0.28f

                    // Garis Grid Horizontal Desibel (dBV)
                    for (r in 1..4) {
                        val gy = h * (r / 5f)
                        drawLine(TerminalBorder, Offset(0f, gy), Offset(w, gy), 1.2f)
                    }

                    // Gambar 32 Batang Frekuensi FFT
                    magnitudes.forEachIndexed { idx, mag ->
                        val barH = (mag * (h - 24f)).coerceAtLeast(6f)
                        val bx = idx * (barWidth + gap)
                        val by = h - barH
                        val barColor = when {
                            mag > 0.75f -> CopperOrange
                            mag > 0.4f -> CopperGold
                            else -> LogicNeonGreen
                        }
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(bx, by),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                Text(
                    text = "FFT 32-BIN SPECTRUM (0 Hz - 128 Hz) • WINDOW: HANNING",
                    color = CopperGold,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                Text(
                    text = "PEAK: ${detectedPeakHz.toInt()} Hz",
                    color = LogicCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        // Kontrol Simulasi Frekuensi Getaran Mesin / Sensor MPU6050
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ANALISIS SPEKTRUM GETARAN MPU6050 & HARMONISA (FFT)",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlotStatBox("PUNCAK FREKUENSI", "${detectedPeakHz.toInt()} Hz", PcbEmeraldDark, Modifier.weight(1f))
                    PlotStatBox("DISTORSI (THD)", String.format("%.1f %%", thdPercent), CopperOrange, Modifier.weight(1f))
                    PlotStatBox("STATUS MESIN", if (thdPercent > 60f) "VIBRASI TINGGI" else "NORMAL", LogicCyan, Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Frekuensi Fundamental Sensor: ${fundamentalHz.toInt()} Hz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PcbEmeraldDark)
                    Text("Harmonisa ke-2: ${(harmonicGain * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CopperOrange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Slider(
                        value = fundamentalHz,
                        onValueChange = { fundamentalHz = it },
                        valueRange = 8f..60f,
                        modifier = Modifier.weight(1f)
                    )
                    Slider(
                        value = harmonicGain,
                        onValueChange = { harmonicGain = it },
                        valueRange = 0.05f..0.95f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2. KOMPONEN SCHEMATIC TO PDF BLUEPRINT & GERBER PCB EXPORTER
// =========================================================================
@Composable
fun SchematicBlueprintExporterCard() {
    val context = LocalContext.current
    var projectName by remember { mutableStateOf("Smart_Sawit_Telemetry_Node_v2") }
    var targetBoard by remember { mutableStateOf("ESP32 DevKit V1 (30-Pin)") }
    val bomList = remember { BlueprintExportEngine.getDefaultBomList() }
    var exportStatusLog by remember {
        mutableStateOf("Siap mengekspor Dokumen Engineering Blueprint (.PDF A4) & Jalur PCB Gerber (.GBR).")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, contentDescription = null, tint = CopperOrange, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SCHEMATIC TO PDF BLUEPRINT & GERBER PCB EXPORTER",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Cetak lembar kerja skema teknik A4, tabel Bill of Materials (BOM), & file Gerber RS-274X",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Nama Proyek IoT", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = targetBoard,
                    onValueChange = { targetBoard = it },
                    label = { Text("Target Board MCU", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabel Ringkas Bill of Materials (BOM)
            Card(
                colors = CardDefaults.cardColors(containerColor = EsdMatBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("📋 DAFTAR KOMPONEN (BILL OF MATERIALS - BOM):", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = PcbEmeraldDark)
                    bomList.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.designator} • ${item.partName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SiliconDark)
                            Text("Rp ${String.format("%,d", item.estPriceIdr)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CopperOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = exportStatusLog,
                    color = LogicNeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val path = BlueprintExportEngine.exportCircuitBlueprintPdf(
                            context = context,
                            projectName = projectName,
                            boardModel = targetBoard,
                            bomItems = bomList
                        )
                        exportStatusLog = "✅ PDF Blueprint Berhasil Dibuat:\nLokasi: $path"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CopperGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export PDF Blueprint", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = {
                        val gbrPreview = BlueprintExportEngine.exportGerberTopCopper(context, projectName)
                        exportStatusLog = "✅ File Gerber RS-274X (.GBR) Tersimpan!\n${gbrPreview.lines().take(5).joinToString("\n")}"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Gerber (.GBR)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}