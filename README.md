# 🐼 BambuMQTT — Pro IoT SCADA Panel, USB/BT Serial Terminal, & Wokwi-Style Simulator
**An Advanced Native Android MQTT v3.1.1 Controller, Serial Waveform Plotter, & Interactive Electronics Simulator**

BambuMQTT adalah aplikasi panel kontrol *Internet of Things* (IoT), terminal komunikasi serial lapangan (*USB OTG & Bluetooth SPP/BLE*), osiloskop grafik (*Serial Plotter*), dan simulator sirkuit elektronik interaktif (*Wokwi-style*) *native* Android yang dirancang khusus untuk *Embedded Engineer* (ESP32/Arduino/ESP8266), teknisi IoT, dan pengembang *Smart Home*. Aplikasi ini mengusung identitas visual papan sirkuit modern dengan tema **Silicon PCB Emerald, Copper Trace Gold, dan Logic Analyzer Cyan**.

---

## 📸 Tampilan Utama (Features Preview)

| Panel SCADA MQTT | BambuSim Interactive | USB OTG & Wi-Fi OTA | Live Serial Plotter | Live Code IDE | Lab MCU & Kamus I2C |
| :---: | :---: | :---: | :---: | :---: | :---: |
| *(Preview SCADA)* | *(Preview BambuSim)* | *(Preview USB/OTA)* | *(Preview Plotter)* | *(Preview IDE)* | *(Preview MCU Lab)* |

---

## ✨ Fitur-Fitur "Super IoT & Embedded Suite" (Core Features)

### 1. 🎮 "BambuSim" — Wokwi-Style Drag & Drop Simulator (New!)
- **Visual Komponen Realistis:** Canvas visual adaptif yang menggambar *ESP32 DevKit V1/ESP8266* (lengkap dengan antena emas & label pin), *OLED Display SSD1306*, *Sensor Suhu DHT22*, *Sensor Jarak HC-SR04*, *Servo SG90*, *Relay 1CH*, *Active Buzzer*, *Potensiometer*, *LED 5mm*, dan *Resistor*.
- **Kalkulator Gelang Warna Dinamis:** Pilih atau ketik nilai hambatan resistor (misal: `220 Ω`, `1 kΩ`, `10 kΩ`), keempat pita warnanya di kanvas akan **berubah warna secara otomatis**!
- **Interactive Drag & Drop:** Geser komponen di kanvas secara bebas menggunakan jari Anda, kabel-kabel *jumper* berwarna akan ikut melengkung secara fleksibel (*Bezier Curves*).
- **Sirkuit Berbasis Daya (VCC & GND):** Komponen **tidak akan aktif/menyala** (menampilkan indikator `UNWIRED` atau `NO POWER`) jika kabel daya `VCC` (3.3V/5V) dan `GND` belum terhubung dengan benar ke pin MCU.
- **Dual-Language Virtual Interpreter:** Tulis kode C++ (Arduino) atau MicroPython, klik **RUN SIM**, dan sirkuit virtual akan berinteraksi secara real-time (LED berkedip, OLED menampilkan teks koding Anda, Servo berputar).

### 2. 🔌 USB OTG Hardware Driver & Wi-Fi OTA Serial Reader (Anti Terpotong)
- **Zero Configuration USB Host:** Deteksi langsung chip jembatan serial yang paling banyak digunakan (*CH340/CH341, CP2102/CP2104, FTDI, Espressif Native USB*) tanpa perlu menginstal driver luar.
- **Live Background Reader:** Membaca data serial (`Serial.println` asli) secara terus-menerus lewat `BulkTransfer` USB OTG atau nirkabel via **Wi-Fi OTA TCP Serial (Telnet/WebSerial Port 23)**.
- **Auto-Reset Bootloader Trigger:** Kirim urutan sinyal hardware `DTR/RTS` dari HP untuk memaksa ESP32 masuk ke *Flash Bootloader Mode* atau melakukan *Hard Reset*.
- **Wi-Fi & MQTT Provisioner:** Tembak konfigurasi *SSID, Password, & IP Broker* baru langsung ke memori NVS/EEPROM ESP32 lewat kabel OTG tanpa perlu membongkar atau kompilasi ulang kode C++.
- **ASCII & HEX View:** Log data yang andal dengan stempel waktu milidetik, dilengkapi tombol-tombol makro cepat (`AT+GMR`, `STATUS?`, `RELAY_ON`).

