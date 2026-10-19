package com.pandakode.bambumqtt

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BambuMqttTheme {
                BambuMqttSplashContent(
                    onTimeout = {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun BambuMqttSplashContent(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2400L)
        onTimeout()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mqtt_splash")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SiliconDark, PcbEmeraldDark, SiliconDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                BambuMqttChipLogoCanvas()
            }

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "BambuMQTT",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "IOT SCADA PANEL & SERIAL TERMINAL",
                color = CopperGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = PcbEmeraldMedium.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "MQTT 1883 • USB/BT Serial • Waveform Plotter • .INO Gen",
                    color = PcbMintLight,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = CopperGold,
                trackColor = PcbEmeraldMedium
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Crafted by PandaKode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Muhammad Saputra • Pekanbaru, Indonesia", color = PcbMintLight.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

@Composable
fun BambuMqttChipLogoCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val chipSize = size.minDimension * 0.56f
        val halfChip = chipSize / 2f

        // Lingkaran Luar PCB
        drawCircle(color = SiliconDark, radius = size.minDimension / 2f - 8f, center = Offset(cx, cy))
        drawCircle(
            color = CopperGold,
            radius = size.minDimension / 2f - 8f,
            center = Offset(cx, cy),
            style = Stroke(width = 5f)
        )

        // Kaki-Kaki Pin IC Mikrokontroler (Atas, Bawah, Kiri, Kanan)
        val pinOffsets = listOf(-0.28f, 0f, 0.28f)
        pinOffsets.forEach { factor ->
            val offset = chipSize * factor
            // Pin Kiri & Kanan
            drawLine(CopperGold, Offset(cx - halfChip - 22f, cy + offset), Offset(cx - halfChip, cy + offset), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(CopperGold, Offset(cx + halfChip, cy + offset), Offset(cx + halfChip + 22f, cy + offset), strokeWidth = 8f, cap = StrokeCap.Round)
            // Pin Atas & Bawah
            drawLine(CopperGold, Offset(cx + offset, cy - halfChip - 22f), Offset(cx + offset, cy - halfChip), strokeWidth = 8f, cap = StrokeCap.Round)
            drawLine(CopperGold, Offset(cx + offset, cy + halfChip), Offset(cx + offset, cy + halfChip + 22f), strokeWidth = 8f, cap = StrokeCap.Round)
        }

        // Bodi Chip IC ESP32/MCU
        drawRoundRect(
            color = PcbEmeraldMedium,
            topLeft = Offset(cx - halfChip, cy - halfChip),
            size = Size(chipSize, chipSize),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = LogicNeonGreen,
            topLeft = Offset(cx - halfChip, cy - halfChip),
            size = Size(chipSize, chipSize),
            cornerRadius = CornerRadius(18f, 18f),
            style = Stroke(width = 4f)
        )

        // Simbol Ruas Bambu di Tengah Chip IC
        val bw = 16f
        val bh = 22f
        drawRoundRect(color = CopperGold, topLeft = Offset(cx - bw / 2, cy - bh - 3f), size = Size(bw, bh), cornerRadius = CornerRadius(4f, 4f))
        drawRoundRect(color = CopperGold, topLeft = Offset(cx - bw / 2, cy + 3f), size = Size(bw, bh), cornerRadius = CornerRadius(4f, 4f))
    }
}