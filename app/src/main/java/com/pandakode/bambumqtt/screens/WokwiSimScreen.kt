package com.pandakode.bambumqtt.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawBuzzerWith2Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawDht22With4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawFlexibleWire
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawHcsr04With4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawLcd16x2With4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawLdrSensorWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawLedWithPins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawMcuVariantBoard
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawNeoPixelRingWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawOledWith4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawPirSensorWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawPotentiometerWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawPushButtonWith2Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawRelayWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawResistor4BandWithPins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawServoWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawSoilSensorWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawStepperWith4Pins
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun WokwiSimScreen() {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Kanvas", "Blynk Dash", "IDE & AI", "20+ Library")

    var selectedBoardVariant by remember { mutableStateOf(McuBoardVariant.ESP32_30PIN) }
    var isSimRunning by remember { mutableStateOf(false) }
    var showLiveSerialDrawer by remember { mutableStateOf(false) } // Default disembunyikan sampai klik RUN agar kanvas lega
    var showAiProjectBuilderDialog by remember { mutableStateOf(false) }
    var simTick by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "sim_fx")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Parameter Interaktif Seluruh 16 Modul Komponen
    var resistorOhms by remember { mutableIntStateOf(220) }
    var dhtTempC by remember { mutableFloatStateOf(32.4f) }
    var dhtHumPct by remember { mutableFloatStateOf(65.0f) }
    var ultrasonicCm by remember { mutableFloatStateOf(45.0f) }
    var soilMoisturePct by remember { mutableFloatStateOf(68.0f) }
    var ldrLuxVal by remember { mutableFloatStateOf(380.0f) }
    var potPercent by remember { mutableFloatStateOf(50.0f) }
    var servoAngle by remember { mutableFloatStateOf(90f) }
    var stepperAngle by remember { mutableFloatStateOf(0f) }
    var oledLine1 by remember { mutableStateOf("PANDAKODE IOT") }
    var oledLine2 by remember { mutableStateOf("T:32.4C D:45cm") }
    var ledRedActive by remember { mutableStateOf(false) }
    var relayActive by remember { mutableStateOf(false) }
    var buzzerActive by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Red) }

    var isVirtualWifiConnected by remember { mutableStateOf(false) }
    var simSerialLogs by remember {
        mutableStateOf(
            listOf(
                "[TIPS] Klik komponen di kanvas untuk Edit, Rotate, Mirror Pin, atau Hapus!",
                "[TIPS] Klik tombol kuning '+ Modul' di kanan bawah kanvas untuk memilih 16 modul."
            )
        )
    }

    fun appendSimSerial(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault()).format(Date())
        simSerialLogs = (simSerialLogs + "[$time] $msg").takeLast(60)
    }

    var dashboardWidgets by remember {
        mutableStateOf(
            listOf(
                DashWidget("dw1", DashWidgetType.TOGGLE_SWITCH, "Pompa Irigasi (V1)", "bambu/relay/pump", "V1 / D2", isOn = false, color = Color(0xFF00E676)),
                DashWidget("dw2", DashWidgetType.PUSH_BUTTON, "Alarm Buzzer (V2)", "bambu/act/beep", "V2 / D5", color = Color(0xFFFF1744)),
                DashWidget("dw3", DashWidgetType.KNOB_ROTARY, "Sudut Servo (V3)", "bambu/pwm/servo", "V3 / D18", value = 90f, max = 180f, unit = "°", color = CopperOrange),
                DashWidget("dw4", DashWidgetType.SLIDER_LEVEL, "Dimmer PWM (V4)", "bambu/pwm/led", "V4 / D2", value = 200f, max = 255f, color = Color(0xFF00E5FF)),
                DashWidget("dw5", DashWidgetType.GAUGE_RADIAL, "Suhu DHT22 (V5)", "bambu/sensor/temp", "V5 / D4", value = 32.4f, max = 100f, unit = "°C", color = Color(0xFFFF5252)),
                DashWidget("dw6", DashWidgetType.VALUE_DISPLAY, "Jarak Sonar (V6)", "bambu/sensor/sonar", "V6 / D19", value = 45f, max = 200f, unit = " cm", color = Color(0xFF2979FF)),
                DashWidget("dw7", DashWidgetType.LED_INDICATOR, "Status Overheat (V7)", "bambu/status/heat", "V7", isOn = false, color = Color(0xFFFF1744)),
                DashWidget("dw8", DashWidgetType.RGB_COLOR_PICKER, "RGB NeoPixel (V8)", "bambu/color", "V8", color = Color.Red),
                DashWidget("dw9", DashWidgetType.STEP_INPUT, "Setpoint Thermostat", "bambu/setpoint", "V9", value = 36f, max = 80f, unit = "°C", color = Color(0xFFE040FB)),
                DashWidget("dw10", DashWidgetType.HISTORICAL_CHART, "SuperChart Suhu", "bambu/history/temp", "V10", value = 32.4f, max = 100f, color = Color(0xFF00E5FF)),
                DashWidget("dw11", DashWidgetType.LCD_16X2, "Blynk LCD 16x2 (V11)", "bambu/lcd", "V11", value = 32.4f, color = Color(0xFF00E676)),
                DashWidget("dw12", DashWidgetType.TERMINAL_MINI, "Cloud Terminal (V12)", "bambu/term", "V12", color = CopperGold)
            )
        )
    }

    val components = remember {
        mutableStateListOf(
            SimComponent("mcu", SimCompType.ESP32_BOARD, "ESP32 DevKit", 14f, 24f),
            SimComponent("dht1", SimCompType.DHT22_SENSOR, "DHT22", 252f, 20f),
            SimComponent("pir1", SimCompType.PIR_MOTION, "PIR Motion", 425f, 20f),
            SimComponent("oled1", SimCompType.OLED_SSD1306, "OLED I2C", 252f, 168f),
            SimComponent("led1", SimCompType.LED_LAMP, "LED Merah", 435f, 168f)
        )
    }

    var wires by remember {
        mutableStateOf(
            listOf(
                classPinWireConnection("w1", "dht1", "VCC", "3V3", JumperWireColor.RED_VCC.color),
                classPinWireConnection("w2", "dht1", "DATA", "D4", JumperWireColor.CYAN_DATA.color),
                classPinWireConnection("w3", "dht1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                classPinWireConnection("w4", "pir1", "VCC", "5V", JumperWireColor.RED_VCC.color),
                classPinWireConnection("w5", "pir1", "OUT", "D15", JumperWireColor.GREEN_SCL.color),
                classPinWireConnection("w6", "pir1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                classPinWireConnection("w7", "oled1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                classPinWireConnection("w8", "oled1", "VCC", "3V3", JumperWireColor.RED_VCC.color),
                classPinWireConnection("w9", "oled1", "SCL", "D22", JumperWireColor.GREEN_SCL.color),
                classPinWireConnection("w10", "oled1", "SDA", "D21", JumperWireColor.GOLD_SDA.color),
                classPinWireConnection("w11", "led1", "A+", "D2", JumperWireColor.RED_VCC.color),
                classPinWireConnection("w12", "led1", "K-", "GND", JumperWireColor.BLACK_GND.color)
            )
        )
    }

    fun isPowerPin(pin: String) = pin in listOf("3V3", "5V", "VIN", "VBUS", "VSYS", "VU")
    fun isSignalPin(pin: String) = pin.startsWith("D") || pin.startsWith("GP") || pin.startsWith("A") || pin in listOf("TX", "RX", "RX0", "TX0")

    fun isComponentPoweredAndWired(comp: SimComponent): Boolean {
        val compWires = wires.filter { it.fromCompId == comp.id }
        return when (comp.type) {
            SimCompType.ESP32_BOARD -> true
            SimCompType.RESISTOR_4BAND -> compWires.isNotEmpty()
            SimCompType.LED_LAMP -> {
                compWires.any { it.fromPinName == "A+" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "K-" && it.toMcuPin == "GND" }
            }
            SimCompType.DHT22_SENSOR -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "DATA" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.OLED_SSD1306, SimCompType.LCD_16X2_I2C -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "SCL" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "SDA" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.SERVO_SG90 -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "SIG" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.HCSR04_SONAR -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "TRIG" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "ECHO" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.PIR_MOTION, SimCompType.POTENTIOMETER -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "OUT" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.SOIL_MOISTURE -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "AOUT" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.LDR_LIGHT -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "DO/AO" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.NEOPIXEL_RING -> {
                compWires.any { it.fromPinName == "5V" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "DIN" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.STEPPER_MOTOR -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "STEP" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.RELAY_1CH -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "IN" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.BUZZER_ACTIVE -> {
                compWires.any { it.fromPinName == "VCC+" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND-" && it.toMcuPin == "GND" }
            }
            SimCompType.PUSH_BUTTON -> {
                compWires.any { it.fromPinName == "PIN1" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
            }
        }
    }

    var codeLang by remember { mutableStateOf("CPP") }
    var cppSketchStore by remember {
        mutableStateOf(
            """
            #include <WiFi.h>
            #include <BlynkSimpleEsp32.h>
            #include <Wire.h>
            #include <Adafruit_SSD1306.h>
            #include <DHT.h>

            char auth[] = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}";
            char ssid[] = "${BambuVirtualNetwork.VIRTUAL_SSID}";
            char pass[] = "${BambuVirtualNetwork.VIRTUAL_PASS}";

            void setup() {
              Serial.begin(115200);
              Blynk.begin(auth, ssid, pass);
              pinMode(2, OUTPUT);
              pinMode(15, INPUT);
              display.println("PANDAKODE IOT");
            }

            void loop() {
              Blynk.run();
              digitalWrite(2, HIGH);
              Serial.println("PIR Motion & DHT22 Telemetry Active!");
              delay(1000);
            }
            """.trimIndent()
        )
    }

    var microPySketchStore by remember {
        mutableStateOf(
            """
            # MicroPython + Virtual Wi-Fi Blynk
            import network, blynklib, ssd1306, dht, time
            from machine import Pin

            WIFI_SSID = "${BambuVirtualNetwork.VIRTUAL_SSID}"
            WIFI_PASS = "${BambuVirtualNetwork.VIRTUAL_PASS}"
            BLYNK_AUTH = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}"

            led = Pin(2, Pin.OUT)
            while True:
                led.on()
                oled.text("MICROPYTHON OK", 0, 0)
                print("MicroPython PIR & DHT22 Active!")
                time.sleep(1)
            """.trimIndent()
        )
    }

    val activeCodeText = if (codeLang == "CPP") cppSketchStore else microPySketchStore

    // Fungsi Menerapkan Rangkaian (Baik dari AI Project maupun Saat Modul Ditekan!)
    fun applyCircuitProject(project: AiGeneratedSimProject, autoRun: Boolean = true) {
        selectedBoardVariant = project.recommendedBoard
        components.clear()
        components.addAll(project.components)
        wires = project.wires
        cppSketchStore = project.cppCode
        microPySketchStore = project.microPythonCode
        oledLine1 = project.initialOledOrLcdLine1
        oledLine2 = project.initialOledOrLcdLine2
        if (autoRun) isSimRunning = true
        activeSubTab = 0
        Toast.makeText(context, "⚡ ${project.projectTitle} dipasang ke kanvas!", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isSimRunning, activeCodeText) {
        if (!isSimRunning) {
            isVirtualWifiConnected = false
            return@LaunchedEffect
        }
        showLiveSerialDrawer = true
        simSerialLogs = emptyList()
        appendSimSerial("Booting ${selectedBoardVariant.displayName} (${selectedBoardVariant.chipName})...")
        delay(300L)

        val hasValidSsid = activeCodeText.contains(BambuVirtualNetwork.VIRTUAL_SSID)
        val hasValidPass = activeCodeText.contains(BambuVirtualNetwork.VIRTUAL_PASS)
        if (hasValidSsid && hasValidPass) {
            isVirtualWifiConnected = true
            appendSimSerial("✅ Wi-Fi Connected (${BambuVirtualNetwork.VIRTUAL_SSID}) | Blynk Cloud: ONLINE")
        } else {
            isVirtualWifiConnected = false
            appendSimSerial("⚠️ Mode Offline Lokal")
        }

        while (isSimRunning) {
            simTick++
            stepperAngle = (stepperAngle + 45f) % 360f
            dhtTempC = (dhtTempC + sin(simTick * 0.6f) * 0.4f).coerceIn(20f, 55f)
            ultrasonicCm = (55f + sin(simTick * 0.8f) * 35f).coerceIn(10f, 190f)

            val ledComp = components.find { it.type == SimCompType.LED_LAMP }
            val dispComp = components.find { it.type == SimCompType.OLED_SSD1306 || it.type == SimCompType.LCD_16X2_I2C }
            val dhtComp = components.find { it.type == SimCompType.DHT22_SENSOR }
            val servoComp = components.find { it.type == SimCompType.SERVO_SG90 }
            val relayComp = components.find { it.type == SimCompType.RELAY_1CH }
            val buzzerComp = components.find { it.type == SimCompType.BUZZER_ACTIVE }

            val isLedWired = ledComp != null && isComponentPoweredAndWired(ledComp)
            val isDispWired = dispComp != null && isComponentPoweredAndWired(dispComp)
            val isDhtWired = dhtComp != null && isComponentPoweredAndWired(dhtComp)
            val isServoWired = servoComp != null && isComponentPoweredAndWired(servoComp)

            if (isDispWired) {
                val dispMatch = Regex("""(?:display\.println|lcd\.print|oled\.text)\(\s*"([^"]+)"""").find(activeCodeText)
                if (dispMatch != null) oledLine1 = dispMatch.groupValues[1].take(16)
                oledLine2 = if (isDhtWired) {
                    String.format("T:%.1fC D:%.0fcm", dhtTempC, ultrasonicCm)
                } else {
                    String.format("Dist:%.0fcm OK", ultrasonicCm)
                }
            }

            if (isServoWired) {
                val blynkServoVal = dashboardWidgets.find { it.id == "dw3" }?.value ?: 90f
                servoAngle = if (simTick % 2 == 0) blynkServoVal else (blynkServoVal - 35f).coerceAtLeast(0f)
            }

            val codeTriggersHigh = activeCodeText.contains("HIGH") || activeCodeText.contains(".on()")
            val blynkSwitchOn = isVirtualWifiConnected && (dashboardWidgets.find { it.id == "dw1" }?.isOn == true)
            ledRedActive = isLedWired && ((codeTriggersHigh && simTick % 2 == 1) || blynkSwitchOn)
            relayActive = relayComp != null && isComponentPoweredAndWired(relayComp) && (codeTriggersHigh || blynkSwitchOn)
            buzzerActive = (buzzerComp != null && isComponentPoweredAndWired(buzzerComp) && (simTick % 2 == 1)) ||
                    (isVirtualWifiConnected && dashboardWidgets.find { it.id == "dw2" }?.isOn == true)

            if (isVirtualWifiConnected) {
                dashboardWidgets = dashboardWidgets.map { widget ->
                    when (widget.id) {
                        "dw5" -> widget.copy(value = dhtTempC)
                        "dw6" -> widget.copy(value = ultrasonicCm)
                        "dw10" -> widget.copy(value = dhtTempC)
                        "dw7" -> widget.copy(isOn = dhtTempC > 40f)
                        else -> widget
                    }
                }
            }

            val serialPrintMatch = Regex("""(?:Serial\.println|print)\(\s*"([^"]+)"""").find(activeCodeText)
            val userPrint = serialPrintMatch?.groupValues?.get(1) ?: "Loop OK"
            appendSimSerial("$userPrint | DHT=${String.format("%.1f", dhtTempC)}°C | Sonar=${ultrasonicCm.toInt()}cm")

            delay(900L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (activeSubTab == idx) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (activeSubTab) {
            0 -> WokwiZeroLagWorkbench(
                selectedBoardVariant = selectedBoardVariant,
                onSelectBoardVariant = { selectedBoardVariant = it },
                components = components,
                wires = wires,
                onWiresChange = { wires = it },
                onReplaceCircuitWithModule = { modType ->
                    val newProj = WokwiAiProjectGenerator.buildSingleModuleCircuit(modType, selectedBoardVariant)
                    applyCircuitProject(newProj, autoRun = true)
                },
                checkPowered = { isComponentPoweredAndWired(it) },
                isSimRunning = isSimRunning,
                isVirtualWifiConnected = isVirtualWifiConnected,
                simTick = simTick,
                animPhase = animPhase,
                onToggleRun = {
                    isSimRunning = !isSimRunning
                    if (!isSimRunning) {
                        ledRedActive = false
                        appendSimSerial("🛑 Simulasi dihentikan.")
                    }
                },
                onOpenAiProjectBuilder = { showAiProjectBuilderDialog = true },
                showLiveSerialDrawer = showLiveSerialDrawer,
                onToggleSerialDrawer = { showLiveSerialDrawer = !showLiveSerialDrawer },
                simSerialLogs = simSerialLogs,
                onClearSerialLogs = { simSerialLogs = emptyList() },
                ledRedOn = ledRedActive,
                relayActive = relayActive,
                buzzerActive = buzzerActive,
                resistorOhms = resistorOhms,
                onResistorChange = { resistorOhms = it },
                dhtTempC = dhtTempC,
                dhtHumPct = dhtHumPct,
                onDhtTempChange = { dhtTempC = it },
                ultrasonicCm = ultrasonicCm,
                onUltrasonicChange = { ultrasonicCm = it },
                soilMoisturePct = soilMoisturePct,
                onSoilMoistureChange = { soilMoisturePct = it },
                ldrLuxVal = ldrLuxVal,
                onLdrLuxChange = { ldrLuxVal = it },
                potPercent = potPercent,
                onPotChange = { potPercent = it },
                servoAngle = servoAngle,
                onServoAngleChange = { servoAngle = it },
                stepperAngle = stepperAngle,
                oledLine1 = oledLine1,
                onOledLine1Change = { oledLine1 = it },
                oledLine2 = oledLine2
            )

            1 -> BlynkDashboardPanel(
                widgets = dashboardWidgets,
                onWidgetsChange = { dashboardWidgets = it },
                isSimRunning = isSimRunning,
                isVirtualWifiConnected = isVirtualWifiConnected,
                selectedColor = selectedColor,
                onColorChange = { selectedColor = it },
                dhtTempC = dhtTempC,
                ultrasonicCm = ultrasonicCm
            )

            2 -> WokwiCodeEditorPanel(
                codeLang = codeLang,
                onLangChange = { codeLang = it },
                codeText = activeCodeText,
                onCodeChange = { newText ->
                    if (codeLang == "CPP") cppSketchStore = newText else microPySketchStore = newText
                },
                onOpenAiProjectBuilder = { showAiProjectBuilderDialog = true },
                onRunNow = {
                    isSimRunning = true
                    activeSubTab = 0
                }
            )

            3 -> BuiltInLibraryCatalogView()
        }
    }

    if (showAiProjectBuilderDialog) {
        var customPrompt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAiProjectBuilderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CopperOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🤖 AI Buat Sirkuit & Koding", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        label = { Text("Ketik ide proyek (Misal: Irigasi Sawit / Radar PIR)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            showAiProjectBuilderDialog = false
                            val proj = WokwiAiProjectGenerator.buildProjectFromPrompt(customPrompt.ifBlank { "Smart Farming Sawit" })
                            applyCircuitProject(proj, autoRun = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = CopperGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rakit Sirkuit + Koding & RUN!", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("⚡ Pilih Template Proyek AI Siap Jalan:", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = PcbEmeraldDark)

                    WokwiAiProjectGenerator.presetProjectNames.forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                showAiProjectBuilderDialog = false
                                val proj = WokwiAiProjectGenerator.buildProjectFromPrompt(preset)
                                applyCircuitProject(proj, autoRun = true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PcbEmeraldDark)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAiProjectBuilderDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
fun WokwiZeroLagWorkbench(
    selectedBoardVariant: McuBoardVariant,
    onSelectBoardVariant: (McuBoardVariant) -> Unit,
    components: MutableList<SimComponent>,
    wires: List<classPinWireConnection>,
    onWiresChange: (List<classPinWireConnection>) -> Unit,
    onReplaceCircuitWithModule: (SimCompType) -> Unit,
    checkPowered: (SimComponent) -> Boolean,
    isSimRunning: Boolean,
    isVirtualWifiConnected: Boolean,
    simTick: Int,
    animPhase: Float,
    onToggleRun: () -> Unit,
    onOpenAiProjectBuilder: () -> Unit,
    showLiveSerialDrawer: Boolean,
    onToggleSerialDrawer: () -> Unit,
    simSerialLogs: List<String>,
    onClearSerialLogs: () -> Unit,
    ledRedOn: Boolean,
    relayActive: Boolean,
    buzzerActive: Boolean,
    resistorOhms: Int,
    onResistorChange: (Int) -> Unit,
    dhtTempC: Float,
    dhtHumPct: Float,
    onDhtTempChange: (Float) -> Unit,
    ultrasonicCm: Float,
    onUltrasonicChange: (Float) -> Unit,
    soilMoisturePct: Float,
    onSoilMoistureChange: (Float) -> Unit,
    ldrLuxVal: Float,
    onLdrLuxChange: (Float) -> Unit,
    potPercent: Float,
    onPotChange: (Float) -> Unit,
    servoAngle: Float,
    onServoAngleChange: (Float) -> Unit,
    stepperAngle: Float,
    oledLine1: String,
    onOledLine1Change: (String) -> Unit,
    oledLine2: String
) {
    val context = LocalContext.current
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showBoardPickerDialog by remember { mutableStateOf(false) }
    var showEditCompDialog by remember { mutableStateOf(false) }
    var isWireEditMode by remember { mutableStateOf(false) }
    var selectedWire by remember { mutableStateOf<classPinWireConnection?>(wires.firstOrNull()) }
    var selectedComp by remember { mutableStateOf<SimComponent?>(components.getOrNull(1)) }
    var pendingPinSource by remember { mutableStateOf<PinHitTarget?>(null) }

    // STATE ZOOM IN / ZOOM OUT SLIDER (50% s/d 200%) & PAN OFFSET KANVAS!
    var canvasZoom by remember { mutableFloatStateOf(0.88f) }
    var canvasPanX by remember { mutableFloatStateOf(0f) }
    var canvasPanY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size(800f, 360f)) }

    // Konversi koordinat sentuhan layar ke koordinat dunia kanvas (memperhitungkan Zoom & Pan)
    fun screenToWorld(touch: Offset): Offset {
        return Offset(
            x = (touch.x - canvasPanX) / canvasZoom,
            y = (touch.y - canvasPanY) / canvasZoom
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. TOOLBAR ATAS + SLIDER ZOOM IN / ZOOM OUT
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        // TOMBOL UTAMA KATALOG MODUL (SANGAT JELAS DI PALING KIRI!)
                        Button(
                            onClick = { showAddPartDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("➕ Pilih/Tambah Modul", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = onOpenAiProjectBuilder,
                            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🤖 AI Proyek", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { showBoardPickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldMedium),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(selectedBoardVariant.displayName.substringBefore(" ("), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = { isWireEditMode = !isWireEditMode },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWireEditMode) Color(0xFF00E676) else Color(0xFF263238)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isWireEditMode) "📐 Geser Kabel: ON" else "📐 Geser Kabel",
                                color = if (isWireEditMode) SiliconDark else CopperGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                WokwiSimRenderEngine.autoArrangeNeatGrid(components, 760f, 340f)
                                wires.forEach {
                                    it.bendX = Float.NaN; it.bendY = Float.NaN
                                    it.bend2X = Float.NaN; it.bend2Y = Float.NaN
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LogicViolet),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("✨ Rapikan", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
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
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                // BARIS SLIDER ZOOM IN / ZOOM OUT KANVAS (50% - 200%) & TOMBOL RESET VIEW!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = null, tint = CopperGold, modifier = Modifier.size(18.dp).clickable {
                        canvasZoom = (canvasZoom - 0.1f).coerceIn(0.5f, 1.8f)
                    })
                    Text(
                        text = "Zoom ${(canvasZoom * 100).toInt()}%",
                        color = PcbMintLight,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Slider(
                        value = canvasZoom,
                        onValueChange = { canvasZoom = it },
                        valueRange = 0.5f..1.8f,
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = CopperGold,
                            activeTrackColor = LogicNeonGreen,
                            inactiveTrackColor = SiliconDark
                        )
                    )
                    Icon(Icons.Default.ZoomIn, contentDescription = null, tint = CopperGold, modifier = Modifier.size(18.dp).clickable {
                        canvasZoom = (canvasZoom + 0.1f).coerceIn(0.5f, 1.8f)
                    })
                    Text(
                        text = "Reset",
                        color = CopperGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(SiliconDark, RoundedCornerShape(6.dp))
                            .clickable {
                                canvasZoom = 0.88f
                                canvasPanX = 0f
                                canvasPanY = 0f
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 2. KANVAS UTAMA DENGAN ZOOM TRANSFORM + TOMBOL FLOATING "+ MODUL" DI KANAN BAWAH!
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070C0A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showLiveSerialDrawer) 235.dp else 290.dp)
                .border(2.dp, if (pendingPinSource != null || isWireEditMode) Color(0xFF00E676) else PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isWireEditMode, selectedBoardVariant, wires, canvasZoom, canvasPanX, canvasPanY) {
                            detectTapGestures { rawTap ->
                                val tapOffset = screenToWorld(rawTap)
                                val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }

                                // 1. Cek ketukan pada titik kaki pin (Direct Pin-to-Pin Tap Wiring)
                                val tappedPin = WokwiSimRenderEngine.findTappedPinTarget(
                                    tapOffset = tapOffset,
                                    components = components,
                                    boardVariant = selectedBoardVariant
                                )
                                if (tappedPin != null) {
                                    val firstPin = pendingPinSource
                                    if (firstPin == null) {
                                        pendingPinSource = tappedPin
                                    } else if (firstPin.isMcu != tappedPin.isMcu) {
                                        val compTarget = if (firstPin.isMcu) tappedPin else firstPin
                                        val mcuTarget = if (firstPin.isMcu) firstPin else tappedPin
                                        val autoColor = when (compTarget.pinName) {
                                            "VCC", "VCC+", "5V", "A+" -> JumperWireColor.RED_VCC.color
                                            "GND", "GND-", "K-" -> JumperWireColor.BLACK_GND.color
                                            "SDA", "TRIG" -> JumperWireColor.GOLD_SDA.color
                                            "SCL", "ECHO" -> JumperWireColor.GREEN_SCL.color
                                            else -> JumperWireColor.CYAN_DATA.color
                                        }
                                        val filtered = wires.filterNot { it.fromCompId == compTarget.compId && it.fromPinName == compTarget.pinName }
                                        val newWire = classPinWireConnection(
                                            id = "w_${System.currentTimeMillis()}",
                                            fromCompId = compTarget.compId,
                                            fromPinName = compTarget.pinName,
                                            toMcuPin = mcuTarget.pinName,
                                            initialColor = autoColor,
                                            initialCurved = false // SIKU-SIKU 90 DERAJAT
                                        )
                                        onWiresChange(filtered + newWire)
                                        selectedWire = newWire
                                        pendingPinSource = null
                                        Toast.makeText(
                                            context,
                                            "🔌 Kabel Siku Terpasang: [${compTarget.compLabel}:${compTarget.pinName}] ➔ [${mcuTarget.pinName}]",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        pendingPinSource = tappedPin
                                    }
                                    return@detectTapGestures
                                }

                                // 2. Cek ketukan pada Handle Kabel
                                if (isWireEditMode && mcuComp != null) {
                                    val hitWire = wires.mapIndexedNotNull { idx, w ->
                                        val src = components.find { it.id == w.fromCompId } ?: return@mapIndexedNotNull null
                                        val sPt = WokwiSimRenderEngine.getComponentPinOffset(src, w.fromPinName)
                                        val ePt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, w.toMcuPin)
                                        val (cp1, cp2) = WokwiSimRenderEngine.getWireControlPoints(w, sPt, ePt, idx)
                                        if (hypot(tapOffset.x - cp1.x, tapOffset.y - cp1.y) < 34f ||
                                            hypot(tapOffset.x - cp2.x, tapOffset.y - cp2.y) < 34f
                                        ) w else null
                                    }.lastOrNull()
                                    if (hitWire != null) {
                                        selectedWire = hitWire
                                        return@detectTapGestures
                                    }
                                }

                                pendingPinSource = null

                                // 3. Cek ketukan pada Komponen (Untuk memilih, mengedit, rotate, mirror, atau menghapus!)
                                val hit = components.lastOrNull { comp ->
                                    val w = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                                    val h = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                                    tapOffset.x >= comp.x - 10f && tapOffset.x <= comp.x + w + 10f &&
                                            tapOffset.y >= comp.y - 10f && tapOffset.y <= comp.y + h + 10f
                                }
                                selectedComp = hit
                            }
                        }
                        .pointerInput(isWireEditMode, selectedBoardVariant, wires, canvasZoom) {
                            var draggedComp: SimComponent? = null
                            var draggedWire: classPinWireConnection? = null
                            var draggingSecondHandle = false
                            var isPanningCanvas = false

                            detectDragGestures(
                                onDragStart = { rawStart ->
                                    val startOffset = screenToWorld(rawStart)
                                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }
                                    if (isWireEditMode && mcuComp != null) {
                                        for ((idx, w) in wires.withIndex().reversed()) {
                                            val src = components.find { it.id == w.fromCompId } ?: continue
                                            val sPt = WokwiSimRenderEngine.getComponentPinOffset(src, w.fromPinName)
                                            val ePt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, w.toMcuPin)
                                            val (cp1, cp2) = WokwiSimRenderEngine.getWireControlPoints(w, sPt, ePt, idx)
                                            if (hypot(startOffset.x - cp1.x, startOffset.y - cp1.y) < 36f) {
                                                if (w.bendX.isNaN()) { w.bendX = cp1.x; w.bendY = cp1.y }
                                                if (w.bend2X.isNaN()) { w.bend2X = cp2.x; w.bend2Y = cp2.y }
                                                draggedWire = w
                                                draggingSecondHandle = false
                                                selectedWire = w
                                                return@detectDragGestures
                                            }
                                            if (hypot(startOffset.x - cp2.x, startOffset.y - cp2.y) < 36f) {
                                                if (w.bendX.isNaN()) { w.bendX = cp1.x; w.bendY = cp1.y }
                                                if (w.bend2X.isNaN()) { w.bend2X = cp2.x; w.bend2Y = cp2.y }
                                                draggedWire = w
                                                draggingSecondHandle = true
                                                selectedWire = w
                                                return@detectDragGestures
                                            }
                                        }
                                    }

                                    draggedComp = components.lastOrNull { comp ->
                                        val w = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                                        val h = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                                        startOffset.x >= comp.x - 15f && startOffset.x <= comp.x + w + 15f &&
                                                startOffset.y >= comp.y - 15f && startOffset.y <= comp.y + h + 15f
                                    }
                                    if (draggedComp != null) {
                                        selectedComp = draggedComp
                                        isPanningCanvas = false
                                    } else {
                                        // Jika menyeret ruang kosong di kanvas -> Geser Kamera Kanvas (Pan)!
                                        isPanningCanvas = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val scaledDx = dragAmount.x / canvasZoom
                                    val scaledDy = dragAmount.y / canvasZoom

                                    val w = draggedWire
                                    if (w != null) {
                                        if (draggingSecondHandle) {
                                            w.bend2X += scaledDx
                                            w.bend2Y += scaledDy
                                        } else {
                                            w.bendX += scaledDx
                                            w.bendY += scaledDy
                                        }
                                        return@detectDragGestures
                                    }
                                    val c = draggedComp
                                    if (c != null) {
                                        c.x = (c.x + scaledDx).coerceIn(-120f, 1100f)
                                        c.y = (c.y + scaledDy).coerceIn(-120f, 800f)
                                    } else if (isPanningCanvas) {
                                        canvasPanX = (canvasPanX + dragAmount.x).coerceIn(-450f, 350f)
                                        canvasPanY = (canvasPanY + dragAmount.y).coerceIn(-350f, 350f)
                                    }
                                },
                                onDragEnd = {
                                    draggedComp = null
                                    draggedWire = null
                                    isPanningCanvas = false
                                }
                            )
                        }
                ) {
                    canvasSize = size
                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }

                    // Transformasi Zoom & Pan Kamera Kanvas
                    withTransform({
                        translate(left = canvasPanX, top = canvasPanY)
                        scale(scaleX = canvasZoom, scaleY = canvasZoom, pivot = Offset.Zero)
                    }) {
                        // Grid Titik Breadboard
                        val step = 32f
                        for (gx in 0..28) {
                            for (gy in 0..16) {
                                drawCircle(Color(0xFF15251E), radius = 2f, center = Offset(gx * step, gy * step))
                            }
                        }

                        // 1. Gambar Semua Komponen
                        components.forEach { comp ->
                            val isPowered = checkPowered(comp)
                            if (selectedComp?.id == comp.id && !isWireEditMode) {
                                val boxW = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                                val boxH = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                                drawRoundRect(
                                    color = CopperGold.copy(alpha = 0.35f),
                                    topLeft = Offset(comp.x - 6f, comp.y - 6f),
                                    size = Size(boxW + 12f, boxH + 12f),
                                    cornerRadius = CornerRadius(12f, 12f),
                                    style = Stroke(width = 3f)
                                )
                            }
                            when (comp.type) {
                                SimCompType.ESP32_BOARD -> drawMcuVariantBoard(comp, selectedBoardVariant, isSimRunning, isVirtualWifiConnected, pendingPinSource)
                                SimCompType.RESISTOR_4BAND -> drawResistor4BandWithPins(comp, resistorOhms, pendingPinSource)
                                SimCompType.LED_LAMP -> drawLedWithPins(comp, ledRedOn && isPowered, resistorOhms, isPowered, pendingPinSource)
                                SimCompType.DHT22_SENSOR -> drawDht22With4Pins(comp, dhtTempC, dhtHumPct, isSimRunning, animPhase, isPowered, pendingPinSource)
                                SimCompType.OLED_SSD1306 -> drawOledWith4Pins(comp, isSimRunning && isPowered, oledLine1, oledLine2, isPowered, pendingPinSource)
                                SimCompType.LCD_16X2_I2C -> drawLcd16x2With4Pins(comp, isSimRunning && isPowered, oledLine1, oledLine2, isPowered, pendingPinSource)
                                SimCompType.SERVO_SG90 -> drawServoWith3Pins(comp, if (isPowered) servoAngle else 0f, isPowered, pendingPinSource)
                                SimCompType.HCSR04_SONAR -> drawHcsr04With4Pins(comp, ultrasonicCm, isSimRunning, animPhase, isPowered, pendingPinSource)
                                SimCompType.PIR_MOTION -> drawPirSensorWith3Pins(comp, isSimRunning, animPhase, isPowered, pendingPinSource)
                                SimCompType.SOIL_MOISTURE -> drawSoilSensorWith3Pins(comp, soilMoisturePct, isSimRunning, animPhase, isPowered, pendingPinSource)
                                SimCompType.LDR_LIGHT -> drawLdrSensorWith3Pins(comp, ldrLuxVal, isSimRunning, animPhase, isPowered, pendingPinSource)
                                SimCompType.NEOPIXEL_RING -> drawNeoPixelRingWith3Pins(comp, isSimRunning, simTick, isPowered, pendingPinSource)
                                SimCompType.STEPPER_MOTOR -> drawStepperWith4Pins(comp, if (isSimRunning && isPowered) stepperAngle else 0f, isPowered, pendingPinSource)
                                SimCompType.RELAY_1CH -> drawRelayWith3Pins(comp, relayActive && isPowered, isPowered, pendingPinSource)
                                SimCompType.BUZZER_ACTIVE -> drawBuzzerWith2Pins(comp, buzzerActive && isPowered, animPhase, isPowered, pendingPinSource)
                                SimCompType.POTENTIOMETER -> drawPotentiometerWith3Pins(comp, potPercent, isPowered, pendingPinSource)
                                SimCompType.PUSH_BUTTON -> drawPushButtonWith2Pins(comp, isSimRunning && (simTick % 2 == 1), isPowered, pendingPinSource)
                            }
                        }

                        // 2. Gambar Semua Kabel Siku-Siku (Orthogonal 90°)
                        if (mcuComp != null) {
                            wires.forEachIndexed { idx, wire ->
                                val sourceComp = components.find { it.id == wire.fromCompId }
                                if (sourceComp != null) {
                                    val startPt = WokwiSimRenderEngine.getComponentPinOffset(sourceComp, wire.fromPinName)
                                    val endPt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, wire.toMcuPin)
                                    drawFlexibleWire(
                                        wire = wire,
                                        start = startPt,
                                        end = endPt,
                                        channelIndex = idx,
                                        showWaypointHandles = isWireEditMode,
                                        isSelectedWire = isWireEditMode && selectedWire?.id == wire.id
                                    )
                                }
                            }
                        }
                    }
                }

                // TOMBOL FLOATING BESAR "+ MODUL" DI POJOK KANAN BAWAH KANVAS (PASTI TERLIHAT!)
                ExtendedFloatingActionButton(
                    onClick = { showAddPartDialog = true },
                    containerColor = CopperGold,
                    contentColor = SiliconDark,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .height(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah / Pilih Modul (16)", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                // Banner Petunjuk di Kiri Atas Kanvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(SiliconDark.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (pendingPinSource != null) "🟢 Pin [${pendingPinSource!!.compLabel}:${pendingPinSource!!.pinName}] -> Ketuk Pin Tujuan!"
                        else "👆 Klik komponen untuk Edit/Rotate/Mirror/Hapus • Klik titik kaki untuk sambung kabel siku",
                        color = if (pendingPinSource != null) Color(0xFF00E676) else CopperGold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // =================================================================
        // 3. PANEL INSPEKTOR KOMPONEN TERPILIH (EDIT, ROTATE 90°, MIRROR PIN, & HAPUS!)
        // =================================================================
        if (selectedComp != null) {
            val activeComp = selectedComp!!
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🎯 ${activeComp.label}",
                        color = CopperGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )

                    // Tombol EDIT Parameter Komponen
                    Button(
                        onClick = { showEditCompDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Edit Nilai", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }

                    // Tombol ROTATE 90 DERAJAT
                    Button(
                        onClick = { activeComp.rotationDeg = (activeComp.rotationDeg + 90) % 360 },
                        colors = ButtonDefaults.buttonColors(containerColor = LogicCyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Rotate (${activeComp.rotationDeg}°)", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }

                    // Tombol MIRROR PIN (FLIP URUTAN KAKI KIRI <-> KANAN)
                    Button(
                        onClick = { activeComp.isMirrored = !activeComp.isMirrored },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeComp.isMirrored) Color(0xFF00E676) else LogicViolet
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (activeComp.isMirrored) "Mirror: ON" else "Mirror Pin", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                    }

                    // Tombol HAPUS KOMPONEN & KABELNYA DARI LAYAR KANVAS!
                    if (activeComp.type != SimCompType.ESP32_BOARD) {
                        Button(
                            onClick = {
                                val targetId = activeComp.id
                                onWiresChange(wires.filterNot { it.fromCompId == targetId })
                                components.remove(activeComp)
                                selectedComp = components.firstOrNull()
                                Toast.makeText(context, "🗑️ ${activeComp.label} & kabelnya dihapus dari kanvas!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Hapus", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                        }
                    } else {
                        Button(
                            onClick = { showBoardPickerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Ganti Board MCU", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Bar Kontrol Kabel saat Mode Geser Kabel Aktif
        if (isWireEditMode && selectedWire != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !selectedWire!!.isCurvedStyle,
                            onClick = { selectedWire!!.isCurvedStyle = !selectedWire!!.isCurvedStyle },
                            label = {
                                Text(
                                    text = if (!selectedWire!!.isCurvedStyle) "📐 Kabel Siku 90°" else "〰️ Kabel Lengkung",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                        IconButton(
                            onClick = {
                                onWiresChange(wires.filterNot { it.id == selectedWire!!.id })
                                selectedWire = wires.firstOrNull()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Kabel", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        JumperWireColor.entries.take(7).forEach { wc ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(wc.color)
                                    .border(if (selectedWire!!.wireColor == wc.color) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedWire!!.wireColor = wc.color }
                            )
                        }
                    }
                }
            }
        }

        // 4. LIVE SERIAL MONITOR DRAWER
        if (showLiveSerialDrawer) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .border(1.dp, PcbEmeraldMedium, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("💻 LIVE SERIAL MONITOR (115200 bps)", color = CopperGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Bersihkan", color = LogicCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onClearSerialLogs() })
                            Text("Tutup [X]", color = Color(0xFFFF5252), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onToggleSerialDrawer() })
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(simSerialLogs) { line ->
                            Text(line, color = LogicNeonGreen, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }

        // 5. PANEL BAWAH RINGKAS: PILIHAN RESISTOR & SLIDER FISIKA
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🎨 Resistor: ${ResistorCatalog.formatOhmLabel(resistorOhms)}", fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 11.sp)
                    Text(ResistorCatalog.getFourBandNames(resistorOhms), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CopperOrange, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ResistorCatalog.standardValuesOhms.forEach { ohm ->
                        FilterChip(
                            selected = resistorOhms == ohm,
                            onClick = { onResistorChange(ohm) },
                            label = { Text(ResistorCatalog.formatOhmLabel(ohm), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PcbEmeraldDark,
                                selectedLabelColor = CopperGold
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🌡️ Suhu DHT22: ${String.format("%.1f", dhtTempC)}°C", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = PcbEmeraldDark)
                    Text("🚶 Jarak Orang: ${ultrasonicCm.toInt()} cm", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = LogicCyan)
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
            }
        }
    }

    // =====================================================================
    // DIALOG 1: EDIT KOMPONEN SPESIFIK YANG DIKLIK DI KANVAS!
    // =====================================================================
    if (showEditCompDialog && selectedComp != null) {
        val target = selectedComp!!
        var tempLabel by remember(target) { mutableStateOf(target.label) }

        AlertDialog(
            onDismissRequest = { showEditCompDialog = false },
            title = { Text("⚙️ Edit Komponen: ${target.type.title}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it; target.label = it },
                        label = { Text("Nama / Label Komponen di Kanvas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { target.rotationDeg = (target.rotationDeg + 90) % 360 },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Rotate (${target.rotationDeg}°)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { target.isMirrored = !target.isMirrored },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (target.isMirrored) "🪞 Mirror: ON" else "🪞 Mirror Pin", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Pengaturan Khusus Sesuai Jenis Komponen yang Diklik!
                    when (target.type) {
                        SimCompType.RESISTOR_4BAND -> {
                            Text("Pilih Ukuran Hambatan Resistor (${ResistorCatalog.formatOhmLabel(resistorOhms)}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ResistorCatalog.standardValuesOhms.forEach { ohm ->
                                    FilterChip(
                                        selected = resistorOhms == ohm,
                                        onClick = { onResistorChange(ohm) },
                                        label = { Text(ResistorCatalog.formatOhmLabel(ohm), fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        SimCompType.DHT22_SENSOR -> {
                            Text("Atur Suhu Simulasi DHT22 (${String.format("%.1f", dhtTempC)} °C):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = dhtTempC, onValueChange = onDhtTempChange, valueRange = 15f..60f)
                        }
                        SimCompType.HCSR04_SONAR, SimCompType.PIR_MOTION -> {
                            Text("Atur Jarak Target Orang (${ultrasonicCm.toInt()} cm):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = ultrasonicCm, onValueChange = onUltrasonicChange, valueRange = 2f..200f)
                        }
                        SimCompType.SOIL_MOISTURE -> {
                            Text("Atur Kelembaban Tanah (${soilMoisturePct.toInt()}%):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = soilMoisturePct, onValueChange = onSoilMoistureChange, valueRange = 0f..100f)
                        }
                        SimCompType.LDR_LIGHT -> {
                            Text("Atur Intensitas Cahaya LDR (${ldrLuxVal.toInt()} Lux):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = ldrLuxVal, onValueChange = onLdrLuxChange, valueRange = 10f..1000f)
                        }
                        SimCompType.SERVO_SG90 -> {
                            Text("Atur Sudut Awal Servo SG90 (${servoAngle.toInt()}°):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = servoAngle, onValueChange = onServoAngleChange, valueRange = 0f..180f)
                        }
                        SimCompType.POTENTIOMETER -> {
                            Text("Putaran Potensiometer (${potPercent.toInt()}%):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Slider(value = potPercent, onValueChange = onPotChange, valueRange = 0f..100f)
                        }
                        SimCompType.OLED_SSD1306, SimCompType.LCD_16X2_I2C -> {
                            OutlinedTextField(
                                value = oledLine1,
                                onValueChange = onOledLine1Change,
                                label = { Text("Teks Layar Baris 1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {}
                    }

                    if (target.type != SimCompType.ESP32_BOARD) {
                        Button(
                            onClick = {
                                onWiresChange(wires.filterNot { it.fromCompId == target.id })
                                components.remove(target)
                                selectedComp = components.firstOrNull()
                                showEditCompDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus Komponen Ini dari Kanvas", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEditCompDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark)
                ) {
                    Text("Simpan", color = CopperGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // =====================================================================
    // DIALOG 2: GALERI KATALOG 16 MODUL BERGAMBAR (VISUAL PREVIEW + KLIK LANGSUNG GANTI RANGKAIAN!)
    // =====================================================================
    if (showAddPartDialog) {
        val addableTypes = SimCompType.entries.filter { it != SimCompType.ESP32_BOARD }
        // Default true sesuai permintaan: Ketika modul ditekan, rangkaian & kabelnya otomatis menggantikan kanvas!
        var replaceCanvasMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddPartDialog = false },
            title = {
                Column {
                    Text("📦 Katalog 16 Modul & Preview Komponen", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        FilterChip(
                            selected = replaceCanvasMode,
                            onClick = { replaceCanvasMode = true },
                            label = { Text("⚡ Klik = Rakit & Ganti Kanvas", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold) }
                        )
                        FilterChip(
                            selected = !replaceCanvasMode,
                            onClick = { replaceCanvasMode = false },
                            label = { Text("➕ Tambah Saja", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    addableTypes.forEach { type ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1713)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, PcbEmeraldMedium, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (replaceCanvasMode) {
                                        showAddPartDialog = false
                                        onReplaceCircuitWithModule(type)
                                    } else {
                                        val idx = components.size
                                        val newComp = SimComponent(
                                            id = "${type.name.lowercase()}_$idx",
                                            type = type,
                                            initialLabel = type.title.substringBefore(" "),
                                            initialX = 255f,
                                            initialY = 30f
                                        )
                                        components.add(newComp)
                                        WokwiSimRenderEngine.autoArrangeNeatGrid(components, 760f, 340f)
                                        selectedComp = newComp
                                        showAddPartDialog = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // KOTAK PREVIEW GAMBAR ASLI KOMPONEN!
                                Box(
                                    modifier = Modifier
                                        .size(width = 115.dp, height = 92.dp)
                                        .background(Color(0xFF070C0A), RoundedCornerShape(10.dp))
                                        .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val previewComp = SimComponent("prev", type, type.title.take(8), 8f, 6f)
                                        withTransform({
                                            scale(scaleX = 0.62f, scaleY = 0.62f, pivot = Offset(10f, 10f))
                                        }) {
                                            when (type) {
                                                SimCompType.RESISTOR_4BAND -> drawResistor4BandWithPins(previewComp, 220, null)
                                                SimCompType.LED_LAMP -> drawLedWithPins(previewComp, true, 220, true, null)
                                                SimCompType.DHT22_SENSOR -> drawDht22With4Pins(previewComp, 32.5f, 65f, true, 0.3f, true, null)
                                                SimCompType.OLED_SSD1306 -> drawOledWith4Pins(previewComp, true, "OLED 0.96\"", "I2C 0x3C", true, null)
                                                SimCompType.LCD_16X2_I2C -> drawLcd16x2With4Pins(previewComp, true, "LCD 16x2 I2C", "READY 0x27", true, null)
                                                SimCompType.SERVO_SG90 -> drawServoWith3Pins(previewComp, 120f, true, null)
                                                SimCompType.HCSR04_SONAR -> drawHcsr04With4Pins(previewComp, 45f, true, 0.3f, true, null)
                                                SimCompType.PIR_MOTION -> drawPirSensorWith3Pins(previewComp, true, 0.3f, true, null)
                                                SimCompType.SOIL_MOISTURE -> drawSoilSensorWith3Pins(previewComp, 68f, true, 0.3f, true, null)
                                                SimCompType.LDR_LIGHT -> drawLdrSensorWith3Pins(previewComp, 380f, true, 0.3f, true, null)
                                                SimCompType.NEOPIXEL_RING -> drawNeoPixelRingWith3Pins(previewComp, true, 2, true, null)
                                                SimCompType.STEPPER_MOTOR -> drawStepperWith4Pins(previewComp, 45f, true, null)
                                                SimCompType.RELAY_1CH -> drawRelayWith3Pins(previewComp, true, true, null)
                                                SimCompType.BUZZER_ACTIVE -> drawBuzzerWith2Pins(previewComp, true, 0.3f, true, null)
                                                SimCompType.POTENTIOMETER -> drawPotentiometerWith3Pins(previewComp, 50f, true, null)
                                                SimCompType.PUSH_BUTTON -> drawPushButtonWith2Pins(previewComp, true, true, null)
                                                else -> {}
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(type.title, color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    Text(type.shortDesc, color = Color.White, fontSize = 11.sp)
                                    Text(
                                        text = "Kaki Pin: ${type.pinNames.joinToString(" • ")}",
                                        color = LogicCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Surface(
                                        color = if (replaceCanvasMode) CopperOrange else PcbEmeraldMedium,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = if (replaceCanvasMode) "⚡ Ketuk -> Langsung Rakit & Pasang Kabel" else "➕ Ketuk -> Tambah ke Kanvas",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
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

    // DIALOG PILIH 8 VARIAN MIKROKONTROLER
    if (showBoardPickerDialog) {
        AlertDialog(
            onDismissRequest = { showBoardPickerDialog = false },
            title = { Text("🎛️ Pilih Board Mikrokontroler", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    McuBoardVariant.entries.forEach { variant ->
                        OutlinedButton(
                            onClick = {
                                onSelectBoardVariant(variant)
                                showBoardPickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(variant.displayName, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 12.sp)
                                    Text("${variant.totalPhysicalPins} Pin", fontWeight = FontWeight.Bold, color = CopperOrange, fontSize = 11.sp)
                                }
                                Text("Chip: ${variant.chipName}", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBoardPickerDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
fun WokwiCodeEditorPanel(
    codeLang: String,
    onLangChange: (String) -> Unit,
    codeText: String,
    onCodeChange: (String) -> Unit,
    onOpenAiProjectBuilder: () -> Unit,
    onRunNow: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📶 VIRTUAL WI-FI BLYNK CREDENTIALS:",
                            color = CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\" | PASS: \"${BambuVirtualNetwork.VIRTUAL_PASS}\"",
                            color = LogicNeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onOpenAiProjectBuilder,
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Buat Proyek", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = codeLang == "CPP",
                        onClick = { onLangChange("CPP") },
                        label = { Text("Arduino C++ (.ino)", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
                    )
                    FilterChip(
                        selected = codeLang == "MICROPYTHON",
                        onClick = { onLangChange("MICROPYTHON") },
                        label = { Text("MicroPython (.py)", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRunNow,
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = CopperGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Compile & Run",
                        color = CopperGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            OutlinedTextField(
                value = codeText,
                onValueChange = onCodeChange,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, color = LogicNeonGreen),
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
fun BlynkDashboardPanel(
    widgets: List<DashWidget>,
    onWidgetsChange: (List<DashWidget>) -> Unit,
    isSimRunning: Boolean,
    isVirtualWifiConnected: Boolean,
    selectedColor: Color,
    onColorChange: (Color) -> Unit,
    dhtTempC: Float,
    ultrasonicCm: Float
) {
    var showAddBlynkDialog by remember { mutableStateOf(false) }
    val chartPath = remember { Path() }

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
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = if (isVirtualWifiConnected) Color(0xFF00E676) else CopperGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isVirtualWifiConnected) "🟢 BLYNK CLOUD ONLINE (VIRTUAL AP)" else "⚪ BLYNK VIRTUAL AP STANDBY",
                                color = if (isVirtualWifiConnected) Color(0xFF00E676) else CopperGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\" | Pass: \"${BambuVirtualNetwork.VIRTUAL_PASS}\"",
                                color = LogicCyan,
                                fontFamily = FontFamily.Monospace,
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
                        Text("+ Widget", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
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
                                            chartPath.reset()
                                            pts.forEachIndexed { i, v ->
                                                val xPos = (i / (pts.size - 1).toFloat()) * w
                                                val yPos = h - (v / 80f).coerceIn(0f, 1f) * h
                                                if (i == 0) chartPath.moveTo(xPos, yPos) else chartPath.lineTo(xPos, yPos)
                                            }
                                            drawPath(chartPath, color = widget.color, style = Stroke(3.5f))
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
                                            text = if (isVirtualWifiConnected) "> WIFI: ${BambuVirtualNetwork.VIRTUAL_SSID}\n> V5=${String.format("%.1f", dhtTempC)}C" else "> OFFLINE: Cek SSID/Pass di Koding",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = if (isVirtualWifiConnected) LogicNeonGreen else CopperGold
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

@Composable
fun BuiltInLibraryCatalogView() {
    val libs = remember {
        listOf(
            BuiltInLibInfo("<WiFi.h> & <ESP8266WiFi.h>", "C++", "Networking", "Koneksi Virtual Wi-Fi (${BambuVirtualNetwork.VIRTUAL_SSID}) untuk ESP32 & ESP8266", "WiFi.begin(ssid, pass); WiFi.localIP();"),
            BuiltInLibInfo("<PubSubClient.h>", "C++", "IoT MQTT", "Klien Broker MQTT Port 1883 Publish & Subscribe", "client.connect(\"ESP32\"); client.publish(topic, payload);"),
            BuiltInLibInfo("<BlynkSimpleEsp32.h>", "C++", "Blynk Cloud", "Integrasi Virtual Pin V1-V12 dengan Token ${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}", "Blynk.begin(auth, ssid, pass); Blynk.virtualWrite(V5, t);"),
            BuiltInLibInfo("<Wire.h> & <SPI.h>", "C++", "Core Bus", "Komunikasi I2C (SDA=D21, SCL=D22) & SPI High-Speed", "Wire.begin(21, 22); SPI.begin();"),
            BuiltInLibInfo("<Adafruit_SSD1306.h> & <Adafruit_GFX.h>", "C++", "Display", "Layar OLED 0.96\" & 1.3\" 128x64 I2C (Alamat 0x3C)", "display.println(\"TEKS\"); display.display();"),
            BuiltInLibInfo("<LiquidCrystal_I2C.h>", "C++", "Display", "Layar LCD Karakter 16x2 & 20x4 I2C PCF8574 (0x27)", "lcd.init(); lcd.backlight(); lcd.print(\"HELLO\");"),
            BuiltInLibInfo("<DHT.h> (Adafruit Unified)", "C++", "Env Sensor", "Sensor Suhu & Kelembaban Udara DHT11 / DHT22", "dht.begin(); float t = dht.readTemperature();"),
            BuiltInLibInfo("<Adafruit_BME280.h> & <BMP280.h>", "C++", "Env Sensor", "Sensor Suhu, Tekanan Barometrik & Ketinggian I2C (0x76)", "bme.readTemperature(); bme.readAltitude(1013.25);"),
            BuiltInLibInfo("<OneWire.h> & <DallasTemperature.h>", "C++", "Temp Probe", "Sensor Suhu Air/Tanah Tahan Air DS18B20", "sensors.requestTemperatures(); sensors.getTempCByIndex(0);"),
            BuiltInLibInfo("<NewPing.h>", "C++", "Ultrasonic", "Sensor Jarak Ultrasonik HC-SR04 & JSN-SR04T Waterproof", "NewPing sonar(TRIG, ECHO, 200); sonar.ping_cm();"),
            BuiltInLibInfo("<ESP32Servo.h> & <Servo.h>", "C++", "Actuator", "Kontrol Sudut Motor Servo SG90 / MG996R PWM (0-180°)", "servo.attach(18); servo.write(135);"),
            BuiltInLibInfo("<Adafruit_NeoPixel.h> & <FastLED.h>", "C++", "RGB LED", "Kontrol Cincin & Strip LED RGB WS2812B", "ring.setPixelColor(0, ring.Color(255, 0, 0)); ring.show();"),
            BuiltInLibInfo("<Stepper.h> & <AccelStepper.h>", "C++", "Motor", "Kontrol Presisi Stepper Motor 28BYJ-48 / NEMA17", "stepper.setSpeed(60); stepper.step(2048);"),
            BuiltInLibInfo("<Adafruit_MPU6050.h>", "C++", "IMU Sensor", "Sensor Gyroscope & Accelerometer 6-Axis I2C (0x68)", "mpu.getEvent(&a, &g, &temp);"),
            BuiltInLibInfo("<BH1750.h>", "C++", "Optical", "Sensor Intensitas Cahaya Matahari Digital Lux (0x23)", "float lux = lightMeter.readLightLevel();"),
            BuiltInLibInfo("<Adafruit_INA219.h>", "C++", "Power Meter", "Sensor Tegangan DC & Arus Baterai/Solar Panel I2C (0x40)", "float mA = ina219.getCurrent_mA();"),
            BuiltInLibInfo("<ModbusMaster.h>", "C++", "Industrial", "Komunikasi RS485 Modbus RTU (Sensor NPK Tanah / PLC)", "node.readHoldingRegisters(0x0000, 4);"),
            BuiltInLibInfo("machine (Pin, ADC, PWM, I2C, UART)", "MicroPython", "Core Hardware", "Akses Langsung Register GPIO, PWM, & ADC 12-Bit", "led = Pin(2, Pin.OUT); adc = ADC(Pin(34))"),
            BuiltInLibInfo("ssd1306, dht, hcsr04 & neopixel", "MicroPython", "Display/Sensor", "Driver OLED I2C, DHT22, Ultrasonik & NeoPixel Ring", "oled.text('PANDAKODE', 0, 0); np[0] = (255,0,0)"),
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