### 3. 🎛️ Custom IoT SCADA Panel & Auto ESP32 Code Generator
- **Interactive SCADA Widgets:** Tambahkan *Switch Relay*, *PWM Slider (0-255)*, dan *Radial Sensor Gauge* (berbasis Canvas grafis) untuk menguji broker MQTT lokal maupun publik.
- **Auto-Generated C++ (.ino) Sketch:** Menghasilkan kode program utuh berdasarkan widget yang Anda buat di layar HP, siap disalin ke Arduino IDE.
- **Offline Loopback Simulator:** Uji responsivitas widget dan simulasi data tanpa perlu jaringan internet atau broker luar.

### 4. 📈 Live Oscilloscope & Serial Waveform Plotter
- **Dual-Channel Plotter:** Visualisasikan data mentah atau pembacaan sensor analog (seperti sensor getaran, PID, atau ADC) secara real-time.
- **Signal Analyzer:** Lengkap dengan metrik *Peak* (Min/Max) dan tombol **Freeze/Run** untuk mempermudah analisis data lapangan.

### 5. 📦 Payload Studio & Modbus RTU CRC-16
- **Modbus RTU CRC-16 Calculator:** Menghitung checksum heksadesimal 2-byte untuk debugging sensor industri RS485 (*Low Byte First*, polinomial `0xA001`).
- **JSON Payload Builder:** Menguji dan menstandardisasi pengiriman format payload JSON sensor untuk broker MQTT.

### 6. 🔬 Embedded Hardware Lab
- **ESP32 Deep-Sleep Battery Estimator:** Hitung konsumsi arus rata-rata ($\text{mA}$) dan perkiraan umur baterai IoT (dalam hari/bulan) secara akurat.
- **Voltage Divider & ADC Quantizer:** Mengukur resistor pembagi tegangan agar aman (maksimal 3.3V) bagi pin ADC 12-bit ESP32.
- **Interactive ESP32 & ESP8266 Pinout Guide & Kamus I2C:** Referensi cepat pin yang aman digunakan serta identifikasi cepat alamat I2C (seperti `0x3C` untuk OLED, `0x76` untuk BME280) di lapangan.

---

## 📚 Katalog Library Internal (20+ Pre-Installed Libraries)
Aplikasi ini sudah dilengkapi 20 library bawaan siap pakai (tidak perlu instalasi manual):
- **C++ Arduino:** `<WiFi.h>`, `<ESP8266WiFi.h>`, `<PubSubClient.h>`, `<Wire.h>`, `<SPI.h>`, `<Adafruit_SSD1306.h>`, `<LiquidCrystal_I2C.h>`, `<DHT.h>`, `<Adafruit_BME280.h>`, `<DallasTemperature.h>`, `<NewPing.h>`, `<ESP32Servo.h>`, `<Adafruit_NeoPixel.h>`, `<ModbusMaster.h>`, `<ArduinoJson.h>`, `<Preferences.h>`.
- **MicroPython:** `machine` (Pin, ADC, PWM, I2C, UART), `ssd1306`, `dht`, `hcsr04`, `neopixel`, `umqtt.simple`, `urequests`.

---

## 🛠️ Stack Teknologi & Driver API
- **UI & Graphics:** Kotlin, Jetpack Compose, Canvas-based Custom 3D Vector & Bezier Curves.
- **USB & Serial Driver:** Android USB Host API (`bulkTransfer` on Endpoint IN & OUT), USB Request `controlTransfer` (DTR, RTS, Baudrate Config), Android Bluetooth Adapter API.
- **Data & Engine:** Pure Kotlin TCP Socket (Zero Dependencies MQTT v3.1.1 Packet Builder), Modbus RTU Checksum Engine, Slidin Scale Resistor EIA 4-Band Multiplier.

---

## 👨‍💻 Kontributor
Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** (Pekanbaru, Riau, Indonesia)  
🔗 GitHub: [@breto1](https://github.com/breto1)
