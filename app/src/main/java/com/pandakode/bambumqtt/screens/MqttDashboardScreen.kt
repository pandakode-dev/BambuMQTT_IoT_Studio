package com.pandakode.bambumqtt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.IotWidgetItem
import com.pandakode.bambumqtt.model.WidgetType
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.MqttNativeEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var brokerHost by remember { mutableStateOf("broker.hivemq.com") }
    var brokerPort by remember { mutableStateOf("1883") }
    var isLoopbackSim by remember { mutableStateOf(true) } // Default Offline Loopback aktif agar langsung responsif
    var statusLog by remember { mutableStateOf("Mode Loopback Offline Aktif (Siap Uji Instan)") }
    var showCodeDialog by remember { mutableStateOf(false) }
    var showAddWidgetDialog by remember { mutableStateOf(false) }

    var widgets by remember {
        mutableStateOf(
            listOf(
                IotWidgetItem(1, "Suhu Boiler DHT22", "pandakode/sensor/temp", 32, WidgetType.SENSOR_GAUGE, "°C", 0f, 100f, 34.6f),
                IotWidgetItem(2, "Kelembaban Tanah", "pandakode/sensor/soil", 33, WidgetType.SENSOR_GAUGE, "%", 0f, 100f, 68.2f),
                IotWidgetItem(3, "Pompa Irigasi Utama", "pandakode/relay/pump1", 23, WidgetType.SWITCH_RELAY, isOn = true),
                IotWidgetItem(4, "Lampu Sorot Gudang", "pandakode/relay/light", 22, WidgetType.SWITCH_RELAY, isOn = false),
                IotWidgetItem(5, "Kecepatan Kipas PWM", "pandakode/pwm/fan", 18, WidgetType.PWM_SLIDER, "PWM", 0f, 255f, 180f)
            )
        )
    }

    // Simulasi Telemetri Sensor Real-time saat mode Loopback aktif
    LaunchedEffect(isLoopbackSim) {
        while (isLoopbackSim) {
            delay(2200L)
            widgets = widgets.map { w ->
                if (w.type == WidgetType.SENSOR_GAUGE) {
                    val delta = (Random.nextFloat() - 0.48f) * 2.5f
                    w.copy(currentValue = (w.currentValue + delta).coerceIn(w.minVal, w.maxVal))
                } else w
            }
        }
    }

    fun sendMqttAction(topic: String, payload: String) {
        if (isLoopbackSim) {
            statusLog = "[LOOPBACK TX] Topic: $topic | Payload: $payload"
        } else {
            statusLog = "Mengirim paket TCP MQTT ke $brokerHost..."
            scope.launch {
                val port = brokerPort.toIntOrNull() ?: 1883
                val res = MqttNativeEngine.publishMqttPacketTcp(
                    brokerHost = brokerHost,
                    brokerPort = port,
                    clientId = "BambuMQTT_${Random.nextInt(1000, 9999)}",
                    topic = topic,
                    payload = payload
                )
                statusLog = res.getOrElse { "Gagal TCP (${it.message}) - Gunakan Loopback untuk tes offline" }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. HEADER KONFIGURASI BROKER MQTT & TOMBOL AUTO-CODE GENERATOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hub, contentDescription = null, tint = CopperGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "MQTT BROKER ENGINE (PORT 1883)",
                                    color = CopperGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = statusLog,
                                    color = PcbMintLight,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brokerHost,
                            onValueChange = { brokerHost = it },
                            label = { Text("Broker IP / Host", color = PcbMintLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CopperGold,
                                unfocusedBorderColor = PcbMintLight.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1.8f)
                        )
                        OutlinedTextField(
                            value = brokerPort,
                            onValueChange = { brokerPort = it },
                            label = { Text("Port", color = PcbMintLight) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CopperGold,
                                unfocusedBorderColor = PcbMintLight.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isLoopbackSim,
                                onCheckedChange = {
                                    isLoopbackSim = it
                                    statusLog = if (it) "Mode Loopback Offline Aktif" else "Mode Live TCP Socket ($brokerHost)"
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = CopperGold, checkedTrackColor = PcbEmeraldMedium)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loopback Simulator", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Tombol Fitur Killer: Auto Generate Kode ESP32 (.ino)
                        Button(
                            onClick = { showCodeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kode .INO", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. BARIS RADIAL SENSOR GAUGES
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRI SENSOR & AKTUATOR REAL-TIME",
                    fontWeight = FontWeight.ExtraBold,
                    color = PcbEmeraldDark,
                    fontSize = 13.sp
                )
                TextButton(onClick = { showAddWidgetDialog = true }) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = CopperOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Tambah Widget", color = CopperOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            val gauges = widgets.filter { it.type == WidgetType.SENSOR_GAUGE }
            if (gauges.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gauges.take(2).forEach { gauge ->
                        RadialGaugeCard(
                            widget = gauge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. DAFTAR WIDGET SWITCH RELAY & SLIDER PWM
        items(widgets.filter { it.type != WidgetType.SENSOR_GAUGE }, key = { it.id }) { widget ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (widget.isOn || widget.currentValue > 0) PcbEmeraldDark else Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (widget.type == WidgetType.SWITCH_RELAY) Icons.Default.PowerSettingsNew else Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (widget.isOn || widget.currentValue > 0) CopperGold else Color.DarkGray
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(widget.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PcbEmeraldDark)
                                Text(
                                    text = "Topic: ${widget.topic} • GPIO ${widget.gpioPin}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        if (widget.type == WidgetType.SWITCH_RELAY) {
                            Switch(
                                checked = widget.isOn,
                                onCheckedChange = { checked ->
                                    widgets = widgets.map {
                                        if (it.id == widget.id) it.copy(isOn = checked) else it
                                    }
                                    sendMqttAction(widget.topic, if (checked) "1" else "0")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CopperGold,
                                    checkedTrackColor = PcbEmeraldDark
                                )
                            )
                        } else {
                            Text(
                                text = "${widget.currentValue.toInt()} / ${widget.maxVal.toInt()}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = CopperOrange,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (widget.type == WidgetType.PWM_SLIDER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = widget.currentValue,
                            onValueChange = { newVal ->
                                widgets = widgets.map {
                                    if (it.id == widget.id) it.copy(currentValue = newVal) else it
                                }
                            },
                            onValueChangeFinished = {
                                sendMqttAction(widget.topic, widget.currentValue.toInt().toString())
                            },
                            valueRange = widget.minVal..widget.maxVal,
                            colors = SliderDefaults.colors(
                                thumbColor = CopperOrange,
                                activeTrackColor = PcbEmeraldDark
                            )
                        )
                    }
                }
            }
        }
    }

    // DIALOG AUTO-GENERATED ESP32 ARDUINO CODE (.INO)
    if (showCodeDialog) {
        val generatedCode = remember(widgets, brokerHost, brokerPort) {
            MqttNativeEngine.generateEsp32ArduinoSketch(
                brokerIp = brokerHost,
                brokerPort = brokerPort.toIntOrNull() ?: 1883,
                widgets = widgets
            )
        }
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = {
                Text("Auto-Generated ESP32 Sketch (.ino)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = generatedCode,
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ESP32_MQTT_Sketch", generatedCode))
                        Toast.makeText(context, "Kode C++ ESP32 berhasil disalin ke Clipboard!", Toast.LENGTH_LONG).show()
                        showCodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark)
                ) {
                    Text("Salin Kode (.ino)", color = CopperGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) { Text("Tutup") }
            }
        )
    }

    // DIALOG TAMBAH WIDGET BARU
    if (showAddWidgetDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newTopic by remember { mutableStateOf("pandakode/io/pin4") }
        var newPin by remember { mutableStateOf("4") }
        var selectedType by remember { mutableStateOf(WidgetType.SWITCH_RELAY) }

        AlertDialog(
            onDismissRequest = { showAddWidgetDialog = false },
            title = { Text("Tambah Widget Panel IoT", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Nama Perangkat / Sensor") })
                    OutlinedTextField(value = newTopic, onValueChange = { newTopic = it }, label = { Text("MQTT Topic") })
                    OutlinedTextField(value = newPin, onValueChange = { newPin = it }, label = { Text("Pin GPIO ESP32") })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedType == WidgetType.SWITCH_RELAY,
                            onClick = { selectedType = WidgetType.SWITCH_RELAY },
                            label = { Text("Switch Relay") }
                        )
                        FilterChip(
                            selected = selectedType == WidgetType.PWM_SLIDER,
                            onClick = { selectedType = WidgetType.PWM_SLIDER },
                            label = { Text("Slider PWM") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nextId = widgets.size + 1
                        widgets = widgets + IotWidgetItem(
                            id = nextId,
                            title = if (newTitle.isBlank()) "Aktuator #$nextId" else newTitle,
                            topic = newTopic,
                            gpioPin = newPin.toIntOrNull() ?: 4,
                            type = selectedType,
                            maxVal = 255f
                        )
                        showAddWidgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark)
                ) {
                    Text("Simpan Widget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWidgetDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun RadialGaugeCard(widget: IotWidgetItem, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SiliconDark),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = widget.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = widget.topic,
                color = LogicCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(115.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Busur Latar Belakang
                    drawArc(
                        color = TerminalBorder,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Busur Nilai Sensor
                    val fraction = ((widget.currentValue - widget.minVal) / (widget.maxVal - widget.minVal)).coerceIn(0f, 1f)
                    drawArc(
                        color = if (fraction > 0.8f) CopperOrange else LogicNeonGreen,
                        startAngle = 135f,
                        sweepAngle = 270f * fraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f%s", widget.currentValue, widget.unit),
                        color = CopperGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "GPIO ${widget.gpioPin}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}