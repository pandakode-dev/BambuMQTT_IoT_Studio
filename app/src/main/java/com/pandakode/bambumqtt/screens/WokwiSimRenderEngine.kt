package com.pandakode.bambumqtt.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.pandakode.bambumqtt.ui.theme.*
import kotlin.math.log10

private val SharedPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 11.5f
    typeface = Typeface.MONOSPACE
    isFakeBoldText = true
}

private val SharedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 16f
    isFakeBoldText = true
}

private val SharedWirePath = Path()

object WokwiSimRenderEngine {

    // 1. ALGORITMA AUTO-RAPIKAN PRESISI (ANTI MENUMPUK & ANTI TABRAKAN KABEL!)
    fun autoArrangeNeatGrid(components: List<SimComponent>, canvasWidth: Float, canvasHeight: Float) {
        val mcu = components.find { it.type == SimCompType.ESP32_BOARD }
        mcu?.let {
            it.x = 12f
            it.y = 22f
        }

        val others = components.filter { it.type != SimCompType.ESP32_BOARD }
        // Susun komponen ke dalam Grid 3 Kolom x Baris Rapi di sebelah kanan MCU
        // Beri jarak aman 245px dari kiri (agar ruang tengah dipakai khusus untuk jalur kabel Manhattan)
        val startX = 248f
        val colSpacing = 172f
        val rowSpacing = 138f

        others.forEachIndexed { index, comp ->
            val col = index % 3
            val row = index / 3
            comp.x = (startX + col * colSpacing).coerceAtMost((canvasWidth - comp.type.hitWidth - 10f).coerceAtLeast(startX))
            comp.y = (18f + row * rowSpacing).coerceAtMost((canvasHeight - comp.type.hitHeight - 10f).coerceAtLeast(18f))
        }
    }

    // 2. KOORDINAT KAKI PIN MIKROKONTROLER (MENDUKUNG KAKI KIRI & KAKI KANAN SESUAI VARIAN BOARD!)
    fun getMcuPinOffset(mcuComp: SimComponent, variant: McuBoardVariant, pinName: String): Offset {
        val rightIdx = variant.rightPins.indexOf(pinName)
        if (rightIdx >= 0) {
            val stepY = (variant.boardHeight - 58f) / variant.rightPins.size.coerceAtLeast(1)
            return Offset(mcuComp.x + variant.boardWidth - 6f, mcuComp.y + 42f + rightIdx * stepY)
        }
        val leftIdx = variant.leftPins.indexOf(pinName).coerceAtLeast(0)
        val stepY = (variant.boardHeight - 58f) / variant.leftPins.size.coerceAtLeast(1)
        return Offset(mcuComp.x + 6f, mcuComp.y + 42f + leftIdx * stepY)
    }

    // 3. KOORDINAT KAKI PIN KOMPONEN SENSOR / AKTUATOR
    fun getComponentPinOffset(comp: SimComponent, pinName: String): Offset {
        val pins = comp.type.pinNames
        val idx = pins.indexOf(pinName).coerceAtLeast(0)
        return when (comp.type) {
            SimCompType.RESISTOR_4BAND -> if (idx == 0) Offset(comp.x, comp.y + 25f) else Offset(comp.x + 130f, comp.y + 25f)
            SimCompType.LED_LAMP -> Offset(comp.x + 22f + idx * 26f, comp.y + 88f)
            SimCompType.DHT22_SENSOR -> Offset(comp.x + 16f + idx * 24f, comp.y + 128f)
            SimCompType.OLED_SSD1306 -> Offset(comp.x + 36f + idx * 26f, comp.y + 10f)
            SimCompType.SERVO_SG90 -> Offset(comp.x + 22f + idx * 28f, comp.y + 92f)
            SimCompType.HCSR04_SONAR -> Offset(comp.x + 28f + idx * 28f, comp.y + 95f)
            SimCompType.RELAY_1CH -> Offset(comp.x + 22f + idx * 28f, comp.y + 98f)
            SimCompType.BUZZER_ACTIVE -> Offset(comp.x + 24f + idx * 30f, comp.y + 84f)
            SimCompType.POTENTIOMETER -> Offset(comp.x + 20f + idx * 26f, comp.y + 88f)
            else -> Offset(comp.x, comp.y)
        }
    }

