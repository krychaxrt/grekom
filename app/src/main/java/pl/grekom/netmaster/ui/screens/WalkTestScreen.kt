package pl.grekom.netmaster.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import pl.grekom.netmaster.MainViewModel
import pl.grekom.netmaster.data.MeasurementPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalkTestScreen(
    vm: MainViewModel,
    onShareFile: (File) -> Unit,
    onOpenFile: (File) -> Unit
) {
    val wt by vm.walkTest.collectAsState()
    val isMeasuring by vm.isMeasuring.collectAsState()
    val lastExport by vm.lastExport.collectAsState()
    val status by vm.status.collectAsState()
    val driveMode by vm.driveMode.collectAsState()

    if (wt == null) {
        StartSection(onStart = { name, site -> vm.startWalkTest(name, site) })
    } else {
        ActiveSection(
            vm = vm,
            wtName = wt!!.name,
            points = wt!!.points,
            isMeasuring = isMeasuring,
            status = status,
            lastExport = lastExport,
            driveMode = driveMode,
            onShareFile = onShareFile,
            onOpenFile = onOpenFile
        )
    }
}

@Composable
private fun StartSection(onStart: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var site by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nowy walk-test", style = MaterialTheme.typography.headlineSmall)
        Text("Wprowadź dane i rozpocznij serię pomiarów.", color = Color.Gray)
        OutlinedTextField(value = name, onValueChange = { name = it },
            label = { Text("Nazwa") }, singleLine = true,
            placeholder = { Text("np. KRK Galeria") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = site, onValueChange = { site = it },
            label = { Text("Kod lokalizacji T-Mobile") }, singleLine = true,
            placeholder = { Text("np. KRK0123") },
            modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { onStart(name, site) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Rozpocznij walk-test")
        }
    }
}

@Composable
private fun ActiveSection(
    vm: MainViewModel,
    wtName: String,
    points: List<MeasurementPoint>,
    isMeasuring: Boolean,
    status: String,
    lastExport: File?,
    driveMode: Boolean,
    onShareFile: (File) -> Unit,
    onOpenFile: (File) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(wtName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Punktów: ${points.size}", color = Color.White.copy(0.8f), fontSize = 11.sp)
                }
                IconButton(onClick = { showFinishDialog = true }) {
                    Icon(Icons.Default.Close, "Zakończ", tint = Color.White)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { if (!isMeasuring) showAddDialog = true },
                enabled = !isMeasuring,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp))
                Text(if (isMeasuring) "Pomiar…" else "Nowy pomiar")
            }
            if (driveMode) {
                FilledTonalButton(onClick = { vm.stopDriveMode() }, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE74C3C), contentColor = Color.White)) {
                    Icon(Icons.Default.Stop, null); Spacer(Modifier.width(4.dp)); Text("Stop drive")
                }
            } else {
                OutlinedButton(onClick = { vm.startDriveMode() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.DirectionsCar, null); Spacer(Modifier.width(4.dp)); Text("Drive-test")
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp, modifier = Modifier.padding(12.dp))
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(points, key = { it.id }) { p -> PointCard(p, onDelete = { vm.removePoint(p.id) }) }
            item { Spacer(Modifier.height(120.dp)) }
        }

        if (points.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { vm.exportCsv { onOpenFile(it) } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.TableChart, null); Spacer(Modifier.width(4.dp)); Text("CSV")
                }
                Button(onClick = { vm.exportPdf { onOpenFile(it) } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(4.dp)); Text("PDF")
                }
            }
            lastExport?.let { file ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onOpenFile(file) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(4.dp))
                        Text("Otwórz ${file.extension.uppercase()}")
                    }
                    Button(onClick = { onShareFile(file) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Wyślij")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPointDialog(
            defaultLabel = "P${points.size + 1}",
            onDismiss = { showAddDialog = false },
            onConfirm = { label, notes, photoPath, withSpeed ->
                vm.addPoint(label, notes, photoPath, withSpeed)
                showAddDialog = false
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Zakończyć walk-test?") },
            text = { Text("Pamiętaj żeby wygenerować PDF/CSV przed zakończeniem.") },
            confirmButton = {
                TextButton(onClick = { vm.finishWalkTest(); showFinishDialog = false }) {
                    Text("Zakończ")
                }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun PointCard(p: MeasurementPoint, onDelete: () -> Unit) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(p.timestamp))
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${p.label}  ·  $timeStr", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Gray) }
            }
            Text("${p.rf.tech}  ·  ${p.rf.bandLabel}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RSRP ${p.rf.rsrpStr()}", fontSize = 12.sp)
                Text("RSRQ ${p.rf.rsrqStr()}", fontSize = 12.sp)
                Text("SNR ${p.rf.sinrStr()}", fontSize = 12.sp)
            }
            p.speed?.let { s ->
                if (s.success) {
                    Text("↓ ${"%.1f".format(s.downloadMbps)} / ↑ ${"%.1f".format(s.uploadMbps)} Mbps · ${s.pingMs} ms",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (p.neighbors.isNotEmpty()) {
                Text("Sąsiadów: ${p.neighbors.size}", fontSize = 11.sp, color = Color.Gray)
            }
            if (p.notes.isNotBlank()) Text("📝 ${p.notes}", fontSize = 12.sp, color = Color.DarkGray)
            if (p.photoPath != null) Text("📷 Zdjęcie", fontSize = 12.sp, color = Color.DarkGray)
        }
    }
}

@Composable
private fun AddPointDialog(
    defaultLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, notes: String, photoPath: String?, withSpeed: Boolean) -> Unit
) {
    val context = LocalContext.current
    var label by rememberSaveable { mutableStateOf(defaultLabel) }
    var notes by rememberSaveable { mutableStateOf("") }
    var withSpeed by rememberSaveable { mutableStateOf(true) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val photoUri = remember {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val f = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        photoPath = f.absolutePath
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (!success) photoPath = null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy punkt pomiarowy") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("Etykieta") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Notatki") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withSpeed, onCheckedChange = { withSpeed = it })
                    Text("Speed test (~15s)")
                }
                OutlinedButton(onClick = { cameraLauncher.launch(photoUri) },
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PhotoCamera, null); Spacer(Modifier.width(8.dp))
                    Text(if (photoPath != null) "Zdjęcie ✓" else "Zrób zdjęcie")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(label, notes, photoPath, withSpeed) }) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
