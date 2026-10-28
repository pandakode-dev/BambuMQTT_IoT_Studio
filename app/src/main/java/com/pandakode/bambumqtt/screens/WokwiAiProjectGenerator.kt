package com.pandakode.bambumqtt.ui.screens

// =========================================================================
// DATA CLASS MODEL PROYEK HASIL RAKITAN AI / MODUL INSTAN
// =========================================================================
data class AiGeneratedSimProject(
    val projectTitle: String,
    val summaryDescription: String,
    val recommendedBoard: McuBoardVariant,
    val components: List<SimComponent>,
    val wires: List<classPinWireConnection>,
    val cppCode: String,
    val microPythonCode: String,
    val initialOledOrLcdLine1: String,
    val initialOledOrLcdLine2: String
)

// =========================================================================
// ENGINE PERAKIT SIRKUIT, KABEL SIKU-SIKU & KODING OTOMATIS BAMBU-AI
// =========================================================================
object WokwiAiProjectGenerator {

    val presetProjectNames = listOf(
        "🌱 Smart Farming Irigasi Sawit (Soil + DHT22 + Relay + LCD 16x2)",
        "🚨 Smart Home Security Radar (PIR + Ultrasonik + Buzzer + Servo + OLED)",
        "🌈 Studio Lampu Pintar RGB & Cahaya (LDR + NeoPixel Ring + Potensio)",
        "🏭 Kontrol Motor Industri & Suhu (Stepper 28BYJ + DHT22 + Relay + LCD)",
        "🚪 Palang Pintu Parkir Otomatis (Ultrasonik HC-SR04 + Servo + LCD + Buzzer)"
    )

    // 1. KETIKA MODUL DI KATALOG DITEKAN -> OTOMATIS RAKIT RANGKAIAN & KABEL SIKU-SIKU KE KANVAS!
    fun buildSingleModuleCircuit(
        moduleType: SimCompType,
        boardVariant: McuBoardVariant = McuBoardVariant.ESP32_30PIN
    ): AiGeneratedSimProject {
        val mcu = SimComponent(
            id = "mcu",
            type = SimCompType.ESP32_BOARD,
            initialLabel = boardVariant.displayName.substringBefore(" ("),
            initialX = 14f,
            initialY = 24f
        )
        val mainComp = SimComponent(
            id = "mod1",
            type = moduleType,
            initialLabel = moduleType.title.substringBefore(" "),
            initialX = 265f,
            initialY = 26f
        )
        val comps = mutableListOf(mcu, mainComp)
        val wires = mutableListOf<classPinWireConnection>()

        when (moduleType) {
            SimCompType.DHT22_SENSOR -> {
                val oled = SimComponent("oled1", SimCompType.OLED_SSD1306, "OLED Monitor", 265f, 180f)
                comps.add(oled)
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "DATA", "D4", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w3", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w4", "oled1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w5", "oled1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w6", "oled1", "SCL", "D22", JumperWireColor.GREEN_SCL.color))
                wires.add(classPinWireConnection("w7", "oled1", "SDA", "D21", JumperWireColor.GOLD_SDA.color))
            }

