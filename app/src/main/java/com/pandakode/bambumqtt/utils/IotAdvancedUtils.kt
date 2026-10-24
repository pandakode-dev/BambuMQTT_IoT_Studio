package com.pandakode.bambumqtt.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

data class TelemetryRecord(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val topic: String,
    val sensorName: String,
    val value: String,
    val unit: String
)

data class LanNodeItem(
    val ipAddress: String,
    val deviceType: String,
    val rttMs: Long,
    val isOnline: Boolean
)

object IotAdvancedUtils {

    // 1. EXPORT DATA TELEMETRI SENSOR KE FILE .CSV (Downloads/BambuMQTT)
    fun exportTelemetryToCsv(context: Context, records: List<TelemetryRecord>): Boolean {
        if (records.isEmpty()) {
            Toast.makeText(context, "Belum ada rekaman telemetri untuk diekspor!", Toast.LENGTH_SHORT).show()
            return false
        }
        val sb = StringBuilder()
        sb.append("No,Timestamp_WIB,MQTT_Topic,Nama_Sensor,Nilai,Satuan\n")
        records.forEachIndexed { idx, rec ->
            sb.append("${idx + 1},${rec.timestamp},${rec.topic},${rec.sensorName},${rec.value},${rec.unit}\n")
        }

        val fileName = "BambuMQTT_Telemetry_${System.currentTimeMillis() / 1000}.csv"
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BambuMQTT")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val out: OutputStream? = resolver.openOutputStream(uri)
                out?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "✅ CSV Tersimpan: Downloads/BambuMQTT/$fileName", Toast.LENGTH_LONG).show()
                true
            } else false
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal ekspor CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // 2. KIRIM NOTIFIKASI WEBHOOK KE TELEGRAM BOT / DISCORD / INFLUXDB
    suspend fun triggerWebhookAlert(
        webhookUrl: String,
        messageText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(webhookUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true

            val jsonBody = """{"content":"$messageText","text":"$messageText"}"""
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            conn.disconnect()
            if (code in 200..299) {
                Result.success("Webhook Terkirim (HTTP $code): $messageText")
            } else {
                Result.failure(Exception("HTTP Response Code: $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. MQTTS TLS / SSL PORT 8883 HANDSHAKE & CERTIFICATE VERIFIER
    suspend fun testMqttsTlsConnection(
        brokerHost: String,
        tlsPort: Int = 8883
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            val sslSocket = factory.createSocket() as SSLSocket
            sslSocket.connect(InetSocketAddress(brokerHost, tlsPort), 4000)
            sslSocket.soTimeout = 4000
            sslSocket.startHandshake()

            val session = sslSocket.session
            val cipher = session.cipherSuite
            val protocol = session.protocol
            sslSocket.close()

            Result.success("🔒 TLS Handshake Sukses! Protocol: $protocol | Cipher: $cipher")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. PING & SCAN NODE ESP32 DI JARINGAN WI-FI LOKAL
    suspend fun pingHostLatency(ipAddress: String): LanNodeItem = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val addr = InetAddress.getByName(ipAddress)
            val reachable = addr.isReachable(1200)
            val rtt = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            val label = when {
                ipAddress.endsWith(".1") -> "Wi-Fi Router / Gateway"
                ipAddress.endsWith(".100") || ipAddress.endsWith(".250") -> "Local Mosquitto MQTT Broker"
                else -> "ESP32 / IoT Node Active"
            }
            LanNodeItem(ipAddress, label, rtt, reachable)
        } catch (_: Exception) {
            LanNodeItem(ipAddress, "Unreachable Node", 0L, false)
        }
    }

    // 5. PARSER PERINTAH SUARA BAHASA INDONESIA -> AKSI MQTT OTOMATIS
    fun parseIndonesianVoiceCommand(spokenText: String): Triple<String, String, String> {
        val lower = spokenText.lowercase()
        return when {
            lower.contains("nyalakan pompa") || lower.contains("hidupkan pompa") ->
                Triple("pandakode/relay/pump1", "1", "✅ Menyala: Pompa Irigasi Utama diaktifkan!")
            lower.contains("matikan pompa") ->
                Triple("pandakode/relay/pump1", "0", "🛑 Mati: Pompa Irigasi Utama dimatikan!")
            lower.contains("nyalakan lampu") || lower.contains("hidupkan lampu") ->
                Triple("pandakode/relay/light", "1", "💡 Menyala: Lampu Sorot Gudang diaktifkan!")
            lower.contains("matikan lampu") ->
                Triple("pandakode/relay/light", "0", "🌑 Mati: Lampu Sorot Gudang dimatikan!")
            lower.contains("kipas penuh") || lower.contains("kipas maksimal") ->
                Triple("pandakode/pwm/fan", "255", "🌀 PWM 255: Kipas diatur ke kecepatan penuh!")
            lower.contains("cek suhu") || lower.contains("status sensor") ->
                Triple("pandakode/cmd/status", "REQ_TEMP", "🌡️ Meminta laporan suhu DHT22 dari ESP32...")
            else ->
                Triple("pandakode/voice/raw", spokenText, "🎙️ Perintah dikirim ke topik voice: \"$spokenText\"")
        }
    }
}