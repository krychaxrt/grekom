package pl.grekom.netmaster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.grekom.netmaster.MainViewModel

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val scope = rememberCoroutineScope()
    var openCellIdKey by rememberSaveable { mutableStateOf("") }
    var localCsv by rememberSaveable { mutableStateOf("") }
    var alertRsrp by rememberSaveable { mutableStateOf(-110f) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        openCellIdKey = vm.getOpenCellIdKey()
        alertRsrp = vm.getAlertRsrp().toFloat()
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Lokalizacja stacji bazowej")
        Text(
            "Aplikacja pyta serwer OpenCellID (darmowy klucz) o lokalizację stacji na podstawie Cell ID.",
            fontSize = 12.sp, color = Color.Gray
        )
        OutlinedTextField(
            value = openCellIdKey,
            onValueChange = { openCellIdKey = it },
            label = { Text("Klucz API OpenCellID") },
            placeholder = { Text("pk.XXXXXXXXXXXXXXXX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    scope.launch { vm.saveOpenCellIdKey(openCellIdKey) }
                }) { Icon(Icons.Default.Save, null) }
            }
        )
        Text(
            "Zarejestruj się na opencellid.org/register.php — darmowo, 1000 zapytań/dzień.",
            fontSize = 11.sp, color = Color.Gray
        )

        Divider()
        SectionTitle("Lokalna baza T-Mobile (opcjonalnie)")
        Text(
            "Wklej CSV z lokalizacjami stacji (format: cellid,lat,lon,adres w jednej linii). Ma pierwszeństwo przed OpenCellID.",
            fontSize = 12.sp, color = Color.Gray
        )
        OutlinedTextField(
            value = localCsv,
            onValueChange = { localCsv = it },
            label = { Text("CSV") },
            placeholder = { Text("58407953,50.06470,19.94500,Kraków Komasy 3\n...") },
            modifier = Modifier.fillMaxWidth().height(160.dp),
            minLines = 6
        )
        Button(onClick = { scope.launch { vm.saveLocalCsv(localCsv) } },
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Zapisz CSV")
        }

        Divider()
        SectionTitle("Alarm słabego sygnału")
        Text("Powiadomienie gdy RSRP spadnie poniżej: ${alertRsrp.toInt()} dBm",
            fontSize = 12.sp)
        Slider(
            value = alertRsrp,
            onValueChange = { alertRsrp = it },
            onValueChangeFinished = { vm.saveAlertRsrp(alertRsrp.toInt()) },
            valueRange = -120f..-80f,
            steps = 7
        )

        Divider()
        SectionTitle("O aplikacji")
        Text("Grekom NetMaster v2.0", fontWeight = FontWeight.Bold)
        Text("Walk-test RF dla DAS i sieci T-Mobile.", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}