            SimCompType.HCSR04_SONAR -> {
                val lcd = SimComponent("lcd1", SimCompType.LCD_16X2_I2C, "LCD Jarak", 265f, 175f)
                comps.add(lcd)
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "TRIG", "D5", JumperWireColor.GOLD_SDA.color))
                wires.add(classPinWireConnection("w3", "mod1", "ECHO", "D19", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w4", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w5", "lcd1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w6", "lcd1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w7", "lcd1", "SDA", "D21", JumperWireColor.GOLD_SDA.color))
                wires.add(classPinWireConnection("w8", "lcd1", "SCL", "D22", JumperWireColor.GREEN_SCL.color))
            }

            SimCompType.PIR_MOTION -> {
                val buz = SimComponent("buz1", SimCompType.BUZZER_ACTIVE, "Alarm Buzzer", 435f, 26f)
                val led = SimComponent("led1", SimCompType.LED_LAMP, "LED Alarm", 265f, 175f)
                comps.addAll(listOf(buz, led))
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "OUT", "D15", JumperWireColor.GREEN_SCL.color))
                wires.add(classPinWireConnection("w3", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w4", "buz1", "VCC+", "D2", JumperWireColor.ORANGE_PWM.color))
                wires.add(classPinWireConnection("w5", "buz1", "GND-", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w6", "led1", "A+", "D2", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w7", "led1", "K-", "GND", JumperWireColor.BLACK_GND.color))
            }

            SimCompType.SOIL_MOISTURE -> {
                val relay = SimComponent("rel1", SimCompType.RELAY_1CH, "Relay Pompa", 435f, 26f)
                val lcd = SimComponent("lcd1", SimCompType.LCD_16X2_I2C, "LCD Kebun", 265f, 175f)
                comps.addAll(listOf(relay, lcd))
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w3", "mod1", "AOUT", "D34", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w4", "rel1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w5", "rel1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w6", "rel1", "IN", "D2", JumperWireColor.ORANGE_PWM.color))
                wires.add(classPinWireConnection("w7", "lcd1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w8", "lcd1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w9", "lcd1", "SDA", "D21", JumperWireColor.GOLD_SDA.color))
                wires.add(classPinWireConnection("w10", "lcd1", "SCL", "D22", JumperWireColor.GREEN_SCL.color))
            }

            SimCompType.LDR_LIGHT -> {
                val neo = SimComponent("neo1", SimCompType.NEOPIXEL_RING, "Lampu Taman", 435f, 26f)
                comps.add(neo)
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w3", "mod1", "DO/AO", "D4", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w4", "neo1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w5", "neo1", "5V", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w6", "neo1", "DIN", "D5", JumperWireColor.GREEN_SCL.color))
            }

            SimCompType.OLED_SSD1306 -> {
                wires.add(classPinWireConnection("w1", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w2", "mod1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w3", "mod1", "SCL", "D22", JumperWireColor.GREEN_SCL.color))
                wires.add(classPinWireConnection("w4", "mod1", "SDA", "D21", JumperWireColor.GOLD_SDA.color))
            }

            SimCompType.LCD_16X2_I2C -> {
                wires.add(classPinWireConnection("w1", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w2", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w3", "mod1", "SDA", "D21", JumperWireColor.GOLD_SDA.color))
                wires.add(classPinWireConnection("w4", "mod1", "SCL", "D22", JumperWireColor.GREEN_SCL.color))
            }

            SimCompType.SERVO_SG90 -> {
                val pot = SimComponent("pot1", SimCompType.POTENTIOMETER, "Kontrol Sudut", 265f, 175f)
                comps.add(pot)
                wires.add(classPinWireConnection("w1", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w2", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w3", "mod1", "SIG", "D18", JumperWireColor.ORANGE_PWM.color))
                wires.add(classPinWireConnection("w4", "pot1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w5", "pot1", "OUT", "D34", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w6", "pot1", "GND", "GND", JumperWireColor.BLACK_GND.color))
            }

            SimCompType.STEPPER_MOTOR -> {
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w3", "mod1", "STEP", "D18", JumperWireColor.ORANGE_PWM.color))
                wires.add(classPinWireConnection("w4", "mod1", "DIR", "D19", JumperWireColor.CYAN_DATA.color))
            }

            SimCompType.NEOPIXEL_RING -> {
                wires.add(classPinWireConnection("w1", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w2", "mod1", "5V", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w3", "mod1", "DIN", "D5", JumperWireColor.GREEN_SCL.color))
            }

            SimCompType.RELAY_1CH -> {
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w3", "mod1", "IN", "D2", JumperWireColor.ORANGE_PWM.color))
            }

            SimCompType.BUZZER_ACTIVE -> {
                wires.add(classPinWireConnection("w1", "mod1", "VCC+", "D2", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND-", "GND", JumperWireColor.BLACK_GND.color))
            }

            SimCompType.LED_LAMP, SimCompType.RESISTOR_4BAND -> {
                val extra = if (moduleType == SimCompType.LED_LAMP) {
                    SimComponent("res1", SimCompType.RESISTOR_4BAND, "Resistor 220Ω", 265f, 175f)
                } else {
                    SimComponent("led1", SimCompType.LED_LAMP, "LED Merah", 435f, 26f)
                }
                comps.add(extra)
                val ledId = if (moduleType == SimCompType.LED_LAMP) "mod1" else "led1"
                val resId = if (moduleType == SimCompType.RESISTOR_4BAND) "mod1" else "res1"
                wires.add(classPinWireConnection("w1", resId, "P1", "D2", JumperWireColor.GOLD_SDA.color))
                wires.add(classPinWireConnection("w2", ledId, "A+", "D2", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w3", ledId, "K-", "GND", JumperWireColor.BLACK_GND.color))
            }

            SimCompType.POTENTIOMETER -> {
                val servo = SimComponent("srv1", SimCompType.SERVO_SG90, "Servo SG90", 435f, 26f)
                comps.add(servo)
                wires.add(classPinWireConnection("w1", "mod1", "VCC", "3V3", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w2", "mod1", "OUT", "D34", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w3", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w4", "srv1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w5", "srv1", "VCC", "5V", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w6", "srv1", "SIG", "D18", JumperWireColor.ORANGE_PWM.color))
            }

            SimCompType.PUSH_BUTTON -> {
                val led = SimComponent("led1", SimCompType.LED_LAMP, "LED Indikator", 435f, 26f)
                comps.add(led)
                wires.add(classPinWireConnection("w1", "mod1", "PIN1", "D4", JumperWireColor.CYAN_DATA.color))
                wires.add(classPinWireConnection("w2", "mod1", "GND", "GND", JumperWireColor.BLACK_GND.color))
                wires.add(classPinWireConnection("w3", "led1", "A+", "D2", JumperWireColor.RED_VCC.color))
                wires.add(classPinWireConnection("w4", "led1", "K-", "GND", JumperWireColor.BLACK_GND.color))
            }

            else -> {}
        }

        return AiGeneratedSimProject(
            projectTitle = "Rangkaian Modul: ${moduleType.title}",
            summaryDescription = "Rangkaian & kabel siku-siku untuk ${moduleType.title} telah dipasang otomatis ke kanvas!",
            recommendedBoard = boardVariant,
            components = comps,
            wires = wires,
            cppCode = """
                // AUTO-GENERATED CIRCUIT FOR: ${moduleType.title.uppercase()}
                #include <WiFi.h>
                #include <BlynkSimpleEsp32.h>
                #include <Wire.h>
                #include <Adafruit_SSD1306.h>
                #include <LiquidCrystal_I2C.h>
                #include <DHT.h>
                #include <ESP32Servo.h>

                char auth[] = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}";
                char ssid[] = "${BambuVirtualNetwork.VIRTUAL_SSID}";
                char pass[] = "${BambuVirtualNetwork.VIRTUAL_PASS}";

                void setup() {
                  Serial.begin(115200);
                  Blynk.begin(auth, ssid, pass);
                  pinMode(2, OUTPUT);
                  display.println("${moduleType.title.take(15).uppercase()}");
                  servo.write(135);
                }

                void loop() {
                  Blynk.run();
                  digitalWrite(2, HIGH);
                  Serial.println("Modul ${moduleType.title} Aktif & Terhubung!");
                  delay(1000);
                }
            """.trimIndent(),
            microPythonCode = """
                # AUTO-GENERATED MICROPYTHON FOR: ${moduleType.title.uppercase()}
                from machine import Pin, ADC, I2C
                import time

                led = Pin(2, Pin.OUT)
                while True:
                    led.on()
                    oled.text("${moduleType.title.take(15).uppercase()}", 0, 0)
                    print("MicroPython ${moduleType.title} Running!")
                    time.sleep(1)
            """.trimIndent(),
            initialOledOrLcdLine1 = moduleType.title.take(15).uppercase(),
            initialOledOrLcdLine2 = "STATUS: ONLINE"
        )
    }

    // 2. MESIN AI PEMBUAT PROYEK LENGKAP DARI KALIMAT / PROMPT PENGGUNA
    fun buildProjectFromPrompt(userPrompt: String): AiGeneratedSimProject {
        val p = userPrompt.lowercase()
        return when {
            p.contains("sawit") || p.contains("tanah") || p.contains("soil") || p.contains("irigasi") || p.contains("farming") -> {
                val comps = listOf(
                    SimComponent("mcu", SimCompType.ESP32_BOARD, "ESP32 DevKit", 14f, 24f),
                    SimComponent("soil1", SimCompType.SOIL_MOISTURE, "Soil Sensor", 252f, 20f),
                    SimComponent("dht1", SimCompType.DHT22_SENSOR, "DHT22", 425f, 20f),
                    SimComponent("relay1", SimCompType.RELAY_1CH, "Pompa Relay", 595f, 20f),
                    SimComponent("lcd1", SimCompType.LCD_16X2_I2C, "LCD 16x2 I2C", 252f, 165f),
                    SimComponent("led1", SimCompType.LED_LAMP, "LED Status", 448f, 165f)
                )
                val wires = listOf(
                    classPinWireConnection("w1", "soil1", "VCC", "3V3", JumperWireColor.RED_VCC.color),
                    classPinWireConnection("w2", "soil1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                    classPinWireConnection("w3", "soil1", "AOUT", "D34", JumperWireColor.CYAN_DATA.color),
                    classPinWireConnection("w4", "dht1", "VCC", "3V3", JumperWireColor.RED_VCC.color),
                    classPinWireConnection("w5", "dht1", "DATA", "D4", JumperWireColor.GOLD_SDA.color),
                    classPinWireConnection("w6", "dht1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                    classPinWireConnection("w7", "relay1", "VCC", "5V", JumperWireColor.RED_VCC.color),
                    classPinWireConnection("w8", "relay1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                    classPinWireConnection("w9", "relay1", "IN", "D2", JumperWireColor.ORANGE_PWM.color),
                    classPinWireConnection("w10", "lcd1", "GND", "GND", JumperWireColor.BLACK_GND.color),
                    classPinWireConnection("w11", "lcd1", "VCC", "5V", JumperWireColor.RED_VCC.color),
                    classPinWireConnection("w12", "lcd1", "SDA", "D21", JumperWireColor.GOLD_SDA.color),
                    classPinWireConnection("w13", "lcd1", "SCL", "D22", JumperWireColor.GREEN_SCL.color),
                    classPinWireConnection("w14", "led1", "A+", "D2", JumperWireColor.RED_VCC.color),
                    classPinWireConnection("w15", "led1", "K-", "GND", JumperWireColor.BLACK_GND.color)
                )
                AiGeneratedSimProject(
                    projectTitle = "Smart Farming Irigasi Kebun Sawit",
                    summaryDescription = "AI merakit Sensor Tanah, DHT22, Relay Pompa, & LCD 16x2 dengan kabel siku-siku!",
                    recommendedBoard = McuBoardVariant.ESP32_30PIN,
                    components = comps,
                    wires = wires,
                    cppCode = """
                        #include <WiFi.h>
                        #include <BlynkSimpleEsp32.h>
                        #include <LiquidCrystal_I2C.h>
                        #include <DHT.h>

                        char auth[] = "${BambuVirtualNetwork.VIRTUAL_BLYNK_TOKEN}";
                        char ssid[] = "${BambuVirtualNetwork.VIRTUAL_SSID}";
                        char pass[] = "${BambuVirtualNetwork.VIRTUAL_PASS}";

                        void setup() {
                          Serial.begin(115200);
                          Blynk.begin(auth, ssid, pass);
                          pinMode(2, OUTPUT);
                          lcd.print("SAWIT SMART IOT");
                        }

                        void loop() {
                          Blynk.run();
                          digitalWrite(2, HIGH);
                          Serial.println("AI Smart Farming: Pompa ON & Tanah Terpantau!");
                          delay(1000);
                        }
                    """.trimIndent(),
                    microPythonCode = """
                        from machine import Pin, ADC
                        import dht, time

                        pump_relay = Pin(2, Pin.OUT)
                        while True:
                            pump_relay.on()
                            oled.text("SAWIT SMART PY", 0, 0)
                            print("MicroPython Smart Farming Active!")
                            time.sleep(1)
                    """.trimIndent(),
                    initialOledOrLcdLine1 = "SAWIT SMART IOT",
                    initialOledOrLcdLine2 = "Soil:68% Pump:ON"
                )
            }

            p.contains("keamanan") || p.contains("security") || p.contains("pir") || p.contains("alarm") -> {
                buildSingleModuleCircuit(SimCompType.PIR_MOTION)
            }

            p.contains("rgb") || p.contains("neopixel") || p.contains("ldr") || p.contains("cahaya") -> {
                buildSingleModuleCircuit(SimCompType.LDR_LIGHT)
            }

            p.contains("stepper") || p.contains("industri") || p.contains("motor") -> {
                buildSingleModuleCircuit(SimCompType.STEPPER_MOTOR)
            }

            else -> {
                buildSingleModuleCircuit(SimCompType.HCSR04_SONAR)
            }
        }
    }
}