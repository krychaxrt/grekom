package pl.grekom.netmaster.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RfSample(
    val tech: String = "-",
    val operator: String = "-",
    val mccMnc: String = "-",
    val cellId: Long = -1,
    val enbId: Long = -1,        // eNodeB = cellId >> 8 for LTE
    val sectorId: Int = -1,       // = cellId & 0xFF for LTE
    val pci: Int = -1,
    val tac: Int = -1,
    val earfcn: Int = -1,
    val band: Int = -1,           // band number (1, 3, 7, 8, 20, 28, 78...)
    val bandLabel: String = "-",  // "B8 (900)"
    val bandwidth: String = "-",  // "15 MHz" or "15+15+15+15 MHz"
    val rsrp: Int = Int.MAX_VALUE,
    val rsrq: Int = Int.MAX_VALUE,
    val sinr: Int = Int.MAX_VALUE,
    val rssi: Int = Int.MAX_VALUE,
    val cqi: Int = Int.MAX_VALUE,
    val nrAvailable: Boolean = false,
    val nrState: String = "-"      // "Connected", "Not Restricted", "None"
) {
    fun rsrpStr() = if (rsrp == Int.MAX_VALUE) "-" else "$rsrp"
    fun rsrqStr() = if (rsrq == Int.MAX_VALUE) "-" else "$rsrq"
    fun sinrStr() = if (sinr == Int.MAX_VALUE) "-" else "$sinr"
    fun rssiStr() = if (rssi == Int.MAX_VALUE) "-" else "$rssi"
    fun cqiStr() = if (cqi == Int.MAX_VALUE) "-" else "$cqi"

    /** Color-coded signal quality level. */
    fun rsrpLevel(): SignalLevel = when {
        rsrp == Int.MAX_VALUE -> SignalLevel.UNKNOWN
        rsrp >= -80 -> SignalLevel.EXCELLENT
        rsrp >= -90 -> SignalLevel.GOOD
        rsrp >= -100 -> SignalLevel.FAIR
        rsrp >= -110 -> SignalLevel.POOR
        else -> SignalLevel.VERY_POOR
    }
}

enum class SignalLevel { EXCELLENT, GOOD, FAIR, POOR, VERY_POOR, UNKNOWN }

/** A single neighbor cell (not registered, visible to modem). */
data class NeighborCell(
    val tech: String,
    val pci: Int,
    val cellId: Long = -1,
    val earfcn: Int = -1,
    val band: Int = -1,
    val bandLabel: String = "-",
    val rsrp: Int = Int.MAX_VALUE,
    val rsrq: Int = Int.MAX_VALUE,
    val sinr: Int = Int.MAX_VALUE
) {
    fun rsrpStr() = if (rsrp == Int.MAX_VALUE) "-" else "$rsrp"
    fun rsrqStr() = if (rsrq == Int.MAX_VALUE) "-" else "$rsrq"
    fun sinrStr() = if (sinr == Int.MAX_VALUE) "-" else "$sinr"
}

/** Resolved base-station lat/lon from OpenCellID or local CSV. */
data class CellLocation(
    val lat: Double,
    val lon: Double,
    val range: Int = 0,
    val source: String = "OpenCellID",
    val address: String? = null
)

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
    val label: String,
    val rf: RfSample,
    val neighbors: List<NeighborCell> = emptyList(),
    val speed: SpeedResult? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val accuracy: Float? = null,
    val servingCellLocation: CellLocation? = null,
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
    val siteCode: String,
    val createdAt: Long,
    val points: List<MeasurementPoint> = emptyList()
)

/** Entry in the cell-change diary (serving cell handover, band change, tech change). */
data class LogEntry(
    val timestamp: Long,
    val type: LogEventType,
    val from: String,
    val to: String,
    val rsrp: Int = Int.MAX_VALUE
) {
    fun timeStr(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

enum class LogEventType { CELL_CHANGE, BAND_CHANGE, TECH_CHANGE, SIGNAL_ALERT }
