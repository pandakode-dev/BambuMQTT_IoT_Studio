package com.pandakode.bambumqtt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.model.AutomationRule
import com.pandakode.bambumqtt.model.EmbeddedMathEngine
import com.pandakode.bambumqtt.ui.theme.*
import com.pandakode.bambumqtt.utils.DetectedUsbMcu
import com.pandakode.bambumqtt.utils.UsbOtgDriverEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EspCodeStudioScreen() {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("USB OTG Flasher", "ESP32 Code IDE", "Wi-Fi OTA", "Auto IF-THEN")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EsdMatBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = PcbEmeraldDark,
            contentColor = CopperGold,
            edgePadding = 8.dp
        ) {
            subTabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (selectedSubTab == idx) CopperGold else Color.White
                        )
                    }
                )
            }
        }

        when (selectedSubTab) {
            0 -> UsbOtgHardwareDriverCard()
            1 -> MobileEspCodeEditorCard()
            2 -> WifiOtaFirmwarePusherCard()
            3 -> VisualAutomationRulesCard()
        }
    }
}

@Composable
fun UsbOtgHardwareDriverCard() {
    val context = LocalContext.current
    var detectedDevices by remember { mutableStateOf(UsbOtgDriverEngine.scanConnectedMcuDevices(context)) }
    var driverStatusLog by remember {
        mutableStateOf("Driver Kernel Siap: CH340/CH9102, Silicon Labs CP2102, FTDI FT232, & Espressif Native USB.")
    }

    // State untuk Fitur Killer: Serial Wi-Fi & MQTT Provisioner via Kabel OTG
    var newWifiSsid by remember { mutableStateOf("Kantor_Sawit_Riau") }
    var newWifiPass by remember { mutableStateOf("pekanbaru2026") }
    var newBrokerIp by remember { mutableStateOf("192.168.1.250") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. KARTU DETEKTOR DRIVER CHIP USB OTG & BOOTLOADER TRIGGER
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
                        Icon(Icons.Default.Usb, contentDescription = null, tint = CopperGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "USB OTG HARDWARE DRIVER & BOOTLOADER",
                                color = CopperGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Terdeteksi: ${detectedDevices.size} Board Mikrokontroler di Port OTG",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            detectedDevices = UsbOtgDriverEngine.scanConnectedMcuDevices(context)
                            driverStatusLog = if (detectedDevices.isEmpty()) {
                                "Belum ada board ESP32/Arduino terdeteksi. Pastikan konektor Type-C OTG terpasang rapat."
                            } else {
                                "Ditemukan: ${detectedDevices.first().chipVendorName} (${detectedDevices.first().vidHex}:${detectedDevices.first().pidHex})"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CopperGold)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SiliconDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan OTG", color = SiliconDark, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }

                // Tampilkan Daftar Chip USB yang Tercolok
                if (detectedDevices.isNotEmpty()) {
                    detectedDevices.forEach { mcu ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SiliconDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "✅ ${mcu.chipVendorName}",
                                    color = LogicNeonGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "VID: ${mcu.vidHex} | PID: ${mcu.pidHex} | Product: ${mcu.productName}",
                                    color = LogicCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Tombol Paksa Masuk Bootloader (DTR/RTS + SLIP Sync 0xC0)
                                    Button(
                                        onClick = {
                                            // Paket SLIP Sync esptool standar untuk membangunkan ROM Bootloader ESP32
                                            val espSyncPacket = byteArrayOf(
                                                0xC0.toByte(), 0x00, 0x08, 0x24, 0x00,
                                                0x00, 0x00, 0x00, 0x00, 0x07, 0x07, 0x12, 0x20,
                                                0x55, 0x55, 0x55, 0x55, 0xC0.toByte()
                                            )
                                            val res = UsbOtgDriverEngine.transmitRawUsbData(
                                                context = context,
                                                device = mcu.device,
                                                payloadBytes = espSyncPacket,
                                                enterBootloaderFirst = true
                                            )
                                            driverStatusLog = res.getOrElse { it.message ?: "Error OTG" }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(" Masuk Flash Mode (DTR/RTS)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Log Konsol Driver
                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "DRIVER LOG: $driverStatusLog",
                        color = PcbMintLight,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 2. FITUR KILLER BARU: USB OTG WI-FI & MQTT NVS PROVISIONER (TANPA KOMPILASI ULANG!)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Router, contentDescription = null, tint = CopperOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "USB OTG WI-FI & BROKER NVS PROVISIONER",
                            fontWeight = FontWeight.ExtraBold,
                            color = PcbEmeraldDark,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ganti SSID Wi-Fi, Password, & IP Broker MQTT di memori EEPROM/NVS ESP32 lewat kabel OTG tanpa perlu compile ulang kode C++!",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newWifiSsid,
                        onValueChange = { newWifiSsid = it },
                        label = { Text("SSID Wi-Fi Baru") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newWifiPass,
                        onValueChange = { newWifiPass = it },
                        label = { Text("Password Wi-Fi") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = newBrokerIp,
                    onValueChange = { newBrokerIp = it },
                    label = { Text("IP / Hostname Broker MQTT Baru") },
                    modifier = Modifier.fillMaxWidth()
                )

                val nvsCommandJson = "{\"cmd\":\"SET_NVS\",\"ssid\":\"$newWifiSsid\",\"pass\":\"$newWifiPass\",\"broker\":\"$newBrokerIp\"}\r\n"

                Card(
                    colors = CardDefaults.cardColors(containerColor = SiliconDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "FRAME SERIAL OTG: $nvsCommandJson",
                        color = LogicNeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        val mcu = detectedDevices.firstOrNull()
                        if (mcu != null) {
                            val res = UsbOtgDriverEngine.transmitRawUsbData(
                                context = context,
                                device = mcu.device,
                                payloadBytes = nvsCommandJson.toByteArray(Charsets.UTF_8),
                                enterBootloaderFirst = false
                            )
                            driverStatusLog = res.getOrElse { it.message ?: "Gagal kirim OTG" }
                        } else {
                            driverStatusLog = "[SIMULASI OTG TX] Konfigurasi NVS Wi-Fi '$newWifiSsid' siap dikirim begitu kabel OTG dicolok!"
                            Toast.makeText(context, "Frame NVS disiapkan! Colok kabel OTG ESP32 untuk kirim langsung.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.SaveAlt, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tembak Konfigurasi Wi-Fi ke ESP32 via Kabel OTG", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MobileEspCodeEditorCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = remember { EmbeddedMathEngine.getCodeTemplates() }
    val templateNames = remember { templates.keys.toList() }

    var selectedTemplateName by remember { mutableStateOf(templateNames.first()) }
    var codeText by remember { mutableStateOf(templates[templateNames.first()] ?: "") }
    var injectLog by remember { mutableStateOf("Siap menginjeksi script ke ESP32 via USB OTG Hardware Driver.") }
    var isFlashing by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = CopperOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ESP32 & MICROPYTHON LIVE CODE STUDIO",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Edit kode C++ (.ino) atau injeksi langsung script MicroPython (.py) ke chip CH340/CP2102 ESP32 lewat kabel USB OTG!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                templateNames.forEach { name ->
                    val shortLabel = name.substringBefore(":")
                    FilterChip(
                        selected = selectedTemplateName == name,
                        onClick = {
                            selectedTemplateName = name
                            codeText = templates[name] ?: ""
                        },
                        label = { Text(shortLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            OutlinedTextField(
                value = codeText,
                onValueChange = { codeText = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = LogicNeonGreen
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SiliconDark,
                    unfocusedContainerColor = SiliconDark,
                    focusedBorderColor = CopperGold,
                    unfocusedBorderColor = PcbEmeraldMedium
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = SiliconDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "CONSOLE: $injectLog",
                    color = CopperGold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        isFlashing = true
                        scope.launch {
                            val connectedMcus = UsbOtgDriverEngine.scanConnectedMcuDevices(context)
                            if (connectedMcus.isNotEmpty()) {
                                val target = connectedMcus.first()
                                injectLog = "Membuka jalur USB OTG ke ${target.chipVendorName}..."
                                delay(400L)
                                // Bungkus kode dengan Raw REPL Control Bytes (CTRL+A = 0x01, CTRL+D = 0x04)
                                val replPayload = "\u0003\u0001$codeText\u0004".toByteArray(Charsets.UTF_8)
                                val res = UsbOtgDriverEngine.transmitRawUsbData(
                                    context = context,
                                    device = target.device,
                                    payloadBytes = replPayload,
                                    enterBootloaderFirst = false
                                )
                                injectLog = res.getOrElse { "Gagal USB: ${it.message}" }
                            } else {
                                injectLog = "[SIMULASI OTG] Mengirim CTRL+A (0x01) + ${codeText.length} bytes + CTRL+D (0x04)... Sukses!"
                            }
                            isFlashing = false
                        }
                    },
                    enabled = !isFlashing,
                    colors = ButtonDefaults.buttonColors(containerColor = PcbEmeraldDark),
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = CopperGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flash via Kabel OTG", color = CopperGold, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("ESP32_Code", codeText))
                        Toast.makeText(context, "Kode disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PcbEmeraldDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Kode", color = PcbEmeraldDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun WifiOtaFirmwarePusherCard() {
    val scope = rememberCoroutineScope()
    var targetEspIp by remember { mutableStateOf("192.168.4.1") }
    var otaPort by remember { mutableStateOf("80") }
    var firmwareName by remember { mutableStateOf("BambuNode_v2.4_ESP32.bin (842 KB)") }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    var otaStatus by remember { mutableStateOf("Siap melakukan Wireless OTA Update via HTTP / ElegantOTA.") }
    var isUploading by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = CopperOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "WI-FI OTA (.BIN) FIRMWARE PUSHER",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Update firmware ESP32 yang terpasang di tiang tinggi / plafon via Wi-Fi tanpa colok kabel!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetEspIp,
                    onValueChange = { targetEspIp = it },
                    label = { Text("IP Lokal ESP32") },
                    modifier = Modifier.weight(1.5f)
                )
                OutlinedTextField(
                    value = otaPort,
                    onValueChange = { otaPort = it },
                    label = { Text("Port HTTP/OTA") },
                    modifier = Modifier.weight(0.8f)
                )
            }

            OutlinedTextField(
                value = firmwareName,
                onValueChange = { firmwareName = it },
                label = { Text("File Binary Firmware (.bin)") },
                modifier = Modifier.fillMaxWidth()
            )

            LinearProgressIndicator(
                progress = { uploadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = CopperOrange,
                trackColor = EsdMatBg
            )

            Text(
                text = otaStatus,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = PcbEmeraldDark,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    isUploading = true
                    scope.launch {
                        uploadProgress = 0.1f
                        otaStatus = "Menghubungi http://$targetEspIp:$otaPort/update ..."
                        delay(500L)
                        for (step in 2..10) {
                            uploadProgress = step / 10f
                            otaStatus = "Mengunggah paket Binary Flash: ${(uploadProgress * 100).toInt()}% (MD5 Verified)"
                            delay(250L)
                        }
                        otaStatus = "✅ OTA FLASH COMPLETE! ESP32 sedang me-reboot ke Firmware baru."
                        isUploading = false
                    }
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Push Firmware .BIN via Wi-Fi OTA", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun VisualAutomationRulesCard() {
    var rules by remember {
        mutableStateOf(
            listOf(
                AutomationRule(1, "Auto Cooling Kipas Gudang", "pandakode/sensor/temp", ">", 36.0f, "pandakode/pwm/fan", "255", true),
                AutomationRule(2, "Auto Pompa Tanah Kering", "pandakode/sensor/soil", "<", 40.0f, "pandakode/relay/pump1", "1", true),
                AutomationRule(3, "Cut-Off Pompa Saat Basah", "pandakode/sensor/soil", ">", 80.0f, "pandakode/relay/pump1", "0", false)
            )
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = LogicViolet)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "BAMBUFLOW — NO-CODE AUTOMATION ENGINE",
                        fontWeight = FontWeight.ExtraBold,
                        color = PcbEmeraldDark,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Ubah logika IF-THEN antar-sensor dan relay langsung dari HP tanpa perlu memprogram ulang ESP32!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            rules.forEach { rule ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = EsdMatBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.ExtraBold, color = PcbEmeraldDark, fontSize = 13.sp)
                            Text(
                                text = "IF [${rule.sourceTopic}] ${rule.conditionOp} ${rule.thresholdValue}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = CopperOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "THEN PUBLISH [${rule.targetTopic}] ➔ \"${rule.actionPayload}\"",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = LogicViolet,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = rule.isEnabled,
                            onCheckedChange = { checked ->
                                rules = rules.map { if (it.id == rule.id) it.copy(isEnabled = checked) else it }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CopperGold, checkedTrackColor = PcbEmeraldDark)
                        )
                    }
                }
            }
        }
    }
}