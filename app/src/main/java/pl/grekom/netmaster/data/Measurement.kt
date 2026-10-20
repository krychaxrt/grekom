package pl.grekom.netmaster.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RfSample(
    val tech: String = "-",           // LTE, NR, GSM, WCDMA
    val operator: String = "-",       // T-Mobile, etc.
    val mccMnc: String = "-",
    val cellId: Long = -1,
    val pci: Int = -1,
    val tac: Int = -1,
    val earfcn: Int = -1,
    val bandLabel: String = "-",      // "B8 (900)", "B1 (2100)"
    val rsrp: Int = Int.MAX_VALUE,    // dBm
    val rsrq: Int = Int.MAX_VALUE,    // dB
    val sinr: Int = Int.MAX_VALUE,    // dB
    val rssi: Int = Int.MAX_VALUE     // dBm
) {
    fun rsrpStr() = if (rsrp == Int.MAX_VALUE) "-" else "$rsrp dBm"
    fun rsrqStr() = if (rsrq == Int.MAX_VALUE) "-" else "$rsrq dB"
    fun sinrStr() = if (sinr == Int.MAX_VALUE) "-" else "$sinr dB"
    fun rssiStr() = if (rssi == Int.MAX_VALUE) "-" else "$rssi dBm"
}

data class SpeedResult(
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val pingMs: Int = 0,
    val success: Boolean = false,
    val error: String? = null
)

data class MeasurementPoint(
    val id: String,
    val timestamp: Long,
    val label: String,              // "P1", "P2", or user-named
    val rf: RfSample,
    val speed: SpeedResult?,
    val lat: Double? = null,
    val lon: Double? = null,
    val accuracy: Float? = null,
    val photoPath: String? = null,
    val notes: String = ""
) {
    fun timeStr(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    fun gpsStr(): String =
        if (lat != null && lon != null)
            String.format(Locale.US, "%.6f, %.6f (±%.0fm)", lat, lon, accuracy ?: 0f)
        else "-"
}

data class WalkTest(
    val id: String,
    val name: String,
    val siteCode: String,              // for Grekom: T-Mobile site code
    val createdAt: Long,
    val points: List<MeasurementPoint> = emptyList()
)
