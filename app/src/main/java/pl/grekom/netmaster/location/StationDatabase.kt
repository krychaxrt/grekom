package pl.grekom.netmaster.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader

/**
 * In-memory database of mobile network stations from UKE permits (loaded from assets).
 * Provides nearest-station lookup by operator + GPS.
 */
object StationDatabase {

    data class Station(
        val operator: String,
        val mccMnc: String,
        val stationId: String,
        val lat: Double,
        val lon: Double,
        val city: String,
        val address: String,
        val azimuths: List<Int>,
        val bands: List<String>
    ) {
        fun fullAddress(): String {
            val parts = mutableListOf<String>()
            if (city.isNotBlank()) parts += city
            if (address.isNotBlank()) parts += address
            return parts.joinToString(", ").ifBlank { "Stacja $stationId" }
        }
    }

    private val loadMutex = Mutex()
    @Volatile private var stations: List<Station> = emptyList()
    @Volatile private var loaded = false

    suspend fun ensureLoaded(context: Context) = loadMutex.withLock {
        if (loaded) return@withLock
        withContext(Dispatchers.IO) {
            try {
                val list = mutableListOf<Station>()
                context.assets.open("stations.csv").bufferedReader(Charsets.UTF_8).use { br ->
                    br.readLine() // skip header
                    while (true) {
                        val line = br.readLine() ?: break
                        parseLine(line)?.let { list += it }
                    }
                }
                stations = list
                loaded = true
            } catch (_: Exception) {
                loaded = true
            }
        }
    }

    private fun parseLine(line: String): Station? {
        val fields = parseCsvRow(line)
        if (fields.size < 9) return null
        return try {
            Station(
                operator = fields[0],
                mccMnc = fields[1],
                stationId = fields[2],
                lat = fields[3].toDouble(),
                lon = fields[4].toDouble(),
                city = fields[5],
                address = fields[6],
                azimuths = fields[7].split(",")
                    .mapNotNull { it.trim().toIntOrNull() },
                bands = fields[8].split("|").filter { it.isNotBlank() }
            )
        } catch (_: Exception) { null }
    }

    private fun parseCsvRow(line: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                    else inQuotes = false
                } else sb.append(c)
            } else {
                when (c) {
                    ',' -> { out += sb.toString(); sb.clear() }
                    '"' -> inQuotes = true
                    else -> sb.append(c)
                }
            }
            i++
        }
        out += sb.toString()
        return out
    }

    /** Find nearest station from the given operator within radiusMeters. */
    fun findNearest(
        operator: String,
        lat: Double,
        lon: Double,
        radiusMeters: Double = 500.0
    ): Pair<Station, Double>? {
        if (!loaded || stations.isEmpty()) return null
        val op = operator.lowercase()
        var best: Pair<Station, Double>? = null
        for (s in stations) {
            if (!s.operator.equals(op, ignoreCase = true)) continue
            val d = CellLocator.distance(lat, lon, s.lat, s.lon)
            if (d > radiusMeters) continue
            if (best == null || d < best.second) best = s to d
        }
        return best
    }

    /** All stations within radius, from any operator. */
    fun findAllNear(lat: Double, lon: Double, radiusMeters: Double = 1500.0): List<Pair<Station, Double>> {
        if (!loaded || stations.isEmpty()) return emptyList()
        val out = mutableListOf<Pair<Station, Double>>()
        for (s in stations) {
            val d = CellLocator.distance(lat, lon, s.lat, s.lon)
            if (d <= radiusMeters) out += s to d
        }
        return out.sortedBy { it.second }
    }

    val size get() = stations.size
    val isLoaded get() = loaded
}
