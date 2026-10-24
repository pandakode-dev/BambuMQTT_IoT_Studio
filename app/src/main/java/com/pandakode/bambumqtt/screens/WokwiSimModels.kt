package com.pandakode.bambumqtt.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.pandakode.bambumqtt.ui.theme.*
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object BambuVirtualNetwork {
    const val VIRTUAL_SSID = "PandaKode_SimNet"
    const val VIRTUAL_PASS = "blynk1234"
    const val VIRTUAL_BLYNK_TOKEN = "BAMBU-BLYNK-2026"
}

// PALET 10 WARNA KABEL JUMPER STANDAR WOKWI / ELEKTRONIKA
enum class JumperWireColor(val label: String, val color: Color) {
    RED_VCC("Merah (VCC)", Color(0xFFFF1744)),
    BLACK_GND("Abu/Hitam (GND)", Color(0xFF90A4AE)),
    GOLD_SDA("Kuning Emas", Color(0xFFFFB703)),
    CYAN_DATA("Biru Cyan", Color(0xFF00E5FF)),
    GREEN_SCL("Hijau Neon", Color(0xFF00E676)),
    ORANGE_PWM("Oranye", Color(0xFFFB8500)),
    VIOLET_RX("Ungu", Color(0xFFAA00FF)),
    BLUE_TX("Biru Tua", Color(0xFF2979FF)),
    WHITE_AUX("Putih", Color(0xFFFFFFFF)),
    BROWN_AIN("Coklat", Color(0xFF8D6E63))
}

