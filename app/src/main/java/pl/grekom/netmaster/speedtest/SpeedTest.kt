package pl.grekom.netmaster.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.grekom.netmaster.data.SpeedResult
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.system.measureNanoTime

/**
 * Simple speedtest using Cloudflare speed endpoints (no account needed).
 * Download: GET /__down?bytes=N  — streams N bytes of binary.
 * Upload: POST /__up with body of N bytes.
 */
class SpeedTest {

    companion object {
        private const val DOWN_URL = "https://speed.cloudflare.com/__down?bytes=25000000" // 25 MB
        private const val UP_URL = "https://speed.cloudflare.com/__up"
        private const val UPLOAD_BYTES = 5_000_000 // 5 MB
        private const val PING_HOST = "1.1.1.1"
    }

    suspend fun run(onProgress: (String) -> Unit = {}): SpeedResult = withContext(Dispatchers.IO) {
        try {
            onProgress("Ping…")
            val ping = measurePing()

            onProgress("Download…")
            val down = measureDownload()

            onProgress("Upload…")
            val up = measureUpload()

            SpeedResult(
                downloadMbps = down,
                uploadMbps = up,
                pingMs = ping,
                success = true
            )
        } catch (e: Exception) {
            SpeedResult(success = false, error = e.message ?: "Błąd pomiaru")
        }
    }

    private fun measurePing(): Int {
        var total = 0L
        var ok = 0
        repeat(4) {
            try {
                val addr = InetAddress.getByName(PING_HOST)
                val ns = measureNanoTime {
                    // TCP connect as ping proxy (ICMP needs root)
                    val sock = java.net.Socket()
                    sock.connect(java.net.InetSocketAddress(addr, 80), 2000)
                    sock.close()
                }
                total += ns / 1_000_000
                ok++
            } catch (_: Exception) { /* ignore single failure */ }
        }
        return if (ok > 0) (total / ok).toInt() else 999
    }

    private fun measureDownload(): Double {
        val conn = URL(DOWN_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "GET"
        var bytes = 0L
        val ns = measureNanoTime {
            conn.inputStream.use { input ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    bytes += n
                }
            }
        }
        conn.disconnect()
        val seconds = ns / 1e9
        if (seconds <= 0 || bytes <= 0) return 0.0
        return (bytes * 8.0 / 1_000_000.0) / seconds
    }

    private fun measureUpload(): Double {
        val payload = ByteArray(UPLOAD_BYTES) { (it and 0xFF).toByte() }
        val conn = URL(UP_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 60_000
        conn.doOutput = true
        conn.requestMethod = "POST"
        conn.setFixedLengthStreamingMode(payload.size)
        conn.setRequestProperty("Content-Type", "application/octet-stream")

        val ns = measureNanoTime {
            conn.outputStream.use { out: OutputStream ->
                var i = 0
                val chunk = 16 * 1024
                while (i < payload.size) {
                    val n = minOf(chunk, payload.size - i)
                    out.write(payload, i, n)
                    i += n
                }
                out.flush()
            }
            // Drain response
            conn.inputStream.use { it.copyTo(ByteArrayOutputStream()) }
        }
        conn.disconnect()
        val seconds = ns / 1e9
        if (seconds <= 0) return 0.0
        return (payload.size * 8.0 / 1_000_000.0) / seconds
    }
}
