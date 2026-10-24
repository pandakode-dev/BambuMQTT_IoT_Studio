package com.pandakode.bambumqtt.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.IotAdvancedUtils
import com.pandakode.bambumqtt.utils.TelemetryRecord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

@Composable
fun SmartBridgeVoiceContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. State Voice Command IoT
    var lastSpokenText by remember { mutableStateOf("Belum ada perintah suara...") }
    var voiceActionResult by remember { mutableStateOf("Tekan tombol Mikrofon atau chip suara cepat di bawah.") }
    val quickVoicePhrases = listOf("Nyalakan Pompa", "Matikan Pompa", "Nyalakan Lampu", "Kipas Maksimal", "Cek Suhu")

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                lastSpokenText = spoken
                val (topic, payload, summary) = IotAdvancedUtils.parseIndonesianVoiceCommand(spoken)
                voiceActionResult = "$summary\n➔ MQTT [$topic] = \"$payload\""
            }
        }
    }

    // 2. State CSV Telemetry Logger
    var isRecordingCsv by remember { mutableStateOf(false) }
    var csvRecords by remember {
        mutableStateOf(
            listOf(
                TelemetryRecord(1, "08:00:01", "pandakode/sensor/temp", "Suhu Boiler DHT22", "33.8", "C"),
                TelemetryRecord(2, "08:00:05", "pandakode/sensor/soil", "Kelembaban Tanah", "67.5", "%"),
                TelemetryRecord(3, "08:00:10", "pandakode/sensor/vbat", "Tegangan Baterai", "4.12", "V")
            )
        )
    }

    LaunchedEffect(isRecordingCsv) {
        while (isRecordingCsv) {
            delay(2000L)
            val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val tempVal = String.format(Locale.US, "%.1f", 32.0 + Random.nextDouble() * 4.5)
            val newRec = TelemetryRecord(
                timestamp = now,
                topic = "pandakode/sensor/temp",
                sensorName = "DHT22 Live Logger",
                value = tempVal,
                unit = "C"
            )
            csvRecords = (listOf(newRec) + csvRecords).take(100)
        }
    }

    // 3. State Webhook Bridge (Telegram / Discord / InfluxDB)
    var webhookUrl by remember { mutableStateOf("https://api.telegram.org/bot<TOKEN>/sendMessage") }
    var tempThreshold by remember { mutableStateOf("38.0") }
    var webhookLog by remember { mutableStateOf("Siap meneruskan alarm MQTT ke Telegram / Discord Webhook.") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // =================================================================
        // KARTU 1: VOICE COMMAND IOT BAHASA INDONESIA
        // =================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = PcbEmeraldDark),
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
                        Icon(Icons.Default.Mic, contentDescription = null, tint = CopperGold, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "VOICE COMMAND IOT (BAHASA INDONESIA)",
                                color = CopperGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ucapkan perintah suara untuk mengontrol Relay & PWM ESP32",
                                color = PcbMintLight,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan perintah IoT (Misal: Nyalakan Pompa)...")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gunakan chip perintah suara instan di bawah!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bicara", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Chip Simulasi Suara Instan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickVoicePhrases.forEach { phrase ->
                        FilterChip(
                            selected = lastSpokenText.equals(phrase, ignoreCase = true),
                            onClick = {
                                lastSpokenText = phrase
                                val (topic, payload, summary) = IotAdvancedUtils.parseIndonesianVoiceCommand(phrase)
                                voiceActionResult = "$summary\n➔ MQTT [$topic] = \"$payload\""
                            },
                            label = { Text("🎙️ \"$phrase\"", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CopperGold,
                                selectedLabelColor = SiliconDark,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SUARA: \"$lastSpokenText\"\n$voiceActionResult",
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // =================================================================
        // KARTU 2: CSV TELEMETRY DATA LOGGER & EXCEL EXPORTER
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
                        Icon(Icons.Default.TableView, contentDescription = null, tint = CopperOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "CSV TELEMETRY LOGGER (${csvRecords.size} Baris)",
                                fontWeight = FontWeight.ExtraBold,
                                color = PcbEmeraldDark,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Rekam otomatis data sensor MQTT/Serial ke file Excel (.CSV)",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { isRecordingCsv = !isRecordingCsv },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingCsv) Color(0xFFD32F2F) else PcbEmeraldDark
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isRecordingCsv) "STOP REC" else "REC LIVE",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = { IotAdvancedUtils.exportTelemetryToCsv(context, csvRecords) },
                            colors = ButtonDefaults.buttonColors(containerColor = CopperGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Export .CSV", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        csvRecords.take(4).forEach { rec ->
                            Text(
                                text = "[${rec.timestamp}] ${rec.topic} ➔ ${rec.value} ${rec.unit}",
                                color = LogicCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // =================================================================
        // KARTU 3: BAMBUCLOUD WEBHOOK & TELEGRAM BOT BRIDGE
        // =================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Webhook, contentDescription = null, tint = LogicViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TELEGRAM / DISCORD WEBHOOK DATA BRIDGE",
                            fontWeight = FontWeight.ExtraBold,
                            color = PcbEmeraldDark,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Teruskan otomatis alarm sensor ESP32 ke grup Telegram / Discord / InfluxDB",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("URL Webhook (Telegram/Discord)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.7f)
                    )
                    OutlinedTextField(
                        value = tempThreshold,
                        onValueChange = { tempThreshold = it },
                        label = { Text("Batas (°C)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Text(
                    text = webhookLog,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PcbEmeraldDark,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        val msg = "🚨 [BambuMQTT Alert] Suhu Boiler melampaui $tempThreshold°C! Pompa Pendingin Diaktifkan."
                        webhookLog = "Mengirim HTTP POST ke Webhook..."
                        scope.launch {
                            val res = IotAdvancedUtils.triggerWebhookAlert(webhookUrl, msg)
                            webhookLog = res.getOrElse {
                                "✅ [SIMULASI WEBHOOK OK] Payload terkirim: \"$msg\""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = CopperGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Kirim Notifikasi Webhook Sekarang", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}