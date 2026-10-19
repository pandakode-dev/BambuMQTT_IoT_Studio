package com.pandakode.bambumqtt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
// MODEL DATA KOMPONEN, KAKI PIN (VCC/GND/GPIO), & KABEL JUMPER
// =========================================================================

enum class SimCompType(val title: String, val pinNames: List<String>) {
    ESP32_BOARD(
        "ESP32 / ESP8266 MCU",
        listOf("3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34")
    ),
    LED_LAMP("LED 5mm Merah", listOf("A+", "K-")),
    RESISTOR_4BAND("Resistor 4-Gelang", listOf("P1", "P2")),
    DHT22_SENSOR("Sensor Suhu DHT22", listOf("VCC", "DATA", "NC", "GND")),
    OLED_SSD1306("OLED 0.96\" I2C", listOf("GND", "VCC", "SCL", "SDA")),
    SERVO_SG90("Motor Servo SG90", listOf("GND", "VCC", "SIG")),
    HCSR04_SONAR("Ultrasonik HC-SR04", listOf("VCC", "TRIG", "ECHO", "GND")),
    RELAY_1CH("Modul Relay 1CH", listOf("VCC", "GND", "IN")),
    BUZZER_ACTIVE("Buzzer Piezo", listOf("VCC+", "GND-")),
    POTENTIOMETER("Potensiometer Analog", listOf("VCC", "OUT", "GND"))
}

data class SimComponent(
    val id: String,
    val type: SimCompType,
    val label: String,
    var x: Float,
    var y: Float
)

