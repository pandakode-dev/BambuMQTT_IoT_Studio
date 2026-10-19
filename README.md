# 🐼 BambuMQTT — Pro IoT SCADA Panel, Blynk Dashboard, USB/OTA Serial & Wokwi Simulator
**An Advanced Native Android MQTT v3.1.1 Controller, Blynk-Style Cloud Studio, USB OTG Serial Terminal, & Zero-Lag Circuit Simulator**

BambuMQTT adalah aplikasi panel kontrol *Internet of Things* (IoT), studio dashboard bergaya *Blynk*, terminal komunikasi serial lapangan (*USB OTG & Wi-Fi OTA*), osiloskop grafik (*Serial Plotter*), dan simulator sirkuit elektronik interaktif (*Wokwi-style*) *native* Android yang dirancang khusus untuk *Embedded Engineer* (ESP32/ESP8266/Arduino), teknisi otomasi PLC/Modbus, dan pengembang *Smart Home*. Aplikasi ini mengusung identitas visual papan sirkuit tercetak modern dengan tema **Silicon PCB Emerald, Copper Trace Gold, dan Logic Analyzer Cyan**.

---

## 📸 Tampilan Utama (Features Preview)

| Panel SCADA MQTT | Kanvas Wokwi Sim | Blynk IoT Dashboard | USB OTG & Wi-Fi OTA | Live Code IDE & OTA | Lab MCU & Kamus I2C |
| :---: | :---: | :---: | :---: | :---: | :---: |
| <img width="216" height="480" alt="Panel SCADA MQTT" src="https://github.com/user-attachments/assets/3df22ea0-ffb8-4ca6-8ae2-d56fff91a9d6" /> | <img width="216" height="480" alt="Kanvas Wokwi Sim" src="https://github.com/user-attachments/assets/dceb5fe1-2a28-4feb-8d0b-5f461b52d111" /> | <img width="216" height="480" alt="Blynk IoT Dashboard" src="https://github.com/user-attachments/assets/d78e6687-e59e-4b13-84c6-c90d68abbf71" /> | <img width="216" height="480" alt="USB OTG   Wi-Fi OTA" src="https://github.com/user-attachments/assets/83f8ec5f-bc96-41b0-a66d-98465ec125c7" /> | <img width="216" height="480" alt="Live Code IDE   OTA" src="https://github.com/user-attachments/assets/eee70506-f572-411b-afc0-d6deeb787d9a" />
 | <img width="216" height="480" alt="Lab MCU   Kamus I2C" src="https://github.com/user-attachments/assets/6baccae6-aa5f-4f74-8bdd-cc0952425507" /> |

---

## ✨ Fitur-Fitur "Super IoT & Embedded Suite" (Core Features)

### 1. 🎮 "BambuSim" — Zero-Lag Wokwi-Style Circuit Simulator
- **Visual Komponen Realistis Berlabel Pin:** Merender *ESP32 DevKit V1 / ESP8266 NodeMCU* (lengkap dengan label kaki `3V3`, `5V`, `GND`, `D2`, `D4`, `D5`, `D18`, `D19`, `D21`, `D22`, `D34`), *OLED SSD1306 I2C*, *Sensor DHT22*, *Ultrasonik HC-SR04*, *Servo SG90*, *Modul Relay 1CH*, *Buzzer Piezo*, *Potensiometer*, *LED 5mm*, dan *Resistor 4-Gelang*.
- **Zero-Lag 120 FPS Drag & Drop + Tap-to-Move:** Arsitektur *Direct MutableFloatState* dan *Shared Native Paint* memastikan pergeseran komponen sangat mulus tanpa jeda, dilengkapi fitur **Tap-to-Teleport** (ketuk komponen lalu ketuk ruang kosong untuk pindah instan) serta tombol **✨ Auto-Rapikan** agar komponen tidak menumpuk.
- **Kalkulator Gelang Warna Resistor Dinamis:** Pilih nilai hambatan (`100 Ω`, `220 Ω`, `330 Ω`, `1k Ω`, `4.7k Ω`, `10k Ω`), keempat pita warna pada tubuh resistor langsung berubah warna secara *real-time* sesuai standar EIA internasional!
- **Validasi Daya Sirkuit Nyata (`VCC` & `GND`):** Komponen **tidak akan aktif/menyala** (menampilkan peringatan `UNWIRED` / `NO VCC`) apabila kaki `VCC` dan `GND` belum dihubungkan ke pin daya mikrokontroler. Dilengkapi tombol **⚡ Auto-Wire Semua Pin** dalam 1 klik.
- **Dual-Language Virtual Interpreter (C++ & MicroPython):** Tulis koding Arduino C++ (`.ino`) atau MicroPython (`.py`) dan jalankan simulasi secara langsung.

