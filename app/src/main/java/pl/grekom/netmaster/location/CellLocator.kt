package pl.grekom.netmaster.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pl.grekom.netmaster.data.CellLocation
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Resolves eNodeB/CellId → lat/lon via OpenCellID (requires free API key).
 * Falls back to local CSV lookup if provided.
 */
class CellLocator(
    private val apiKey: String?,
    private val localCsv: Map<Long, CellLocation> = emptyMap()
) {

    private val cache = mutableMapOf<Long, CellLocation?>()

    suspend fun lookup(mccMncStr: String, lac: Int, cellId: Long, radio: String = "LTE"): CellLocation? =
        withContext(Dispatchers.IO) {
            if (cellId <= 0) return@withContext null
            if (cache.containsKey(cellId)) return@withContext cache[cellId]

            // Local CSV takes precedence
            localCsv[cellId]?.let {
                cache[cellId] = it
                return@withContext it
            }

            if (apiKey.isNullOrBlank()) return@withContext null

            val mcc = mccMncStr.take(3).toIntOrNull() ?: return@withContext null
            val mnc = mccMncStr.drop(3).toIntOrNull() ?: return@withContext null

            try {
                val url = buildString {
                    append("https://opencellid.org/cell/get?key=")
                    append(URLEncoder.encode(apiKey, "UTF-8"))
                    append("&mcc=$mcc&mnc=$mnc&lac=$lac&cellid=$cellId&radio=$radio&format=json")
                }
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                val code = conn.responseCode
                if (code != 200) {
                    cache[cellId] = null
                    return@withContext null
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.has("lat") && json.has("lon")) {
                    val loc = CellLocation(
                        lat = json.getDouble("lat"),
                        lon = json.getDouble("lon"),
                        range = json.optInt("range", 0),
                        source = "OpenCellID"
                    )
                    cache[cellId] = loc
                    loc
                } else {
                    cache[cellId] = null
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

    companion object {
        /** Parse user-supplied CSV: cellid,lat,lon,address */
        fun parseCsv(text: String): Map<Long, CellLocation> {
            val result = mutableMapOf<Long, CellLocation>()
            text.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                val parts = trimmed.split(",", ";")
                if (parts.size < 3) return@forEach
                val id = parts[0].trim().toLongOrNull() ?: return@forEach
                val lat = parts[1].trim().toDoubleOrNull() ?: return@forEach
                val lon = parts[2].trim().toDoubleOrNull() ?: return@forEach
                val addr = parts.getOrNull(3)?.trim()
                result[id] = CellLocation(lat, lon, source = "Lokalna baza", address = addr)
            }
            return result
        }

        /** Compute bearing (0-360°) from one point to another. */
        fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val φ1 = Math.toRadians(lat1)
            val φ2 = Math.toRadians(lat2)
            val Δλ = Math.toRadians(lon2 - lon1)
            val y = Math.sin(Δλ) * Math.cos(φ2)
            val x = Math.cos(φ1) * Math.sin(φ2) - Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ)
            val θ = Math.atan2(y, x)
            return (Math.toDegrees(θ) + 360) % 360
        }

        /** Haversine distance in meters. */
        fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val φ1 = Math.toRadians(lat1)
            val φ2 = Math.toRadians(lat2)
            val Δφ = Math.toRadians(lat2 - lat1)
            val Δλ = Math.toRadians(lon2 - lon1)
            val a = Math.sin(Δφ / 2).let { it * it } +
                    Math.cos(φ1) * Math.cos(φ2) *
                    Math.sin(Δλ / 2).let { it * it }
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return r * c
        }
    }
}
