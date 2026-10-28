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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.sin

private val SharedPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 10.5f
    typeface = Typeface.MONOSPACE
    isFakeBoldText = true
}

private val SharedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 14f
    isFakeBoldText = true
}

private val SharedWirePath = Path()

object WokwiSimRenderEngine {

    fun autoArrangeNeatGrid(components: List<SimComponent>, canvasWidth: Float, canvasHeight: Float) {
        val mcu = components.find { it.type == SimCompType.ESP32_BOARD }
        mcu?.let {
            it.x = 14f
            it.y = 24f
        }

        val others = components.filter { it.type != SimCompType.ESP32_BOARD }
        val startX = 252f
        val colSpacing = 175f
        val rowSpacing = 145f

        others.forEachIndexed { index, comp ->
            val col = index % 3
            val row = index / 3
            comp.x = (startX + col * colSpacing).coerceAtMost((canvasWidth - comp.type.hitWidth - 8f).coerceAtLeast(startX))
            comp.y = (20f + row * rowSpacing).coerceAtMost((canvasHeight - comp.type.hitHeight - 8f).coerceAtLeast(20f))
        }
    }

    // 1. KOORDINAT KAKI MCU (MENDUKUNG MIRROR KIRI <-> KANAN PADA BOARD MCU!)
    fun getMcuPinOffset(mcuComp: SimComponent, variant: McuBoardVariant, pinName: String): Offset {
        val leftList = if (mcuComp.isMirrored) variant.rightPins else variant.leftPins
        val rightList = if (mcuComp.isMirrored) variant.leftPins else variant.rightPins

        val rightIdx = rightList.indexOf(pinName)
        if (rightIdx >= 0) {
            val stepY = (variant.boardHeight - 58f) / rightList.size.coerceAtLeast(1)
            return Offset(mcuComp.x + variant.boardWidth, mcuComp.y + 40f + rightIdx * stepY)
        }
        val leftIdx = leftList.indexOf(pinName).coerceAtLeast(0)
        val stepY = (variant.boardHeight - 58f) / leftList.size.coerceAtLeast(1)
        return Offset(mcuComp.x, mcuComp.y + 40f + leftIdx * stepY)
    }

    // 2. KOORDINAT KAKI KOMPONEN (MENDUKUNG MIRROR URUTAN PIN & ROTASI!)
    fun getOrderedPins(comp: SimComponent): List<String> {
        return if (comp.isMirrored) comp.type.pinNames.reversed() else comp.type.pinNames
    }

    fun getComponentPinOffset(comp: SimComponent, pinName: String): Offset {
        val pins = getOrderedPins(comp)
        val idx = pins.indexOf(pinName).coerceAtLeast(0)
        return when (comp.type) {
            SimCompType.RESISTOR_4BAND -> if (idx == 0) Offset(comp.x, comp.y + 25f) else Offset(comp.x + 130f, comp.y + 25f)
            SimCompType.OLED_SSD1306 -> Offset(comp.x + 34f + idx * 26f, comp.y + 6f)
            SimCompType.LCD_16X2_I2C -> Offset(comp.x + 32f + idx * 28f, comp.y + 6f)
            SimCompType.SOIL_MOISTURE -> Offset(comp.x + 20f + idx * 26f, comp.y + 6f)
            SimCompType.LED_LAMP -> Offset(comp.x + 22f + idx * 26f, comp.y + 92f)
            SimCompType.DHT22_SENSOR -> Offset(comp.x + 16f + idx * 24f, comp.y + 130f)
            SimCompType.SERVO_SG90 -> Offset(comp.x + 22f + idx * 28f, comp.y + 96f)
            SimCompType.HCSR04_SONAR -> Offset(comp.x + 26f + idx * 28f, comp.y + 98f)
            SimCompType.PIR_MOTION -> Offset(comp.x + 24f + idx * 26f, comp.y + 98f)
            SimCompType.LDR_LIGHT -> Offset(comp.x + 20f + idx * 26f, comp.y + 92f)
            SimCompType.NEOPIXEL_RING -> Offset(comp.x + 28f + idx * 26f, comp.y + 106f)
            SimCompType.STEPPER_MOTOR -> Offset(comp.x + 20f + idx * 24f, comp.y + 104f)
            SimCompType.RELAY_1CH -> Offset(comp.x + 22f + idx * 28f, comp.y + 100f)
            SimCompType.BUZZER_ACTIVE -> Offset(comp.x + 24f + idx * 30f, comp.y + 88f)
            SimCompType.POTENTIOMETER -> Offset(comp.x + 20f + idx * 26f, comp.y + 92f)
            SimCompType.PUSH_BUTTON -> Offset(comp.x + 22f + idx * 34f, comp.y + 84f)
            else -> Offset(comp.x, comp.y)
        }
    }