### 2. 📱 "Bambu-Blynk" — Interactive IoT Cloud Dashboard (12 Jenis Widget)
- **Sinkronisasi Langsung dengan Simulator & MQTT:** Dilengkapi 12 variasi widget bergaya *Blynk IoT*: *Toggle Switch*, *Push Button Momentary*, *Rotary Knob*, *PWM Slider*, *Radial Gauge*, *Digital Value Display*, *Status Alarm LED*, *RGB NeoPixel Color Picker*, *Stepper Input (+/-)*, *Live SuperChart*, *Virtual LCD 16x2*, dan *Cloud Terminal*.
- **Custom Widget Builder:** Tambahkan widget baru pada *Virtual Pin (`V1` – `V12`)* secara dinamis dalam hitungan detik.

### 3. 🔌 USB OTG Hardware Driver & Wi-Fi OTA Serial Monitor
- **Native Kernel USB Host Driver (`usb_device_filter.xml`):** Mendukung penuh chip **WCH CH340/CH9102**, **Silicon Labs CP2102/CP2104**, **FTDI FT232RL**, **Espressif Native USB (ESP32-S3/C3)**, dan **Arduino Official** tanpa instalasi aplikasi tambahan.
- **Live Background Serial Reader (USB & Wi-Fi OTA):** Membaca aliran data `Serial.println()` ESP32 secara *real-time* baik melalui kabel USB OTG (`BulkTransfer Endpoint IN`) maupun nirkabel via **Wi-Fi OTA TCP Stream (Port 23)**.
- **Auto-Reset Bootloader (`DTR/RTS`) & NVS Wi-Fi Provisioner:** Memicu ESP32 masuk ke *Flash Download Mode* langsung dari HP, serta mengganti *SSID Wi-Fi, Password, & IP Broker MQTT* ke memori NVS ESP32 lewat kabel OTG tanpa perlu kompilasi ulang kode C++!

### 4. 🎛️ Custom IoT SCADA Panel & Auto `.INO` Sketch Generator (Port 1883)
- **Pure Native TCP MQTT v3.1.1 Engine:** Mengirim dan menguji paket protokol MQTT langsung di atas *native socket* lengkap dengan **Offline Loopback Simulator**.
- **Auto ESP32 Sketch Generator:** Menghasilkan kode program C++ utuh (`WiFi.h` + `PubSubClient.h`) secara otomatis berdasarkan daftar *Topic* dan *Pin GPIO* yang Anda rancang di layar HP.

### 5. 📈 Live Oscilloscope Plotter, Modbus CRC-16 & Hardware Lab
- **Dual-Channel Serial Waveform Plotter:** Osiloskop grafik serial *real-time* dengan fitur *Freeze/Run* dan *Peak Inspector (Min/Max)*.
- **Industrial Modbus RTU CRC-16 & JSON Studio:** Penghitung checksum 2-byte heksadesimal (`0xA001`) untuk sensor industri RS485 dan validator JSON MQTT.
- **Embedded Hardware Lab:** Estimator umur baterai **ESP32 Deep-Sleep**, kalkulator pembagi tegangan aman **ADC 3.3V (12-Bit)**, tabel keamanan **Pinout ESP32**, dan **Kamus Alamat Sensor I2C (`0x27`, `0x3C`, `0x76`, `0x68`)**.

---

## 📚 Katalog 20+ Library Internal (Pre-Installed)
- **C++ Arduino:** `<WiFi.h>`, `<ESP8266WiFi.h>`, `<PubSubClient.h>`, `<BlynkSimpleEsp32.h>`, `<Wire.h>`, `<SPI.h>`, `<Adafruit_SSD1306.h>`, `<LiquidCrystal_I2C.h>`, `<DHT.h>`, `<Adafruit_BME280.h>`, `<DallasTemperature.h>`, `<NewPing.h>`, `<ESP32Servo.h>`, `<Adafruit_NeoPixel.h>`, `<Adafruit_MPU6050.h>`, `<BH1750.h>`, `<Adafruit_INA219.h>`, `<ModbusMaster.h>`, `<ArduinoJson.h>`, `<Preferences.h>`.
- **MicroPython:** `machine` (Pin, ADC, PWM, I2C, UART), `ssd1306`, `dht`, `hcsr04`, `neopixel`, `umqtt.simple`, `blynklib`, `urequests`.

---

## 👨‍💻 Kontributor
Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** (Pekanbaru, Riau, Indonesia)  
🔗 GitHub: [@breto1](https://github.com/breto1)
