package com.pandakode.bambumqtt.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow

// =========================================================================
// GLOBAL CACHED PAINTS & PATH (ZERO ALLOCATION DI DALAM DRAW LOOP = ANTI LAG!)
// =========================================================================

private val SharedPinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 13f
    typeface = Typeface.MONOSPACE
    isFakeBoldText = true
}

private val SharedTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 17f
    isFakeBoldText = true
}

private val SharedWirePath = Path()

// =========================================================================
// ENUM & DATA MODEL UNTUK DASHBOARD IOT GAYA BLYNK
// =========================================================================

enum class DashWidgetType(val label: String) {
    TOGGLE_SWITCH("Switch Relay"),
    PUSH_BUTTON("Push Button"),
    KNOB_ROTARY("Rotary Knob"),
    SLIDER_LEVEL("PWM Slider"),
    GAUGE_RADIAL("Radial Gauge"),
    VALUE_DISPLAY("Digital Value"),
    LED_INDICATOR("Status LED"),
    RGB_COLOR_PICKER("RGB Zebra Picker"),
    HISTORICAL_CHART("SuperChart Live"),
    STEP_INPUT("Step Control (+/-)"),
    LCD_16X2("Virtual LCD 16x2"),
    TERMINAL_MINI("Blynk Terminal")
}

data class DashWidget(
    val id: String,
    val type: DashWidgetType,
    val title: String,
    val topic: String,
    val pin: String,
    var value: Float = 0f,
    var isOn: Boolean = false,
    val unit: String = "",
    val color: Color = CopperGold,
    val max: Float = 100f
)

// =========================================================================
// ENUM & DATA MODEL KOMPONEN DENGAN STATE KOORDINAT LANGSUNG (ZERO-LAG!)
// =========================================================================

enum class SimCompType(val title: String, val pinNames: List<String>, val hitWidth: Float, val hitHeight: Float) {
    ESP32_BOARD("ESP32 / ESP8266 MCU", listOf("3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34"), 185f, 265f),
    LED_LAMP("LED 5mm Merah", listOf("A+", "K-"), 70f, 95f),
    RESISTOR_4BAND("Resistor 4-Gelang", listOf("P1", "P2"), 130f, 60f),
    DHT22_SENSOR("Sensor Suhu DHT22", listOf("VCC", "DATA", "NC", "GND"), 105f, 130f),
    OLED_SSD1306("OLED 0.96\" I2C", listOf("GND", "VCC", "SCL", "SDA"), 148f, 105f),
    SERVO_SG90("Motor Servo SG90", listOf("GND", "VCC", "SIG"), 105f, 100f),
    HCSR04_SONAR("Ultrasonik HC-SR04", listOf("VCC", "TRIG", "ECHO", "GND"), 135f, 100f),
    RELAY_1CH("Modul Relay 1CH", listOf("VCC", "GND", "IN"), 100f, 105f),
    BUZZER_ACTIVE("Buzzer Piezo", listOf("VCC+", "GND-"), 80f, 95f),
    POTENTIOMETER("Potensiometer Analog", listOf("VCC", "OUT", "GND"), 90f, 95f)
}

@Stable
class SimComponent(
    val id: String,
    val type: SimCompType,
    val label: String,
    initialX: Float,
    initialY: Float
) {
    // Koordinat disimpan sebagai MutableFloatState agar saat digeser TIDAK merecompose List!
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
}

data class classPinWireConnection(
    val id: String,
    val fromCompId: String,
    val fromPinName: String,
    val toMcuPin: String,
    val wireColor: Color
)

data class BuiltInLibInfo(
    val headerName: String,
    val lang: String,
    val category: String,
    val supportedHardware: String,
    val exampleApi: String
)