enum class McuBoardVariant(
    val id: String,
    val displayName: String,
    val chipName: String,
    val totalPhysicalPins: Int,
    val pcbColor: Color,
    val leftPins: List<String>,
    val rightPins: List<String>,
    val boardWidth: Float,
    val boardHeight: Float
) {
    ESP32_30PIN(
        id = "esp32_30",
        displayName = "ESP32 DevKit V1 (30-Pin)",
        chipName = "ESP32-WROOM-32",
        totalPhysicalPins = 30,
        pcbColor = Color(0xFF1A221E),
        leftPins = listOf("EN", "VP", "VN", "D34", "D35", "D32", "D33", "D25", "D26", "D27", "D14", "D12", "D13", "GND", "VIN"),
        rightPins = listOf("3V3", "GND", "D15", "D2", "D4", "RX2", "TX2", "D5", "D18", "D19", "D21", "RX0", "TX0", "D22", "D23"),
        boardWidth = 195f,
        boardHeight = 295f
    ),
    ESP32_38PIN(
        id = "esp32_38",
        displayName = "ESP32 NodeMCU (38-Pin Lengkap)",
        chipName = "ESP32-WROOM-32D",
        totalPhysicalPins = 38,
        pcbColor = Color(0xFF141B29),
        leftPins = listOf("3V3", "EN", "VP", "VN", "D34", "D35", "D32", "D33", "D25", "D26", "D27", "D14", "D12", "GND", "D13", "SD2", "SD3", "CMD", "5V"),
        rightPins = listOf("GND", "D23", "D22", "TX0", "RX0", "D21", "GND", "D19", "D18", "D5", "D17", "D16", "D4", "D0", "D2", "D15", "SD1", "SD0", "CLK"),
        boardWidth = 200f,
        boardHeight = 330f
    ),
    WEMOS_D1_MINI(
        id = "wemos_d1",
        displayName = "Wemos D1 Mini (16-Pin Ringkas)",
        chipName = "ESP8266EX-MINI",
        totalPhysicalPins = 16,
        pcbColor = Color(0xFF0D47A1),
        leftPins = listOf("RST", "A0", "D0", "D5", "D6", "D7", "D8", "3V3"),
        rightPins = listOf("TX", "RX", "D1", "D2", "D3", "D4", "GND", "5V"),
        boardWidth = 175f,
        boardHeight = 195f
    ),
    ESP32_C3_MINI(
        id = "esp32_c3",
        displayName = "ESP32-C3 SuperMini (13-Pin)",
        chipName = "RISC-V ESP32-C3",
        totalPhysicalPins = 13,
        pcbColor = Color(0xFF4A148C),
        leftPins = listOf("5V", "GND", "3V3", "D4", "D3", "D2", "D1", "D0"),
        rightPins = listOf("D21", "D20", "D10", "D9", "D8", "D7", "D6", "D5"),
        boardWidth = 170f,
        boardHeight = 190f
    ),
    NODEMCU_ESP8266(
        id = "nodemcu_8266",
        displayName = "NodeMCU ESP8266 V3 (30-Pin)",
        chipName = "ESP-12E 8266",
        totalPhysicalPins = 30,
        pcbColor = Color(0xFF102027),
        leftPins = listOf("A0", "GND", "VU", "S3", "S2", "S1", "SC", "S0", "SK", "GND", "3V3", "EN", "RST", "GND", "VIN"),
        rightPins = listOf("D0", "D1", "D2", "D3", "D4", "3V3", "GND", "D5", "D6", "D7", "D8", "RX", "TX", "GND", "3V3"),
        boardWidth = 195f,
        boardHeight = 290f
    ),
    ARDUINO_UNO_R3(
        id = "arduino_uno",
        displayName = "Arduino Uno R3 (ATmega328P)",
        chipName = "ATMEGA328P-PU",
        totalPhysicalPins = 28,
        pcbColor = Color(0xFF006064),
        leftPins = listOf("3V3", "5V", "GND", "VIN", "A0", "A1", "A2", "A3", "A4", "A5"),
        rightPins = listOf("D13", "D12", "D11", "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "TX", "RX"),
        boardWidth = 200f,
        boardHeight = 280f
    ),
    ARDUINO_NANO(
        id = "arduino_nano",
        displayName = "Arduino Nano V3 (30-Pin)",
        chipName = "ATMEGA328P-AU",
        totalPhysicalPins = 30,
        pcbColor = Color(0xFF01579B),
        leftPins = listOf("D13", "3V3", "REF", "A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7", "5V", "RST", "GND", "VIN"),
        rightPins = listOf("D12", "D11", "D10", "D9", "D8", "D7", "D6", "D5", "D4", "D3", "D2", "GND", "RST", "RX0", "TX1"),
        boardWidth = 185f,
        boardHeight = 290f
    ),
    RASPBERRY_PI_PICO(
        id = "raspi_pico",
        displayName = "Raspberry Pi Pico W (RP2040)",
        chipName = "RP2040 DUAL M0+",
        totalPhysicalPins = 40,
        pcbColor = Color(0xFF1B5E20),
        leftPins = listOf("GP0", "GP1", "GND", "GP2", "GP3", "GP4", "GP5", "GND", "GP6", "GP7", "GP8", "GP9", "GND", "GP10", "GP11"),
        rightPins = listOf("VBUS", "VSYS", "GND", "3V3", "ADC2", "ADC1", "ADC0", "GND", "GP22", "GP21", "GP20", "GP19", "GP18", "GND", "GP16"),
        boardWidth = 190f,
        boardHeight = 295f
    );

    val allConnectablePins: List<String>
        get() = (rightPins + leftPins).distinct()
}

object ResistorCatalog {
    val standardValuesOhms = listOf(
        10, 22, 33, 47, 68,
        100, 150, 220, 330, 470, 560, 680,
        1000, 1500, 2200, 3300, 4700, 6800,
        10000, 22000, 47000, 100000, 470000, 1000000
    )

    fun formatOhmLabel(ohms: Int): String {
        return when {
            ohms >= 1_000_000 -> "${ohms / 1_000_000}M Ω"
            ohms >= 1_000 -> {
                val k = ohms / 1000.0
                if (ohms % 1000 == 0) "${ohms / 1000}k Ω" else "${k}k Ω"
            }
            else -> "$ohms Ω"
        }
    }

    fun digitToColor(digit: Int): Color {
        return when (digit.coerceIn(0, 9)) {
            0 -> Color(0xFF141414)
            1 -> Color(0xFF6D4C41)
            2 -> Color(0xFFD50000)
            3 -> Color(0xFFFF6D00)
            4 -> Color(0xFFFFEA00)
            5 -> Color(0xFF00C853)
            6 -> Color(0xFF2962FF)
            7 -> Color(0xFFAA00FF)
            8 -> Color(0xFF757575)
            else -> Color(0xFFFFFFFF)
        }
    }

    fun digitToName(digit: Int): String {
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

    fun getFourBandColors(ohms: Int): List<Color> {
        val safe = ohms.coerceAtLeast(10)
        val exp = (floor(log10(safe.toDouble())).toInt() - 1).coerceIn(0, 9)
        val twoDigits = (safe / 10.0.pow(exp.toDouble())).toInt().coerceIn(10, 99)
        val d1 = twoDigits / 10
        val d2 = twoDigits % 10
        return listOf(
            digitToColor(d1),
            digitToColor(d2),
            digitToColor(exp),
            Color(0xFFFFD700)
        )
    }

    fun getFourBandNames(ohms: Int): String {
        val safe = ohms.coerceAtLeast(10)
        val exp = (floor(log10(safe.toDouble())).toInt() - 1).coerceIn(0, 9)
        val twoDigits = (safe / 10.0.pow(exp.toDouble())).toInt().coerceIn(10, 99)
        val d1 = twoDigits / 10
        val d2 = twoDigits % 10
        return "${digitToName(d1)} • ${digitToName(d2)} • ${digitToName(exp)} • Emas (±5%)"
    }
}

enum class SimCompType(val title: String, val pinNames: List<String>, val hitWidth: Float, val hitHeight: Float) {
    ESP32_BOARD("Mikrokontroler Utama", emptyList(), 195f, 295f),
    LED_LAMP("LED 5mm Merah", listOf("A+", "K-"), 72f, 95f),
    RESISTOR_4BAND("Resistor 4-Gelang", listOf("P1", "P2"), 130f, 58f),
    DHT22_SENSOR("Sensor Suhu DHT22", listOf("VCC", "DATA", "NC", "GND"), 105f, 130f),
    OLED_SSD1306("OLED 0.96\" I2C", listOf("GND", "VCC", "SCL", "SDA"), 148f, 105f),
    SERVO_SG90("Motor Servo SG90", listOf("GND", "VCC", "SIG"), 105f, 100f),
    HCSR04_SONAR("Ultrasonik HC-SR04", listOf("VCC", "TRIG", "ECHO", "GND"), 135f, 100f),
    RELAY_1CH("Modul Relay 1CH", listOf("VCC", "GND", "IN"), 100f, 105f),
    BUZZER_ACTIVE("Buzzer Piezo", listOf("VCC+", "GND-"), 82f, 95f),
    POTENTIOMETER("Potensiometer Analog", listOf("VCC", "OUT", "GND"), 92f, 95f)
}

@Stable
class SimComponent(
    val id: String,
    val type: SimCompType,
    val label: String,
    initialX: Float,
    initialY: Float
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
}

// MODEL KABEL JUMPER DENGAN WARNA DINAMIS & KOORDINAT BELOKAN (WAYPOINT) YANG BISA DIGESER JARI!
@Stable
class classPinWireConnection(
    val id: String,
    val fromCompId: String,
    val fromPinName: String,
    val toMcuPin: String,
    initialColor: Color,
    initialBendX: Float = Float.NaN, // NaN = Gunakan kanal default sampai digeser pengguna
    initialBendY: Float = Float.NaN
) {
    var wireColor by mutableStateOf(initialColor)
    var bendX by mutableFloatStateOf(initialBendX)
    var bendY by mutableFloatStateOf(initialBendY)
}

enum class DashWidgetType(val label: String) {
    TOGGLE_SWITCH("Switch Relay"),
    PUSH_BUTTON("Push Button"),
    KNOB_ROTARY("Rotary Knob"),
    SLIDER_LEVEL("PWM Slider"),
    GAUGE_RADIAL("Radial Gauge"),
    VALUE_DISPLAY("Digital Value"),
    LED_INDICATOR("Status LED"),
    RGB_COLOR_PICKER("RGB Zebra Picker"),
    HISTORICAL_CHART("SuperChart Live"),
    STEP_INPUT("Step Control (+/-)"),
    LCD_16X2("Virtual LCD 16x2"),
    TERMINAL_MINI("Blynk Terminal")
}

data class DashWidget(
    val id: String,
    val type: DashWidgetType,
    val title: String,
    val topic: String,
    val pin: String,
    var value: Float = 0f,
    var isOn: Boolean = false,
    val unit: String = "",
    val color: Color = CopperGold,
    val max: Float = 100f
)

data class BuiltInLibInfo(
    val headerName: String,
    val lang: String,
    val category: String,
    val supportedHardware: String,
    val exampleApi: String
)