package pl.grekom.netmaster

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.grekom.netmaster.data.*
import pl.grekom.netmaster.export.CsvExporter
import pl.grekom.netmaster.location.AppSettings
import pl.grekom.netmaster.location.CellLocator
import pl.grekom.netmaster.location.StationDatabase
import pl.grekom.netmaster.pdf.PdfGenerator
import pl.grekom.netmaster.rf.RfCollector
import pl.grekom.netmaster.speedtest.SpeedTest
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val rf = RfCollector(app)
    private val speed = SpeedTest()
    private val fused = LocationServices.getFusedLocationProviderClient(app)

    private val _liveRf = MutableStateFlow(RfSample())
    val liveRf: StateFlow<RfSample> = _liveRf.asStateFlow()

    private val _liveNeighbors = MutableStateFlow<List<NeighborCell>>(emptyList())
    val liveNeighbors: StateFlow<List<NeighborCell>> = _liveNeighbors.asStateFlow()

    private val _liveLocation = MutableStateFlow<Location?>(null)
    val liveLocation: StateFlow<Location?> = _liveLocation.asStateFlow()

    /** Last 60 RSRP samples (one every ~2.5s = ~2.5 min). */
    private val _rsrpHistory = MutableStateFlow<List<Int>>(emptyList())
    val rsrpHistory: StateFlow<List<Int>> = _rsrpHistory.asStateFlow()

    private val _servingCellLocation = MutableStateFlow<CellLocation?>(null)
    val servingCellLocation: StateFlow<CellLocation?> = _servingCellLocation.asStateFlow()

    private val _walkTest = MutableStateFlow<WalkTest?>(null)
    val walkTest: StateFlow<WalkTest?> = _walkTest.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _lastExport = MutableStateFlow<File?>(null)
    val lastExport: StateFlow<File?> = _lastExport.asStateFlow()

    private val _driveMode = MutableStateFlow(false)
    val driveMode: StateFlow<Boolean> = _driveMode.asStateFlow()

    private var driveJob: Job? = null
    private var liveJob: Job? = null
    private var lastServingCellId: Long = -1
    private var lastBand: Int = -1
    private var lastTech: String = ""
    private var alertRsrp: Int = -110

    private val _nearbyStations = MutableStateFlow<List<Pair<StationDatabase.Station, Double>>>(emptyList())
    val nearbyStations: StateFlow<List<Pair<StationDatabase.Station, Double>>> = _nearbyStations.asStateFlow()

    init {
        startLiveLoop()
        viewModelScope.launch {
            alertRsrp = AppSettings.alertRsrp(app)
            StationDatabase.ensureLoaded(app)
        }
    }

    private fun startLiveLoop() {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            while (true) {
                val snap = rf.snapshot()
                _liveRf.value = snap.serving
                _liveNeighbors.value = snap.neighbors
                if (snap.serving.rsrp != Int.MAX_VALUE) {
                    _rsrpHistory.value = (_rsrpHistory.value + snap.serving.rsrp).takeLast(60)
                }
                detectChanges(snap.serving)
                lookupStation(snap.serving)
                refreshLocation()
                delay(2500)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshLocation() {
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                _liveLocation.value = loc
                loc?.let { refreshNearbyStations(it.latitude, it.longitude) }
            }
        } catch (_: Exception) {}
    }

    private fun refreshNearbyStations(lat: Double, lon: Double) {
        if (!StationDatabase.isLoaded) return
        // 1500 m radius for map markers / city view
        _nearbyStations.value = StationDatabase.findAllNear(lat, lon, 1500.0).take(50)
        // Set serving-cell location if OpenCellID hasn't resolved it yet
        if (_servingCellLocation.value == null) {
            val operator = _liveRf.value.operator
            val nearest = StationDatabase.findNearest(operator, lat, lon, 500.0)
            if (nearest != null) {
                val (s, dist) = nearest
                _servingCellLocation.value = CellLocation(
                    lat = s.lat,
                    lon = s.lon,
                    range = dist.toInt(),
                    source = "UKE (szac.)",
                    address = s.fullAddress()
                )
            }
        }
    }

    private fun detectChanges(serving: RfSample) {
        val now = System.currentTimeMillis()
        if (lastServingCellId == -1L) {
            lastServingCellId = serving.cellId
            lastBand = serving.band
            lastTech = serving.tech
            return
        }
        if (serving.cellId != lastServingCellId && serving.cellId > 0) {
            addLog(LogEntry(now, LogEventType.CELL_CHANGE,
                "ID $lastServingCellId", "ID ${serving.cellId}", serving.rsrp))
            lastServingCellId = serving.cellId
        }
        if (serving.band != lastBand && serving.band > 0) {
            addLog(LogEntry(now, LogEventType.BAND_CHANGE,
                bandLabelOf(lastBand, lastTech), serving.bandLabel, serving.rsrp))
            lastBand = serving.band
        }
        if (serving.tech != lastTech && serving.tech != "-") {
            addLog(LogEntry(now, LogEventType.TECH_CHANGE, lastTech, serving.tech, serving.rsrp))
            lastTech = serving.tech
        }
        // Signal alert (debounced: only once per cell change)
        if (serving.rsrp != Int.MAX_VALUE && serving.rsrp < alertRsrp) {
            val lastAlert = _log.value.firstOrNull { it.type == LogEventType.SIGNAL_ALERT }
            if (lastAlert == null || now - lastAlert.timestamp > 60_000) {
                addLog(LogEntry(now, LogEventType.SIGNAL_ALERT,
                    "", "RSRP = ${serving.rsrp} dBm", serving.rsrp))
            }
        }
    }

    private fun bandLabelOf(band: Int, tech: String): String =
        if (band <= 0) "-" else if (tech.startsWith("5G")) "n$band" else "B$band"

    private fun addLog(entry: LogEntry) {
        _log.value = (listOf(entry) + _log.value).take(500)
    }

    private var lookupInFlight = false
    private fun lookupStation(serving: RfSample) {
        if (lookupInFlight) return
        if (serving.cellId <= 0) {
            _servingCellLocation.value = null
            return
        }
        val existing = _servingCellLocation.value
        if (existing != null && lastServingCellId == serving.cellId) return
        lookupInFlight = true
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val key = AppSettings.openCellIdKey(ctx).ifBlank { null }
                val csvText = AppSettings.localCsv(ctx)
                val local = if (csvText.isNotBlank()) CellLocator.parseCsv(csvText) else emptyMap()
                val locator = CellLocator(key, local)
                val radio = if (serving.tech.startsWith("5G")) "NR" else serving.tech
                val loc = locator.lookup(serving.mccMnc, serving.tac, serving.cellId, radio)
                _servingCellLocation.value = loc
            } finally {
                lookupInFlight = false
            }
        }
    }

    // --- Walk-test ---

    fun startWalkTest(name: String, siteCode: String) {
        _walkTest.value = WalkTest(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Walk-test" },
            siteCode = siteCode,
            createdAt = System.currentTimeMillis()
        )
        _lastExport.value = null
    }

    fun addPoint(label: String, notes: String, photoPath: String?, includeSpeedTest: Boolean) {
        val current = _walkTest.value ?: return
        viewModelScope.launch {
            _isMeasuring.value = true
            try {
                _status.value = "Odczyt RF…"
                val snap = rf.snapshot()
                _liveRf.value = snap.serving
                _liveNeighbors.value = snap.neighbors

                _status.value = "GPS…"
                val loc = tryGetLocation()

                val speedResult: SpeedResult? = if (includeSpeedTest) {
                    speed.run { msg -> _status.value = msg }
                } else null

                val point = MeasurementPoint(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    label = label.ifBlank { "P${current.points.size + 1}" },
                    rf = snap.serving,
                    neighbors = snap.neighbors,
                    speed = speedResult,
                    lat = loc?.latitude,
                    lon = loc?.longitude,
                    accuracy = loc?.accuracy,
                    servingCellLocation = _servingCellLocation.value,
                    photoPath = photoPath,
                    notes = notes
                )
                _walkTest.value = current.copy(points = current.points + point)
                _status.value = "Zapisano ${point.label}"
            } catch (e: Exception) {
                _status.value = "Błąd: ${e.message}"
            } finally {
                _isMeasuring.value = false
            }
        }
    }

    fun removePoint(id: String) {
        val current = _walkTest.value ?: return
        _walkTest.value = current.copy(points = current.points.filterNot { it.id == id })
    }

    fun exportPdf(onDone: (File) -> Unit) {
        val current = _walkTest.value ?: return
        viewModelScope.launch {
            try {
                _status.value = "Generowanie PDF…"
                val file = PdfGenerator(getApplication()).generate(current)
                _lastExport.value = file
                _status.value = "PDF gotowy"
                onDone(file)
            } catch (e: Exception) {
                _status.value = "Błąd PDF: ${e.message}"
            }
        }
    }

    fun exportCsv(onDone: (File) -> Unit) {
        val current = _walkTest.value ?: return
        viewModelScope.launch {
            try {
                _status.value = "Generowanie CSV…"
                val file = CsvExporter(getApplication()).export(current)
                _lastExport.value = file
                _status.value = "CSV gotowy"
                onDone(file)
            } catch (e: Exception) {
                _status.value = "Błąd CSV: ${e.message}"
            }
        }
    }

    fun finishWalkTest() {
        _walkTest.value = null
        _lastExport.value = null
        _status.value = ""
    }

    // --- Drive-test mode ---

    fun startDriveMode() {
        if (_walkTest.value == null) {
            startWalkTest("Drive-test", "")
        }
        _driveMode.value = true
        driveJob?.cancel()
        driveJob = viewModelScope.launch {
            val interval = AppSettings.driveInterval(getApplication())
            while (_driveMode.value) {
                addPoint("Auto-${(_walkTest.value?.points?.size ?: 0) + 1}", "", null, false)
                delay(interval * 1000L)
            }
        }
    }

    fun stopDriveMode() {
        _driveMode.value = false
        driveJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryGetLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (_: Exception) { null }
    }

    // --- Settings proxy ---

    fun saveOpenCellIdKey(key: String) {
        viewModelScope.launch { AppSettings.setOpenCellIdKey(getApplication(), key) }
    }

    fun saveLocalCsv(csv: String) {
        viewModelScope.launch { AppSettings.setLocalCsv(getApplication(), csv) }
    }

    fun saveAlertRsrp(v: Int) {
        alertRsrp = v
        viewModelScope.launch { AppSettings.setAlertRsrp(getApplication(), v) }
    }

    suspend fun getOpenCellIdKey(): String = AppSettings.openCellIdKey(getApplication())
    suspend fun getAlertRsrp(): Int = AppSettings.alertRsrp(getApplication())
}
