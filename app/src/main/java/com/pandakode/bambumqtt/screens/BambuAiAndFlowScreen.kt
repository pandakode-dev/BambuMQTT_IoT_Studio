package com.pandakode.bambumqtt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import kotlinx.coroutines.delay

@Stable
class FlowNodeItem(
    val id: String,
    val category: String, // "INPUT", "LOGIC", "OUTPUT"
    val title: String,
    val subtitle: String,
    val badgeColor: Color,
    initialX: Float,
    initialY: Float
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
}

data class FlowLinkEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val color: Color
)

private val NodeTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    textSize = 14f
    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
}

private val NodeSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.LTGRAY
    textSize = 11.5f
    typeface = Typeface.MONOSPACE
}

private val SharedBezierPath = Path()

@Composable
fun BambuAiAndFlowContent() {
    val context = LocalContext.current

    // 1. STATE BAMBU-AI AGENT (CHAT TO CODE & CIRCUIT DOCTOR)
    var aiPromptInput by remember { mutableStateOf("") }
    var aiResponseTitle by remember { mutableStateOf("🤖 BambuAI Embedded Engineer Siap Membantu!") }
    var aiGeneratedCode by remember {
        mutableStateOf(
            """
            // Hasil Diagnosa Sirkuit & Rekomendasi Koding BambuAI:
            // [OK] Pin GPIO 32 & 33 aman digunakan untuk ADC1 saat Wi-Fi aktif.
            // [OK] Gunakan millis() sebagai pengganti delay() agar MQTT tidak terputus.
            void readSensorSafe() {
              float temp = dht.readTemperature();
              if (!isnan(temp) && temp > 36.0) {
                digitalWrite(23, HIGH); // Aktifkan Kipas Pendingin
                client.publish("pandakode/alarm", "OVERHEAT_DETECTED");
              }
            }
            """.trimIndent()
        )
    }

    val quickAiPrompts = listOf(
        "Diagnosa Sirkuit",
        "Koding DHT22 + OLED",
        "Koding Modbus RS485",
        "Koding DeepSleep Hemat Baterai",
        "Koding PID Kipas PWM"
    )

    fun runBambuAiEngine(prompt: String) {
        val p = prompt.lowercase()
        when {
            p.contains("diagnosa") || p.contains("cek") -> {
                aiResponseTitle = "🔍 Hasil Diagnosa Sirkuit oleh BambuAI Doctor:"
                aiGeneratedCode = """
                    [DIAGNOSA HARDWARE & WIRING SELESAI]
                    1. ✅ Tegangan Kerja: Sensor I2C OLED (0x3C) & DHT22 berada di jalur aman 3.3V.
                    2. ⚠️ Peringatan ADC2: Jangan gunakan GPIO 25/26/27 untuk analogRead() saat Wi-Fi/MQTT menyala, gunakan GPIO 32/33/34 (ADC1).
                    3. ✅ Arus LED: Resistor 220 Ω pada GPIO 2 membatasi arus ke 6.8 mA (Aman di bawah batas 12 mA per pin ESP32).
                    4. 💡 Saran Optimasi: Tambahkan kapasitor 100uF di dekat pin 5V & GND jika menggunakan Motor Servo SG90 agar ESP32 tidak Brownout Reset.
                """.trimIndent()
            }
            p.contains("modbus") || p.contains("rs485") -> {
                aiResponseTitle = "🤖 BambuAI: Sketch ESP32 Modbus RTU RS485 (Sensor NPK Tanah)"
                aiGeneratedCode = """
                    #include <ModbusMaster.h>
                    #define MAX485_DE_RE 4
                    ModbusMaster node;

                    void preTransmission() { digitalWrite(MAX485_DE_RE, HIGH); }
                    void postTransmission() { digitalWrite(MAX485_DE_RE, LOW); }

                    void setup() {
                      Serial.begin(115200);
                      Serial2.begin(9600, SERIAL_8N1, 16, 17); // RX2=16, TX2=17
                      pinMode(MAX485_DE_RE, OUTPUT);
                      digitalWrite(MAX485_DE_RE, LOW);
                      node.begin(1, Serial2); // Slave ID 1
                      node.preTransmission(preTransmission);
                      node.postTransmission(postTransmission);
                    }

                    void loop() {
                      if (node.readHoldingRegisters(0x001E, 3) == node.ku8MBSuccess) {
                        uint16_t nitrogen = node.getResponseBuffer(0);
                        Serial.printf("Nitrogen Tanah: %d mg/kg\n", nitrogen);
                      }
                      delay(2000);
                    }
                """.trimIndent()
            }
            p.contains("deepsleep") || p.contains("baterai") -> {
                aiResponseTitle = "🤖 BambuAI: Sketch ESP32 Ultra-Low Power Deep-Sleep"
                aiGeneratedCode = """
                    #include <WiFi.h>
                    #define uS_TO_S_FACTOR 1000000ULL
                    #define TIME_TO_SLEEP  900 // Tidur 15 Menit (900 detik)

                    RTC_DATA_ATTR int bootCount = 0;

                    void setup() {
                      Serial.begin(115200);
                      bootCount++;
                      Serial.printf(" Siklus Bangun ke-%d: Kirim Telemetri MQTT...\n", bootCount);
                      // Masuk ke mode tidur ekstrem (Hanya konsumsi ~10 uA!)
                      esp_sleep_enable_timer_wakeup(TIME_TO_SLEEP * uS_TO_S_FACTOR);
                      esp_deep_sleep_start();
                    }
                    void loop() {}
                """.trimIndent()
            }
            p.contains("pid") || p.contains("pwm") -> {
                aiResponseTitle = "🤖 BambuAI: Kontroler PID Suhu ke Kipas PWM ESP32"
                aiGeneratedCode = """
                    float setpoint = 35.0, kp = 12.0, ki = 0.5, kd = 2.0;
                    float integral = 0, prevError = 0;

                    int computeFanPwm(float currentTemp) {
                      float error = currentTemp - setpoint;
                      integral += error;
                      float derivative = error - prevError;
                      prevError = error;
                      int pwm = (int)(kp * error + ki * integral + kd * derivative);
                      return constrain(pwm, 0, 255);
                    }
                """.trimIndent()
            }
            else -> {
                aiResponseTitle = "🤖 BambuAI: Koding C++ Non-Blocking DHT22 + OLED SSD1306 + MQTT"
                aiGeneratedCode = """
                    #include <Wire.h>
                    #include <Adafruit_SSD1306.h>
                    #include <DHT.h>

                    DHT dht(4, DHT22);
                    unsigned long lastTick = 0;

                    void setup() {
                      Serial.begin(115200);
                      dht.begin();
                      Wire.begin(21, 22);
                    }

                    void loop() {
                      if (millis() - lastTick >= 2000) {
                        lastTick = millis();
                        float t = dht.readTemperature();
                        Serial.printf("{\"temp\":%.1f,\"prompt\":\"%s\"}\n", t, "${prompt.take(20)}");
                      }
                    }
                """.trimIndent()
            }
        }
    }

    // 2. STATE BAMBUFLOW VISUAL NODE-RED CANVAS
    var isFlowRunning by remember { mutableStateOf(true) }
    var pulseProgress by remember { mutableFloatStateOf(0f) }
    var simulatedTempInput by remember { mutableFloatStateOf(38.4f) }

    val flowNodes = remember {
        mutableStateListOf(
            FlowNodeItem("n1", "INPUT", "MQTT In: DHT22", "38.4 °C (Pin D4)", LogicCyan, 16f, 24f),
            FlowNodeItem("n2", "INPUT", "Sonar HC-SR04", "42 cm (Pin D5)", LogicCyan, 16f, 128f),
            FlowNodeItem("n3", "LOGIC", "IF Temp > 36°C", "Hysteresis Gate", CopperOrange, 235f, 24f),
            FlowNodeItem("n4", "LOGIC", "JSON Formatter", "Pack Telemetry", LogicViolet, 235f, 128f),
            FlowNodeItem("n5", "OUTPUT", "Relay Pompa D2", "STATE: HIGH (ON)", Color(0xFF00E676), 455f, 24f),
            FlowNodeItem("n6", "OUTPUT", "Telegram Hook", "POST Alert Sent!", CopperGold, 455f, 128f)
        )
    }

    val flowEdges = remember {
        listOf(
            FlowLinkEdge("n1", "n3", LogicCyan),
            FlowLinkEdge("n1", "n4", LogicViolet),
            FlowLinkEdge("n2", "n4", LogicCyan),
            FlowLinkEdge("n3", "n5", Color(0xFF00E676)),
            FlowLinkEdge("n4", "n6", CopperGold)
        )
    }

    LaunchedEffect(isFlowRunning) {
        while (isFlowRunning) {
            delay(50L)
            pulseProgress = (pulseProgress + 0.04f) % 1.0f
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // =================================================================
        // KARTU 1: BAMBU-AI AGENT (CHAT TO CODE & CIRCUIT DOCTOR)
        // =================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CopperGold, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BAMBU-AI AGENT — CODE & CIRCUIT DOCTOR",
                            color = CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tanyakan koding ESP32/Modbus atau minta AI mendiagnosa jalur sirkuit Anda",
                            color = PcbMintLight,
                            fontSize = 11.sp
                        )
                    }
                }

                // Chip Prompt Cepat AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAiPrompts.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { runBambuAiEngine(preset) },
                            label = { Text("✨ $preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = PcbEmeraldMedium,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Input Chat ke BambuAI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text("Ketik: Buat koding sensor / diagnosa...", color = PcbMintLight.copy(alpha = 0.6f), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CopperGold,
                            unfocusedBorderColor = PcbMintLight.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (aiPromptInput.isNotBlank()) {
                                runBambuAiEngine(aiPromptInput)
                            } else {
                                runBambuAiEngine("Diagnosa Sirkuit")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tanya AI", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(aiResponseTitle, color = CopperGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("BambuAI_Code", aiGeneratedCode))
                                    Toast.makeText(context, "Hasil BambuAI disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Copy", color = LogicCyan, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = aiGeneratedCode,
                            color = LogicNeonGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
        }

        // =================================================================
        // KARTU 2: BAMBUFLOW VISUAL NODE-RED DRAG & DROP AUTOMATION
        // =================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = CopperOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "BAMBUFLOW — VISUAL NODE-RED CANVAS",
                                fontWeight = FontWeight.ExtraBold,
                                color = PcbEmeraldDark,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Geser blok Node dengan jari & pantau aliran paket data real-time!",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = { isFlowRunning = !isFlowRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFlowRunning) PcbEmeraldDark else CopperOrange
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFlowRunning) "🟢 FLOW DEPLOYED" else "⏸ PAUSED",
                            color = CopperGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Kanvas Interaktif Node-RED (Zero-Lag Drag & Drop + Animasi Paket Data Berjalan!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(225.dp)
                        .background(Color(0xFF09110E), RoundedCornerShape(14.dp))
                        .border(2.dp, PcbEmeraldMedium, RoundedCornerShape(14.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                var draggedNode: FlowNodeItem? = null
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        draggedNode = flowNodes.lastOrNull { n ->
                                            offset.x >= n.x && offset.x <= n.x + 158f &&
                                                    offset.y >= n.y && offset.y <= n.y + 66f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val node = draggedNode ?: return@detectDragGestures
                                        node.x = (node.x + dragAmount.x).coerceIn(8f, size.width - 162f)
                                        node.y = (node.y + dragAmount.y).coerceIn(8f, size.height - 70f)
                                    },
                                    onDragEnd = { draggedNode = null }
                                )
                            }
                    ) {
                        // Grid Halus Kanvas Node-RED
                        val step = 30f
                        for (gx in 0..(size.width / step).toInt()) {
                            drawLine(Color(0xFF14241D), Offset(gx * step, 0f), Offset(gx * step, size.height), 1f)
                        }
                        for (gy in 0..(size.height / step).toInt()) {
                            drawLine(Color(0xFF14241D), Offset(0f, gy * step), Offset(size.width, gy * step), 1f)
                        }

                        // Gambar Kabel Penghubung Antar-Node + Titik Paket Data yang Mengalir!
                        flowEdges.forEach { edge ->
                            val fromNode = flowNodes.find { it.id == edge.fromNodeId }
                            val toNode = flowNodes.find { it.id == edge.toNodeId }
                            if (fromNode != null && toNode != null) {
                                val start = Offset(fromNode.x + 156f, fromNode.y + 32f)
                                val end = Offset(toNode.x, toNode.y + 32f)
                                SharedBezierPath.reset()
                                SharedBezierPath.moveTo(start.x, start.y)
                                val midX = (start.x + end.x) * 0.5f
                                SharedBezierPath.cubicTo(midX, start.y, midX, end.y, end.x, end.y)
                                drawPath(SharedBezierPath, color = edge.color, style = Stroke(width = 4f, cap = StrokeCap.Round))

                                // Titik Cahaya Paket Data Mengalir dari Input -> Logic -> Output
                                if (isFlowRunning) {
                                    val px = start.x + (end.x - start.x) * pulseProgress
                                    val py = start.y + (end.y - start.y) * pulseProgress
                                    drawCircle(color = Color.White, radius = 6.5f, center = Offset(px, py))
                                    drawCircle(color = edge.color, radius = 4.5f, center = Offset(px, py))
                                }
                            }
                        }

                        // Gambar Blok-Blok Node-RED
                        flowNodes.forEach { node ->
                            val nw = 156f
                            val nh = 64f
                            drawRoundRect(
                                color = Color(0xFF182821),
                                topLeft = Offset(node.x, node.y),
                                size = Size(nw, nh),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            // Pita Warna Kategori di Kiri Node
                            drawRoundRect(
                                color = node.badgeColor,
                                topLeft = Offset(node.x, node.y),
                                size = Size(12f, nh),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawRoundRect(
                                color = node.badgeColor,
                                topLeft = Offset(node.x, node.y),
                                size = Size(nw, nh),
                                cornerRadius = CornerRadius(12f, 12f),
                                style = Stroke(width = 2f)
                            )

                            // Port Konektor Kiri & Kanan
                            drawCircle(color = CopperGold, radius = 5f, center = Offset(node.x, node.y + nh / 2f))
                            drawCircle(color = CopperGold, radius = 5f, center = Offset(node.x + nw, node.y + nh / 2f))

                            drawContext.canvas.nativeCanvas.apply {
                                drawText(node.title, node.x + 20f, node.y + 26f, NodeTitlePaint)
                                val dynamicSub = if (node.id == "n1") "${String.format("%.1f", simulatedTempInput)} °C (Pin D4)"
                                else if (node.id == "n5") if (simulatedTempInput > 36f) "RELAY: HIGH (ON)" else "RELAY: LOW (OFF)"
                                else node.subtitle
                                drawText(dynamicSub, node.x + 20f, node.y + 48f, NodeSubPaint)
                            }
                        }
                    }
                }

                // Slider Uji Trigger Flow Node-RED Secara Langsung
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🌡️ Uji Input Sensor Node-RED: ${String.format("%.1f", simulatedTempInput)} °C", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PcbEmeraldDark)
                    Text(
                        text = if (simulatedTempInput > 36f) "🔥 TRIGGER ACTIVE (>36°C)" else "✅ NORMAL (<36°C)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (simulatedTempInput > 36f) CopperOrange else PcbEmeraldDark
                    )
                }
                Slider(
                    value = simulatedTempInput,
                    onValueChange = { simulatedTempInput = it },
                    valueRange = 20f..55f,
                    colors = SliderDefaults.colors(thumbColor = CopperOrange, activeTrackColor = PcbEmeraldDark)
                )
            }
        }
    }
}