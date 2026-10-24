package com.pandakode.bambumqtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandakode.bambumqtt.ui.screens.*
import com.pandakode.bambumqtt.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BambuMqttTheme {
                BambuMqttMainApp()
            }
        }
    }
}

enum class IotNavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("SCADA", Icons.Default.DashboardCustomize),
    SERIAL("Serial", Icons.Default.Terminal),
    WOKWI_SIM("Wokwi Sim", Icons.Default.Memory),
    CODE_IDE("IDE/OTA", Icons.Default.Code),
    CLOUD_HUB("Cloud/CAM", Icons.Default.CloudSync),
    HARDWARE("Lab MCU", Icons.Default.DeveloperBoard)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BambuMqttMainApp() {
    var currentTab by remember { mutableStateOf(IotNavTab.DASHBOARD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = CopperGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BambuMQTT IoT Studio",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "SCADA, Wokwi Sim, Voice IoT, ESP-CAM & OTA • by PandaKode",
                                color = PcbMintLight,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PcbEmeraldDark)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PcbEmeraldDark,
                tonalElevation = 8.dp
            ) {
                IotNavTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) PcbEmeraldDark else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (selected) CopperGold else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = CopperGold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                IotNavTab.DASHBOARD -> MqttDashboardScreen()
                IotNavTab.SERIAL -> SerialMonitorScreen()
                IotNavTab.WOKWI_SIM -> WokwiSimScreen()
                IotNavTab.CODE_IDE -> EspCodeStudioScreen()
                IotNavTab.CLOUD_HUB -> PayloadStudioScreen()
                IotNavTab.HARDWARE -> HardwareLabScreen()
            }
        }
    }
}