@Composable
fun WokwiSimScreen() {
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Kanvas Sirkuit", "Blynk IoT Dash", "Koding C++/Py", "20+ Library")

    var mcuBoardModel by remember { mutableStateOf("ESP32") }
    var isSimRunning by remember { mutableStateOf(false) }
    var simTick by remember { mutableIntStateOf(0) }

    // Parameter Interaktif Komponen
    var resistorOhms by remember { mutableIntStateOf(220) }
    var dhtTempC by remember { mutableFloatStateOf(31.4f) }
    var dhtHumPct by remember { mutableFloatStateOf(65.0f) }
    var ultrasonicCm by remember { mutableFloatStateOf(45.0f) }
    var potPercent by remember { mutableFloatStateOf(50.0f) }
    var servoAngle by remember { mutableFloatStateOf(90f) }
    var oledLine1 by remember { mutableStateOf("PANDAKODE IOT") }
    var oledLine2 by remember { mutableStateOf("T:31.4C D:45cm") }
    var ledRedActive by remember { mutableStateOf(false) }
    var relayActive by remember { mutableStateOf(false) }
    var buzzerActive by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Red) }

    var simSerialOutput by remember {
        mutableStateOf("[ZERO-LAG ENGINE] Geser komponen langsung atau Ketuk Komponen -> Ketuk Titik Tujuan!")
    }

    // 12 Widget Lengkap ala Blynk IoT
    var dashboardWidgets by remember {
        mutableStateOf(
            listOf(
                DashWidget("dw1", DashWidgetType.TOGGLE_SWITCH, "Pompa Irigasi (V1)", "bambu/relay/pump", "D2", isOn = false, color = Color(0xFF00E676)),
                DashWidget("dw2", DashWidgetType.PUSH_BUTTON, "Alarm Buzzer (V2)", "bambu/act/beep", "D5", color = Color(0xFFFF1744)),
                DashWidget("dw3", DashWidgetType.KNOB_ROTARY, "Sudut Servo (V3)", "bambu/pwm/servo", "D18", value = 90f, max = 180f, unit = "°", color = CopperOrange),
                DashWidget("dw4", DashWidgetType.SLIDER_LEVEL, "Dimmer PWM (V4)", "bambu/pwm/led", "D2", value = 200f, max = 255f, color = Color(0xFF00E5FF)),
                DashWidget("dw5", DashWidgetType.GAUGE_RADIAL, "Suhu DHT22 (V5)", "bambu/sensor/temp", "D4", value = 31.4f, max = 100f, unit = "°C", color = Color(0xFFFF5252)),
                DashWidget("dw6", DashWidgetType.VALUE_DISPLAY, "Jarak Sonar (V6)", "bambu/sensor/sonar", "D19", value = 45f, max = 200f, unit = " cm", color = Color(0xFF2979FF)),
                DashWidget("dw7", DashWidgetType.LED_INDICATOR, "Status Overheat (V7)", "bambu/status/heat", "D19", isOn = false, color = Color(0xFFFF1744)),
                DashWidget("dw8", DashWidgetType.RGB_COLOR_PICKER, "RGB NeoPixel (V8)", "bambu/color", "D5", color = Color.Red),
                DashWidget("dw9", DashWidgetType.STEP_INPUT, "Setpoint Thermostat", "bambu/setpoint", "D34", value = 36f, max = 80f, unit = "°C", color = Color(0xFFE040FB)),
                DashWidget("dw10", DashWidgetType.HISTORICAL_CHART, "SuperChart Suhu", "bambu/history/temp", "D4", value = 31.4f, max = 100f, color = Color(0xFF00E5FF)),
                DashWidget("dw11", DashWidgetType.LCD_16X2, "Blynk LCD 16x2 (V11)", "bambu/lcd", "I2C", value = 31.4f, color = Color(0xFF00E676)),
                DashWidget("dw12", DashWidgetType.TERMINAL_MINI, "Cloud Terminal (V12)", "bambu/term", "TX/RX", color = CopperGold)
            )
        )
    }

    // Daftar Komponen Menggunakan mutableStateListOf Agar Sangat Ringan!
    val components = remember {
        mutableStateListOf(
            SimComponent("mcu", SimCompType.ESP32_BOARD, "ESP32 DevKit", 14f, 40f),
            SimComponent("res1", SimCompType.RESISTOR_4BAND, "Resistor", 250f, 18f),
            SimComponent("led1", SimCompType.LED_LAMP, "LED Merah", 435f, 10f),
            SimComponent("dht1", SimCompType.DHT22_SENSOR, "DHT22", 545f, 20f),
            SimComponent("oled1", SimCompType.OLED_SSD1306, "OLED I2C", 255f, 195f),
            SimComponent("servo1", SimCompType.SERVO_SG90, "Servo SG90", 455f, 195f)
        )
    }

    // Daftar Kabel Jumper Pin-ke-Pin
    var wires by remember {
        mutableStateOf(
            listOf(
                classPinWireConnection("w1", "res1", "P1", "D2", Color(0xFFFFB703)),
                classPinWireConnection("w2", "led1", "A+", "D2", Color(0xFFFF5252)),
                classPinWireConnection("w3", "led1", "K-", "GND", Color(0xFF90A4AE)),
                classPinWireConnection("w4", "dht1", "VCC", "3V3", Color(0xFFFF1744)),
                classPinWireConnection("w5", "dht1", "DATA", "D4", Color(0xFF00E5FF)),
                classPinWireConnection("w6", "dht1", "GND", "GND", Color(0xFF90A4AE)),
                classPinWireConnection("w7", "oled1", "GND", "GND", Color(0xFF90A4AE)),
                classPinWireConnection("w8", "oled1", "VCC", "3V3", Color(0xFFFF1744)),
                classPinWireConnection("w9", "oled1", "SCL", "D22", Color(0xFF00F5D4)),
                classPinWireConnection("w10", "oled1", "SDA", "D21", Color(0xFFFFB703)),
                classPinWireConnection("w11", "servo1", "GND", "GND", Color(0xFF90A4AE)),
                classPinWireConnection("w12", "servo1", "VCC", "5V", Color(0xFFFF1744)),
                classPinWireConnection("w13", "servo1", "SIG", "D18", Color(0xFFFB8500))
            )
        )
    }

    fun isComponentPoweredAndWired(comp: SimComponent): Boolean {
        val compWires = wires.filter { it.fromCompId == comp.id }
        return when (comp.type) {
            SimCompType.ESP32_BOARD -> true
            SimCompType.RESISTOR_4BAND -> compWires.isNotEmpty()
            SimCompType.LED_LAMP -> {
                compWires.any { it.fromPinName == "A+" && it.toMcuPin.startsWith("D") } &&
                        compWires.any { it.fromPinName == "K-" && it.toMcuPin == "GND" }
            }
            SimCompType.DHT22_SENSOR -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "DATA" && it.toMcuPin.startsWith("D") }
            }
            SimCompType.OLED_SSD1306 -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "SCL" && it.toMcuPin == "D22" } &&
                        compWires.any { it.fromPinName == "SDA" && it.toMcuPin == "D21" }
            }
            SimCompType.SERVO_SG90 -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "SIG" && it.toMcuPin.startsWith("D") }
            }
            SimCompType.HCSR04_SONAR -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "TRIG" && it.toMcuPin.startsWith("D") } &&
                        compWires.any { it.fromPinName == "ECHO" && it.toMcuPin.startsWith("D") }
            }
            SimCompType.RELAY_1CH -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "IN" && it.toMcuPin.startsWith("D") }
            }
            SimCompType.BUZZER_ACTIVE -> {
                compWires.any { it.fromPinName == "VCC+" && it.toMcuPin.startsWith("D") } &&
                        compWires.any { it.fromPinName == "GND-" && it.toMcuPin == "GND" }
            }
            SimCompType.POTENTIOMETER -> {
                compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "OUT" && it.toMcuPin == "D34" }
            }
        }
    }

    var codeLang by remember { mutableStateOf("CPP") }
    var simCodeText by remember {
        mutableStateOf(
            """
            #include <Wire.h>
            #include <Adafruit_SSD1306.h>
            #include <DHT.h>
            #include <ESP32Servo.h>
            #include <NewPing.h>

            void setup() {
              Serial.begin(115200);
              pinMode(2, OUTPUT);
              display.println("PANDAKODE IOT");
              servo.write(135);
            }

            void loop() {
              digitalWrite(2, HIGH);
              Serial.println("Sensor Active & Power Verified!");
              delay(1000);
            }
            """.trimIndent()
        )
    }

    // Engine Eksekusi Sirkuit & Sinkronisasi ke Blynk Dashboard
    LaunchedEffect(isSimRunning, simCodeText, dhtTempC, ultrasonicCm, resistorOhms, wires) {
        if (!isSimRunning) return@LaunchedEffect
        while (isSimRunning) {
            simTick++

            val ledComp = components.find { it.type == SimCompType.LED_LAMP }
            val oledComp = components.find { it.type == SimCompType.OLED_SSD1306 }
            val dhtComp = components.find { it.type == SimCompType.DHT22_SENSOR }
            val servoComp = components.find { it.type == SimCompType.SERVO_SG90 }
            val relayComp = components.find { it.type == SimCompType.RELAY_1CH }
            val buzzerComp = components.find { it.type == SimCompType.BUZZER_ACTIVE }

            val isLedWired = ledComp != null && isComponentPoweredAndWired(ledComp)
            val isOledWired = oledComp != null && isComponentPoweredAndWired(oledComp)
            val isDhtWired = dhtComp != null && isComponentPoweredAndWired(dhtComp)
            val isServoWired = servoComp != null && isComponentPoweredAndWired(servoComp)

            if (isOledWired) {
                val oledMatch = Regex("""(?:display\.println|oled\.text)\(\s*"([^"]+)"""").find(simCodeText)
                if (oledMatch != null) oledLine1 = oledMatch.groupValues[1].take(16)
                oledLine2 = if (isDhtWired) {
                    String.format("T:%.1fC D:%.0fcm", dhtTempC, ultrasonicCm)
                } else {
                    "DHT22 UNWIRED!"
                }
            }

            if (isServoWired) {
                val blynkServoVal = dashboardWidgets.find { it.id == "dw3" }?.value ?: 90f
                servoAngle = if (simTick % 2 == 0) blynkServoVal else (blynkServoVal - 30f).coerceAtLeast(0f)
            }

            val codeTriggersHigh = simCodeText.contains("HIGH") || simCodeText.contains(".on()")
            val blynkSwitchOn = dashboardWidgets.find { it.id == "dw1" }?.isOn == true
            ledRedActive = isLedWired && ((codeTriggersHigh && simTick % 2 == 1) || blynkSwitchOn)
            relayActive = relayComp != null && isComponentPoweredAndWired(relayComp) && (codeTriggersHigh || blynkSwitchOn)
            buzzerActive = (buzzerComp != null && isComponentPoweredAndWired(buzzerComp) && (simTick % 2 == 1)) ||
                    (dashboardWidgets.find { it.id == "dw2" }?.isOn == true)

            dashboardWidgets = dashboardWidgets.map { widget ->
                when (widget.id) {
                    "dw5" -> widget.copy(value = dhtTempC)
                    "dw6" -> widget.copy(value = ultrasonicCm)
                    "dw10" -> widget.copy(value = dhtTempC)
                    "dw7" -> widget.copy(isOn = dhtTempC > 40f)
                    else -> widget
                }
            }

            val unwiredList = components.filter { it.type != SimCompType.ESP32_BOARD && !isComponentPoweredAndWired(it) }
            simSerialOutput = if (unwiredList.isEmpty()) {
                "[TICK #$simTick] ✅ ALL POWERED | D2=${if (ledRedActive) "HIGH" else "LOW"} | DHT=${dhtTempC}°C | Servo=${servoAngle.toInt()}°"
            } else {
                "⚠️ UNWIRED: [${unwiredList.joinToString { it.label }}] -> Klik '🔌 Kabel' lalu klik 'Auto-Wire'!"
            }

            delay(800L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = PcbEmeraldDark,
            contentColor = CopperGold
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = activeSubTab == idx,
                    onClick = { activeSubTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = if (activeSubTab == idx) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (activeSubTab) {
            0 -> WokwiZeroLagWorkbench(
                mcuBoardModel = mcuBoardModel,
                onToggleMcuModel = { mcuBoardModel = if (mcuBoardModel == "ESP32") "ESP8266" else "ESP32" },
                components = components,
                wires = wires,
                onWiresChange = { wires = it },
                checkPowered = { isComponentPoweredAndWired(it) },
                isSimRunning = isSimRunning,
                onToggleRun = {
                    isSimRunning = !isSimRunning
                    if (!isSimRunning) {
                        ledRedActive = false
                        simSerialOutput = "[STOPPED] Simulasi dihentikan."
                    }
                },
                ledRedOn = ledRedActive,
                relayActive = relayActive,
                buzzerActive = buzzerActive,
                resistorOhms = resistorOhms,
                onResistorChange = { resistorOhms = it },
                dhtTempC = dhtTempC,
                onDhtTempChange = { dhtTempC = it },
                ultrasonicCm = ultrasonicCm,
                onUltrasonicChange = { ultrasonicCm = it },
                potPercent = potPercent,
                servoAngle = servoAngle,
                oledLine1 = oledLine1,
                oledLine2 = oledLine2,
                simSerialOutput = simSerialOutput
            )

            1 -> BlynkDashboardPanel(
                widgets = dashboardWidgets,
                onWidgetsChange = { dashboardWidgets = it },
                isSimRunning = isSimRunning,
                selectedColor = selectedColor,
                onColorChange = { selectedColor = it },
                dhtTempC = dhtTempC,
                ultrasonicCm = ultrasonicCm
            )

            2 -> WokwiCodeEditorPanel(
                codeLang = codeLang,
                onLangChange = { lang ->
                    codeLang = lang
                    simCodeText = if (lang == "CPP") {
                        """
                        #include <Wire.h>
                        #include <Adafruit_SSD1306.h>
                        #include <DHT.h>
                        #include <ESP32Servo.h>

                        void setup() {
                          Serial.begin(115200);
                          pinMode(2, OUTPUT);
                          display.println("BAMBUSIM C++");
                          servo.write(120);
                        }

                        void loop() {
                          digitalWrite(2, HIGH);
                          delay(1000);
                        }
                        """.trimIndent()
                    } else {
                        """
                        from machine import Pin, I2C, PWM
                        import ssd1306, dht, time

                        led = Pin(2, Pin.OUT)
                        while True:
                            led.on()
                            oled.text("MICROPYTHON OK", 0, 0)
                            servo.angle(120)
                            time.sleep(1)
                        """.trimIndent()
                    }
                },
                codeText = simCodeText,
                onCodeChange = { simCodeText = it },
                onRunNow = {
                    isSimRunning = true
                    activeSubTab = 0
                }
            )

            3 -> BuiltInLibraryCatalogView()
        }
    }
}

// =========================================================================
// WORKBENCH KANVAS ZERO-LAG (DIKELUARKAN DARI VERTICAL SCROLL AGAR 100% MULUS!)
// =========================================================================

@Composable
fun WokwiZeroLagWorkbench(
    mcuBoardModel: String,
    onToggleMcuModel: () -> Unit,
    components: MutableList<SimComponent>,
    wires: List<classPinWireConnection>,
    onWiresChange: (List<classPinWireConnection>) -> Unit,
    checkPowered: (SimComponent) -> Boolean,
    isSimRunning: Boolean,
    onToggleRun: () -> Unit,
    ledRedOn: Boolean,
    relayActive: Boolean,
    buzzerActive: Boolean,
    resistorOhms: Int,
    onResistorChange: (Int) -> Unit,
    dhtTempC: Float,
    onDhtTempChange: (Float) -> Unit,
    ultrasonicCm: Float,
    onUltrasonicChange: (Float) -> Unit,
    potPercent: Float,
    servoAngle: Float,
    oledLine1: String,
    oledLine2: String,
    simSerialOutput: String
) {
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showWireManagerDialog by remember { mutableStateOf(false) }
    var selectedComp by remember { mutableStateOf<SimComponent?>(components.getOrNull(1)) }
    val presetResistorValues = listOf(100, 220, 330, 470, 1000, 4700, 10000)

    // Fungsi Auto-Layout: Merapikan semua komponen yang menumpuk dalam 1 klik!
    fun autoArrangeComponents() {
        val mcu = components.find { it.type == SimCompType.ESP32_BOARD }
        mcu?.let {
            it.x = 12f
            it.y = 35f
        }
        val others = components.filter { it.type != SimCompType.ESP32_BOARD }
        others.forEachIndexed { index, comp ->
            val col = index % 2
            val row = index / 2
            comp.x = 245f + col * 195f
            comp.y = 18f + row * 125f
        }
    }

    // PENTING: Column utama TIDAK memakai verticalScroll di sekitar Canvas agar sentuhan jari 100% mulus!
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. TOOLBAR ATAS RINGKAS
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { showAddPartDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ Komponen", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showWireManagerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = LogicCyan),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔌 Kabel (${wires.size})", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }

                        // TOMBOL BARU: RAPIKAN POSISI OTOMATIS (ANTI NUMPUK!)
                        Button(
                            onClick = { autoArrangeComponents() },
                            colors = ButtonDefaults.buttonColors(containerColor = LogicViolet),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("✨ Auto-Rapikan", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onToggleMcuModel,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(mcuBoardModel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onToggleRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimRunning) Color(0xFFD32F2F) else Color(0xFF00E676)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSimRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSimRunning) Color.White else SiliconDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isSimRunning) "STOP" else "RUN",
                            color = if (isSimRunning) Color.White else SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 2. KANVAS ZERO-LAG (MENDUKUNG DRAG LANGSUNG + TAP-TO-TELEPORT + D-PAD!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070C0A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .border(2.dp, PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        // FITUR 1: TAP KOMPONEN LALU TAP TEMPAT KOSONG UNTUK PINDAH INSTAN TANPA GESER!
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val hit = components.lastOrNull { comp ->
                                    tapOffset.x >= comp.x - 15f && tapOffset.x <= comp.x + comp.type.hitWidth + 15f &&
                                            tapOffset.y >= comp.y - 15f && tapOffset.y <= comp.y + comp.type.hitHeight + 15f
                                }
                                if (hit != null) {
                                    selectedComp = hit
                                } else if (selectedComp != null) {
                                    // Pindahkan komponen terpilih langsung ke titik yang diketuk!
                                    val target = selectedComp!!
                                    target.x = (tapOffset.x - target.type.hitWidth / 2f).coerceIn(6f, size.width - target.type.hitWidth)
                                    target.y = (tapOffset.y - target.type.hitHeight / 2f).coerceIn(6f, size.height - target.type.hitHeight)
                                }
                            }
                        }
                        // FITUR 2: DRAG & DROP ULTRA-MULUS (MUTATING FLOAT STATE LANGSUNG TANPA COPY LIST!)
                        .pointerInput(Unit) {
                            var draggedComp: SimComponent? = null
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    draggedComp = components.lastOrNull { comp ->
                                        startOffset.x >= comp.x - 20f && startOffset.x <= comp.x + comp.type.hitWidth + 20f &&
                                                startOffset.y >= comp.y - 20f && startOffset.y <= comp.y + comp.type.hitHeight + 20f
                                    }
                                    if (draggedComp != null) {
                                        selectedComp = draggedComp
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val c = draggedComp ?: return@detectDragGestures
                                    c.x = (c.x + dragAmount.x).coerceIn(6f, size.width - c.type.hitWidth)
                                    c.y = (c.y + dragAmount.y).coerceIn(6f, size.height - c.type.hitHeight)
                                },
                                onDragEnd = { draggedComp = null },
                                onDragCancel = { draggedComp = null }
                            )
                        }
                ) {
                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }

                    // 1. Gambar Semua Kabel Penghubung (Menggunakan SharedWirePath agar Zero-Allocation)
                    if (mcuComp != null) {
                        wires.forEach { wire ->
                            val sourceComp = components.find { it.id == wire.fromCompId }
                            if (sourceComp != null) {
                                val startPt = getComponentPinOffset(sourceComp, wire.fromPinName)
                                val endPt = getMcuPinOffset(mcuComp, wire.toMcuPin)
                                drawFastWire(startPt, endPt, wire.wireColor)
                            }
                        }
                    }

                    // 2. Gambar Komponen + Bingkai Sorotan Kuning jika Sedang Dipilih
                    components.forEach { comp ->
                        val isPowered = checkPowered(comp)
                        if (selectedComp?.id == comp.id) {
                            drawRoundRect(
                                color = CopperGold.copy(alpha = 0.28f),
                                topLeft = Offset(comp.x - 6f, comp.y - 6f),
                                size = Size(comp.type.hitWidth + 12f, comp.type.hitHeight + 12f),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 3f)
                            )
                        }
                        when (comp.type) {
                            SimCompType.ESP32_BOARD -> drawMcuBoardWithPinLabels(comp.x, comp.y, mcuBoardModel, isSimRunning)
                            SimCompType.RESISTOR_4BAND -> drawResistor4BandWithPins(comp.x, comp.y, resistorOhms)
                            SimCompType.LED_LAMP -> drawLedWithPins(comp.x, comp.y, ledRedOn && isPowered, resistorOhms, isPowered)
                            SimCompType.DHT22_SENSOR -> drawDht22With4Pins(comp.x, comp.y, dhtTempC, isPowered)
                            SimCompType.OLED_SSD1306 -> drawOledWith4Pins(comp.x, comp.y, isSimRunning && isPowered, oledLine1, oledLine2, isPowered)
                            SimCompType.SERVO_SG90 -> drawServoWith3Pins(comp.x, comp.y, if (isPowered) servoAngle else 0f, isPowered)
                            SimCompType.HCSR04_SONAR -> drawHcsr04With4Pins(comp.x, comp.y, ultrasonicCm, isPowered)
                            SimCompType.RELAY_1CH -> drawRelayWith3Pins(comp.x, comp.y, relayActive && isPowered, isPowered)
                            SimCompType.BUZZER_ACTIVE -> drawBuzzerWith2Pins(comp.x, comp.y, buzzerActive && isPowered, isPowered)
                            SimCompType.POTENTIOMETER -> drawPotentiometerWith3Pins(comp.x, comp.y, potPercent, isPowered)
                        }
                    }
                }

                // Overlay Petunjuk & D-Pad Geser Presisi di Pojok Kanvas
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(SiliconDark.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Terpilih: ${selectedComp?.label ?: "-"} (Drag / Tap Kanvas untuk Pindah)",
                        color = CopperGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedComp != null && selectedComp?.type != SimCompType.ESP32_BOARD) {
                        IconButton(
                            onClick = { components.remove(selectedComp); selectedComp = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 3. PANEL BAWAH (SCROLLABLE TERSENDIRI AGAR TIDAK MENGGANGGU KANVAS!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bandNames = getResistorColorBandNames(resistorOhms)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎨 Resistor: $resistorOhms Ω", fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 12.sp)
                    Text("Gelang: $bandNames", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CopperOrange, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetResistorValues.forEach { ohm ->
                        val label = if (ohm >= 1000) "${ohm / 1000.0}k Ω".replace(".0", "") else "$ohm Ω"
                        FilterChip(
                            selected = resistorOhms == ohm,
                            onClick = { onResistorChange(ohm) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PcbEmeraldDark,
                                selectedLabelColor = CopperGold
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🌡️ DHT22 (D4): ${String.format("%.1f", dhtTempC)}°C", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PcbEmeraldDark)
                    Text("📏 HC-SR04: ${ultrasonicCm.toInt()} cm", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LogicCyan)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Slider(
                        value = dhtTempC,
                        onValueChange = onDhtTempChange,
                        valueRange = 15f..60f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = CopperOrange, activeTrackColor = PcbEmeraldDark)
                    )
                    Slider(
                        value = ultrasonicCm,
                        onValueChange = onUltrasonicChange,
                        valueRange = 2f..200f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = LogicCyan, activeTrackColor = PcbEmeraldDark)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = simSerialOutput,
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    // DIALOG TAMBAH KOMPONEN BARU (OTOMATIS DITEMPATKAN DI POSISI RAPI & LANGSUNG AUTO-WIRE!)
    if (showAddPartDialog) {
        val addableTypes = listOf(
            SimCompType.HCSR04_SONAR,
            SimCompType.RELAY_1CH,
            SimCompType.BUZZER_ACTIVE,
            SimCompType.POTENTIOMETER,
            SimCompType.LED_LAMP,
            SimCompType.RESISTOR_4BAND,
            SimCompType.DHT22_SENSOR,
            SimCompType.OLED_SSD1306,
            SimCompType.SERVO_SG90
        )

        AlertDialog(
            onDismissRequest = { showAddPartDialog = false },
            title = { Text("➕ Tambah Komponen Sirkuit", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    addableTypes.forEach { type ->
                        OutlinedButton(
                            onClick = {
                                val idx = components.size
                                val newId = "${type.name.lowercase()}_$idx"
                                val newComp = SimComponent(
                                    id = newId,
                                    type = type,
                                    label = type.title.substringBefore(" "),
                                    initialX = 250f + (idx % 2) * 185f,
                                    initialY = 25f + ((idx / 2) % 2) * 145f
                                )
                                components.add(newComp)
                                selectedComp = newComp
                                showAddPartDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(type.title, fontWeight = FontWeight.Bold, color = PcbEmeraldDark, fontSize = 12.sp)
                                Text("Pin: ${type.pinNames.joinToString(",")}", fontSize = 10.sp, color = CopperOrange, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPartDialog = false }) { Text("Tutup") }
            }
        )
    }

    // DIALOG MANAJEMEN KABEL PIN-KE-PIN
    if (showWireManagerDialog) {
        val nonMcuComps = components.filter { it.type != SimCompType.ESP32_BOARD }
        var wireTargetComp by remember { mutableStateOf(nonMcuComps.firstOrNull()) }
        var selectedCompPin by remember { mutableStateOf(wireTargetComp?.type?.pinNames?.firstOrNull() ?: "VCC") }
        val mcuPins = SimCompType.ESP32_BOARD.pinNames
        var selectedMcuPin by remember { mutableStateOf("3V3") }

        AlertDialog(
            onDismissRequest = { showWireManagerDialog = false },
            title = { Text("🔌 Pasang / Lepas Kabel Pin", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val autoWires = mutableListOf<classPinWireConnection>()
                            components.forEach { c ->
                                when (c.type) {
                                    SimCompType.LED_LAMP -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "A+", "D2", Color(0xFFFF5252)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "K-", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.RESISTOR_4BAND -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "P1", "D2", CopperGold))
                                    }
                                    SimCompType.DHT22_SENSOR -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "DATA", "D4", LogicCyan))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.OLED_SSD1306 -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "SCL", "D22", LogicNeonGreen))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_4", c.id, "SDA", "D21", CopperGold))
                                    }
                                    SimCompType.SERVO_SG90 -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "SIG", "D18", CopperOrange))
                                    }
                                    SimCompType.HCSR04_SONAR -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "TRIG", "D5", LogicCyan))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "ECHO", "D19", LogicNeonGreen))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_4", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.RELAY_1CH -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "IN", "D2", CopperOrange))
                                    }
                                    SimCompType.BUZZER_ACTIVE -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "VCC+", "D2", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "GND-", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.POTENTIOMETER -> {
                                        autoWires.add(classPinWireConnection("aw_${c.id}_1", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_2", c.id, "OUT", "D34", LogicCyan))
                                        autoWires.add(classPinWireConnection("aw_${c.id}_3", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    else -> {}
                                }
                            }
                            onWiresChange(autoWires)
                            showWireManagerDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚡ Auto-Wire Semua VCC, GND & GPIO", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }

                    Text("1. Pilih Komponen:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        nonMcuComps.forEach { comp ->
                            FilterChip(
                                selected = wireTargetComp?.id == comp.id,
                                onClick = {
                                    wireTargetComp = comp
                                    selectedCompPin = comp.type.pinNames.first()
                                },
                                label = { Text(comp.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (wireTargetComp != null) {
                        Text("2. Kaki Pin pada ${wireTargetComp!!.label}:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            wireTargetComp!!.type.pinNames.forEach { pinName ->
                                FilterChip(
                                    selected = selectedCompPin == pinName,
                                    onClick = { selectedCompPin = pinName },
                                    label = { Text(pinName, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Text("3. Kaki Pin MCU Target:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mcuPins.forEach { mPin ->
                                FilterChip(
                                    selected = selectedMcuPin == mPin,
                                    onClick = { selectedMcuPin = mPin },
                                    label = { Text(mPin, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val color = when (selectedMcuPin) {
                                    "3V3", "5V" -> Color(0xFFFF1744)
                                    "GND" -> Color(0xFF90A4AE)
                                    else -> LogicCyan
                                }
                                val filtered = wires.filterNot { it.fromCompId == wireTargetComp!!.id && it.fromPinName == selectedCompPin }
                                onWiresChange(
                                    filtered + classPinWireConnection(
                                        id = "w_${System.currentTimeMillis()}",
                                        fromCompId = wireTargetComp!!.id,
                                        fromPinName = selectedCompPin,
                                        toMcuPin = selectedMcuPin,
                                        wireColor = color
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pasang: [${wireTargetComp!!.label}:$selectedCompPin] ➔ [$selectedMcuPin]", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    HorizontalDivider()
                    Text("Daftar Kabel Terpasang (${wires.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    wires.forEach { w ->
                        val compName = components.find { it.id == w.fromCompId }?.label ?: w.fromCompId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• $compName [${w.fromPinName}] ➔ MCU [${w.toMcuPin}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            IconButton(
                                onClick = { onWiresChange(wires.filterNot { it.id == w.id }) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cabut", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWireManagerDialog = false }) { Text("Selesai") }
            }
        )
    }
}

// =========================================================================
// BLYNK IOT DASHBOARD PANEL (12 JENIS WIDGET INTERAKTIF + TAMBAH WIDGET!)
// =========================================================================

@Composable
fun BlynkDashboardPanel(
    widgets: List<DashWidget>,
    onWidgetsChange: (List<DashWidget>) -> Unit,
    isSimRunning: Boolean,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    dhtTempC: Float,
    ultrasonicCm: Float
) {
    var showAddBlynkDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SiliconDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("BAMBU-BLYNK CLOUD STUDIO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text(
                            text = if (isSimRunning) "🟢 ONLINE • Sinkron dengan Kanvas Sirkuit" else "⚪ STANDBY • Klik RUN di Kanvas untuk Live Sync",
                            color = if (isSimRunning) Color(0xFF00E676) else CopperGold,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = { showAddBlynkDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Widget Blynk", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
        }

        widgets.chunked(2).forEach { rowWidgets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowWidgets.forEach { widget ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(widget.title, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 11.sp, maxLines = 1)
                                Surface(color = widget.color.copy(alpha = 0.18f), shape = RoundedCornerShape(4.dp)) {
                                    Text(widget.pin, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PcbEmeraldDark, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }

                            when (widget.type) {
                                DashWidgetType.TOGGLE_SWITCH -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (widget.isOn) "HIGH (ON)" else "LOW (OFF)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = widget.color)
                                        Switch(
                                            checked = widget.isOn,
                                            onCheckedChange = { checked ->
                                                onWidgetsChange(widgets.map { if (it.id == widget.id) it.copy(isOn = checked) else it })
                                            }
                                        )
                                    }
                                }

                                DashWidgetType.PUSH_BUTTON -> {
                                    Button(
                                        onClick = {
                                            onWidgetsChange(widgets.map { if (it.id == widget.id) it.copy(isOn = !it.isOn) else it })
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (widget.isOn) widget.color else PcbEmeraldDark),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (widget.isOn) "🔊 BEEP ACTIVE" else "PUSH TO BEEP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                DashWidgetType.KNOB_ROTARY, DashWidgetType.SLIDER_LEVEL -> {
                                    Slider(
                                        value = widget.value,
                                        onValueChange = { newVal ->
                                            onWidgetsChange(widgets.map { if (it.id == widget.id) it.copy(value = newVal) else it })
                                        },
                                        valueRange = 0f..widget.max
                                    )
                                    Text("Nilai: ${widget.value.toInt()}${widget.unit}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }

                                DashWidgetType.GAUGE_RADIAL -> {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp).align(Alignment.CenterHorizontally)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(Color.LightGray, startAngle = 135f, sweepAngle = 270f, useCenter = false, style = Stroke(7f, cap = StrokeCap.Round))
                                            val sweep = (widget.value / widget.max).coerceIn(0f, 1f) * 270f
                                            drawArc(widget.color, startAngle = 135f, sweepAngle = sweep, useCenter = false, style = Stroke(8f, cap = StrokeCap.Round))
                                        }
                                        Text("${widget.value.toInt()}${widget.unit}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = widget.color)
                                    }
                                }

                                DashWidgetType.VALUE_DISPLAY -> {
                                    Text(
                                        text = "${widget.value.toInt()}${widget.unit}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = widget.color
                                    )
                                    Text("Topic: ${widget.topic}", fontSize = 9.sp, color = Color.Gray)
                                }

                                DashWidgetType.LED_INDICATOR -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (widget.isOn) widget.color else Color.LightGray)
                                        )
                                        Text(if (widget.isOn) "ALARM (>40°C)" else "NORMAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                DashWidgetType.RGB_COLOR_PICKER -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Cyan).forEach { col ->
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(2.dp, if (selectedColor == col) Color.Black else Color.Transparent, CircleShape)
                                                    .clickable { onColorChange(col) }
                                            )
                                        }
                                    }
                                }

                                DashWidgetType.STEP_INPUT -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = { onWidgetsChange(widgets.map { if (it.id == widget.id) it.copy(value = (it.value - 1f).coerceAtLeast(0f)) else it }) },
                                            modifier = Modifier.size(28.dp)
                                        ) { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp)) }

                                        Text("${widget.value.toInt()}${widget.unit}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)

                                        FilledTonalIconButton(
                                            onClick = { onWidgetsChange(widgets.map { if (it.id == widget.id) it.copy(value = (it.value + 1f).coerceAtMost(widget.max)) else it }) },
                                            modifier = Modifier.size(28.dp)
                                        ) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    }
                                }

                                DashWidgetType.HISTORICAL_CHART -> {
                                    Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(SiliconDark, RoundedCornerShape(8.dp))) {
                                        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                            val w = size.width
                                            val h = size.height
                                            val pts = listOf(22f, 28f, 26f, 32f, 30f, 35f, dhtTempC)
                                            SharedWirePath.reset()
                                            pts.forEachIndexed { i, v ->
                                                val xPos = (i / (pts.size - 1).toFloat()) * w
                                                val yPos = h - (v / 80f).coerceIn(0f, 1f) * h
                                                if (i == 0) SharedWirePath.moveTo(xPos, yPos) else SharedWirePath.lineTo(xPos, yPos)
                                            }
                                            drawPath(SharedWirePath, color = widget.color, style = Stroke(3.5f))
                                        }
                                    }
                                }

                                DashWidgetType.LCD_16X2 -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF1B5E20), RoundedCornerShape(6.dp))
                                            .border(1.5.dp, SiliconDark, RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Column {
                                            Text("BLYNK LCD 16x2", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFB9F6CA), fontWeight = FontWeight.Bold)
                                            Text("T:${String.format("%.1f", dhtTempC)}C D:${ultrasonicCm.toInt()}cm", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.Yellow, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                DashWidgetType.TERMINAL_MINI -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SiliconDark, RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "> MQTT OK\n> V5=${String.format("%.1f", dhtTempC)}C",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = LogicNeonGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBlynkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlynkDialog = false },
            title = { Text("➕ Tambah Widget Blynk Baru", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DashWidgetType.entries.forEach { wType ->
                        OutlinedButton(
                            onClick = {
                                val nextIdx = widgets.size + 1
                                onWidgetsChange(
                                    widgets + DashWidget(
                                        id = "dw_$nextIdx",
                                        type = wType,
                                        title = "${wType.label} (V$nextIdx)",
                                        topic = "bambu/v$nextIdx",
                                        pin = "V$nextIdx",
                                        value = 50f,
                                        color = CopperOrange
                                    )
                                )
                                showAddBlynkDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(wType.label, fontWeight = FontWeight.Bold, color = PcbEmeraldDark)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddBlynkDialog = false }) { Text("Tutup") }
            }
        )
    }
}

// =========================================================================
// FAST ZERO-ALLOCATION RENDERING ENGINE (MENGGUNAKAN SHARED PAINT & PATH!)
// =========================================================================

private fun getMcuPinOffset(mcuComp: SimComponent, pinName: String): Offset {
    val rightPins = listOf("3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34")
    val idx = rightPins.indexOf(pinName).coerceAtLeast(0)
    return Offset(mcuComp.x + 178f, mcuComp.y + 42f + idx * 20f)
}

private fun getComponentPinOffset(comp: SimComponent, pinName: String): Offset {
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

private fun DrawScope.drawFastWire(start: Offset, end: Offset, wireColor: Color) {
    SharedWirePath.reset()
    SharedWirePath.moveTo(start.x, start.y)
    val ctrlX = (start.x + end.x) * 0.5f
    val ctrlY = minOf(start.y, end.y) - 24f
    SharedWirePath.quadraticBezierTo(ctrlX, ctrlY, end.x, end.y)
    drawPath(SharedWirePath, color = wireColor, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
    drawCircle(color = CopperGold, radius = 4f, center = start)
    drawCircle(color = CopperGold, radius = 4f, center = end)
}

private fun DrawScope.drawMcuBoardWithPinLabels(x: Float, y: Float, boardModel: String, isRunning: Boolean) {
    val w = 185f
    val h = 265f
    drawRoundRect(color = Color(0xFF1A221E), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f))
    drawRoundRect(color = CopperGold, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f), style = Stroke(width = 3f))

    drawRoundRect(color = Color(0xFFB0BEC5), topLeft = Offset(x + 18f, y + 32f), size = Size(90f, 80f), cornerRadius = CornerRadius(6f, 6f))
    drawContext.canvas.nativeCanvas.apply {
        SharedTitlePaint.color = android.graphics.Color.BLACK
        SharedTitlePaint.textSize = 18f
        drawText(boardModel, x + 32f, y + 70f, SharedTitlePaint)
        SharedTitlePaint.textSize = 12f
        drawText(if (boardModel == "ESP32") "WROOM-32" else "NodeMCU", x + 28f, y + 92f, SharedTitlePaint)
    }

    val rightPins = listOf("3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34")
    rightPins.forEachIndexed { idx, pName ->
        val py = y + 36f + idx * 20f
        val padColor = when (pName) {
            "3V3", "5V" -> Color(0xFFFF1744)
            "GND" -> Color(0xFF90A4AE)
            else -> CopperGold
        }
        drawRoundRect(color = padColor, topLeft = Offset(x + w - 16f, py), size = Size(14f, 12f), cornerRadius = CornerRadius(2f, 2f))

        drawContext.canvas.nativeCanvas.apply {
            SharedPinPaint.color = when (pName) {
                "3V3", "5V" -> android.graphics.Color.rgb(255, 82, 82)
                "GND" -> android.graphics.Color.LTGRAY
                else -> android.graphics.Color.rgb(255, 215, 64)
            }
            drawText(pName, x + w - 50f, py + 10f, SharedPinPaint)
        }
    }

    drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(x + 35f, y + h - 22f))
    drawCircle(color = if (isRunning) Color(0xFF00E5FF) else Color(0xFF263238), radius = 6.5f, center = Offset(x + 85f, y + h - 22f))
}

private fun DrawScope.drawResistor4BandWithPins(x: Float, y: Float, ohms: Int) {
    drawLine(Color(0xFFB0BEC5), Offset(x, y + 25f), Offset(x + 30f, y + 25f), strokeWidth = 6f)
    drawLine(Color(0xFFB0BEC5), Offset(x + 100f, y + 25f), Offset(x + 130f, y + 25f), strokeWidth = 6f)

    drawRoundRect(color = Color(0xFFE6CCB2), topLeft = Offset(x + 25f, y + 8f), size = Size(80f, 34f), cornerRadius = CornerRadius(14f, 14f))
    val bands = calculateResistorBands(ohms)
    val bandXPositions = listOf(x + 38f, x + 53f, x + 68f, x + 88f)
    bands.forEachIndexed { idx, bandColor ->
        drawRect(color = bandColor, topLeft = Offset(bandXPositions[idx], y + 9f), size = Size(8f, 32f))
    }

    drawContext.canvas.nativeCanvas.apply {
        SharedPinPaint.color = android.graphics.Color.WHITE
        drawText("P1", x, y + 18f, SharedPinPaint)
        drawText("P2", x + 112f, y + 18f, SharedPinPaint)
        SharedPinPaint.color = android.graphics.Color.YELLOW
        drawText("${ohms}Ω", x + 42f, y + 56f, SharedPinPaint)
    }
}

private fun DrawScope.drawLedWithPins(x: Float, y: Float, isOn: Boolean, resistorOhms: Int, isPowered: Boolean) {
    drawLine(Color(0xFFFF5252), Offset(x + 22f, y + 52f), Offset(x + 22f, y + 88f), strokeWidth = 5f)
    drawLine(Color(0xFFB0BEC5), Offset(x + 48f, y + 52f), Offset(x + 48f, y + 88f), strokeWidth = 5f)

    val intensity = (1f - (resistorOhms / 12000f)).coerceIn(0.35f, 1f)
    val bulbColor = if (isOn) Color(0xFFFF1744).copy(alpha = intensity) else Color(0xFF4A151B)

    if (isOn) {
        drawCircle(color = Color(0xFFFF5252).copy(alpha = 0.35f * intensity), radius = 42f, center = Offset(x + 35f, y + 28f))
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

private fun DrawScope.drawDht22With4Pins(x: Float, y: Float, tempC: Float, isPowered: Boolean) {
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
        SharedTitlePaint.textSize = 16f
        drawText("DHT22", x + 22f, y + h - 22f, SharedTitlePaint)
        SharedPinPaint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
        drawText(if (isPowered) "${String.format("%.1f", tempC)}°C" else "NO VCC/GND", x + 12f, y + h - 5f, SharedPinPaint)
    }
}

private fun DrawScope.drawOledWith4Pins(x: Float, y: Float, isActive: Boolean, line1: String, line2: String, isPowered: Boolean) {
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

private fun DrawScope.drawServoWith3Pins(x: Float, y: Float, angleDeg: Float, isPowered: Boolean) {
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

private fun DrawScope.drawHcsr04With4Pins(x: Float, y: Float, distCm: Float, isPowered: Boolean) {
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

private fun DrawScope.drawRelayWith3Pins(x: Float, y: Float, isRelayOn: Boolean, isPowered: Boolean) {
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

private fun DrawScope.drawBuzzerWith2Pins(x: Float, y: Float, isBeeping: Boolean, isPowered: Boolean) {
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

private fun DrawScope.drawPotentiometerWith3Pins(x: Float, y: Float, pct: Float, isPowered: Boolean) {
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

private fun digitToResistorColor(digit: Int): Color {
    return when (digit.coerceIn(0, 9)) {
        0 -> Color(0xFF1B1B1B)
        1 -> Color(0xFF795548)
        2 -> Color(0xFFD32F2F)
        3 -> Color(0xFFFF6D00)
        4 -> Color(0xFFFFEA00)
        5 -> Color(0xFF2E7D32)
        6 -> Color(0xFF1565C0)
        7 -> Color(0xFF8E24AA)
        8 -> Color(0xFF757575)
        else -> Color(0xFFFFFFFF)
    }
}

private fun digitToColorName(digit: Int): String {
    return when (digit.coerceIn(0, 9)) {
        0 -> "Hitam"
        1 -> "Coklat"
        2 -> "Merah"
        3 -> "Oranye"
        4 -> "Kuning"
        5 -> "Hijau"
        6 -> "Biru"
        7 -> "Ungu"
        8 -> "Abu"
        else -> "Putih"
    }
}

private fun calculateResistorBands(ohms: Int): List<Color> {
    val safeOhms = ohms.coerceAtLeast(10)
    val exp = (log10(safeOhms.toDouble()).toInt() - 1).coerceAtLeast(0)
    val twoDigits = (safeOhms / 10.0.pow(exp.toDouble())).toInt().coerceIn(10, 99)
    val d1 = twoDigits / 10
    val d2 = twoDigits % 10
    return listOf(
        digitToResistorColor(d1),
        digitToResistorColor(d2),
        digitToResistorColor(exp),
        Color(0xFFFFD700)
    )
}

private fun getResistorColorBandNames(ohms: Int): String {
    val safeOhms = ohms.coerceAtLeast(10)
    val exp = (log10(safeOhms.toDouble()).toInt() - 1).coerceAtLeast(0)
    val twoDigits = (safeOhms / 10.0.pow(exp.toDouble())).toInt().coerceIn(10, 99)
    val d1 = twoDigits / 10
    val d2 = twoDigits % 10
    return "${digitToColorName(d1)}-${digitToColorName(d2)}-${digitToColorName(exp)}-Emas"
}

@Composable
fun WokwiCodeEditorPanel(
    codeLang: String,
    onLangChange: (String) -> Unit,
    codeText: String,
    onCodeChange: (String) -> Unit,
    onRunNow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = codeLang == "CPP",
                        onClick = { onLangChange("CPP") },
                        label = { Text("Arduino C++ (.ino)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = codeLang == "MICROPYTHON",
                        onClick = { onLangChange("MICROPYTHON") },
                        label = { Text("MicroPython (.py)", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                Button(
                    onClick = onRunNow,
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compile & Run", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            OutlinedTextField(
                value = codeText,
                onValueChange = onCodeChange,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = LogicNeonGreen),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SiliconDark,
                    unfocusedContainerColor = SiliconDark,
                    focusedBorderColor = CopperGold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun BuiltInLibraryCatalogView() {
    val libs = remember {
        listOf(
            BuiltInLibInfo("<WiFi.h> & <ESP8266WiFi.h>", "C++", "Networking", "Koneksi Wi-Fi STA/AP untuk ESP32 & ESP8266 NodeMCU", "WiFi.begin(ssid, pass); WiFi.localIP();"),
            BuiltInLibInfo("<PubSubClient.h>", "C++", "IoT MQTT", "Klien Broker MQTT Port 1883 Publish & Subscribe", "client.connect(\"ESP32\"); client.publish(topic, payload);"),
            BuiltInLibInfo("<BlynkSimpleEsp32.h>", "C++", "Blynk Cloud", "Integrasi Virtual Pin V1-V12 Blynk SCADA Dashboard", "Blynk.virtualWrite(V5, tempC); BLYNK_WRITE(V1)"),
            BuiltInLibInfo("<Wire.h> & <SPI.h>", "C++", "Core Bus", "Komunikasi I2C (SDA=D21, SCL=D22) & SPI High-Speed", "Wire.begin(21, 22); SPI.begin();"),
            BuiltInLibInfo("<Adafruit_SSD1306.h> & <Adafruit_GFX.h>", "C++", "Display", "Layar OLED 0.96\" & 1.3\" 128x64 I2C (Alamat 0x3C)", "display.println(\"TEKS\"); display.display();"),
            BuiltInLibInfo("<LiquidCrystal_I2C.h>", "C++", "Display", "Layar LCD Karakter 16x2 & 20x4 I2C PCF8574 (0x27)", "lcd.init(); lcd.backlight(); lcd.print(\"HELLO\");"),
            BuiltInLibInfo("<DHT.h> (Adafruit Unified)", "C++", "Env Sensor", "Sensor Suhu & Kelembaban Udara DHT11 / DHT22", "dht.begin(); float t = dht.readTemperature();"),
            BuiltInLibInfo("<Adafruit_BME280.h> & <BMP280.h>", "C++", "Env Sensor", "Sensor Suhu, Tekanan Barometrik & Ketinggian I2C (0x76)", "bme.readTemperature(); bme.readAltitude(1013.25);"),
            BuiltInLibInfo("<OneWire.h> & <DallasTemperature.h>", "C++", "Temp Probe", "Sensor Suhu Air/Tanah Tahan Air DS18B20", "sensors.requestTemperatures(); sensors.getTempCByIndex(0);"),
            BuiltInLibInfo("<NewPing.h>", "C++", "Ultrasonic", "Sensor Jarak Ultrasonik HC-SR04 & JSN-SR04T Waterproof", "NewPing sonar(TRIG, ECHO, 200); sonar.ping_cm();"),
            BuiltInLibInfo("<ESP32Servo.h> & <Servo.h>", "C++", "Actuator", "Kontrol Sudut Motor Servo SG90 / MG996R PWM (0-180°)", "servo.attach(18); servo.write(135);"),
            BuiltInLibInfo("<Adafruit_NeoPixel.h> & <FastLED.h>", "C++", "RGB LED", "Kontrol Strip LED RGB Addressable WS2812B", "strip.setPixelColor(0, strip.Color(255, 0, 0)); strip.show();"),
            BuiltInLibInfo("<Adafruit_MPU6050.h>", "C++", "IMU Sensor", "Sensor Gyroscope & Accelerometer 6-Axis I2C (0x68)", "mpu.getEvent(&a, &g, &temp);"),
            BuiltInLibInfo("<BH1750.h>", "C++", "Optical", "Sensor Intensitas Cahaya Matahari Digital Lux (0x23)", "float lux = lightMeter.readLightLevel();"),
            BuiltInLibInfo("<Adafruit_INA219.h>", "C++", "Power Meter", "Sensor Tegangan DC & Arus Baterai/Solar Panel I2C (0x40)", "float mA = ina219.getCurrent_mA();"),
            BuiltInLibInfo("<ModbusMaster.h>", "C++", "Industrial", "Komunikasi RS485 Modbus RTU (Sensor NPK Tanah / PLC)", "node.readHoldingRegisters(0x0000, 4);"),
            BuiltInLibInfo("<ArduinoJson.h> & <Preferences.h>", "C++", "Storage/Data", "Serialisasi Payload JSON & Penyimpanan EEPROM NVS", "StaticJsonDocument<200> doc; prefs.putString(\"ssid\", v);"),
            BuiltInLibInfo("machine (Pin, ADC, PWM, I2C, UART)", "MicroPython", "Core Hardware", "Akses Langsung Register GPIO, PWM, & ADC 12-Bit", "led = Pin(2, Pin.OUT); adc = ADC(Pin(34))"),
            BuiltInLibInfo("ssd1306, dht & hcsr04", "MicroPython", "Display/Sensor", "Driver OLED I2C, Sensor DHT22 & Ultrasonik", "oled.text('PANDAKODE', 0, 0); d.measure()"),
            BuiltInLibInfo("umqtt.simple & blynklib", "MicroPython", "IoT Network", "Klien Broker MQTT & Blynk Cloud MicroPython", "client.publish(b'pandakode/temp', b'32.5')")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📚 20+ LIBRARY BAWAAN BAMBUSIM (C++ & MICROPYTHON)",
            fontWeight = FontWeight.ExtraBold,
            color = PcbEmeraldDark,
            fontSize = 13.sp
        )
        libs.forEach { lib ->
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
                        Text(lib.headerName, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 12.sp)
                        Surface(color = if (lib.lang == "C++") CopperOrange else LogicViolet, shape = RoundedCornerShape(6.dp)) {
                            Text("${lib.lang} • ${lib.category}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text(lib.supportedHardware, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("API: ${lib.exampleApi}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = LogicCyan)
                }
            }
        }
    }
}