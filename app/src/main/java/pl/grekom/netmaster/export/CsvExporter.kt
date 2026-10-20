package pl.grekom.netmaster.export

import android.content.Context
import pl.grekom.netmaster.data.WalkTest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporter(private val context: Context) {

    fun export(walkTest: WalkTest): File {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = walkTest.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outDir, "WalkTest_${safe}_$stamp.csv")

        file.bufferedWriter().use { w ->
            w.write("label;timestamp;tech;operator;mccmnc;band;earfcn;cell_id;enb_id;sector;pci;tac;")
            w.write("rsrp_dbm;rsrq_db;sinr_db;rssi_dbm;cqi;")
            w.write("bandwidth;")
            w.write("dl_mbps;ul_mbps;ping_ms;")
            w.write("gps_lat;gps_lon;gps_acc_m;")
            w.write("cell_lat;cell_lon;distance_m;cell_source;cell_address;")
            w.write("notes\n")
            val tf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            walkTest.points.forEach { p ->
                val r = p.rf
                val s = p.speed
                val c = p.servingCellLocation
                val dist = if (c != null && p.lat != null && p.lon != null)
                    pl.grekom.netmaster.location.CellLocator.distance(p.lat, p.lon, c.lat, c.lon).toInt().toString()
                else ""
                w.write(csv(p.label)); w.write(";")
                w.write(tf.format(Date(p.timestamp))); w.write(";")
                w.write(csv(r.tech)); w.write(";")
                w.write(csv(r.operator)); w.write(";")
                w.write(csv(r.mccMnc)); w.write(";")
                w.write(csv(r.bandLabel)); w.write(";")
                w.write(if (r.earfcn > 0) r.earfcn.toString() else ""); w.write(";")
                w.write(if (r.cellId > 0) r.cellId.toString() else ""); w.write(";")
                w.write(if (r.enbId > 0) r.enbId.toString() else ""); w.write(";")
                w.write(if (r.sectorId >= 0) r.sectorId.toString() else ""); w.write(";")
                w.write(if (r.pci >= 0) r.pci.toString() else ""); w.write(";")
                w.write(if (r.tac > 0) r.tac.toString() else ""); w.write(";")
                w.write(if (r.rsrp != Int.MAX_VALUE) r.rsrp.toString() else ""); w.write(";")
                w.write(if (r.rsrq != Int.MAX_VALUE) r.rsrq.toString() else ""); w.write(";")
                w.write(if (r.sinr != Int.MAX_VALUE) r.sinr.toString() else ""); w.write(";")
                w.write(if (r.rssi != Int.MAX_VALUE) r.rssi.toString() else ""); w.write(";")
                w.write(if (r.cqi != Int.MAX_VALUE) r.cqi.toString() else ""); w.write(";")
                w.write(csv(r.bandwidth)); w.write(";")
                w.write(s?.let { String.format(Locale.US, "%.2f", it.downloadMbps) } ?: ""); w.write(";")
                w.write(s?.let { String.format(Locale.US, "%.2f", it.uploadMbps) } ?: ""); w.write(";")
                w.write(s?.pingMs?.toString() ?: ""); w.write(";")
                w.write(p.lat?.let { String.format(Locale.US, "%.6f", it) } ?: ""); w.write(";")
                w.write(p.lon?.let { String.format(Locale.US, "%.6f", it) } ?: ""); w.write(";")
                w.write(p.accuracy?.toInt()?.toString() ?: ""); w.write(";")
                w.write(c?.lat?.let { String.format(Locale.US, "%.6f", it) } ?: ""); w.write(";")
                w.write(c?.lon?.let { String.format(Locale.US, "%.6f", it) } ?: ""); w.write(";")
                w.write(dist); w.write(";")
                w.write(csv(c?.source ?: "")); w.write(";")
                w.write(csv(c?.address ?: "")); w.write(";")
                w.write(csv(p.notes))
                w.write("\n")
            }
        }
        return file
    }

    private fun csv(s: String): String =
        if (s.contains(";") || s.contains("\"") || s.contains("\n"))
            "\"${s.replace("\"", "\"\"")}\""
        else s
}
