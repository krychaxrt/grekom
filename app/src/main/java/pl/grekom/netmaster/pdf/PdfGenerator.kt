package pl.grekom.netmaster.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import pl.grekom.netmaster.R
import pl.grekom.netmaster.data.MeasurementPoint
import pl.grekom.netmaster.data.WalkTest
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    companion object {
        private const val PAGE_W = 595    // A4 @ 72 dpi
        private const val PAGE_H = 842
        private const val MARGIN = 36f
    }

    private val titlePaint = Paint().apply {
        color = Color.BLACK
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val h2Paint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10f
        isAntiAlias = true
    }
    private val smallPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 8f
        isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }
    private val headerBg = Paint().apply { color = Color.rgb(230, 0, 70) } // Grekom/T-Mobile magenta
    private val headerText = Paint().apply {
        color = Color.WHITE
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
    }

    fun generate(walkTest: WalkTest): File {
        val doc = PdfDocument()

        // ---- Cover page ----
        drawCoverPage(doc, walkTest)

        // ---- Summary table ----
        drawSummaryPage(doc, walkTest)

        // ---- Per-point detail pages ----
        walkTest.points.forEachIndexed { idx, point ->
            drawPointPage(doc, walkTest, point, idx + 1)
        }

        val outDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safe = walkTest.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(outDir, "WalkTest_${safe}_$stamp.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun drawCoverPage(doc: PdfDocument, wt: WalkTest) {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create()
        val page = doc.startPage(info)
        val c = page.canvas

        drawHeader(c, "RAPORT WALK-TEST")

        var y = 150f

        // Try to load logo
        try {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.grekom_logo)
            if (logo != null) {
                val scale = 150f / logo.width
                val h = logo.height * scale
                c.drawBitmap(logo, null, android.graphics.RectF(MARGIN, y, MARGIN + 150f, y + h), null)
                y += h + 20
            }
        } catch (_: Exception) { /* no logo */ }

        c.drawText("Grekom Sp. z o.o.", MARGIN, y, titlePaint); y += 24
        c.drawText("Wykonawca T-Mobile Polska", MARGIN, y, textPaint); y += 40

        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint); y += 24

        drawKV(c, "Nazwa walk-testu:", wt.name, y); y += 20
        drawKV(c, "Kod lokalizacji:", wt.siteCode.ifBlank { "-" }, y); y += 20
        drawKV(c, "Data wykonania:", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(wt.createdAt)), y); y += 20
        drawKV(c, "Liczba pomiarów:", wt.points.size.toString(), y); y += 20

        // Operator / tech summary
        val techSet = wt.points.map { it.rf.tech }.toSet().joinToString(", ")
        val bandSet = wt.points.map { it.rf.bandLabel }.filter { it != "-" }.toSet().joinToString(", ")
        drawKV(c, "Technologie:", techSet.ifBlank { "-" }, y); y += 20
        drawKV(c, "Pasma:", bandSet.ifBlank { "-" }, y); y += 20

        // Stats
        val validRsrp = wt.points.mapNotNull { if (it.rf.rsrp != Int.MAX_VALUE) it.rf.rsrp else null }
        if (validRsrp.isNotEmpty()) {
            y += 10
            c.drawText("Statystyki RSRP:", MARGIN, y, h2Paint); y += 18
            drawKV(c, "  Min:", "${validRsrp.min()} dBm", y); y += 16
            drawKV(c, "  Max:", "${validRsrp.max()} dBm", y); y += 16
            drawKV(c, "  Średnia:", "${validRsrp.average().toInt()} dBm", y); y += 16
        }

        drawFooter(c, 1, 1 + 1 + wt.points.size)
        doc.finishPage(page)
    }

    private fun drawSummaryPage(doc: PdfDocument, wt: WalkTest) {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2).create()
        val page = doc.startPage(info)
        val c = page.canvas
        drawHeader(c, "PODSUMOWANIE POMIARÓW")

        var y = 120f
        // Column headers
        val cols = listOf("#", "Czas", "Tech", "Pasmo", "RSRP", "RSRQ", "SINR", "DL Mbps", "UL Mbps")
        val xs = floatArrayOf(MARGIN, MARGIN + 24, MARGIN + 100, MARGIN + 148, MARGIN + 220, MARGIN + 270, MARGIN + 315, MARGIN + 360, MARGIN + 420)

        c.drawRect(MARGIN - 4, y - 12, PAGE_W - MARGIN + 4, y + 4, headerBg)
        cols.forEachIndexed { i, label -> c.drawText(label, xs[i], y, headerText) }
        y += 18

        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        wt.points.forEachIndexed { idx, p ->
            if (y > PAGE_H - 60) return@forEachIndexed // simple overflow guard
            c.drawText("${idx + 1}", xs[0], y, textPaint)
            c.drawText(timeFmt.format(Date(p.timestamp)), xs[1], y, textPaint)
            c.drawText(p.rf.tech, xs[2], y, textPaint)
            c.drawText(p.rf.bandLabel, xs[3], y, textPaint)
            c.drawText(p.rf.rsrpStr(), xs[4], y, textPaint)
            c.drawText(p.rf.rsrqStr(), xs[5], y, textPaint)
            c.drawText(p.rf.sinrStr(), xs[6], y, textPaint)
            c.drawText(p.speed?.let { String.format(Locale.US, "%.1f", it.downloadMbps) } ?: "-", xs[7], y, textPaint)
            c.drawText(p.speed?.let { String.format(Locale.US, "%.1f", it.uploadMbps) } ?: "-", xs[8], y, textPaint)
            y += 16
            c.drawLine(MARGIN, y - 10, PAGE_W - MARGIN, y - 10, linePaint)
        }

        drawFooter(c, 2, 1 + 1 + wt.points.size)
        doc.finishPage(page)
    }

    private fun drawPointPage(doc: PdfDocument, wt: WalkTest, p: MeasurementPoint, idx: Int) {
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 2 + idx).create()
        val page = doc.startPage(info)
        val c = page.canvas
        drawHeader(c, "PUNKT $idx — ${p.label}")

        var y = 120f
        c.drawText("Czas: ${p.timeStr()}", MARGIN, y, textPaint); y += 16
        c.drawText("GPS: ${p.gpsStr()}", MARGIN, y, textPaint); y += 24

        c.drawText("Parametry RF", MARGIN, y, h2Paint); y += 18
        drawKV(c, "Technologia:", p.rf.tech, y); y += 14
        drawKV(c, "Operator:", "${p.rf.operator} (${p.rf.mccMnc})", y); y += 14
        drawKV(c, "Pasmo:", p.rf.bandLabel, y); y += 14
        drawKV(c, "EARFCN:", if (p.rf.earfcn > 0) p.rf.earfcn.toString() else "-", y); y += 14
        drawKV(c, "Cell ID:", if (p.rf.cellId > 0) p.rf.cellId.toString() else "-", y); y += 14
        drawKV(c, "PCI:", if (p.rf.pci >= 0) p.rf.pci.toString() else "-", y); y += 14
        drawKV(c, "TAC:", if (p.rf.tac > 0) p.rf.tac.toString() else "-", y); y += 14
        drawKV(c, "RSRP:", p.rf.rsrpStr(), y); y += 14
        drawKV(c, "RSRQ:", p.rf.rsrqStr(), y); y += 14
        drawKV(c, "SINR / RSSNR:", p.rf.sinrStr(), y); y += 14
        drawKV(c, "RSSI:", p.rf.rssiStr(), y); y += 24

        c.drawText("Speed test", MARGIN, y, h2Paint); y += 18
        if (p.speed != null && p.speed.success) {
            drawKV(c, "Download:", String.format(Locale.US, "%.2f Mbps", p.speed.downloadMbps), y); y += 14
            drawKV(c, "Upload:", String.format(Locale.US, "%.2f Mbps", p.speed.uploadMbps), y); y += 14
            drawKV(c, "Ping:", "${p.speed.pingMs} ms", y); y += 18
        } else {
            c.drawText(p.speed?.error ?: "Nie wykonano", MARGIN + 10, y, textPaint); y += 18
        }

        // Photo
        p.photoPath?.let { path ->
            try {
                val bmp = BitmapFactory.decodeFile(path)
                if (bmp != null) {
                    val maxW = 240f
                    val maxH = 180f
                    val scale = minOf(maxW / bmp.width, maxH / bmp.height)
                    val w = bmp.width * scale
                    val h = bmp.height * scale
                    val photoX = PAGE_W - MARGIN - w
                    val photoY = 120f
                    c.drawBitmap(bmp, null, android.graphics.RectF(photoX, photoY, photoX + w, photoY + h), null)
                    c.drawText("Zdjęcie:", photoX, photoY - 6, smallPaint)
                }
            } catch (_: Exception) { /* skip */ }
        }

        y = maxOf(y, 340f)
        c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint); y += 14
        c.drawText("Notatki", MARGIN, y, h2Paint); y += 18

        val notes = p.notes.ifBlank { "-" }
        wrapText(notes, PAGE_W - 2 * MARGIN, textPaint).forEach { line ->
            if (y > PAGE_H - 60) return@forEach
            c.drawText(line, MARGIN, y, textPaint); y += 14
        }

        drawFooter(c, 2 + idx, 1 + 1 + wt.points.size)
        doc.finishPage(page)
    }

    private fun drawHeader(c: Canvas, title: String) {
        c.drawRect(0f, 0f, PAGE_W.toFloat(), 80f, headerBg)
        val tp = Paint(headerText).apply { textSize = 18f }
        c.drawText(title, MARGIN, 50f, tp)
        c.drawText("Grekom NetMaster", PAGE_W - MARGIN - 110, 72f, smallPaint.apply { color = Color.WHITE })
    }

    private fun drawFooter(c: Canvas, page: Int, total: Int) {
        val fp = Paint(smallPaint).apply { color = Color.GRAY }
        c.drawLine(MARGIN, PAGE_H - 36f, PAGE_W - MARGIN, PAGE_H - 36f, linePaint)
        c.drawText("Grekom Sp. z o.o. — Walk-test report", MARGIN, PAGE_H - 20f, fp)
        c.drawText("Strona $page / $total", PAGE_W - MARGIN - 60, PAGE_H - 20f, fp)
    }

    private fun drawKV(c: Canvas, k: String, v: String, y: Float) {
        val keyPaint = Paint(textPaint).apply { isFakeBoldText = true }
        c.drawText(k, MARGIN, y, keyPaint)
        c.drawText(v, MARGIN + 110, y, textPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        val bounds = Rect()
        for (word in words) {
            val trial = if (current.isEmpty()) word else "$current $word"
            paint.getTextBounds(trial, 0, trial.length, bounds)
            if (bounds.width() > maxWidth) {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    current = StringBuilder(word)
                } else {
                    lines += word
                    current = StringBuilder()
                }
            } else {
                current = StringBuilder(trial)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