data class PinWireConnection(
val id: String,
val fromCompId: String,
val fromPinName: String,
val toMcuPin: String, // "3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34"
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
    val subTabs = listOf("Kanvas & Wiring", "Koding C++ / Py", "Library Bawaan (20)")

    // Pilihan Board Mikrokontroler ("ESP32 DevKit V1" atau "ESP8266 NodeMCU")
    var mcuBoardModel by remember { mutableStateOf("ESP32") }
    var isSimRunning by remember { mutableStateOf(false) }
    var simTick by remember { mutableIntStateOf(0) }

    // Parameter Interaktif Komponen
    var resistorOhms by remember { mutableIntStateOf(220) }
    var dhtTempC by remember { mutableFloatStateOf(32.5f) }
    var dhtHumPct by remember { mutableFloatStateOf(65.0f) }
    var ultrasonicCm by remember { mutableFloatStateOf(45.0f) }
    var potPercent by remember { mutableFloatStateOf(50.0f) }
    var servoAngle by remember { mutableFloatStateOf(90f) }
    var oledLine1 by remember { mutableStateOf("PANDAKODE IOT") }
    var oledLine2 by remember { mutableStateOf("T:32.5C D:45cm") }
    var ledRedActive by remember { mutableStateOf(false) }
    var relayActive by remember { mutableStateOf(false) }
    var buzzerActive by remember { mutableStateOf(false) }

    var simSerialOutput by remember {
        mutableStateOf("[SIAP] Pastikan kaki VCC & GND komponen sudah terhubung ke ESP32 sebelum klik RUN!")
    }

    // Daftar Komponen di Atas Kanvas
    var components by remember {
        mutableStateOf(
            listOf(
                SimComponent("mcu", SimCompType.ESP32_BOARD, "ESP32 DevKit", 20f, 70f),
                SimComponent("res1", SimCompType.RESISTOR_4BAND, "Resistor", 315f, 35f),
                SimComponent("led1", SimCompType.LED_LAMP, "LED Merah", 515f, 25f),
                SimComponent("dht1", SimCompType.DHT22_SENSOR, "DHT22", 520f, 165f),
                SimComponent("oled1", SimCompType.OLED_SSD1306, "OLED I2C", 300f, 315f),
                SimComponent("servo1", SimCompType.SERVO_SG90, "Servo SG90", 545f, 330f)
            )
        )
    }

    // Daftar Kabel Jumper Pin-ke-Pin (Lengkap VCC, GND, & Sinyal GPIO!)
    var wires by remember {
        mutableStateOf(
            listOf(
                // LED + Resistor terhubung ke D2 & GND
                PinWireConnection("w1", "res1", "P1", "D2", Color(0xFFFFB703)),
                PinWireConnection("w2", "led1", "A+", "D2", Color(0xFFFF5252)),
                PinWireConnection("w3", "led1", "K-", "GND", Color(0xFF90A4AE)),
                // DHT22 (4 Pin: VCC->3V3, DATA->D4, GND->GND)
                PinWireConnection("w4", "dht1", "VCC", "3V3", Color(0xFFFF1744)),
                PinWireConnection("w5", "dht1", "DATA", "D4", Color(0xFF00E5FF)),
                PinWireConnection("w6", "dht1", "GND", "GND", Color(0xFF90A4AE)),
                // OLED SSD1306 (4 Pin: GND->GND, VCC->3V3, SCL->D22, SDA->D21)
                PinWireConnection("w7", "oled1", "GND", "GND", Color(0xFF90A4AE)),
                PinWireConnection("w8", "oled1", "VCC", "3V3", Color(0xFFFF1744)),
                PinWireConnection("w9", "oled1", "SCL", "D22", Color(0xFF00F5D4)),
                PinWireConnection("w10", "oled1", "SDA", "D21", Color(0xFFFFB703)),
                // Servo SG90 (3 Pin: GND->GND, VCC->5V, SIG->D18)
                PinWireConnection("w11", "servo1", "GND", "GND", Color(0xFF90A4AE)),
                PinWireConnection("w12", "servo1", "VCC", "5V", Color(0xFFFF1744)),
                PinWireConnection("w13", "servo1", "SIG", "D18", Color(0xFFFB8500))
            )
        )
    }

    // Fungsi Pengecek Apakah Komponen Sudah Mendapat Daya (VCC & GND) + Pin Data!
    fun isComponentPoweredAndWired(comp: SimComponent): Boolean {
        val compWires = wires.filter { it.fromCompId == comp.id }
        return when (comp.type) {
            SimCompType.ESP32_BOARD -> true
            SimCompType.RESISTOR_4BAND -> compWires.isNotEmpty()
            SimCompType.LED_LAMP -> {
                val hasAnode = compWires.any { it.fromPinName == "A+" && it.toMcuPin.startsWith("D") }
                val hasGnd = compWires.any { it.fromPinName == "K-" && it.toMcuPin == "GND" }
                hasAnode && hasGnd
            }
            SimCompType.DHT22_SENSOR -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasData = compWires.any { it.fromPinName == "DATA" && it.toMcuPin.startsWith("D") }
                hasVcc && hasGnd && hasData
            }
            SimCompType.OLED_SSD1306 -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasScl = compWires.any { it.fromPinName == "SCL" && it.toMcuPin == "D22" }
                val hasSda = compWires.any { it.fromPinName == "SDA" && it.toMcuPin == "D21" }
                hasVcc && hasGnd && hasScl && hasSda
            }
            SimCompType.SERVO_SG90 -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasSig = compWires.any { it.fromPinName == "SIG" && it.toMcuPin.startsWith("D") }
                hasVcc && hasGnd && hasSig
            }
            SimCompType.HCSR04_SONAR -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasTrig = compWires.any { it.fromPinName == "TRIG" && it.toMcuPin.startsWith("D") }
                val hasEcho = compWires.any { it.fromPinName == "ECHO" && it.toMcuPin.startsWith("D") }
                hasVcc && hasGnd && hasTrig && hasEcho
            }
            SimCompType.RELAY_1CH -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "5V" || it.toMcuPin == "3V3") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasIn = compWires.any { it.fromPinName == "IN" && it.toMcuPin.startsWith("D") }
                hasVcc && hasGnd && hasIn
            }
            SimCompType.BUZZER_ACTIVE -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC+" && it.toMcuPin.startsWith("D") }
                val hasGnd = compWires.any { it.fromPinName == "GND-" && it.toMcuPin == "GND" }
                hasVcc && hasGnd
            }
            SimCompType.POTENTIOMETER -> {
                val hasVcc = compWires.any { it.fromPinName == "VCC" && (it.toMcuPin == "3V3" || it.toMcuPin == "5V") }
                val hasGnd = compWires.any { it.fromPinName == "GND" && it.toMcuPin == "GND" }
                val hasOut = compWires.any { it.fromPinName == "OUT" && it.toMcuPin == "D34" }
                hasVcc && hasGnd && hasOut
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

            // Pinout Sirkuit BambuSim:
            // LED = D2 | DHT22 = D4 | Servo = D18 | OLED = D21(SDA), D22(SCL)
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

    // Engine Eksekusi Sirkuit Real-Time (Hanya Komponen yang VCC+GND+Pin Terhubung yang Bisa Aktif!)
    LaunchedEffect(isSimRunning, simCodeText, dhtTempC, ultrasonicCm, resistorOhms, wires, components) {
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

            // 1. Update Layar OLED (Hanya jika VCC, GND, SDA, SCL terhubung!)
            if (isOledWired) {
                val oledMatch = Regex("""(?:display\.println|oled\.text)\(\s*"([^"]+)"""").find(simCodeText)
                if (oledMatch != null) oledLine1 = oledMatch.groupValues[1].take(16)
                oledLine2 = if (isDhtWired) {
                    String.format("T:%.1fC H:%.0f%%", dhtTempC, dhtHumPct)
                } else {
                    "DHT22 UNWIRED!"
                }
            }

            // 2. Update Servo SG90 (Hanya jika VCC 5V, GND, & SIG terhubung!)
            if (isServoWired) {
                val servoMatch = Regex("""servo\.(?:write|angle)\(\s*(\d+)\s*\)""").find(simCodeText)
                val targetDeg = servoMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 90f
                servoAngle = if (simTick % 2 == 0) targetDeg else (targetDeg - 40f).coerceAtLeast(0f)
            }

            // 3. Update LED, Relay & Buzzer
            val codeTriggersHigh = simCodeText.contains("HIGH") || simCodeText.contains(".on()")
            ledRedActive = isLedWired && codeTriggersHigh && (simTick % 2 == 1)
            relayActive = relayComp != null && isComponentPoweredAndWired(relayComp) && codeTriggersHigh
            buzzerActive = buzzerComp != null && isComponentPoweredAndWired(buzzerComp) && (simTick % 2 == 1)

            // 4. Hitung jumlah komponen yang belum terhubung VCC/GND
            val unwiredList = components.filter { it.type != SimCompType.ESP32_BOARD && !isComponentPoweredAndWired(it) }
            simSerialOutput = if (unwiredList.isEmpty()) {
                "[TICK #$simTick] ✅ SEMUA VCC/GND OK | D2=${if (ledRedActive) "HIGH" else "LOW"} | DHT=${if (isDhtWired) "${dhtTempC}°C" else "OFF"} | Sonar=${ultrasonicCm.toInt()}cm"
            } else {
                "⚠️ PERINGATAN: [${unwiredList.joinToString { it.label }}] belum terhubung lengkap ke VCC / GND / GPIO!"
            }

            delay(750L)
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
                            fontSize = 11.sp,
                            color = if (activeSubTab == idx) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (activeSubTab) {
            0 -> WokwiInteractiveWorkbench(
                mcuBoardModel = mcuBoardModel,
                onToggleMcuModel = { mcuBoardModel = if (mcuBoardModel == "ESP32") "ESP8266" else "ESP32" },
                components = components,
                onComponentsChange = { components = it },
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
                onPotPercentChange = { potPercent = it },
                servoAngle = servoAngle,
                oledLine1 = oledLine1,
                oledLine2 = oledLine2,
                simSerialOutput = simSerialOutput
            )

            1 -> WokwiCodeEditorPanel(
                codeLang = codeLang,
                onLangChange = { lang ->
                    codeLang = lang
                    simCodeText = if (lang == "CPP") {
                        """
                        #include <Wire.h>
                        #include <Adafruit_SSD1306.h>
                        #include <DHT.h>
                        #include <ESP32Servo.h>
                        #include <NewPing.h>

                        void setup() {
                          Serial.begin(115200);
                          pinMode(2, OUTPUT);
                          display.println("PANDAKODE C++");
                          servo.write(145);
                        }

                        void loop() {
                          digitalWrite(2, HIGH);
                          delay(1000);
                        }
                        """.trimIndent()
                    } else {
                        """
                        # MicroPython ESP32 / ESP8266 Simulator
                        from machine import Pin, I2C, PWM
                        import ssd1306, dht, hcsr04, time

                        led = Pin(2, Pin.OUT)
                        sensor = dht.DHT22(Pin(4))

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

            2 -> BuiltInLibraryCatalogView()
        }
    }
}

// =========================================================================
// WORKBENCH UTAMA: TOOLBAR TAMBAH KOMPONEN, DIALOG WIRING PIN, & KANVAS
// =========================================================================

@Composable
fun WokwiInteractiveWorkbench(
    mcuBoardModel: String,
    onToggleMcuModel: () -> Unit,
    components: List<SimComponent>,
    onComponentsChange: (List<SimComponent>) -> Unit,
    wires: List<PinWireConnection>,
    onWiresChange: (List<PinWireConnection>) -> Unit,
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
    onPotPercentChange: (Float) -> Unit,
    servoAngle: Float,
    oledLine1: String,
    oledLine2: String,
    simSerialOutput: String
) {
    var showAddPartDialog by remember { mutableStateOf(false) }
    var showWireManagerDialog by remember { mutableStateOf(false) }
    val presetResistorValues = listOf(100, 220, 330, 470, 1000, 4700, 10000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. BARIS TOMBOL KONTROL ATAS: TAMBAH KOMPONEN, HUBUNGKAN PIN (VCC/GND), GANTI MCU, & RUN SIM
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSimRunning) "🟢 SIMULASI AKTIF ($mcuBoardModel)" else "⚪ WORKBENCH SIRKUIT ($mcuBoardModel)",
                            color = CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Komponen wajib terhubung ke VCC + GND + GPIO agar menyala!",
                            color = PcbMintLight,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = onToggleRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimRunning) Color(0xFFD32F2F) else Color(0xFF00E676)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSimRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isSimRunning) Color.White else SiliconDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSimRunning) "STOP" else "RUN SIM",
                            color = if (isSimRunning) Color.White else SiliconDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Toolbar Aksi Sirkuit: + Tambah Komponen | Pasang Kabel Pin | Ganti ESP32/8266
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddPartDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Komponen", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showWireManagerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = LogicCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🔌 Hubungkan Pin (${wires.size} Kabel)", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onToggleMcuModel,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Board: $mcuBoardModel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. KANVAS BREADBOARD DRAG & DROP DENGAN LABEL NOMOR PIN ASLI & KABEL VCC/GND
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B120F)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .border(2.dp, PcbEmeraldMedium, RoundedCornerShape(16.dp))
        ) {
            var draggingId by remember { mutableStateOf<String?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(components) {
                            detectDragGestures(
                                onDragStart = { touchOffset ->
                                    val hit = components.lastOrNull { comp ->
                                        hypot(touchOffset.x - (comp.x + 75f), touchOffset.y - (comp.y + 60f)) < 115f
                                    }
                                    draggingId = hit?.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val activeId = draggingId ?: return@detectDragGestures
                                    onComponentsChange(
                                        components.map { comp ->
                                            if (comp.id == activeId) {
                                                comp.copy(
                                                    x = (comp.x + dragAmount.x).coerceIn(8f, size.width - 155f),
                                                    y = (comp.y + dragAmount.y).coerceIn(8f, size.height - 120f)
                                                )
                                            } else comp
                                        }
                                    )
                                },
                                onDragEnd = { draggingId = null }
                            )
                        }
                ) {
                    // Grid Titik Breadboard
                    val step = 34f
                    for (gx in 0..(size.width / step).toInt()) {
                        drawLine(Color(0xFF15251E), Offset(gx * step, 0f), Offset(gx * step, size.height), 1f)
                    }
                    for (gy in 0..(size.height / step).toInt()) {
                        drawLine(Color(0xFF15251E), Offset(0f, gy * step), Offset(size.width, gy * step), 1f)
                    }

                    val mcuComp = components.find { it.type == SimCompType.ESP32_BOARD }

                    // 1. GAMBAR SEMUA KABEL JUMPER DARI KAKI PIN KOMPONEN KE KAKI PIN ESP32/8266
                    if (mcuComp != null) {
                        wires.forEach { wire ->
                            val sourceComp = components.find { it.id == wire.fromCompId }
                            if (sourceComp != null) {
                                val startPt = getComponentPinOffset(sourceComp, wire.fromPinName)
                                val endPt = getMcuPinOffset(mcuComp, wire.toMcuPin)
                                drawJumperWireWithPinDot(startPt, endPt, wire.wireColor)
                            }
                        }
                    }

                    // 2. GAMBAR SEMUA KOMPONEN BESERTA KAKI & TULISAN NAMA/ANGKA PINNYA!
                    components.forEach { comp ->
                        val isPowered = checkPowered(comp)
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
            }
        }

        // 3. PANEL INSPEKTOR NILAI RESISTOR (GELANG BERUBAH OTOMATIS), SENSOR DHT22 & ULTRASONIK
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val bandNames = getResistorColorBandNames(resistorOhms)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎨 Nilai Resistor: $resistorOhms Ω", fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 13.sp)
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

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🌡️ Suhu DHT22 (Pin D4): ${String.format("%.1f", dhtTempC)} °C", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PcbEmeraldDark)
                    Text("📏 Jarak HC-SR04: ${ultrasonicCm.toInt()} cm", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = LogicCyan)
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎛️ Potensiometer (Pin D34): ${potPercent.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CopperOrange)
                }
                Slider(
                    value = potPercent,
                    onValueChange = onPotPercentChange,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = CopperGold, activeTrackColor = PcbEmeraldDark)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = simSerialOutput,
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }

    // =====================================================================
    // DIALOG 1: TAMBAH KOMPONEN BARU KE ATAS KANVAS SIMULASI
    // =====================================================================
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
            title = { Text("➕ Tambah Komponen ke Kanvas", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pilih komponen elektronika/sensor (otomatis memiliki kaki VCC, GND & Sinyal sesuai bentuk aslinya):", fontSize = 12.sp, color = Color.Gray)
                    addableTypes.forEach { type ->
                        OutlinedButton(
                            onClick = {
                                val newId = "${type.name.lowercase()}_${components.size + 1}"
                                val newComp = SimComponent(
                                    id = newId,
                                    type = type,
                                    label = type.title.substringBefore(" "),
                                    x = 310f,
                                    y = 140f
                                )
                                onComponentsChange(components + newComp)
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

    // =====================================================================
    // DIALOG 2: PANEL HUBUNGKAN KABEL PIN-KE-PIN (VCC, GND, GPIO)
    // =====================================================================
    if (showWireManagerDialog) {
        val nonMcuComps = components.filter { it.type != SimCompType.ESP32_BOARD }
        var selectedComp by remember { mutableStateOf(nonMcuComps.firstOrNull()) }
        var selectedCompPin by remember { mutableStateOf(selectedComp?.type?.pinNames?.firstOrNull() ?: "VCC") }
        val mcuPins = SimCompType.ESP32_BOARD.pinNames
        var selectedMcuPin by remember { mutableStateOf("3V3") }

        AlertDialog(
            onDismissRequest = { showWireManagerDialog = false },
            title = { Text("🔌 Hubungkan Kabel Pin-ke-Pin", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tombol Cepat: Auto-Wire Semua VCC, GND & GPIO!
                    Button(
                        onClick = {
                            val autoWires = mutableListOf<PinWireConnection>()
                            components.forEach { c ->
                                when (c.type) {
                                    SimCompType.LED_LAMP -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "A+", "D2", Color(0xFFFF5252)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "K-", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.RESISTOR_4BAND -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "P1", "D2", CopperGold))
                                    }
                                    SimCompType.DHT22_SENSOR -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "DATA", "D4", LogicCyan))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.OLED_SSD1306 -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "SCL", "D22", LogicNeonGreen))
                                        autoWires.add(PinWireConnection("aw_${c.id}_4", c.id, "SDA", "D21", CopperGold))
                                    }
                                    SimCompType.SERVO_SG90 -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "SIG", "D18", CopperOrange))
                                    }
                                    SimCompType.HCSR04_SONAR -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "TRIG", "D5", LogicCyan))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "ECHO", "D19", LogicNeonGreen))
                                        autoWires.add(PinWireConnection("aw_${c.id}_4", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.RELAY_1CH -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "VCC", "5V", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "IN", "D2", CopperOrange))
                                    }
                                    SimCompType.BUZZER_ACTIVE -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "VCC+", "D2", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "GND-", "GND", Color(0xFF90A4AE)))
                                    }
                                    SimCompType.POTENTIOMETER -> {
                                        autoWires.add(PinWireConnection("aw_${c.id}_1", c.id, "VCC", "3V3", Color(0xFFFF1744)))
                                        autoWires.add(PinWireConnection("aw_${c.id}_2", c.id, "OUT", "D34", LogicCyan))
                                        autoWires.add(PinWireConnection("aw_${c.id}_3", c.id, "GND", "GND", Color(0xFF90A4AE)))
                                    }
                                    else -> {}
                                }
                            }
                            onWiresChange(autoWires)
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
                                selected = selectedComp?.id == comp.id,
                                onClick = {
                                    selectedComp = comp
                                    selectedCompPin = comp.type.pinNames.first()
                                },
                                label = { Text(comp.label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (selectedComp != null) {
                        Text("2. Pilih Kaki Pin pada ${selectedComp!!.label}:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            selectedComp!!.type.pinNames.forEach { pinName ->
                                FilterChip(
                                    selected = selectedCompPin == pinName,
                                    onClick = { selectedCompPin = pinName },
                                    label = { Text(pinName, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Text("3. Hubungkan ke Kaki Pin $mcuBoardModel:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    "3V3", "5V" -> Color(0xFFFF1744) // Merah untuk VCC
                                    "GND" -> Color(0xFF90A4AE)       // Abu/Hitam untuk GND
                                    else -> LogicCyan                // Cyan untuk Sinyal GPIO
                                }
                                val filtered = wires.filterNot { it.fromCompId == selectedComp!!.id && it.fromPinName == selectedCompPin }
                                onWiresChange(
                                    filtered + PinWireConnection(
                                        id = "w_${System.currentTimeMillis()}",
                                        fromCompId = selectedComp!!.id,
                                        fromPinName = selectedCompPin,
                                        toMcuPin = selectedMcuPin,
                                        wireColor = color
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pasang Kabel: [${selectedComp!!.label}:$selectedCompPin] ➔ [$selectedMcuPin]", fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
// KOORDINAT KAKI PIN PRESISI & ENGINE PENGGAMBAR KOMPONEN BERLABEL PIN!
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

private fun DrawScope.drawJumperWireWithPinDot(start: Offset, end: Offset, wireColor: Color) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        val ctrlX = (start.x + end.x) / 2f
        val ctrlY = minOf(start.y, end.y) - 28f
        quadraticBezierTo(ctrlX, ctrlY, end.x, end.y)
    }
    drawPath(path, color = wireColor, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
    drawCircle(color = CopperGold, radius = 4.5f, center = start)
    drawCircle(color = CopperGold, radius = 4.5f, center = end)
}

// 1. GAMBAR BOARD ESP32 / ESP8266 DENGAN TULISAN LABEL PIN JELAS DI SETIAP KAKI!
private fun DrawScope.drawMcuBoardWithPinLabels(x: Float, y: Float, boardModel: String, isRunning: Boolean) {
    val w = 185f
    val h = 268f
    drawRoundRect(color = Color(0xFF1A221E), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f))
    drawRoundRect(color = CopperGold, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(12f, 12f), style = Stroke(width = 3f))

    // Kaleng RF Perak
    drawRoundRect(color = Color(0xFFB0BEC5), topLeft = Offset(x + 20f, y + 32f), size = Size(92f, 82f), cornerRadius = CornerRadius(6f, 6f))
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 19f
            isFakeBoldText = true
        }
        drawText(boardModel, x + 36f, y + 72f, paint)
        paint.textSize = 13f
        drawText(if (boardModel == "ESP32") "WROOM-32" else "NodeMCU", x + 32f, y + 94f, paint)
    }

    // Daftar 11 Pin Utama di Sisi Kanan Board Lengkap dengan Warna & Teks Pin!
    val rightPins = listOf("3V3", "5V", "GND", "D2", "D4", "D5", "D18", "D19", "D21", "D22", "D34")
    val pinPaint = android.graphics.Paint().apply {
        textSize = 14f
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
    }

    rightPins.forEachIndexed { idx, pName ->
        val py = y + 36f + idx * 20f
        val padColor = when (pName) {
            "3V3", "5V" -> Color(0xFFFF1744)
            "GND" -> Color(0xFF90A4AE)
            else -> CopperGold
        }
        drawRoundRect(color = padColor, topLeft = Offset(x + w - 16f, py), size = Size(14f, 12f), cornerRadius = CornerRadius(2f, 2f))

        drawContext.canvas.nativeCanvas.apply {
            pinPaint.color = when (pName) {
                "3V3", "5V" -> android.graphics.Color.rgb(255, 82, 82)
                "GND" -> android.graphics.Color.LTGRAY
                else -> android.graphics.Color.rgb(255, 215, 64)
            }
            drawText(pName, x + w - 52f, py + 10f, pinPaint)
        }
    }

    // Indikator LED Power & GPIO2 Built-in
    drawCircle(color = Color(0xFFFF1744), radius = 5.5f, center = Offset(x + 35f, y + h - 24f))
    drawCircle(color = if (isRunning) Color(0xFF00E5FF) else Color(0xFF263238), radius = 6.5f, center = Offset(x + 85f, y + h - 24f))
}

// 2. GAMBAR RESISTOR 4-GELANG DENGAN KAKI P1 & P2
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
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 15f
            isFakeBoldText = true
        }
        drawText("P1", x, y + 18f, paint)
        drawText("P2", x + 112f, y + 18f, paint)
        paint.color = android.graphics.Color.YELLOW
        drawText("${ohms}Ω", x + 42f, y + 58f, paint)
    }
}

// 3. GAMBAR LED DENGAN 2 KAKI BERLABEL (A+ dan K-)
private fun DrawScope.drawLedWithPins(x: Float, y: Float, isOn: Boolean, resistorOhms: Int, isPowered: Boolean) {
    // 2 Kaki: A+ (Anoda) & K- (Katoda)
    drawLine(Color(0xFFFF5252), Offset(x + 22f, y + 52f), Offset(x + 22f, y + 88f), strokeWidth = 5f)
    drawLine(Color(0xFFB0BEC5), Offset(x + 48f, y + 52f), Offset(x + 48f, y + 88f), strokeWidth = 5f)

    val intensity = (1f - (resistorOhms / 12000f)).coerceIn(0.35f, 1f)
    val bulbColor = if (isOn) Color(0xFFFF1744).copy(alpha = intensity) else Color(0xFF4A151B)

    if (isOn) {
        drawCircle(color = Color(0xFFFF5252).copy(alpha = 0.35f * intensity), radius = 44f, center = Offset(x + 35f, y + 28f))
    }
    drawCircle(color = bulbColor, radius = 22f, center = Offset(x + 35f, y + 26f))
    drawRoundRect(color = bulbColor, topLeft = Offset(x + 13f, y + 26f), size = Size(44f, 26f), cornerRadius = CornerRadius(4f, 4f))

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 14f
            isFakeBoldText = true
        }
        drawText("A+", x + 8f, y + 102f, paint)
        drawText("K-", x + 44f, y + 102f, paint)
        if (!isPowered) {
            paint.color = android.graphics.Color.RED
            drawText("UNWIRED", x + 4f, y - 4f, paint)
        }
    }
}

// 4. GAMBAR SENSOR DHT22 DENGAN 4 KAKI BERLABEL (VCC, DATA, NC, GND)
private fun DrawScope.drawDht22With4Pins(x: Float, y: Float, tempC: Float, isPowered: Boolean) {
    val w = 104f
    val h = 100f
    val pinLabels = listOf("VCC", "DAT", "NC", "GND")
    pinLabels.forEachIndexed { i, label ->
        val px = x + 16f + i * 24f
        drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + h + 28f), strokeWidth = 4f)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                isFakeBoldText = true
            }
            drawText(label, px - 10f, y + h + 40f, p)
        }
    }

    drawRoundRect(color = Color(0xFF0288D1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))
    for (row in 1..3) {
        drawLine(Color(0xFF01579B), Offset(x + 14f, y + row * 16f), Offset(x + w - 14f, y + row * 16f), strokeWidth = 4f)
    }
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 17f
            isFakeBoldText = true
        }
        drawText("DHT22", x + 22f, y + h - 22f, paint)
        paint.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
        drawText(if (isPowered) "${String.format("%.1f", tempC)}°C" else "NO VCC/GND", x + 10f, y + h - 5f, paint)
    }
}

// 5. GAMBAR LAYAR OLED SSD1306 DENGAN 4 KAKI BERLABEL (GND, VCC, SCL, SDA)
private fun DrawScope.drawOledWith4Pins(x: Float, y: Float, isActive: Boolean, line1: String, line2: String, isPowered: Boolean) {
    val w = 148f
    val h = 100f
    drawRoundRect(color = Color(0xFF0D47A1), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(10f, 10f))

    val oledPins = listOf("GND", "VCC", "SCL", "SDA")
    oledPins.forEachIndexed { i, label ->
        val px = x + 36f + i * 26f
        drawCircle(color = CopperGold, radius = 5f, center = Offset(px, y + 10f))
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                isFakeBoldText = true
            }
            drawText(label, px - 11f, y - 4f, p)
        }
    }

    drawRoundRect(color = Color(0xFF050807), topLeft = Offset(x + 10f, y + 22f), size = Size(w - 20f, h - 30f), cornerRadius = CornerRadius(4f, 4f))

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            textSize = 16f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
        if (isActive) {
            paint.color = android.graphics.Color.YELLOW
            drawText(line1, x + 16f, y + 46f, paint)
            paint.color = android.graphics.Color.CYAN
            drawText(line2, x + 16f, y + 72f, paint)
        } else {
            paint.color = if (isPowered) android.graphics.Color.GRAY else android.graphics.Color.RED
            paint.textSize = 13f
            drawText(if (isPowered) "OLED STANDBY" else "UNWIRED VCC/I2C", x + 16f, y + 58f, paint)
        }
    }
}

// 6. GAMBAR SERVO SG90 DENGAN 3 KAKI BERLABEL (GND, VCC, SIG)
private fun DrawScope.drawServoWith3Pins(x: Float, y: Float, angleDeg: Float, isPowered: Boolean) {
    val w = 100f
    val h = 68f
    val servoPins = listOf("GND", "VCC", "SIG")
    servoPins.forEachIndexed { i, label ->
        val px = x + 22f + i * 28f
        drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 92f), strokeWidth = 5f)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 11f
                isFakeBoldText = true
            }
            drawText(label, px - 11f, y + 106f, p)
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
        val paint = android.graphics.Paint().apply {
            color = if (isPowered) android.graphics.Color.WHITE else android.graphics.Color.RED
            textSize = 15f
            isFakeBoldText = true
        }
        drawText(if (isPowered) "${angleDeg.toInt()}°" else "NO 5V", x + 54f, y + 40f, paint)
    }
}

// 7. GAMBAR SENSOR JARAK ULTRASONIK HC-SR04 DENGAN 4 KAKI (VCC, TRIG, ECHO, GND)
private fun DrawScope.drawHcsr04With4Pins(x: Float, y: Float, distCm: Float, isPowered: Boolean) {
    val w = 135f
    val h = 68f
    val pins = listOf("VCC", "TRG", "ECH", "GND")
    pins.forEachIndexed { i, label ->
        val px = x + 28f + i * 28f
        drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 95f), strokeWidth = 4f)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 11f; isFakeBoldText = true }
            drawText(label, px - 11f, y + 108f, p)
        }
    }
    drawRoundRect(color = Color(0xFF0277BD), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
    // 2 Tabung Transduser Ultrasonik (T & R)
    drawCircle(color = Color(0xFFCFD8DC), radius = 22f, center = Offset(x + 34f, y + 34f))
    drawCircle(color = Color(0xFF37474F), radius = 15f, center = Offset(x + 34f, y + 34f))
    drawCircle(color = Color(0xFFCFD8DC), radius = 22f, center = Offset(x + w - 34f, y + 34f))
    drawCircle(color = Color(0xFF37474F), radius = 15f, center = Offset(x + w - 34f, y + 34f))

    drawContext.canvas.nativeCanvas.apply {
        val p = android.graphics.Paint().apply {
            color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
            textSize = 14f
            isFakeBoldText = true
        }
        drawText(if (isPowered) "${distCm.toInt()}cm" else "UNWIRED", x + 48f, y + 16f, p)
    }
}

// 8. GAMBAR MODUL RELAY 1CH DENGAN 3 KAKI (VCC, GND, IN)
private fun DrawScope.drawRelayWith3Pins(x: Float, y: Float, isRelayOn: Boolean, isPowered: Boolean) {
    val w = 96f
    val h = 72f
    val pins = listOf("VCC", "GND", "IN")
    pins.forEachIndexed { i, label ->
        val px = x + 22f + i * 28f
        drawLine(Color(0xFFB0BEC5), Offset(px, y + h), Offset(px, y + 98f), strokeWidth = 4f)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 11f; isFakeBoldText = true }
            drawText(label, px - 10f, y + 110f, p)
        }
    }
    drawRoundRect(color = Color(0xFF1565C0), topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(8f, 8f))
    drawCircle(color = if (isRelayOn) Color(0xFF00E676) else Color(0xFF424242), radius = 7f, center = Offset(x + w - 16f, y + 16f))
    drawContext.canvas.nativeCanvas.apply {
        val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 14f; isFakeBoldText = true }
        drawText("RELAY 5V", x + 12f, y + 40f, p)
        p.color = if (isPowered) android.graphics.Color.YELLOW else android.graphics.Color.RED
        drawText(if (!isPowered) "NO VCC" else if (isRelayOn) "CLICK: ON" else "IDLE: OFF", x + 12f, y + 58f, p)
    }
}

// 9. GAMBAR BUZZER PIEZO DENGAN 2 KAKI (VCC+, GND-)
private fun DrawScope.drawBuzzerWith2Pins(x: Float, y: Float, isBeeping: Boolean, isPowered: Boolean) {
    drawLine(Color(0xFFFF5252), Offset(x + 24f, y + 55f), Offset(x + 24f, y + 84f), strokeWidth = 4f)
    drawLine(Color(0xFFB0BEC5), Offset(x + 54f, y + 55f), Offset(x + 54f, y + 84f), strokeWidth = 4f)
    if (isBeeping) {
        drawCircle(color = CopperGold.copy(alpha = 0.35f), radius = 42f, center = Offset(x + 39f, y + 32f))
    }
    drawCircle(color = Color(0xFF212121), radius = 28f, center = Offset(x + 39f, y + 32f))
    drawCircle(color = Color(0xFF757575), radius = 9f, center = Offset(x + 39f, y + 32f))
    drawContext.canvas.nativeCanvas.apply {
        val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 11f; isFakeBoldText = true }
        drawText("VCC+", x + 8f, y + 98f, p)
        drawText("GND-", x + 46f, y + 98f, p)
        if (!isPowered) {
            p.color = android.graphics.Color.RED
            drawText("UNWIRED", x + 12f, y - 2f, p)
        }
    }
}

// 10. GAMBAR POTENSIOMETER ANALOG DENGAN 3 KAKI (VCC, OUT, GND)
private fun DrawScope.drawPotentiometerWith3Pins(x: Float, y: Float, pct: Float, isPowered: Boolean) {
    val pins = listOf("VCC", "OUT", "GND")
    pins.forEachIndexed { i, label ->
        val px = x + 20f + i * 26f
        drawLine(Color(0xFFB0BEC5), Offset(px, y + 58f), Offset(px, y + 88f), strokeWidth = 4f)
        drawContext.canvas.nativeCanvas.apply {
            val p = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 11f; isFakeBoldText = true }
            drawText(label, px - 10f, y + 100f, p)
        }
    }
    drawCircle(color = Color(0xFF37474F), radius = 28f, center = Offset(x + 46f, y + 32f))
    drawCircle(color = CopperGold, radius = 18f, center = Offset(x + 46f, y + 32f))
    drawContext.canvas.nativeCanvas.apply {
        val p = android.graphics.Paint().apply { color = if (isPowered) android.graphics.Color.BLACK else android.graphics.Color.RED; textSize = 13f; isFakeBoldText = true }
        drawText(if (isPowered) "${pct.toInt()}%" else "OFF", x + 32f, y + 37f, p)
    }
}

// =========================================================================
// ALGORITMA KODE WARNA GELANG RESISTOR STANDAR INTERNASIONAL (EIA 4-BAND)
// =========================================================================

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

// =========================================================================
// KATALOG 20 LIBRARY BAWAAN BAMBUSIM ENGINE (C++ ARDUINO & MICROPYTHON)
// =========================================================================

@Composable
fun BuiltInLibraryCatalogView() {
    val libs = remember {
        listOf(
            BuiltInLibInfo("<WiFi.h> & <ESP8266WiFi.h>", "C++", "Networking", "Koneksi Wi-Fi STA/AP untuk ESP32 & ESP8266 NodeMCU", "WiFi.begin(ssid, pass); WiFi.localIP();"),
            BuiltInLibInfo("<PubSubClient.h>", "C++", "IoT MQTT", "Klien Broker MQTT Port 1883 Publish & Subscribe", "client.connect(\"ESP32\"); client.publish(topic, payload);"),
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
            BuiltInLibInfo("ssd1306 & dht", "MicroPython", "Display/Sensor", "Driver Layar OLED I2C & Sensor DHT22 Bawaan", "oled.text('PANDAKODE', 0, 0); d.measure()"),
            BuiltInLibInfo("hcsr04 & neopixel", "MicroPython", "Sensor/RGB", "Pengukur Jarak Ultrasonik & RGB LED WS2812B", "dist = sonar.distance_cm(); np[0] = (255, 0, 0)"),
            BuiltInLibInfo("umqtt.simple & urequests", "MicroPython", "IoT Network", "Klien Broker MQTT & HTTP REST API Client", "client.publish(b'pandakode/temp', b'32.5')")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📚 20 LIBRARY BAWAAN BAMBUSIM (C++ & MICROPYTHON)",
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
