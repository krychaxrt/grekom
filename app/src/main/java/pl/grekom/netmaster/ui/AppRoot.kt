package pl.grekom.netmaster.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.grekom.netmaster.MainViewModel
import pl.grekom.netmaster.ui.screens.*
import java.io.File

enum class Tab(val label: String, val icon: @Composable () -> Unit) {
    LIVE("Na żywo", { Icon(Icons.Default.Sensors, null) }),
    MAP("Mapa", { Icon(Icons.Default.Map, null) }),
    LOG("Dziennik", { Icon(Icons.Default.List, null) }),
    WALKTEST("Walk-test", { Icon(Icons.Default.Timeline, null) }),
    SETTINGS("Menu", { Icon(Icons.Default.Settings, null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    onShareFile: (File) -> Unit,
    onOpenFile: (File) -> Unit,
    vm: MainViewModel = viewModel()
) {
    var selected by rememberSaveable { mutableStateOf(Tab.LIVE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grekom NetMaster") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Tab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { selected = tab },
                        icon = tab.icon,
                        label = { Text(tab.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (selected) {
                Tab.LIVE -> LiveScreen(vm)
                Tab.MAP -> MapScreen(vm)
                Tab.LOG -> LogScreen(vm)
                Tab.WALKTEST -> WalkTestScreen(vm, onShareFile, onOpenFile)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
