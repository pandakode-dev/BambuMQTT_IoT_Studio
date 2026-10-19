package com.pandakode.bambumqtt.model

enum class WidgetType {
    SWITCH_RELAY,
    PWM_SLIDER,
    SENSOR_GAUGE,
    PUSH_BUTTON
}

data class IotWidgetItem(
    val id: Int,
    val title: String,
    val topic: String,
    val gpioPin: Int,
    val type: WidgetType,
    val unit: String = "",
    val minVal: Float = 0f,
    val maxVal: Float = 100f,
    var currentValue: Float = 0f,
    var isOn: Boolean = false
)

data class SerialLogMessage(
    val id: Long = System.nanoTime(),
    val timestamp: String,
    val direction: String, // "TX" atau "RX"
    val asciiText: String,
    val hexText: String
)

data class McuPinInfo(
    val pinLabel: String,
    val gpioNumber: String,
    val capability: String,
    val statusSafety: String,
    val notes: String
)

data class I2cDeviceInfo(
    val hexAddress: String,
    val deviceName: String,
    val category: String,
    val wiringNotes: String
)

data class AutomationRule(
    val id: Int,
    val name: String,
    val sourceTopic: String,
    val conditionOp: String, // ">", "<", "=="
    val thresholdValue: Float,
    val targetTopic: String,
    val actionPayload: String,
    var isEnabled: Boolean = true
)

object EmbeddedMathEngine {

    // 1. Konversi String ASCII ke HEX Dump
    fun asciiToHex(input: String): String {
        return input.toByteArray().joinToString(" ") { byte ->
            String.format("%02X", byte)
        }
    }