    // 4. MANHATTAN ORTHOGONAL WIRE ROUTING (JALUR KABEL SIKU-SIKU RAPI ALA WOKWI/PCB!)
    // Menghitung posisi titik belok (Elbow Handle) dari sebuah kabel
    fun getWireElbowOffset(wire: classPinWireConnection, start: Offset, end: Offset, channelIndex: Int): Offset {
        val defaultChannelX = 214f + (channelIndex % 6) * 6f
        val defaultMidY = (start.y + end.y) * 0.5f
        val bx = if (wire.bendX.isNaN()) defaultChannelX else wire.bendX
        val by = if (wire.bendY.isNaN()) defaultMidY else wire.bendY
        return Offset(bx, by)
    }

    // Menggambar kabel siku-siku yang mengikuti titik belok (Waypoint) yang diatur pengguna!
    fun DrawScope.drawOrthogonalWire(
        wire: classPinWireConnection,
        start: Offset,
        end: Offset,
        channelIndex: Int,
        showWaypointHandle: Boolean,
        isSelectedWire: Boolean
    ) {
        val elbow = getWireElbowOffset(wire, start, end, channelIndex)

        SharedWirePath.reset()
        SharedWirePath.moveTo(start.x, start.y)
        SharedWirePath.lineTo(start.x, elbow.y)
        SharedWirePath.lineTo(elbow.x, elbow.y)
        SharedWirePath.lineTo(elbow.x, end.y)
        SharedWirePath.lineTo(end.x, end.y)

        drawPath(
            path = SharedWirePath,
            color = wire.wireColor,
            style = Stroke(width = if (isSelectedWire) 6f else 3.8f, cap = StrokeCap.Round)
        )
        drawCircle(color = CopperGold, radius = 4f, center = start)
        drawCircle(color = CopperGold, radius = 4f, center = end)

        // Jika Mode Geser Jalur Kabel Aktif, tampilkan bulatan Handle Waypoint yang bisa ditarik jari!
        if (showWaypointHandle) {
            drawCircle(color = Color.White, radius = 9f, center = elbow)
            drawCircle(color = wire.wireColor, radius = 6.5f, center = elbow)
        }
    }

