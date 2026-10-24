package com.pandakode.bambumqtt.ui.screens

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawBuzzerWith2Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawDht22With4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawHcsr04With4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawLedWithPins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawMcuVariantBoard
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawOledWith4Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawOrthogonalWire
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawPotentiometerWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawRelayWith3Pins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawResistor4BandWithPins
import com.pandakode.bambumqtt.ui.screens.WokwiSimRenderEngine.drawServoWith3Pins
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WokwiSimScreen() {
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Kanvas Sirkuit", "Blynk IoT Dash", "Koding C++/Py", "20+ Library")

    // Varian Board MCU yang dipilih (8 Pilihan Board!)
    var selectedBoardVariant by remember { mutableStateOf(McuBoardVariant.ESP32_30PIN) }
    var isSimRunning by remember { mutableStateOf(false) }
    var showLiveSerialDrawer by remember { mutableStateOf(true) }
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

    // State Koneksi Virtual Wi-Fi & Blynk Cloud
    var isVirtualWifiConnected by remember { mutableStateOf(false) }
    var simSerialLogs by remember {
        mutableStateOf(
            listOf(
                "[SYSTEM] BambuSim Virtual MCU Ready (${McuBoardVariant.ESP32_30PIN.displayName})",
                "[NET] Gunakan SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\" & Pass: \"${BambuVirtualNetwork.VIRTUAL_PASS}\" untuk konek ke Blynk!"
            )
        )
    }

    fun appendSimSerial(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SS", Locale.getDefault()).format(Date())
        simSerialLogs = (simSerialLogs + "[$time] $msg").takeLast(60)
    }

    // 12 Widget Lengkap ala Blynk IoT
    var dashboardWidgets by remember {
        mutableStateOf(
            listOf(
                DashWidget("dw1", DashWidgetType.TOGGLE_SWITCH, "Pompa Irigasi (V1)", "bambu/relay/pump", "V1 / D2", isOn = false, color = Color(0xFF00E676)),
                DashWidget("dw2", DashWidgetType.PUSH_BUTTON, "Alarm Buzzer (V2)", "bambu/act/beep", "V2 / D5", color = Color(0xFFFF1744)),
                DashWidget("dw3", DashWidgetType.KNOB_ROTARY, "Sudut Servo (V3)", "bambu/pwm/servo", "V3 / D18", value = 90f, max = 180f, unit = "°", color = CopperOrange),
                DashWidget("dw4", DashWidgetType.SLIDER_LEVEL, "Dimmer PWM (V4)", "bambu/pwm/led", "V4 / D2", value = 200f, max = 255f, color = Color(0xFF00E5FF)),
                DashWidget("dw5", DashWidgetType.GAUGE_RADIAL, "Suhu DHT22 (V5)", "bambu/sensor/temp", "V5 / D4", value = 31.4f, max = 100f, unit = "°C", color = Color(0xFFFF5252)),
                DashWidget("dw6", DashWidgetType.VALUE_DISPLAY, "Jarak Sonar (V6)", "bambu/sensor/sonar", "V6 / D19", value = 45f, max = 200f, unit = " cm", color = Color(0xFF2979FF)),
                DashWidget("dw7", DashWidgetType.LED_INDICATOR, "Status Overheat (V7)", "bambu/status/heat", "V7", isOn = false, color = Color(0xFFFF1744)),
                DashWidget("dw8", DashWidgetType.RGB_COLOR_PICKER, "RGB NeoPixel (V8)", "bambu/color", "V8", color = Color.Red),
                DashWidget("dw9", DashWidgetType.STEP_INPUT, "Setpoint Thermostat", "bambu/setpoint", "V9", value = 36f, max = 80f, unit = "°C", color = Color(0xFFE040FB)),
                DashWidget("dw10", DashWidgetType.HISTORICAL_CHART, "SuperChart Suhu", "bambu/history/temp", "V10", value = 31.4f, max = 100f, color = Color(0xFF00E5FF)),
                DashWidget("dw11", DashWidgetType.LCD_16X2, "Blynk LCD 16x2 (V11)", "bambu/lcd", "V11", value = 31.4f, color = Color(0xFF00E676)),
                DashWidget("dw12", DashWidgetType.TERMINAL_MINI, "Cloud Terminal (V12)", "bambu/term", "V12", color = CopperGold)
            )
        )
    }

    // Posisi Awal Komponen yang Sudah Tersusun Rapi (3 Kolom Grid)
    val components = remember {
        mutableStateListOf(
            SimComponent("mcu", SimCompType.ESP32_BOARD, "MCU", 12f, 22f),
            SimComponent("res1", SimCompType.RESISTOR_4BAND, "Resistor", 248f, 18f),
            SimComponent("led1", SimCompType.LED_LAMP, "LED Merah", 420f, 18f),
            SimComponent("dht1", SimCompType.DHT22_SENSOR, "DHT22", 592f, 18f),
            SimComponent("oled1", SimCompType.OLED_SSD1306, "OLED I2C", 248f, 156f),
            SimComponent("servo1", SimCompType.SERVO_SG90, "Servo SG90", 420f, 156f)
        )
    }

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
            SimCompType.OLED_SSD1306 -> {
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
            SimCompType.RELAY_1CH -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "IN" && isSignalPin(it.toMcuPin) }
            }
            SimCompType.BUZZER_ACTIVE -> {
                compWires.any { it.fromPinName == "VCC+" && isSignalPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND-" && it.toMcuPin == "GND" }
            }
            SimCompType.POTENTIOMETER -> {
                compWires.any { it.fromPinName == "VCC" && isPowerPin(it.toMcuPin) } &&
                        compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" } &&
                        compWires.any { it.fromPinName == "OUT" && isSignalPin(it.toMcuPin) }
            }
        }
    }

    var codeLang by remember { mutableStateOf("CPP") }
    var simCodeText by remember {
        mutableStateOf(
            """
            #include <WiFi.h>
            #include <BlynkSimpleEsp32.h>
            #include <Wire.h>
            #include <Adafruit_SSD1306.h>
            #include <DHT.h>
            #include <ESP32Servo.h>

            // Kredensial Virtual Wi-Fi & Blynk Khusus BambuSim:
            char auth[] = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}";
            char ssid[] = "${BambuVirtualNetwork.VIRTUAL_SSID}";
            char pass[] = "${BambuVirtualNetwork.VIRTUAL_PASS}";

            void setup() {
              Serial.begin(115200);
              Blynk.begin(auth, ssid, pass);
              pinMode(2, OUTPUT);
              display.println("PANDAKODE IOT");
              servo.write(135);
            }

            void loop() {
              Blynk.run();
              digitalWrite(2, HIGH);
              Serial.println("Telemetry Sent to Blynk V5 & OLED!");
              delay(1000);
            }
            """.trimIndent()
        )
    }

    // Engine Eksekusi Sirkuit, Live Serial Monitor & Virtual Wi-Fi Blynk
    LaunchedEffect(isSimRunning) {
        if (!isSimRunning) {
            isVirtualWifiConnected = false
            return@LaunchedEffect
        }
        showLiveSerialDrawer = true
        simSerialLogs = emptyList()
        appendSimSerial("Booting ${selectedBoardVariant.displayName} (${selectedBoardVariant.chipName})...")
        delay(350L)

        // Cek apakah koding menggunakan SSID & Password Virtual Wi-Fi kita
        val hasValidSsid = simCodeText.contains(BambuVirtualNetwork.VIRTUAL_SSID)
        val hasValidPass = simCodeText.contains(BambuVirtualNetwork.VIRTUAL_PASS)
        if (hasValidSsid && hasValidPass) {
            appendSimSerial("Connecting to Virtual Wi-Fi SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\"...")
            delay(400L)
            isVirtualWifiConnected = true
            appendSimSerial("✅ Wi-Fi Connected! Virtual IP: 10.10.88.24 | Gateway: 10.10.88.1")
            appendSimSerial("☁️ Blynk Auth Token Verified (${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}) -> ONLINE!")
        } else {
            isVirtualWifiConnected = false
            appendSimSerial("⚠️ Virtual Wi-Fi Offline (Gunakan SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\" & Pass: \"${BambuVirtualNetwork.VIRTUAL_PASS}\")")
        }

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

            // Ekstrak isi Serial.println(...) atau print(...) dari kode pengguna untuk ditampilkan di Live Serial Monitor!
            val serialPrintMatch = Regex("""(?:Serial\.println|print)\(\s*"([^"]+)"""").find(simCodeText)
            val userPrint = serialPrintMatch?.groupValues?.get(1) ?: "Loop tick"
            appendSimSerial("$userPrint | T=${String.format("%.1f", dhtTempC)}°C | R=${ResistorCatalog.formatOhmLabel(resistorOhms)} | LED=${if (ledRedActive) "ON" else "OFF"}")

            delay(950L)
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
                selectedBoardVariant = selectedBoardVariant,
                onSelectBoardVariant = { newVariant ->
                    selectedBoardVariant = newVariant
                    appendSimSerial("Board diganti ke: ${newVariant.displayName} (${newVariant.totalPhysicalPins} Pin)")
                },
                components = components,
                wires = wires,
                onWiresChange = { wires = it },
                checkPowered = { isComponentPoweredAndWired(it) },
                isSimRunning = isSimRunning,
                isVirtualWifiConnected = isVirtualWifiConnected,
                onToggleRun = {
                    isSimRunning = !isSimRunning
                    if (!isSimRunning) {
                        ledRedActive = false
                        appendSimSerial("🛑 Simulasi dihentikan oleh pengguna.")
                    }
                },
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
                onDhtTempChange = { dhtTempC = it },
                ultrasonicCm = ultrasonicCm,
                onUltrasonicChange = { ultrasonicCm = it },
                potPercent = potPercent,
                servoAngle = servoAngle,
                oledLine1 = oledLine1,
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
                onLangChange = { lang ->
                    codeLang = lang
                    simCodeText = if (lang == "CPP") {
                        """
                        #include <WiFi.h>
                        #include <BlynkSimpleEsp32.h>
                        #include <Wire.h>
                        #include <Adafruit_SSD1306.h>
                        #include <DHT.h>
                        #include <ESP32Servo.h>

                        char auth[] = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}";
                        char ssid[] = "${BambuVirtualNetwork.VIRTUAL_SSID}";
                        char pass[] = "${BambuVirtualNetwork.VIRTUAL_PASS}";

                        void setup() {
                          Serial.begin(115200);
                          Blynk.begin(auth, ssid, pass);
                          pinMode(2, OUTPUT);
                          display.println("BAMBUSIM C++");
                          servo.write(120);
                        }

                        void loop() {
                          Blynk.run();
                          digitalWrite(2, HIGH);
                          Serial.println("ESP32 Telemetry OK!");
                          delay(1000);
                        }
                        """.trimIndent()
                    } else {
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
                            print("MicroPython Tick Sent to Blynk!")
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

@Composable
fun WokwiZeroLagWorkbench(
    selectedBoardVariant: McuBoardVariant,
    onSelectBoardVariant: (McuBoardVariant) -> Unit,
    components: MutableList<SimComponent>,
    wires: List<classPinWireConnection>,
    onWiresChange: (List<classPinWireConnection>) -> Unit,
    checkPowered: (SimComponent) -> Boolean,
    isSimRunning: Boolean,
    isVirtualWifiConnected: Boolean,
    onToggleRun: () -> Unit,
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
    onDhtTempChange: (Float) -> Unit,
    ultrasonicCm: Float,
    onUltrasonicChange: (Float) -> Unit,
    potPercent: Float,
    servoAngle: Float,
    oledLine1: String,
    oledLine2: String
) {
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showBoardPickerDialog by remember { mutableStateOf(false) }
    var showWireManagerDialog by remember { mutableStateOf(false) }
    var isWireEditMode by remember { mutableStateOf(false) } // Mode Geser Titik Belok & Ganti Warna Kabel!
    var selectedWire by remember { mutableStateOf<classPinWireConnection?>(wires.firstOrNull()) }
    var selectedComp by remember { mutableStateOf<SimComponent?>(components.getOrNull(1)) }
    var canvasSize by remember { mutableStateOf(Size(800f, 360f)) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. TOOLBAR ATAS: PILIH BOARD, + PART, KABEL, MODE GESER KABEL, AUTO-RAPIKAN, & RUN
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
                        onClick = { showBoardPickerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(selectedBoardVariant.displayName.substringBefore(" ("), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showAddPartDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Sensor/Part", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showWireManagerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LogicCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔌 Pin (${wires.size})", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }

                    // TOMBOL BARU: MODE GESER BELOKAN & WARNA KABEL ALA WOKWI!
                    Button(
                        onClick = { isWireEditMode = !isWireEditMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWireEditMode) Color(0xFF00E676) else Color(0xFF263238)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isWireEditMode) "📐 Mode Kabel: ON" else "📐 Atur Kabel",
                            color = if (isWireEditMode) SiliconDark else CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            WokwiSimRenderEngine.autoArrangeNeatGrid(components, canvasSize.width, canvasSize.height)
                            // Reset juga belokan kabel ke kanal rapi
                            wires.forEach {
                                it.bendX = Float.NaN
                                it.bendY = Float.NaN
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LogicViolet),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✨ Auto-Rapikan", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onToggleSerialDrawer,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (showLiveSerialDrawer) "Tutup Serial" else "Buka Serial", color = PcbMintLight, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
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
        }

        // 2. KANVAS SIRKUIT ZERO-LAG (MENDUKUNG GESER KOMPONEN & GESER TITIK BELOK KABEL!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070C0A)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showLiveSerialDrawer) 260.dp else 335.dp)
                .border(2.dp, if (isWireEditMode) Color(0xFF00E676) else PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isWireEditMode, selectedBoardVariant) {
                            detectTapGestures { tapOffset ->
                                val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }
                                // Jika sedang Mode Kabel, cek apakah mengetuk titik Handle Kabel
                                if (isWireEditMode && mcuComp != null) {
                                    val hitWire = wires.mapIndexedNotNull { idx, w ->
                                        val src = components.find { it.id == w.fromCompId } ?: return@mapIndexedNotNull null
                                        val sPt = WokwiSimRenderEngine.getComponentPinOffset(src, w.fromPinName)
                                        val ePt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, w.toMcuPin)
                                        val elbow = WokwiSimRenderEngine.getWireElbowOffset(w, sPt, ePt, idx)
                                        if (kotlin.math.hypot(tapOffset.x - elbow.x, tapOffset.y - elbow.y) < 32f) w else null
                                    }.lastOrNull()
                                    if (hitWire != null) {
                                        selectedWire = hitWire
                                        return@detectTapGestures
                                    }
                                }

                                val hit = components.lastOrNull { comp ->
                                    val w = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                                    val h = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                                    tapOffset.x >= comp.x - 10f && tapOffset.x <= comp.x + w + 10f &&
                                            tapOffset.y >= comp.y - 10f && tapOffset.y <= comp.y + h + 10f
                                }
                                if (hit != null) {
                                    selectedComp = hit
                                } else if (selectedComp != null && !isWireEditMode) {
                                    val target = selectedComp!!
                                    target.x = (tapOffset.x - target.type.hitWidth / 2f).coerceIn(6f, size.width - target.type.hitWidth)
                                    target.y = (tapOffset.y - target.type.hitHeight / 2f).coerceIn(6f, size.height - target.type.hitHeight)
                                }
                            }
                        }
                        .pointerInput(isWireEditMode, selectedBoardVariant) {
                            var draggedComp: SimComponent? = null
                            var draggedWire: classPinWireConnection? = null

                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }
                                    // 1. Cek apakah pengguna menarik bulatan Handle Belokan Kabel
                                    if (isWireEditMode && mcuComp != null) {
                                        draggedWire = wires.mapIndexedNotNull { idx, w ->
                                            val src = components.find { it.id == w.fromCompId } ?: return@mapIndexedNotNull null
                                            val sPt = WokwiSimRenderEngine.getComponentPinOffset(src, w.fromPinName)
                                            val ePt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, w.toMcuPin)
                                            val elbow = WokwiSimRenderEngine.getWireElbowOffset(w, sPt, ePt, idx)
                                            if (kotlin.math.hypot(startOffset.x - elbow.x, startOffset.y - elbow.y) < 36f) {
                                                // Inisialisasi koordinat awal jika masih NaN
                                                if (w.bendX.isNaN()) w.bendX = elbow.x
                                                if (w.bendY.isNaN()) w.bendY = elbow.y
                                                w
                                            } else null
                                        }.lastOrNull()

                                        if (draggedWire != null) {
                                            selectedWire = draggedWire
                                            return@detectDragGestures
                                        }
                                    }

                                    // 2. Jika bukan menarik kabel, geser komponen seperti biasa
                                    draggedComp = components.lastOrNull { comp ->
                                        val w = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                                        val h = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                                        startOffset.x >= comp.x - 15f && startOffset.x <= comp.x + w + 15f &&
                                                startOffset.y >= comp.y - 15f && startOffset.y <= comp.y + h + 15f
                                    }
                                    if (draggedComp != null) selectedComp = draggedComp
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val w = draggedWire
                                    if (w != null) {
                                        w.bendX = (w.bendX + dragAmount.x).coerceIn(10f, size.width - 10f)
                                        w.bendY = (w.bendY + dragAmount.y).coerceIn(10f, size.height - 10f)
                                        return@detectDragGestures
                                    }
                                    val c = draggedComp ?: return@detectDragGestures
                                    c.x = (c.x + dragAmount.x).coerceIn(6f, size.width - c.type.hitWidth)
                                    c.y = (c.y + dragAmount.y).coerceIn(6f, size.height - c.type.hitHeight)
                                },
                                onDragEnd = {
                                    draggedComp = null
                                    draggedWire = null
                                },
                                onDragCancel = {
                                    draggedComp = null
                                    draggedWire = null
                                }
                            )
                        }
                ) {
                    canvasSize = size
                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }

                    // 1. Gambar Jalur Kabel Siku-Siku + Handle Titik Belok
                    if (mcuComp != null) {
                        wires.forEachIndexed { idx, wire ->
                            val sourceComp = components.find { it.id == wire.fromCompId }
                            if (sourceComp != null) {
                                val startPt = WokwiSimRenderEngine.getComponentPinOffset(sourceComp, wire.fromPinName)
                                val endPt = WokwiSimRenderEngine.getMcuPinOffset(mcuComp, selectedBoardVariant, wire.toMcuPin)
                                drawOrthogonalWire(
                                    wire = wire,
                                    start = startPt,
                                    end = endPt,
                                    channelIndex = idx,
                                    showWaypointHandle = isWireEditMode,
                                    isSelectedWire = isWireEditMode && selectedWire?.id == wire.id
                                )
                            }
                        }
                    }

                    // 2. Gambar Semua Komponen
                    components.forEach { comp ->
                        val isPowered = checkPowered(comp)
                        if (selectedComp?.id == comp.id && !isWireEditMode) {
                            val boxW = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardWidth else comp.type.hitWidth
                            val boxH = if (comp.type == SimCompType.ESP32_BOARD) selectedBoardVariant.boardHeight else comp.type.hitHeight
                            drawRoundRect(
                                color = CopperGold.copy(alpha = 0.28f),
                                topLeft = Offset(comp.x - 5f, comp.y - 5f),
                                size = Size(boxW + 10f, boxH + 10f),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 3f)
                            )
                        }
                        when (comp.type) {
                            SimCompType.ESP32_BOARD -> drawMcuVariantBoard(comp.x, comp.y, selectedBoardVariant, isSimRunning, isVirtualWifiConnected)
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

                // Petunjuk Mode Kabel atau Info Wi-Fi Virtual di Kanan Atas Kanvas
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(SiliconDark.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isWireEditMode) "📐 Tarik bulatan putih di kabel untuk geser belokan!"
                        else if (isVirtualWifiConnected) "📶 Wi-Fi: ${BambuVirtualNetwork.VIRTUAL_SSID} (Blynk OK)"
                        else "📶 Wi-Fi Virtual: Standby",
                        color = if (isWireEditMode || isVirtualWifiConnected) Color(0xFF00E676) else CopperGold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // BAR PEMILIH 10 WARNA KABEL INSTAN (MUNCUL SAAT MODE ATUR KABEL AKTIF!)
        if (isWireEditMode && selectedWire != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🎨 Warna Kabel [${selectedWire!!.fromPinName}➔${selectedWire!!.toMcuPin}]:",
                        color = CopperGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JumperWireColor.entries.take(8).forEach { wc ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(wc.color)
                                    .border(
                                        width = if (selectedWire!!.wireColor == wc.color) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedWire!!.wireColor = wc.color }
                            )
                        }
                    }
                }
            }
        }

        // 3. LIVE SERIAL MONITOR DRAWER
        if (showLiveSerialDrawer) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, PcbEmeraldMedium, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💻 LIVE SIMULATOR SERIAL MONITOR (115200 bps)",
                            color = CopperGold,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Bersihkan",
                            color = LogicCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onClearSerialLogs() }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(simSerialLogs) { line ->
                            Text(
                                text = line,
                                color = if (line.contains("✅") || line.contains("☁️")) LogicNeonGreen else if (line.contains("⚠️")) CopperGold else Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. PANEL BAWAH: 24 PILIHAN UKURAN RESISTOR E24 & SLIDER SENSOR
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎨 24 Ukuran Resistor: ${ResistorCatalog.formatOhmLabel(resistorOhms)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 12.sp
                    )
                    Text(
                        text = ResistorCatalog.getFourBandNames(resistorOhms),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CopperOrange,
                        fontSize = 10.sp
                    )
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
                    Text("🌡️ Suhu DHT22: ${String.format("%.1f", dhtTempC)}°C", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PcbEmeraldDark)
                    Text("📏 Jarak Sonar: ${ultrasonicCm.toInt()} cm", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = LogicCyan)
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

    // DIALOG TAMBAH KOMPONEN SENSOR / AKTUATOR
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
                                val newComp = SimComponent(
                                    id = "${type.name.lowercase()}_$idx",
                                    type = type,
                                    label = type.title.substringBefore(" "),
                                    initialX = 250f,
                                    initialY = 30f
                                )
                                components.add(newComp)
                                WokwiSimRenderEngine.autoArrangeNeatGrid(components, canvasSize.width, canvasSize.height)
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

    // DIALOG MANAJEMEN KABEL PIN-KE-PIN + PILIH 10 WARNA KABEL!
    if (showWireManagerDialog) {
        val nonMcuComps = components.filter { it.type != SimCompType.ESP32_BOARD }
        var wireTargetComp by remember { mutableStateOf(nonMcuComps.firstOrNull()) }
        var selectedCompPin by remember { mutableStateOf(wireTargetComp?.type?.pinNames?.firstOrNull() ?: "VCC") }
        val mcuPins = selectedBoardVariant.allConnectablePins
        var selectedMcuPin by remember { mutableStateOf(mcuPins.firstOrNull() ?: "3V3") }
        var pickedWireColor by remember { mutableStateOf(JumperWireColor.CYAN_DATA.color) }

        AlertDialog(
            onDismissRequest = { showWireManagerDialog = false },
            title = { Text("🔌 Pasang Kabel & Pilih Warna", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                    onClick = {
                                        selectedCompPin = pinName
                                        // Otomatis sarankan warna merah untuk VCC dan abu untuk GND
                                        pickedWireColor = when (pinName) {
                                            "VCC", "VCC+", "A+" -> JumperWireColor.RED_VCC.color
                                            "GND", "GND-", "K-" -> JumperWireColor.BLACK_GND.color
                                            else -> JumperWireColor.CYAN_DATA.color
                                        }
                                    },
                                    label = { Text(pinName, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Text("3. Kaki Pin pada ${selectedBoardVariant.displayName}:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mcuPins.forEach { mPin ->
                                FilterChip(
                                    selected = selectedMcuPin == mPin,
                                    onClick = { selectedMcuPin = mPin },
                                    label = { Text(mPin, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Text("4. Pilih Warna Kabel Jumper:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            JumperWireColor.entries.forEach { wc ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(wc.color)
                                        .border(
                                            width = if (pickedWireColor == wc.color) 3.dp else 1.dp,
                                            color = if (pickedWireColor == wc.color) PcbEmeraldDark else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable { pickedWireColor = wc.color }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val filtered = wires.filterNot { it.fromCompId == wireTargetComp!!.id && it.fromPinName == selectedCompPin }
                                val newWire = classPinWireConnection(
                                    id = "w_${System.currentTimeMillis()}",
                                    fromCompId = wireTargetComp!!.id,
                                    fromPinName = selectedCompPin,
                                    toMcuPin = selectedMcuPin,
                                    initialColor = pickedWireColor
                                )
                                onWiresChange(filtered + newWire)
                                selectedWire = newWire
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pasang Kabel: [${wireTargetComp!!.label}:$selectedCompPin] ➔ [$selectedMcuPin]", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
        // KARTU KREDENSIAL VIRTUAL WI-FI & BLYNK CLOUD KHUSUS BAMBUSIM
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
                            Text(
                                text = "Auth Token: ${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}",
                                color = PcbMintLight,
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
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Banner Info Kredensial Virtual Wi-Fi & Blynk
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📶 VIRTUAL WI-FI CREDENTIALS (WAJIB SAMA AGAR BLYNK TERHUBUNG):",
                        color = CopperGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "SSID: \"${BambuVirtualNetwork.VIRTUAL_SSID}\" | PASS: \"${BambuVirtualNetwork.VIRTUAL_PASS}\" | TOKEN: \"${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}\"",
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

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