    // 2. Kalkulator Checksum Industri MODBUS RTU CRC-16 (Polinomial 0xA001)
    fun calculateModbusCrc16(hexString: String): String {
        val cleanHex = hexString.replace(" ", "").replace("0x", "").trim()
        if (cleanHex.isEmpty() || cleanHex.length % 2 != 0) return "INVALID HEX"
        return try {
            val bytes = ByteArray(cleanHex.length / 2) { i ->
                cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            var crc = 0xFFFF
            for (b in bytes) {
                crc = crc xor (b.toInt() and 0xFF)
                for (i in 0 until 8) {
                    crc = if ((crc and 0x0001) != 0) {
                        (crc ushr 1) xor 0xA001
                    } else {
                        crc ushr 1
                    }
                }
            }
            val lowByte = crc and 0xFF
            val highByte = (crc ushr 8) and 0xFF
            String.format("%02X %02X (CRC-16: 0x%04X)", lowByte, highByte, crc)
        } catch (e: Exception) {
            "FORMAT ERROR"
        }
    }

    // 3. Kalkulator Umur Baterai IoT ESP32 Deep-Sleep
    fun calculateBatteryLifeDays(
        batteryCapacityMah: Double,
        activeCurrentMa: Double,
        activeTimeSeconds: Double,
        sleepCurrentUa: Double,
        wakeIntervalMinutes: Double
    ): Pair<Double, Double> {
        if (wakeIntervalMinutes <= 0 || batteryCapacityMah <= 0) return Pair(0.0, 0.0)
        val totalCycleSeconds = wakeIntervalMinutes * 60.0
        val activeSec = activeTimeSeconds.coerceAtMost(totalCycleSeconds)
        val sleepSec = totalCycleSeconds - activeSec
        val sleepCurrentMa = sleepCurrentUa / 1000.0

        val avgCurrentMa = ((activeCurrentMa * activeSec) + (sleepCurrentMa * sleepSec)) / totalCycleSeconds
        val usableCapacityMah = batteryCapacityMah * 0.85
        val totalHours = if (avgCurrentMa > 0) usableCapacityMah / avgCurrentMa else 0.0
        return Pair(avgCurrentMa, totalHours / 24.0)
    }

    // 4. Database Referensi Pinout Penting ESP32 WROOM-32
    fun getEsp32PinGuide(): List<McuPinInfo> = listOf(
        McuPinInfo("GPIO 4, 16, 17, 18, 19, 21, 22, 23", "Safe GPIOs", "PWM / I2C / SPI / Relay", "AMAN", "Pin paling direkomendasikan untuk Output Relay, Sensor I2C (SDA=21, SCL=22), & Servo."),
        McuPinInfo("GPIO 32, 33", "ADC1 + Touch", "Analog Read (12-bit) & Output", "AMAN", "Aman digunakan untuk membaca sensor analog (0-3.3V) meskipun Wi-Fi & MQTT sedang aktif!"),
        McuPinInfo("GPIO 34, 35, 36 (VP), 39 (VN)", "ADC1 Input-Only", "Hanya Input Analog/Digital", "PERHATIAN", "TIDAK BISA dijadikan Output (pinMode OUTPUT gagal) dan tidak memiliki internal Pull-Up."),
        McuPinInfo("GPIO 0, 2, 12, 15", "Strapping Pins", "Bootloader & Flash Mode", "PERHATIAN", "Jangan ditarik HIGH/LOW sembarangan saat nyala awal, ESP32 bisa gagal booting/upload sketch!"),
        McuPinInfo("GPIO 25, 26, 27, 14", "ADC2 Pins", "DAC Audio / Digital I/O", "PERHATIAN", "Fungsi analogRead() pada pin ADC2 MATI ketika Wi-Fi menyala! Gunakan hanya sebagai Digital I/O."),
        McuPinInfo("GPIO 6, 7, 8, 9, 10, 11", "Internal SPI Flash", "Terhubung ke Chip Memori", "BAHAYA", "JANGAN PERNAH dipakai! Akan menyebabkan ESP32 Crash / Guru Meditation Error.")
    )

    // 5. Database Kamus Alamat Hex I2C (Untuk Identifikasi Cepat saat Scan I2C di Lapangan)
    fun getI2cAddressDirectory(): List<I2cDeviceInfo> = listOf(
        I2cDeviceInfo("0x27 / 0x3F", "LCD Karakter 16x2 / 20x4 (PCF8574)", "Display", "VCC=5V, SDA=GPIO21, SCL=GPIO22. Putar trimpot biru di belakang jika teks tidak muncul."),
        I2cDeviceInfo("0x3C / 0x3D", "OLED Graphic Display 0.96\" SSD1306", "Display", "VCC=3.3V, resolusi 128x64 piksel. Library: Adafruit_SSD1306 / U8g2."),
        I2cDeviceInfo("0x76 / 0x77", "BME280 / BMP280 (Suhu, Tekanan, Elevasi)", "Env Sensor", "VCC=3.3V. Jika SDO ke GND = 0x76, jika SDO ke VCC = 0x77."),
        I2cDeviceInfo("0x68 / 0x69", "MPU6050 (Gyro + Accelerometer) / DS3231 RTC", "IMU / Clock", "Alamat 0x68 dipakai bersama oleh sensor gerak MPU6050 dan modul jam RTC DS3231."),
        I2cDeviceInfo("0x48 - 0x4B", "ADS1115 (16-Bit High Precision ADC)", "Analog Ext", "Mengubah sinyal analog menjadi 16-bit via I2C (bebas noise ADC internal ESP32)."),
        I2cDeviceInfo("0x23 / 0x5C", "BH1750 Digital Light Meter (Lux)", "Optical", "Sensor intensitas cahaya matahari untuk Smart Farming / Panel Surya."),
        I2cDeviceInfo("0x40", "INA219 DC Current & Voltage Sensor / PCA9685", "Power / Servo", "INA219 membaca arus baterai hingga 3.2A; PCA9685 mengontrol 16 channel Servo PWM.")
    )

    // 6. Template Siap Pakai untuk Mobile Code Studio (C++ & MicroPython)
    fun getCodeTemplates(): Map<String, String> = mapOf(
        "MicroPython: Live MQTT + DHT22" to """
            # ==========================================
            # MICROPYTHON LIVE REPL SCRIPT - BAMBUMQTT
            # Bisa di-inject langsung ke ESP32 via USB/BT!
            # ==========================================
            import network, time, machine
            from umqtt.simple import MQTTClient

            WIFI_SSID = "PandaKode_Lab"
            WIFI_PASS = "pekanbaru123"
            BROKER = "broker.hivemq.com"

            led = machine.Pin(2, machine.Pin.OUT)
            adc = machine.ADC(machine.Pin(32))
            adc.atten(machine.ADC.ATTN_11DB) # 0 - 3.3V

            def on_msg(topic, msg):
                print("RX:", topic, msg)
                if msg == b"1": led.on()
                else: led.off()

            wlan = network.WLAN(network.STA_IF)
            wlan.active(True)
            wlan.connect(WIFI_SSID, WIFI_PASS)
            while not wlan.isconnected():
                time.sleep(0.5)

            client = MQTTClient("ESP32_MicroPy", BROKER)
            client.set_callback(on_msg)
            client.connect()
            client.subscribe(b"pandakode/relay/pump1")

            while True:
                client.check_msg()
                val = adc.read()
                client.publish(b"pandakode/sensor/temp", str(val))
                time.sleep(2)
        """.trimIndent(),

        "ESP32 C++: Wi-Fi OTA + MQTT Non-Blocking" to """
            // ==========================================
            // ESP32 C++ OTA + MQTT (TANPA DELAY BLOCKING)
            // ==========================================
            #include <WiFi.h>
            #include <PubSubClient.h>
            #include <ArduinoOTA.h>

            const char* ssid = "NAMA_WIFI";
            const char* pass = "PASSWORD_WIFI";
            const char* broker = "192.168.1.100";

            WiFiClient espClient;
            PubSubClient mqtt(espClient);
            unsigned long lastPublish = 0;

            void setup() {
              Serial.begin(115200);
              pinMode(2, OUTPUT);
              WiFi.begin(ssid, pass);
              while (WiFi.status() != WL_CONNECTED) { delay(300); }

              // Aktifkan Wireless Flash OTA dari BambuMQTT
              ArduinoOTA.setHostname("ESP32-PandaKode");
              ArduinoOTA.begin();

              mqtt.setServer(broker, 1883);
            }

            void loop() {
              ArduinoOTA.handle(); // Siap terima update .bin via Wi-Fi
              if (!mqtt.connected()) {
                mqtt.connect("ESP32_OTA_Node");
              }
              mqtt.loop();

              if (millis() - lastPublish > 2000) {
                lastPublish = millis();
                int rawAdc = analogRead(32);
                mqtt.publish("pandakode/sensor/adc", String(rawAdc).c_str());
              }
            }
        """.trimIndent(),

        "ESP32 C++: I2C Scanner + Modbus RS485" to """
            // ==========================================
            // I2C BUS SCANNER & RS485 DIAGNOSTIC
            // ==========================================
            #include <Wire.h>

            void setup() {
              Serial.begin(115200);
              Wire.begin(21, 22); // SDA=21, SCL=22
              Serial.println("[BambuMQTT] Memindai Bus I2C...");
            }

            void loop() {
              int nDevices = 0;
              for (byte address = 1; address < 127; address++) {
                Wire.beginTransmission(address);
                if (Wire.endTransmission() == 0) {
                  Serial.printf("Ditemukan I2C pada: 0x%02X\n", address);
                  nDevices++;
                }
              }
              if (nDevices == 0) Serial.println("Tidak ada modul I2C terdeteksi.");
              delay(5000);
            }
        """.trimIndent()
    )
}