    fun findTappedPinTarget(
        tapOffset: Offset,
        components: List<SimComponent>,
        boardVariant: McuBoardVariant,
        hitRadius: Float = 24f
    ): PinHitTarget? {
        val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }
        if (mcuComp != null) {
            for (pName in boardVariant.allConnectablePins) {
                val pt = getMcuPinOffset(mcuComp, boardVariant, pName)
                if (hypot(tapOffset.x - pt.x, tapOffset.y - pt.y) <= hitRadius) {
                    return PinHitTarget(true, mcuComp.id, boardVariant.displayName.substringBefore(" ("), pName, pt)
                }
            }
        }
        for (comp in components) {
            if (comp.type == SimCompType.ESP32_BOARD) continue
            for (pName in comp.type.pinNames) {
                val pt = getComponentPinOffset(comp, pName)
                if (hypot(tapOffset.x - pt.x, tapOffset.y - pt.y) <= hitRadius) {
                    return PinHitTarget(false, comp.id, comp.label, pName, pt)
                }
            }
        }
        return null
    }

    // 3. TITIK BELOK KABEL SIKU-SIKU (ORTHOGONAL 90 DERAJAT)
    fun getWireControlPoints(
        wire: classPinWireConnection,
        start: Offset,
        end: Offset,
        channelIndex: Int
    ): Pair<Offset, Offset> {
        val laneOffset = (channelIndex % 8) * 6.5f
        val def1X = 216f + laneOffset
        val def1Y = start.y + 14f + (channelIndex % 4) * 5f
        val def2X = 216f + laneOffset
        val def2Y = end.y

        val p1 = Offset(
            if (wire.bendX.isNaN()) def1X else wire.bendX,
            if (wire.bendY.isNaN()) def1Y else wire.bendY
        )
        val p2 = Offset(
            if (wire.bend2X.isNaN()) def2X else wire.bend2X,
            if (wire.bend2Y.isNaN()) def2Y else wire.bend2Y
        )
        return Pair(p1, p2)
    }

    // 4. MENGGAMBAR KABEL SIKU-SIKU (ORTHOGONAL 90°) SEBAGAI DEFAULT UTAMA!
    fun DrawScope.drawFlexibleWire(
        wire: classPinWireConnection,
        start: Offset,
        end: Offset,
        channelIndex: Int,
        showWaypointHandles: Boolean,
        isSelectedWire: Boolean
    ) {
        val (cp1, cp2) = getWireControlPoints(wire, start, end, channelIndex)

        SharedWirePath.reset()
        SharedWirePath.moveTo(start.x, start.y)

        if (!wire.isCurvedStyle) {
            // JALUR SIKU-SIKU 90 DERAJAT PRESISI (ORTHOGONAL PCB ROUTING)
            SharedWirePath.lineTo(start.x, cp1.y)
            SharedWirePath.lineTo(cp1.x, cp1.y)
            SharedWirePath.lineTo(cp1.x, cp2.y)
            SharedWirePath.lineTo(end.x, cp2.y)
            SharedWirePath.lineTo(end.x, end.y)
        } else {
            SharedWirePath.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
        }

        drawPath(
            path = SharedWirePath,
            color = Color.Black.copy(alpha = 0.45f),
            style = Stroke(width = if (isSelectedWire) 7.5f else 5.2f, cap = StrokeCap.Square)
        )
        drawPath(
            path = SharedWirePath,
            color = wire.wireColor,
            style = Stroke(width = if (isSelectedWire) 5.5f else 3.6f, cap = StrokeCap.Square)
        )

        // Titik Solder / Soket Pin di Kedua Ujung Kabel
        drawCircle(color = Color(0xFF263238), radius = 5.2f, center = start)
        drawCircle(color = CopperGold, radius = 3.2f, center = start)
        drawCircle(color = Color(0xFF263238), radius = 5.2f, center = end)
        drawCircle(color = CopperGold, radius = 3.2f, center = end)

        if (showWaypointHandles) {
            drawCircle(color = Color.White, radius = 9f, center = cp1)
            drawCircle(color = wire.wireColor, radius = 6f, center = cp1)
            drawCircle(color = Color.White, radius = 9f, center = cp2)
            drawCircle(color = wire.wireColor, radius = 6f, center = cp2)
        }
    }

    private fun DrawScope.drawPinTerminalDot(
        pt: Offset,
        pinName: String,
        activeSelectedPin: PinHitTarget?,
        compId: String
    ) {
        val isHighlighted = activeSelectedPin != null &&
                activeSelectedPin.compId == compId &&
                activeSelectedPin.pinName == pinName

        val dotColor = when {
            isHighlighted -> Color(0xFF00E676)
            pinName in listOf("VCC", "VCC+", "5V", "3V3", "VIN", "VBUS", "A+") -> Color(0xFFFF1744)
            pinName in listOf("GND", "GND-", "K-") -> Color(0xFF90A4AE)
            else -> CopperGold
        }

        if (isHighlighted) {
            drawCircle(color = Color(0xFF00E676).copy(alpha = 0.45f), radius = 13f, center = pt)
        }
        drawCircle(color = Color(0xFF1B2622), radius = 6f, center = pt)
        drawCircle(color = dotColor, radius = 4.2f, center = pt)
    }

    fun DrawScope.drawMcuVariantBoard(
        comp: SimComponent,
        variant: McuBoardVariant,
        isRunning: Boolean,
        isBlynkConnected: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = variant.boardWidth
        val h = variant.boardHeight
        val leftList = if (comp.isMirrored) variant.rightPins else variant.leftPins
        val rightList = if (comp.isMirrored) variant.leftPins else variant.rightPins

        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            drawRoundRect(color = variant.pcbColor, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f))
            drawRoundRect(color = CopperGold, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f), style = Stroke(width = 2.8f))

            val shieldW = w * 0.44f
            val shieldH = 66f
            val isSilverShield = variant.id.startsWith("esp") || variant == McuBoardVariant.WEMOS_D1_MINI
            drawRoundRect(
                color = if (isSilverShield) Color(0xFFCFD8DC) else Color(0xFF212121),
                topLeft = Offset(x + (w - shieldW) / 2f, y + 26f),
                size = Size(shieldW, shieldH),
                cornerRadius = CornerRadius(6f, 6f)
            )

            drawContext.canvas.nativeCanvas.apply {
                SharedTitlePaint.color = if (isSilverShield) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                SharedTitlePaint.textSize = 12.5f
                drawText(variant.chipName.take(12), x + (w - shieldW) / 2f + 6f, y + 54f, SharedTitlePaint)
                SharedTitlePaint.textSize = 10f
                drawText(if (comp.isMirrored) "MIRRORED PIN" else "${variant.totalPhysicalPins}-PIN MCU", x + (w - shieldW) / 2f + 8f, y + 74f, SharedTitlePaint)
            }

            val leftCount = leftList.size.coerceAtLeast(1)
            val leftStepY = (h - 58f) / leftCount
            leftList.forEachIndexed { idx, pName ->
                val py = y + 40f + idx * leftStepY
                drawLine(CopperGold, Offset(x - 5f, py), Offset(x + 9f, py), strokeWidth = 5f)
                drawPinTerminalDot(Offset(x, py), pName, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = getPinTextColorInt(pName)
                    drawText(pName, x + 13f, py + 4f, SharedPinPaint)
                }
            }

            val rightCount = rightList.size.coerceAtLeast(1)
            val rightStepY = (h - 58f) / rightCount
            rightList.forEachIndexed { idx, pName ->
                val py = y + 40f + idx * rightStepY
                drawLine(CopperGold, Offset(x + w - 9f, py), Offset(x + w + 5f, py), strokeWidth = 5f)
                drawPinTerminalDot(Offset(x + w, py), pName, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = getPinTextColorInt(pName)
                    val textWidthOffset = pName.length * 6.8f
                    drawText(pName, x + w - 14f - textWidthOffset, py + 4f, SharedPinPaint)
                }
            }

            drawCircle(color = Color(0xFFFF1744), radius = 5f, center = Offset(x + 48f, y + h - 14f))
            drawCircle(color = if (isRunning) Color(0xFF00E5FF) else Color(0xFF37474F), radius = 5.5f, center = Offset(x + w / 2f, y + h - 14f))
            drawCircle(color = if (isBlynkConnected) Color(0xFF00E676) else Color(0xFF37474F), radius = 5.5f, center = Offset(x + w - 48f, y + h - 14f))
        }
    }

    private fun getPinTextColorInt(pinName: String): Int {
        return when (pinName) {
            "3V3", "5V", "VIN", "VBUS", "VSYS", "VU" -> android.graphics.Color.rgb(255, 82, 82)
            "GND" -> android.graphics.Color.LTGRAY
            else -> android.graphics.Color.rgb(255, 215, 64)
        }
    }

    fun DrawScope.drawResistor4BandWithPins(comp: SimComponent, ohms: Int, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + 65f, y + 25f)) {
            drawLine(Color(0xFFB0BEC5), Offset(x, y + 25f), Offset(x + 30f, y + 25f), strokeWidth = 6f)
            drawLine(Color(0xFFB0BEC5), Offset(x + 100f, y + 25f), Offset(x + 130f, y + 25f), strokeWidth = 6f)

            drawRoundRect(color = Color(0xFFE6CCB2), topLeft = Offset(x + 25f, y + 8f), size = Size(80f, 34f), cornerRadius = CornerRadius(14f, 14f))
            val bands = ResistorCatalog.getFourBandColors(ohms)
            val bandXPositions = listOf(x + 37f, x + 52f, x + 67f, x + 86f)
            bands.forEachIndexed { idx, bandColor ->
                drawRect(color = bandColor, topLeft = Offset(bandXPositions[idx], y + 9f), size = Size(8.5f, 32f))
            }

            drawPinTerminalDot(Offset(x, y + 25f), pins[0], activePin, comp.id)
            drawPinTerminalDot(Offset(x + 130f, y + 25f), pins[1], activePin, comp.id)

            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(pins[0], x - 4f, y + 14f, SharedPinPaint)
                drawText(pins[1], x + 118f, y + 14f, SharedPinPaint)
                SharedPinPaint.color = android.graphics.Color.YELLOW
                drawText(ResistorCatalog.formatOhmLabel(ohms), x + 36f, y + 55f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawLedWithPins(comp: SimComponent, isOn: Boolean, resistorOhms: Int, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + 35f, y + 45f)) {
            drawLine(Color(0xFFFF5252), Offset(x + 22f, y + 52f), Offset(x + 22f, y + 92f), strokeWidth = 4.5f)
            drawLine(Color(0xFFB0BEC5), Offset(x + 48f, y + 52f), Offset(x + 48f, y + 92f), strokeWidth = 4.5f)

            val intensity = (1f - (log10(resistorOhms.coerceAtLeast(10).toDouble()).toFloat() / 7f)).coerceIn(0.35f, 1f)
            val bulbColor = if (isOn) Color(0xFFFF1744).copy(alpha = intensity) else Color(0xFF4A151B)

            if (isOn) {
                drawCircle(color = Color(0xFFFF5252).copy(alpha = 0.42f * intensity), radius = 46f, center = Offset(x + 35f, y + 28f))
            }
            drawCircle(color = bulbColor, radius = 21f, center = Offset(x + 35f, y + 26f))
            drawRoundRect(color = bulbColor, topLeft = Offset(x + 14f, y + 26f), size = Size(42f, 26f), cornerRadius = CornerRadius(4f, 4f))

            drawPinTerminalDot(Offset(x + 22f, y + 92f), pins[0], activePin, comp.id)
            drawPinTerminalDot(Offset(x + 48f, y + 92f), pins[1], activePin, comp.id)

            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = android.graphics.Color.WHITE
                drawText(pins[0], x + 4f, y + 96f, SharedPinPaint)
                drawText(pins[1], x + 56f, y + 96f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawDht22With4Pins(
        comp: SimComponent,
        tempC: Float,
        humPct: Float,
        isRunning: Boolean,
        animPhase: Float,
        isPowered: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = 104f
        val h = 100f
        val pinLabels = getOrderedPins(comp)

        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pinLabels.forEachIndexed { i, pName ->
                val px = x + 16f + i * 24f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 130f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 130f), pName, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(pName.take(3), px - 10f, y + 144f, SharedPinPaint)
                }
            }

            drawRoundRect(color = Color(0xFF0288D1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))
            for (row in 1..3) {
                drawLine(Color(0xFF01579B), Offset(x + 12f, y + row * 16f), Offset(x + w - 26f, y + row * 16f), strokeWidth = 4f)
            }

            val barH = 54f
            val fillRatio = ((tempC - 10f) / 50f).coerceIn(0.1f, 1f)
            drawRoundRect(color = Color(0xFF013A63), topLeft = Offset(x + w - 18f, y + 12f), size = Size(10f, barH), cornerRadius = CornerRadius(5f, 5f))
            drawRoundRect(
                color = if (tempC > 36f) Color(0xFFFF1744) else Color(0xFFFFB703),
                topLeft = Offset(x + w - 18f, y + 12f + barH * (1f - fillRatio)),
                size = Size(10f, barH * fillRatio),
                cornerRadius = CornerRadius(5f, 5f)
            )

            if (isRunning && isPowered) {
                val liftY = (animPhase * 28f)
                drawCircle(color = Color(0xFFFF5252).copy(alpha = 1f - animPhase), radius = 6f, center = Offset(x + 26f, y - 4f - liftY))
                drawCircle(color = Color(0xFF00E5FF).copy(alpha = 1f - animPhase), radius = 5.5f, center = Offset(x + 54f, y - 8f - liftY * 0.8f))
            }

            drawContext.canvas.nativeCanvas.apply {
                SharedTitlePaint.color = android.graphics.Color.WHITE
                SharedTitlePaint.textSize = 13f
                drawText(comp.label.take(10), x + 12f, y + h - 24f, SharedTitlePaint)
                SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
                drawText(if (isPowered) "${String.format("%.1f", tempC)}°C ${humPct.toInt()}%" else "NO VCC/GND", x + 8f, y + h - 6f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawOledWith4Pins(comp: SimComponent, isActive: Boolean, line1: String, line2: String, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val w = 148f
        val h = 102f
        val oledPins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            drawRoundRect(color = Color(0xFF0D47A1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))
            oledPins.forEachIndexed { i, label ->
                val px = x + 34f + i * 26f
                drawPinTerminalDot(Offset(px, y + 6f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y - 5f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF050807), topLeft = Offset(x + 10f, y + 20f), size = Size(w - 20f, h - 28f), cornerRadius = CornerRadius(4f, 4f))
            drawContext.canvas.nativeCanvas.apply {
                if (isActive) {
                    SharedPinPaint.color = android.graphics.Color.YELLOW
                    drawText(line1, x + 16f, y + 44f, SharedPinPaint)
                    SharedPinPaint.color = android.graphics.Color.CYAN
                    drawText(line2, x + 16f, y + 70f, SharedPinPaint)
                } else {
                    SharedPinPaint.color = if (isPowered) android.graphics.Color.GRAY else android.graphics.Color.RED
                    drawText(if (isPowered) "OLED STANDBY" else "UNWIRED VCC/I2C", x + 16f, y + 56f, SharedPinPaint)
                }
            }
        }
    }

    fun DrawScope.drawLcd16x2With4Pins(comp: SimComponent, isActive: Boolean, line1: String, line2: String, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val w = 160f
        val h = 94f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            drawRoundRect(color = Color(0xFF1B5E20), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
            pins.forEachIndexed { i, label ->
                val px = x + 32f + i * 28f
                drawPinTerminalDot(Offset(px, y + 6f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y - 5f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF111714), topLeft = Offset(x + 8f, y + 18f), size = Size(w - 16f, h - 26f), cornerRadius = CornerRadius(4f, 4f))
            drawRoundRect(
                color = if (isActive) Color(0xFF9CCC65) else Color(0xFF33691E),
                topLeft = Offset(x + 14f, y + 24f),
                size = Size(w - 28f, h - 38f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawContext.canvas.nativeCanvas.apply {
                if (isActive) {
                    SharedPinPaint.color = android.graphics.Color.BLACK
                    drawText(line1.take(16), x + 20f, y + 42f, SharedPinPaint)
                    drawText(line2.take(16), x + 20f, y + 62f, SharedPinPaint)
                } else {
                    SharedPinPaint.color = if (isPowered) android.graphics.Color.DKGRAY else android.graphics.Color.RED
                    drawText(if (isPowered) "LCD 16x2 READY" else "UNWIRED I2C", x + 22f, y + 52f, SharedPinPaint)
                }
            }
        }
    }

    fun DrawScope.drawPirSensorWith3Pins(
        comp: SimComponent,
        isRunning: Boolean,
        animPhase: Float,
        isPowered: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = 100f
        val h = 72f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 24f + i * 26f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 98f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 98f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y + 111f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF2E7D32), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))

            if (isRunning && isPowered) {
                val waveR = 26f + animPhase * 28f
                drawCircle(
                    color = Color(0xFFFF5252).copy(alpha = 1f - animPhase),
                    radius = waveR,
                    center = Offset(x + w / 2f, y + h / 2f),
                    style = Stroke(width = 3f)
                )
                val personX = x + 15f + (sin(animPhase * 2 * PI).toFloat() * 0.5f + 0.5f) * 70f
                drawWalkingPersonIcon(personX, y - 18f, animPhase, Color(0xFF00E676))
            }

            drawCircle(color = Color(0xFFECEFF1), radius = 24f, center = Offset(x + w / 2f, y + h / 2f))
            drawCircle(color = Color(0xFFB0BEC5), radius = 14f, center = Offset(x + w / 2f, y + h / 2f), style = Stroke(width = 2f))
            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = if (!isPowered) android.graphics.Color.RED else if (isRunning) android.graphics.Color.RED else android.graphics.Color.BLACK
                drawText(if (!isPowered) "OFF" else if (isRunning) "GERAK!" else "PIR", x + w / 2f - 18f, y + h / 2f + 4f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawHcsr04With4Pins(
        comp: SimComponent,
        distCm: Float,
        isRunning: Boolean,
        animPhase: Float,
        isPowered: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = 136f
        val h = 68f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 26f + i * 28f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 98f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 98f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label.take(3), px - 10f, y + 111f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF0277BD), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
            drawCircle(color = Color(0xFFCFD8DC), radius = 21f, center = Offset(x + 34f, y + 34f))
            drawCircle(color = Color(0xFF37474F), radius = 14f, center = Offset(x + 34f, y + 34f))
            drawCircle(color = Color(0xFFCFD8DC), radius = 21f, center = Offset(x + w - 34f, y + 34f))
            drawCircle(color = Color(0xFF37474F), radius = 14f, center = Offset(x + w - 34f, y + 34f))

            if (isRunning && isPowered) {
                for (wIdx in 1..2) {
                    val arcOffset = ((animPhase + wIdx * 0.4f) % 1f) * 32f
                    drawArc(
                        color = LogicCyan.copy(alpha = 0.85f),
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(x + 42f, y - 12f - arcOffset),
                        size = Size(52f, 24f),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
                val personX = x + 30f + ((distCm / 200f).coerceIn(0f, 1f)) * 75f
                drawWalkingPersonIcon(personX, y - 22f, animPhase, LogicCyan)
            }

            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
                drawText(if (isPowered) "${distCm.toInt()} cm" else "UNWIRED", x + 46f, y + 16f, SharedPinPaint)
            }
        }
    }

    private fun DrawScope.drawWalkingPersonIcon(px: Float, py: Float, phase: Float, color: Color) {
        val stride = sin(phase * 2 * PI).toFloat() * 6f
        drawCircle(color = color, radius = 4.5f, center = Offset(px, py))
        drawLine(color = color, start = Offset(px, py + 4f), end = Offset(px, py + 14f), strokeWidth = 3f)
        drawLine(color = color, start = Offset(px - stride, py + 10f), end = Offset(px + stride, py + 10f), strokeWidth = 2.5f)
        drawLine(color = color, start = Offset(px, py + 14f), end = Offset(px - stride, py + 22f), strokeWidth = 2.8f)
        drawLine(color = color, start = Offset(px, py + 14f), end = Offset(px + stride, py + 22f), strokeWidth = 2.8f)
    }

    fun DrawScope.drawSoilSensorWith3Pins(
        comp: SimComponent,
        soilPct: Float,
        isRunning: Boolean,
        animPhase: Float,
        isPowered: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = 96f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + 55f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 20f + i * 26f
                drawPinTerminalDot(Offset(px, y + 6f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y - 5f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFFE65100), topLeft = Offset(x + 8f, y + 16f), size = Size(w - 16f, 36f), cornerRadius = CornerRadius(6f, 6f))
            drawRoundRect(color = CopperGold, topLeft = Offset(x + 18f, y + 50f), size = Size(18f, 54f), cornerRadius = CornerRadius(4f, 4f))
            drawRoundRect(color = CopperGold, topLeft = Offset(x + w - 36f, y + 50f), size = Size(18f, 54f), cornerRadius = CornerRadius(4f, 4f))

            if (isRunning && isPowered) {
                val dropY = y + 54f + animPhase * 42f
                drawCircle(color = Color(0xFF00E5FF), radius = 4.5f, center = Offset(x + w / 2f, dropY))
            }

            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = if (isPowered) android.graphics.Color.WHITE else android.graphics.Color.YELLOW
                drawText(if (isPowered) "SOIL:${soilPct.toInt()}%" else "UNWIRED", x + 16f, y + 38f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawLdrSensorWith3Pins(
        comp: SimComponent,
        luxVal: Float,
        isRunning: Boolean,
        animPhase: Float,
        isPowered: Boolean,
        activePin: PinHitTarget?
    ) {
        val x = comp.x
        val y = comp.y
        val w = 96f
        val h = 68f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 20f + i * 26f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 92f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 92f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label.take(3), px - 10f, y + 105f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF0D47A1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
            drawCircle(color = Color(0xFFFFCC80), radius = 18f, center = Offset(x + 30f, y + 34f))
            drawCircle(color = Color(0xFFD84315), radius = 14f, center = Offset(x + 30f, y + 34f), style = Stroke(width = 3f))

            if (isRunning && isPowered) {
                drawCircle(color = Color(0xFFFFEA00), radius = 8f + animPhase * 3f, center = Offset(x + w - 20f, y + 18f))
            }

            drawContext.canvas.nativeCanvas.apply {
                SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
                drawText(if (isPowered) "${luxVal.toInt()}Lx" else "OFF", x + 52f, y + 44f, SharedPinPaint)
            }
        }
    }

    fun DrawScope.drawNeoPixelRingWith3Pins(comp: SimComponent, isActive: Boolean, tick: Int, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val cx = x + 54f
        val cy = y + 48f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(cx, cy)) {
            pins.forEachIndexed { i, label ->
                val px = x + 28f + i * 26f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + 86f), Offset(px, y + 106f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 106f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y + 119f, SharedPinPaint)
                }
            }
            drawCircle(color = Color(0xFF1A1A1A), radius = 42f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF070C0A), radius = 20f, center = Offset(cx, cy))

            val rainbow = listOf(
                Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676),
                Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF4081)
            )
            for (i in 0 until 8) {
                val angleRad = (i * 45.0) * PI / 180.0
                val lx = cx + (31f * cos(angleRad)).toFloat()
                val ly = cy + (31f * sin(angleRad)).toFloat()
                val ledCol = if (isActive && isPowered) rainbow[(i + tick) % 8] else Color(0xFF424242)
                drawCircle(color = ledCol, radius = 6.5f, center = Offset(lx, ly))
            }
        }
    }

    fun DrawScope.drawStepperWith4Pins(comp: SimComponent, angleDeg: Float, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val cx = x + 55f
        val cy = y + 46f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(cx, cy)) {
            pins.forEachIndexed { i, label ->
                val px = x + 20f + i * 24f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + 84f), Offset(px, y + 104f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 104f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label.take(3), px - 10f, y + 117f, SharedPinPaint)
                }
            }
            drawCircle(color = Color(0xFF90A4AE), radius = 40f, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF37474F), radius = 34f, center = Offset(cx, cy))
            rotate(degrees = angleDeg, pivot = Offset(cx, cy)) {
                drawRoundRect(color = CopperGold, topLeft = Offset(cx - 6f, cy - 28f), size = Size(12f, 34f), cornerRadius = CornerRadius(4f, 4f))
            }
        }
    }

    fun DrawScope.drawPushButtonWith2Pins(comp: SimComponent, isPressed: Boolean, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val w = 76f
        val h = 62f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 22f + i * 34f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 84f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 84f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label.take(3), px - 10f, y + 97f, SharedPinPaint)
                }
            }
            drawRoundRect(color = Color(0xFF37474F), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))
            drawCircle(
                color = if (isPressed) Color(0xFF00E676) else Color(0xFFD32F2F),
                radius = 19f,
                center = Offset(x + w / 2f, y + h / 2f)
            )
        }
    }

    fun DrawScope.drawServoWith3Pins(comp: SimComponent, angleDeg: Float, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val w = 100f
        val h = 68f
        val servoPins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            servoPins.forEachIndexed { i, label ->
                val px = x + 22f + i * 28f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 96f), strokeWidth = 5f)
                drawPinTerminalDot(Offset(px, y + 96f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 11f, y + 109f, SharedPinPaint)
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
    }

    fun DrawScope.drawRelayWith3Pins(comp: SimComponent, isRelayOn: Boolean, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val w = 96f
        val h = 72f
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + w / 2f, y + h / 2f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 22f + i * 28f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 100f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 100f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y + 113f, SharedPinPaint)
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
    }

    fun DrawScope.drawBuzzerWith2Pins(comp: SimComponent, isBeeping: Boolean, animPhase: Float, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + 39f, y + 40f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 24f + i * 30f
                drawLine(if (i == 0) Color(0xFFFF5252) else Color(0xFFB0BEC5), Offset(px, y + 55f), Offset(px, y + 88f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 88f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 12f, y + 101f, SharedPinPaint)
                }
            }
            if (isBeeping) {
                drawCircle(
                    color = CopperGold.copy(alpha = 1f - animPhase),
                    radius = 28f + animPhase * 22f,
                    center = Offset(x + 39f, y + 32f),
                    style = Stroke(width = 3f)
                )
            }
            drawCircle(color = Color(0xFF212121), radius = 27f, center = Offset(x + 39f, y + 32f))
            drawCircle(color = Color(0xFF757575), radius = 9f, center = Offset(x + 39f, y + 32f))
        }
    }

    fun DrawScope.drawPotentiometerWith3Pins(comp: SimComponent, pct: Float, isPowered: Boolean, activePin: PinHitTarget?) {
        val x = comp.x
        val y = comp.y
        val pins = getOrderedPins(comp)
        rotate(degrees = comp.rotationDeg.toFloat(), pivot = Offset(x + 46f, y + 45f)) {
            pins.forEachIndexed { i, label ->
                val px = x + 20f + i * 26f
                drawLine(Color(0xFFB0BEC5), Offset(px, y + 58f), Offset(px, y + 92f), strokeWidth = 4f)
                drawPinTerminalDot(Offset(px, y + 92f), label, activePin, comp.id)
                drawContext.canvas.nativeCanvas.apply {
                    SharedPinPaint.color = android.graphics.Color.WHITE
                    drawText(label, px - 10f, y + 105f, SharedPinPaint)
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
}