package com.hematoscope.app.domain.report

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private const val PAGE_W = 595   // A4 @72dpi
private const val PAGE_H = 842
private const val MARGIN = 40f
private val CONTENT_W = PAGE_W - 2 * MARGIN
private val CRIMSON = 0xFF7A0C26.toInt()

/**
 * Renders a [CaseReportData] into a paginated A4 PDF using the platform
 * [PdfDocument] (no third-party dependency). The file is written to the app's
 * cache under `reports/` and returned for sharing.
 */
object CaseReportGenerator {

    suspend fun generate(context: Context, data: CaseReportData): File =
        withContext(Dispatchers.IO) {
            val doc = PdfDocument()
            val layout = PdfLayout(doc)

            layout.header(data)
            layout.caseBlock(data)
            if (data.hasDifferential) layout.differentialBlock(data)
            layout.morphologyBlock(data)
            layout.capturesBlock(data)
            layout.footer()
            layout.finish()

            val dir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
            val safeCode = data.patientCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(dir, "informe_${safeCode}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()
            file
        }

    /** Minimal top-to-bottom layout engine with automatic page breaks. */
    private class PdfLayout(private val doc: PdfDocument) {
        private var pageNumber = 1
        private var page: PdfDocument.Page = startPage()
        private var canvas: Canvas = page.canvas
        private var y = MARGIN

        private val title = paint(20f, CRIMSON, bold = true)
        private val h2 = paint(14f, CRIMSON, bold = true)
        private val body = paint(11f, Color.BLACK)
        private val bodyBold = paint(11f, Color.BLACK, bold = true)
        private val small = paint(9f, Color.DKGRAY)
        private val rule = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }

        private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        private fun startPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
            return doc.startPage(info)
        }

        private fun ensure(space: Float) {
            if (y + space > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNumber++
                page = startPage()
                canvas = page.canvas
                y = MARGIN
            }
        }

        private fun line(text: String, p: Paint, gap: Float = 4f) {
            ensure(p.textSize + gap)
            y += p.textSize
            canvas.drawText(text, MARGIN, y, p)
            y += gap
        }

        private fun paragraph(text: String, p: Paint) {
            for (row in wrap(text, p, CONTENT_W)) line(row, p, gap = 2f)
        }

        private fun spacer(h: Float) { y += h }

        private fun divider() {
            ensure(8f)
            y += 4f
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, rule)
            y += 4f
        }

        fun header(data: CaseReportData) {
            line("HematoScope — Informe de caso", title)
            line("Citomorfología digital · Hematología", small)
            divider()
        }

        fun caseBlock(data: CaseReportData) {
            line("Caso", h2)
            line("Identificador: ${data.patientCode}", bodyBold)
            if (data.description.isNotBlank()) paragraph("Descripción: ${data.description}", body)
            line("Creado: ${dateFmt.format(Date(data.createdAtEpochMs))}", body)
            line("Informe generado: ${dateFmt.format(Date())}", body)
            spacer(8f)
        }

        fun differentialBlock(data: CaseReportData) {
            line("Recuento diferencial", h2)
            line("Leucocitos contados: ${data.wbcTotal} (objetivo ${data.differentialTarget})", body)
            spacer(4f)
            // Table header
            ensure(16f)
            y += body.textSize
            canvas.drawText("Tipo celular", MARGIN, y, bodyBold)
            canvas.drawText("N.º", MARGIN + 330, y, bodyBold)
            canvas.drawText("%", MARGIN + 420, y, bodyBold)
            y += 4f
            divider()
            data.differentialRows.forEach { r ->
                ensure(14f)
                y += body.textSize
                canvas.drawText(r.name, MARGIN, y, body)
                canvas.drawText(r.count.toString(), MARGIN + 330, y, body)
                canvas.drawText("%.1f".format(r.percent), MARGIN + 420, y, body)
                y += 3f
            }
            if (data.nrbcCount > 0) {
                line(
                    "Eritroblastos (NRBC): ${data.nrbcCount}  ·  %.1f por 100 leucocitos"
                        .format(data.nrbcPer100Wbc),
                    body
                )
            }
            spacer(8f)
        }

        fun morphologyBlock(data: CaseReportData) {
            line("Morfología", h2)
            if (data.gradedFindings.isEmpty() && data.qualitativeFindings.isEmpty()) {
                line("Sin hallazgos registrados.", body)
                spacer(8f)
                return
            }
            if (data.gradedFindings.isNotEmpty()) {
                line("Serie roja (semicuantitativo):", bodyBold)
                data.gradedFindings.forEach { line("•  ${it.name}: ${it.gradeLabel}", body) }
            }
            if (data.qualitativeFindings.isNotEmpty()) {
                spacer(4f)
                line("Hallazgos cualitativos:", bodyBold)
                paragraph(data.qualitativeFindings.joinToString(", "), body)
            }
            spacer(8f)
        }

        fun capturesBlock(data: CaseReportData) {
            line("Campos capturados (${data.capturePaths.size})", h2)
            if (data.capturePaths.isEmpty()) {
                line("Sin campos guardados.", body)
                return
            }
            val thumb = 150f
            val cols = 3
            val gap = 12f
            var col = 0
            var rowTop = 0f
            data.capturePaths.forEach { path ->
                val bmp = decodeScaled(path, thumb.toInt()) ?: return@forEach
                if (col == 0) {
                    ensure(thumb + gap)
                    rowTop = y
                }
                val x = MARGIN + col * (thumb + gap)
                val scale = minOf(thumb / bmp.width, thumb / bmp.height)
                val w = bmp.width * scale
                val h = bmp.height * scale
                canvas.drawBitmap(
                    bmp,
                    null,
                    Rect(x.toInt(), rowTop.toInt(), (x + w).toInt(), (rowTop + h).toInt()),
                    null
                )
                col++
                if (col >= cols) {
                    col = 0
                    y = rowTop + thumb + gap
                }
            }
            if (col != 0) y = rowTop + thumb + gap
            spacer(8f)
        }

        fun footer() {
            divider()
            paragraph(
                "Documento generado por HematoScope como apoyo a la decisión. No sustituye " +
                    "el criterio del profesional de laboratorio ni constituye un diagnóstico certificado.",
                small
            )
        }

        fun finish() { doc.finishPage(page) }

        // --- helpers ---
        private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
            this.textSize = size
            this.color = color
            isAntiAlias = true
            isFakeBoldText = bold
        }

        private fun wrap(text: String, p: Paint, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (p.measureText(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) lines.add(current.toString())
                    current = StringBuilder(word)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
            return if (lines.isEmpty()) listOf("") else lines
        }

        private fun decodeScaled(path: String, target: Int): android.graphics.Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            val largest = max(bounds.outWidth, bounds.outHeight)
            while (largest / sample > target * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeFile(path, opts)
        }
    }
}
