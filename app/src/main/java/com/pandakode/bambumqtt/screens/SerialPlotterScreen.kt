package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SerialPlotterScreen() {
    var isRunning by remember { mutableStateOf(true) }
    var phase by remember { mutableFloatStateOf(0f) }

    // Buffer 40 titik sampel untuk Channel 1 (ADC Voltage) & Channel 2 (PWM Setpoint)
    var ch1Samples by remember { mutableStateOf(List(40) { 50f }) }
    var ch2Samples by remember { mutableStateOf(List(40) { 50f }) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(120L)
            phase += 0.25f
            val newCh1 = (50f + 32f * sin(phase) + (Random.nextFloat() - 0.5f) * 6f).coerceIn(5f, 95f)
            val newCh2 = (50f + 22f * cos(phase * 0.7f)).coerceIn(5f, 95f)
            ch1Samples = ch1Samples.drop(1) + newCh1
            ch2Samples = ch2Samples.drop(1) + newCh2
        }
    }

    val ch1Latest = ch1Samples.lastOrNull() ?: 0f
    val ch1Min = ch1Samples.minOrNull() ?: 0f
    val ch1Max = ch1Samples.maxOrNull() ?: 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER KONTROL OSILOSKOP SERIAL PLOTTER
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LIVE SERIAL WAVEFORM PLOTTER",
                            color = CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "CH1: Sensor ADC1 (Hijau) | CH2: PID Target (Cyan)",
                            color = PcbMintLight,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) CopperOrange else CopperGold
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SiliconDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "FREEZE" else "RUN",
                        color = SiliconDark,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. LAYAR OSILOSKOP DUAL-CHANNEL (CANVAS)
        Card(
            colors = CardDefaults.cardColors(containerColor = SiliconDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .border(2.dp, PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Gambar Grid Osiloskop
                    val cols = 8
                    val rows = 6
                    for (i in 0..cols) {
                        val x = i * (w / cols)
                        drawLine(TerminalBorder, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
                    }
                    for (j in 0..rows) {
                        val y = j * (h / rows)
                        drawLine(TerminalBorder, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                    }

                    // Fungsi gambar kurva sinyal
                    fun drawWaveform(samples: List<Float>, color: Color) {
                        if (samples.size < 2) return
                        val path = Path()
                        val stepX = w / (samples.size - 1)
                        samples.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = h - (value / 100f) * h
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = color, style = Stroke(width = 4.5f))
                    }

                    drawWaveform(ch2Samples, LogicCyan)
                    drawWaveform(ch1Samples, LogicNeonGreen)
                }

                Text(
                    text = "TIME/DIV: 120ms • SCALE: 0-100%",
                    color = CopperGold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        // 3. KARTU METRIK TELEMETRI PUNCAK (MIN / MAX / CURRENT)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ANALISIS SINYAL TELEMETRI REAL-TIME",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlotStatBox("CH1 LIVE", String.format("%.1f", ch1Latest), PcbEmeraldDark, Modifier.weight(1f))
                    PlotStatBox("CH1 MIN", String.format("%.1f", ch1Min), LogicCyan, Modifier.weight(1f))
                    PlotStatBox("CH1 MAX (PEAK)", String.format("%.1f", ch1Max), CopperOrange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PlotStatBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(EsdMatBg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, color = color, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}