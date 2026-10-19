package com.pandakode.bambumqtt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet Warna: Silicon PCB Emerald, Copper Trace Gold & Logic Analyzer Cyan
val PcbEmeraldDark = Color(0xFF0A3622)       // Hijau Tua Solder Mask PCB (Header & Nav)
val PcbEmeraldMedium = Color(0xFF14532D)     // Hijau Sirkuit Medium
val PcbMintLight = Color(0xFFD1FAE5)         // Hijau Muda Indikator Aktif

val CopperGold = Color(0xFFFFB703)           // Emas Pad Tembaga PCB (Aksen Utama)
val CopperOrange = Color(0xFFFB8500)         // Oranye Tembaga Panas / PWM Highlight
val CopperSoftBg = Color(0xFFFFF8E7)         // Background Kartu Aksen

val LogicCyan = Color(0xFF00BBF9)            // Biru Sinyal Osiloskop (Channel 1 / RX)
val LogicNeonGreen = Color(0xFF00F5D4)       // Hijau Neon Terminal (Channel 2 / TX)
val LogicViolet = Color(0xFF9B5DE5)          // Ungu Protokol HEX / Modbus

val EsdMatBg = Color(0xFFF2F5F3)             // Abu-Hijau Matras Antistatik (Background App)
val SiliconDark = Color(0xFF0B1310)          // Hitam Chip IC (Layar Serial Monitor & Plotter)
val TerminalBorder = Color(0xFF1E3A2F)       // Garis Grid Osiloskop

private val BambuMqttColorScheme = lightColorScheme(
    primary = PcbEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = PcbMintLight,
    onPrimaryContainer = PcbEmeraldDark,
    secondary = CopperOrange,
    onSecondary = Color.White,
    secondaryContainer = CopperSoftBg,
    onSecondaryContainer = CopperOrange,
    tertiary = LogicCyan,
    background = EsdMatBg,
    surface = Color.White,
    onBackground = SiliconDark,
    onSurface = SiliconDark
)

@Composable
fun BambuMqttTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BambuMqttColorScheme,
        content = content
    )
}