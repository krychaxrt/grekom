package pl.grekom.netmaster

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import pl.grekom.netmaster.data.MeasurementPoint
import pl.grekom.netmaster.data.RfSample
import pl.grekom.netmaster.data.SpeedResult
import pl.grekom.netmaster.data.WalkTest
import pl.grekom.netmaster.pdf.PdfGenerator
import pl.grekom.netmaster.rf.RfCollector
import pl.grekom.netmaster.speedtest.SpeedTest
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

class WalkTestViewModel(app: Application) : AndroidViewModel(app) {

    private val rf = RfCollector(app)
    private val speed = SpeedTest()
    private val fused = LocationServices.getFusedLocationProviderClient(app)

    private val _walkTest = MutableStateFlow<WalkTest?>(null)
    val walkTest: StateFlow<WalkTest?> = _walkTest.asStateFlow()

    private val _liveRf = MutableStateFlow(RfSample())
    val liveRf: StateFlow<RfSample> = _liveRf.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _lastPdf = MutableStateFlow<File?>(null)
    val lastPdf: StateFlow<File?> = _lastPdf.asStateFlow()

    fun refreshLiveRf() {
        viewModelScope.launch {
            _liveRf.value = rf.snapshot()
        }
    }

    fun startWalkTest(name: String, siteCode: String) {
        _walkTest.value = WalkTest(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Walk-test" },
            siteCode = siteCode,
            createdAt = System.currentTimeMillis()
        )
        _lastPdf.value = null
    }

    fun addPoint(
        label: String,
        notes: String,
        photoPath: String?,
        includeSpeedTest: Boolean
    ) {
        val current = _walkTest.value ?: return
        viewModelScope.launch {
            _isMeasuring.value = true
            try {
                _status.value = "Odczyt RF…"
                val rfSample = rf.snapshot()
                _liveRf.value = rfSample

                _status.value = "GPS…"
                val loc = tryGetLocation()

                val speedResult: SpeedResult? = if (includeSpeedTest) {
                    speed.run { msg -> _status.value = msg }
                } else null

                val point = MeasurementPoint(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    label = label.ifBlank { "P${current.points.size + 1}" },
                    rf = rfSample,
                    speed = speedResult,
                    lat = loc?.latitude,
                    lon = loc?.longitude,
                    accuracy = loc?.accuracy,
                    photoPath = photoPath,
                    notes = notes
                )
                _walkTest.value = current.copy(points = current.points + point)
                _status.value = "Zapisano punkt ${point.label}"
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
                _lastPdf.value = file
                _status.value = "PDF gotowy: ${file.name}"
                onDone(file)
            } catch (e: Exception) {
                _status.value = "Błąd PDF: ${e.message}"
            }
        }
    }

    fun finishWalkTest() {
        _walkTest.value = null
        _lastPdf.value = null
        _status.value = ""
    }

    @SuppressLint("MissingPermission")
    private suspend fun tryGetLocation(): Location? {
        return try {
            suspendCancellableCoroutine { cont ->
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc -> cont.resume(loc) }
                    .addOnFailureListener { cont.resume(null) }
            }
        } catch (_: Exception) { null }
    }
}