    // 5. MENGGAMBAR 8 VARIAN BOARD MIKROKONTROLER DENGAN KAKI KIRI & KANAN SESUAI JUMLAH ASLINYA!
    fun DrawScope.drawMcuVariantBoard(
        x: Float,
        y: Float,
        variant: McuBoardVariant,
        isRunning: Boolean,
        isBlynkConnected: Boolean
    ) {
        val w = variant.boardWidth
        val h = variant.boardHeight

        // PCB Utama sesuai warna khas board (Hitam ESP32, Biru Wemos/Nano, Teal Uno, Hijau Pico)
        drawRoundRect(color = variant.pcbColor, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f))
        drawRoundRect(color = CopperGold, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f), style = Stroke(width = 2.8f))

        // Chip / Kaleng RF di Tengah Board
        val shieldW = w * 0.44f
        val shieldH = 68f
        val isSilverShield = variant.id.startsWith("esp") || variant == McuBoardVariant.WEMOS_D1_MINI
        drawRoundRect(
            color = if (isSilverShield) Color(0xFFCFD8DC) else Color(0xFF212121),
            topLeft = Offset(x + (w - shieldW) / 2f, y + 26f),
            size = Size(shieldW, shieldH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawContext.canvas.nativeCanvas.apply {
            SharedTitlePaint.color = if (isSilverShield) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            SharedTitlePaint.textSize = 13.5f
            drawText(variant.chipName.take(12), x + (w - shieldW) / 2f + 6f, y + 56f, SharedTitlePaint)
            SharedTitlePaint.textSize = 11f
            drawText("${variant.totalPhysicalPins}-PIN MCU", x + (w - shieldW) / 2f + 10f, y + 76f, SharedTitlePaint)
        }

        // Gambar Deretan Kaki KIRI (Left Header Pins)
        val leftCount = variant.leftPins.size.coerceAtLeast(1)
        val leftStepY = (h - 58f) / leftCount
        variant.leftPins.forEachIndexed { idx, pName ->
            val py = y + 36f + idx * leftStepY
            val padColor = getPinPadColor(pName)
            drawRoundRect(color = padColor, topLeft = Offset(x + 2f, py), size = Size(11f, 9f), cornerRadius = CornerRadius(2f, 2f))
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = getPinTextColorInt(pName)
                drawText(pName, x + 16f, py + 8f, SharedPinPaint)
            }
        }

        // Gambar Deretan Kaki KANAN (Right Header Pins)
        val rightCount = variant.rightPins.size.coerceAtLeast(1)
        val rightStepY = (h - 58f) / rightCount
        variant.rightPins.forEachIndexed { idx, pName ->
            val py = y + 36f + idx * rightStepY
            val padColor = getPinPadColor(pName)
            drawRoundRect(color = padColor, topLeft = Offset(x + w - 13f, py), size = Size(11f, 9f), cornerRadius = CornerRadius(2f, 2f))
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = getPinTextColorInt(pName)
                val textWidthOffset = pName.length * 7.2f
                drawText(pName, x + w - 16f - textWidthOffset, py + 8f, SharedPinPaint)
            }
        }

        // Indikator LED Power, Status Run & Virtual Wi-Fi Blynk di Bawah Board
        drawCircle(color = Color(0xFFFF1744), radius = 5f, center = Offset(x + 48f, y + h - 14f))
        drawCircle(color = if (isRunning) Color(0xFF00E5FF) else Color(0xFF37474F), radius = 5.5f, center = Offset(x + w / 2f, y + h - 14f))
        drawCircle(color = if (isBlynkConnected) Color(0xFF00E676) else Color(0xFF37474F), radius = 5.5f, center = Offset(x + w - 48f, y + h - 14f))
    }

    private fun getPinPadColor(pinName: String): Color {
        return when (pinName) {
            "3V3", "5V", "VIN", "VBUS", "VSYS", "VU" -> Color(0xFFFF1744)
            "GND" -> Color(0xFF90A4AE)
            else -> CopperGold
        }
    }

    private fun getPinTextColorInt(pinName: String): Int {
        return when (pinName) {
            "3V3", "5V", "VIN", "VBUS", "VSYS", "VU" -> android.graphics.Color.rgb(255, 82, 82)
            "GND" -> android.graphics.Color.LTGRAY
            else -> android.graphics.Color.rgb(255, 215, 64)
        }
    }

    // 6. MENGGAMBAR RESISTOR 4-GELANG DINAMIS (SESUAI 24 NILAI E24!)
    fun DrawScope.drawResistor4BandWithPins(x: Float, y: Float, ohms: Int) {
        drawLine(Color(0xFFB0BEC5), Offset(x, y + 25f), Offset(x + 30f, y + 25f), strokeWidth = 6f)
        drawLine(Color(0xFFB0BEC5), Offset(x + 100f, y + 25f), Offset(x + 130f, y + 25f), strokeWidth = 6f)

        drawRoundRect(color = Color(0xFFE6CCB2), topLeft = Offset(x + 25f, y + 8f), size = Size(80f, 34f), cornerRadius = CornerRadius(14f, 14f))
        val bands = ResistorCatalog.getFourBandColors(ohms)
        val bandXPositions = listOf(x + 37f, x + 52f, x + 67f, x + 86f)
        bands.forEachIndexed { idx, bandColor ->
            drawRect(color = bandColor, topLeft = Offset(bandXPositions[idx], y + 9f), size = Size(8.5f, 32f))
        }

        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = android.graphics.Color.WHITE
            drawText("P1", x, y + 18f, SharedPinPaint)
            drawText("P2", x + 112f, y + 18f, SharedPinPaint)
            SharedPinPaint.color = android.graphics.Color.YELLOW
            drawText(ResistorCatalog.formatOhmLabel(ohms), x + 36f, y + 55f, SharedPinPaint)
        }
    }

    fun DrawScope.drawLedWithPins(x: Float, y: Float, isOn: Boolean, resistorOhms: Int, isPowered: Boolean) {
        drawLine(Color(0xFFFF5252), Offset(x + 22f, y + 52f), Offset(x + 22f, y + 88f), strokeWidth = 5f)
        drawLine(Color(0xFFB0BEC5), Offset(x + 48f, y + 52f), Offset(x + 48f, y + 88f), strokeWidth = 5f)

        val intensity = (1f - (log10(resistorOhms.coerceAtLeast(10).toDouble()).toFloat() / 7f)).coerceIn(0.35f, 1f)
        val bulbColor = if (isOn) Color(0xFFFF1744).copy(alpha = intensity) else Color(0xFF4A151B)

        if (isOn) {
            drawCircle(color = Color(0xFFFF5252).copy(alpha = 0.38f * intensity), radius = 42f, center = Offset(x + 35f, y + 28f))
        }
        drawCircle(color = bulbColor, radius = 21f, center = Offset(x + 35f, y + 26f))
        drawRoundRect(color = bulbColor, topLeft = Offset(x + 14f, y + 26f), size = Size(42f, 26f), cornerRadius = CornerRadius(4f, 4f))

        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = android.graphics.Color.WHITE
            drawText("A+", x + 8f, y + 102f, SharedPinPaint)
            drawText("K-", x + 44f, y + 102f, SharedPinPaint)
            if (!isPowered) {
                SharedPinPaint.color = android.graphics.Color.RED
                drawText("UNWIRED", x + 4f, y - 2f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawDht22With4Pins(x: Float, y: Float, tempC: Float, isPowered: Boolean) {
        val w = 104f
        val h = 100f
        val pinLabels = listOf("VCC", "DAT", "NC", "GND")
        pinLabels.forEachIndexed { i, label ->
            val px = x + 16f + i * 24f
            drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + h + 28f), strokeWidth = 4f)
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 10f, y + h + 40f, SharedPinPaint)
            }
        }

        drawRoundRect(color = Color(0xFF0288D1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))
        for (row in 1..3) {
            drawLine(Color(0xFF01579B), Offset(x + 14f, y + row * 16f), Offset(x + w - 14f, y + row * 16f), strokeWidth = 4f)
        }
        drawContext.canvas.nativeCanvas.apply {
            SharedTitlePaint.color = android.graphics.Color.WHITE
            SharedTitlePaint.textSize = 15f
            drawText("DHT22", x + 24f, y + h - 22f, SharedTitlePaint)
            SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
            drawText(if (isPowered) "${String.format("%.1f", tempC)}°C" else "NO VCC/GND", x + 12f, y + h - 5f, SharedPinPaint)
        }
    }

    fun DrawScope.drawOledWith4Pins(x: Float, y: Float, isActive: Boolean, line1: String, line2: String, isPowered: Boolean) {
        val w = 148f
        val h = 100f
        drawRoundRect(color = Color(0xFF0D47A1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))

        val oledPins = listOf("GND", "VCC", "SCL", "SDA")
        oledPins.forEachIndexed { i, label ->
            val px = x + 36f + i * 26f
            drawCircle(color = CopperGold, radius = 5f, center = Offset(px, y + 10f))
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 11f, y - 4f, SharedPinPaint)
            }
        }

        drawRoundRect(color = Color(0xFF050807), topLeft = Offset(x + 10f, y + 22f), size = Size(w - 20f, h - 30f), cornerRadius = CornerRadius(4f, 4f))

        drawContext.canvas.nativeCanvas.apply {
            if (isActive) {
                SharedPinPaint.color = android.graphics.Color.YELLOW
                drawText(line1, x + 16f, y + 46f, SharedPinPaint)
                SharedPinPaint.color = android.graphics.Color.CYAN
                drawText(line2, x + 16f, y + 72f, SharedPinPaint)
            } else {
                SharedPinPaint.color = if (isPowered) android.graphics.Color.GRAY else android.graphics.Color.RED
                drawText(if (isPowered) "OLED STANDBY" else "UNWIRED VCC/I2C", x + 16f, y + 58f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawServoWith3Pins(x: Float, y: Float, angleDeg: Float, isPowered: Boolean) {
        val w = 100f
        val h = 68f
        val servoPins = listOf("GND", "VCC", "SIG")
        servoPins.forEachIndexed { i, label ->
            val px = x + 22f + i * 28f
            drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 92f), strokeWidth = 5f)
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 11f, y + 106f, SharedPinPaint)
            }
        }

        drawRoundRect(color = Color(0xFF1565C0), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
        val gearCenter = Offset(x + 32f, y + 34f)
        drawCircle(color = Color.White, radius = 14f, center = gearCenter)

        rotate(degrees = angleDeg - 90f, pivot = gearCenter) {
            drawRoundRect(color = Color.White, topLeft = Offset(gearCenter.x - 6f, gearCenter.y - 42f), size = Size(12f, 50f), cornerRadius = CornerRadius(6f, 6f))
            drawCircle(color = CopperOrange, radius = 4f, center = gearCenter)
        }

        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = if (isPowered) android.graphics.Color.WHITE else android.graphics.Color.RED
            drawText(if (isPowered) "${angleDeg.toInt()}°" else "NO 5V", x + 54f, y + 40f, SharedPinPaint)
        }
    }

    fun DrawScope.drawHcsr04With4Pins(x: Float, y: Float, distCm: Float, isPowered: Boolean) {
        val w = 135f
        val h = 68f
        val pins = listOf("VCC", "TRG", "ECH", "GND")
        pins.forEachIndexed { i, label ->
            val px = x + 28f + i * 28f
            drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 95f), strokeWidth = 4f)
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 11f, y + 108f, SharedPinPaint)
            }
        }
        drawRoundRect(color = Color(0xFF0277BD), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
        drawCircle(color = Color(0xFFCFD8DC), radius = 21f, center = Offset(x + 34f, y + 34f))
        drawCircle(color = Color(0xFF37474F), radius = 14f, center = Offset(x + 34f, y + 34f))
        drawCircle(color = Color(0xFFCFD8DC), radius = 21f, center = Offset(x + w - 34f, y + 34f))
        drawCircle(color = Color(0xFF37474F), radius = 14f, center = Offset(x + w - 34f, y + 34f))

        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
            drawText(if (isPowered) "${distCm.toInt()}cm" else "UNWIRED", x + 48f, y + 16f, SharedPinPaint)
        }
    }

    fun DrawScope.drawRelayWith3Pins(x: Float, y: Float, isRelayOn: Boolean, isPowered: Boolean) {
        val w = 96f
        val h = 72f
        val pins = listOf("VCC", "GND", "IN")
        pins.forEachIndexed { i, label ->
            val px = x + 22f + i * 28f
            drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 98f), strokeWidth = 4f)
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 10f, y + 110f, SharedPinPaint)
            }
        }
        drawRoundRect(color = Color(0xFF1565C0), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
        drawCircle(color = if (isRelayOn) Color(0xFF00E676) else Color(0xFF424242), radius = 7f, center = Offset(x + w - 16f, y + 16f))
        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = android.graphics.Color.WHITE
            drawText("RELAY 5V", x + 12f, y + 40f, SharedPinPaint)
            SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
            drawText(if (!isPowered) "NO VCC" else if (isRelayOn) "CLICK: ON" else "IDLE: OFF", x + 12f, y + 58f, SharedPinPaint)
        }
    }

    fun DrawScope.drawBuzzerWith2Pins(x: Float, y: Float, isBeeping: Boolean, isPowered: Boolean) {
        drawLine(Color(0xFFFF5252), Offset(x + 24f, y + 55f), Offset(x + 24f, y + 84f), strokeWidth = 4f)
        drawLine(Color(0xFFB0BEC5), Offset(x + 54f, y + 55f), Offset(x + 54f, y + 84f), strokeWidth = 4f)
        if (isBeeping) {
            drawCircle(color = CopperGold.copy(alpha = 0.35f), radius = 40f, center = Offset(x + 39f, y + 32f))
        }
        drawCircle(color = Color(0xFF212121), radius = 27f, center = Offset(x + 39f, y + 32f))
        drawCircle(color = Color(0xFF757575), radius = 9f, center = Offset(x + 39f, y + 32f))
        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = android.graphics.Color.WHITE
            drawText("VCC+", x + 6f, y + 98f, SharedPinPaint)
            drawText("GND-", x + 44f, y + 98f, SharedPinPaint)
            if (!isPowered) {
                SharedPinPaint.color = android.graphics.Color.RED
                drawText("UNWIRED", x + 10f, y - 2f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawPotentiometerWith3Pins(x: Float, y: Float, pct: Float, isPowered: Boolean) {
        val pins = listOf("VCC", "OUT", "GND")
        pins.forEachIndexed { i, label ->
            val px = x + 20f + i * 26f
            drawLine(Color(0xFFB0BEC5), Offset(px, y + 58f), Offset(px, y + 88f), strokeWidth = 4f)
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(label, px - 10f, y + 100f, SharedPinPaint)
            }
        }
        drawCircle(color = Color(0xFF37474F), radius = 27f, center = Offset(x + 46f, y + 32f))
        drawCircle(color = CopperGold, radius = 17f, center = Offset(x + 46f, y + 32f))
        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = if (isPowered) android.graphics.Color.BLACK else android.graphics.Color.RED
            drawText(if (isPowered) "${pct.toInt()}%" else "OFF", x + 32f, y + 37f, SharedPinPaint)
        }
    }
}