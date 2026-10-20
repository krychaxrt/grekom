package pl.grekom.netmaster.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pl.grekom.netmaster.WalkTestViewModel
import pl.grekom.netmaster.data.MeasurementPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppRoot(
    onSharePdf: (File) -> Unit,
    onOpenPdf: (File) -> Unit,
    vm: WalkTestViewModel = viewModel()
) {
    val walkTest by vm.walkTest.collectAsState()

    if (walkTest == null) {
        StartScreen(onStart = { name, site -> vm.startWalkTest(name, site) })
    } else {
        WalkTestScreen(
            vm = vm,
            onSharePdf = onSharePdf,
            onOpenPdf = onOpenPdf
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(onStart: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var site by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grekom NetMaster") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Nowy walk-test", style = MaterialTheme.typography.headlineSmall)
            Text("Wprowadź dane lokalizacji i rozpocznij serię pomiarów.", color = Color.Gray)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa walk-testu") },
                placeholder = { Text("np. KRK Galeria Krakowska") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = site,
                onValueChange = { site = it },
                label = { Text("Kod lokalizacji T-Mobile") },
                placeholder = { Text("np. KRK0123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkTestScreen(
    vm: WalkTestViewModel,
    onSharePdf: (File) -> Unit,
    onOpenPdf: (File) -> Unit
) {
    val context = LocalContext.current
    val walkTest by vm.walkTest.collectAsState()
    val liveRf by vm.liveRf.collectAsState()
    val status by vm.status.collectAsState()
    val isMeasuring by vm.isMeasuring.collectAsState()
    val lastPdf by vm.lastPdf.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    // Refresh live RF every 2s
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshLiveRf()
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(walkTest?.name ?: "Walk-test") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showFinishDialog = true }) {
                        Icon(Icons.Default.Close, "Zakończ")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!isMeasuring) showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isMeasuring) "Pomiar…" else "Nowy pomiar")
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            // Live RF card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Aktualny sygnał", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LiveMetric("Tech", liveRf.tech)
                        LiveMetric("Pasmo", liveRf.bandLabel)
                        LiveMetric("RSRP", liveRf.rsrpStr())
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LiveMetric("RSRQ", liveRf.rsrqStr())
                        LiveMetric("SINR", liveRf.sinrStr())
                        LiveMetric("PCI", if (liveRf.pci >= 0) liveRf.pci.toString() else "-")
                    }
                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }

            // Points list
            val points = walkTest?.points.orEmpty()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Punkty: ${points.size}", fontWeight = FontWeight.Bold)
                if (points.isNotEmpty()) {
                    TextButton(onClick = {
                        vm.exportPdf { file -> onOpenPdf(file) }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Generuj PDF")
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(points, key = { it.id }) { point ->
                    PointCard(point, onDelete = { vm.removePoint(point.id) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            lastPdf?.let { pdf ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onOpenPdf(pdf) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(4.dp)); Text("Otwórz PDF")
                    }
                    Button(onClick = { onSharePdf(pdf) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Wyślij")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPointDialog(
            defaultLabel = "P${(walkTest?.points?.size ?: 0) + 1}",
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
            text = { Text("Wszystkie dane zostaną usunięte z pamięci. Upewnij się, że masz wygenerowany PDF.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.finishWalkTest()
                    showFinishDialog = false
                }) { Text("Zakończ", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
fun LiveMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PointCard(point: MeasurementPoint, onDelete: () -> Unit) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(point.timestamp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${point.label}  ·  $timeStr", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Usuń", tint = Color.Gray) }
            }
            Text("${point.rf.tech}  ·  ${point.rf.bandLabel}", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("RSRP: ${point.rf.rsrpStr()}", fontSize = 12.sp)
                Text("RSRQ: ${point.rf.rsrqStr()}", fontSize = 12.sp)
                Text("SINR: ${point.rf.sinrStr()}", fontSize = 12.sp)
            }
            point.speed?.let { s ->
                if (s.success) {
                    Text(
                        "↓ ${"%.1f".format(s.downloadMbps)} Mbps  ·  ↑ ${"%.1f".format(s.uploadMbps)} Mbps  ·  ${s.pingMs} ms",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text("Speed test: ${s.error ?: "błąd"}", fontSize = 12.sp, color = Color.Red)
                }
            }
            if (point.notes.isNotBlank()) {
                Text("📝 ${point.notes}", fontSize = 12.sp, color = Color.DarkGray)
            }
            if (point.photoPath != null) {
                Text("📷 Zdjęcie dołączone", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPointDialog(
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
        val photoDir = File(context.cacheDir, "photos").apply { mkdirs() }
        val photoFile = File(photoDir, "photo_${System.currentTimeMillis()}.jpg")
        photoPath = photoFile.absolutePath
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) photoPath = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy punkt pomiarowy") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Etykieta (np. P1, Pietro 2 wejscie)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notatki") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withSpeed, onCheckedChange = { withSpeed = it })
                    Text("Wykonaj speed test (~15s)")
                }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(photoUri) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photoPath != null) "Zdjęcie zrobione ✓ (zmień)" else "Zrób zdjęcie")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(label, notes, photoPath, withSpeed) }) {
                Text("Wykonaj pomiar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}
