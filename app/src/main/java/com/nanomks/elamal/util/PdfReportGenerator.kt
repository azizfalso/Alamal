package com.nanomks.elamal.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nanomks.elamal.data.EntryType
import com.nanomks.elamal.data.Settlement
import com.nanomks.elamal.data.WorkEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateAndSharePdf(
        context: Context,
        reportTitle: String,
        entries: List<WorkEntry>,
        settlements: List<Settlement>,
        isDarkMode: Boolean
    ) {
        try {
            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val bgColor = if (isDarkMode) Color.parseColor("#0F172A") else Color.parseColor("#FFFFFF")
            val textColor = if (isDarkMode) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
            val subTextColor = if (isDarkMode) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
            val gridColor = if (isDarkMode) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")
            val headerBg = if (isDarkMode) Color.parseColor("#1E293B") else Color.parseColor("#F1F5F9")

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val dfDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val dfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            var totalHourlyMinutes = 0L
            var totalRemote = 0.0
            var totalCredit = 0.0
            var netTotal = 0.0
            for (e in entries) {
                netTotal += e.calculatedAmount
                when (e.type) {
                    EntryType.HOURLY -> {
                        if (e.endTime != null) {
                            totalHourlyMinutes += (((e.endTime - e.startTime) / 60000L) - e.breakMinutes).coerceAtLeast(0)
                        }
                    }
                    EntryType.REMOTE -> totalRemote += e.calculatedAmount
                    EntryType.CREDIT -> totalCredit += e.calculatedAmount
                }
            }

            val chunks = if (entries.isEmpty()) listOf(emptyList()) else entries.chunked(20)

            chunks.forEachIndexed { pageIndex, pageEntries ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(bgColor)

                paint.style = Paint.Style.FILL
                paint.color = textColor
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    "تقرير «العمل» - $reportTitle (${pageIndex + 1}/${chunks.size})",
                    (pageWidth - 36).toFloat(),
                    46f,
                    paint
                )

                var y = 68f
                if (pageIndex == 0) {
                    val cardTop = 65f
                    val cardBottom = 130f
                    val cardW = 165f

                    // Card 1: Net Total
                    paint.color = if (isDarkMode) Color.parseColor("#064E3B") else Color.parseColor("#D1FAE5")
                    canvas.drawRoundRect(RectF(pageWidth - 36f - cardW, cardTop, pageWidth - 36f, cardBottom), 10f, 10f, paint)
                    paint.color = if (isDarkMode) Color.parseColor("#34D399") else Color.parseColor("#065F46")
                    paint.textSize = 11f
                    canvas.drawText("الصافي الإجمالي", pageWidth - 48f, cardTop + 24f, paint)
                    paint.textSize = 15f
                    canvas.drawText(ElamalUtils.formatMoney(netTotal), pageWidth - 48f, cardTop + 48f, paint)

                    // Card 2: Total Hours («23 س 58 د»)
                    val c2Right = pageWidth - 36f - cardW - 14f
                    paint.color = if (isDarkMode) Color.parseColor("#1E3A8A") else Color.parseColor("#DBEAFE")
                    canvas.drawRoundRect(RectF(c2Right - cardW, cardTop, c2Right, cardBottom), 10f, 10f, paint)
                    paint.color = if (isDarkMode) Color.parseColor("#60A5FA") else Color.parseColor("#1E40AF")
                    paint.textSize = 11f
                    canvas.drawText("مجموع الساعات", c2Right - 12f, cardTop + 24f, paint)
                    paint.textSize = 15f
                    canvas.drawText(ElamalUtils.formatDurationArabic(totalHourlyMinutes), c2Right - 12f, cardTop + 48f, paint)

                    // Card 3: Credit & Remote
                    val c3Right = c2Right - cardW - 14f
                    paint.color = if (isDarkMode) Color.parseColor("#7F1D1D") else Color.parseColor("#FEE2E2")
                    canvas.drawRoundRect(RectF(36f, cardTop, c3Right, cardBottom), 10f, 10f, paint)
                    paint.color = if (isDarkMode) Color.parseColor("#F87171") else Color.parseColor("#991B1B")
                    paint.textSize = 11f
                    canvas.drawText("الكريدي / عن بعد", c3Right - 12f, cardTop + 24f, paint)
                    paint.textSize = 13f
                    canvas.drawText("${String.format(Locale.US, "%.1f", totalCredit)} / +${String.format(Locale.US, "%.1f", totalRemote)}", c3Right - 12f, cardTop + 48f, paint)

                    y = 152f
                }

                val leftX = 36f
                val rightX = pageWidth - 36f
                val rowH = 26f
                val col1 = rightX - 95f
                val col2 = col1 - 80f
                val col3 = col2 - 95f
                val col4 = col3 - 150f

                // Table Header
                paint.style = Paint.Style.FILL
                paint.color = headerBg
                canvas.drawRect(leftX, y, rightX, y + rowH, paint)
                paint.color = gridColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(leftX, y, rightX, y + rowH, paint)
                canvas.drawLine(col1, y, col1, y + rowH, paint)
                canvas.drawLine(col2, y, col2, y + rowH, paint)
                canvas.drawLine(col3, y, col3, y + rowH, paint)
                canvas.drawLine(col4, y, col4, y + rowH, paint)

                paint.style = Paint.Style.FILL
                paint.color = textColor
                paint.textSize = 11f
                canvas.drawText("التاريخ", rightX - 8f, y + 17f, paint)
                canvas.drawText("النوع", col1 - 8f, y + 17f, paint)
                canvas.drawText("المدة", col2 - 8f, y + 17f, paint)
                canvas.drawText("الملاحظة", col3 - 8f, y + 17f, paint)
                canvas.drawText("الأجرة", col4 - 8f, y + 17f, paint)
                y += rowH

                for (e in pageEntries) {
                    paint.style = Paint.Style.STROKE
                    paint.color = gridColor
                    canvas.drawRect(leftX, y, rightX, y + rowH, paint)
                    canvas.drawLine(col1, y, col1, y + rowH, paint)
                    canvas.drawLine(col2, y, col2, y + rowH, paint)
                    canvas.drawLine(col3, y, col3, y + rowH, paint)
                    canvas.drawLine(col4, y, col4, y + rowH, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = textColor
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(dfDate.format(Date(e.startTime)), rightX - 8f, y + 17f, paint)

                    val badgeBg = when (e.type) {
                        EntryType.HOURLY -> Color.parseColor("#059669")
                        EntryType.REMOTE -> Color.parseColor("#2563EB")
                        EntryType.CREDIT -> Color.parseColor("#DC2626")
                    }
                    val badgeTxt = when (e.type) {
                        EntryType.HOURLY -> "ساعات"
                        EntryType.REMOTE -> "عن بعد"
                        EntryType.CREDIT -> "كريدي"
                    }
                    paint.color = badgeBg
                    canvas.drawRoundRect(RectF(col2 + 8f, y + 4f, col1 - 8f, y + rowH - 4f), 6f, 6f, paint)
                    paint.color = Color.WHITE
                    paint.textSize = 10f
                    canvas.drawText(badgeTxt, col1 - 14f, y + 17f, paint)

                    paint.color = textColor
                    paint.textSize = 10f
                    val durStr = if (e.type == EntryType.HOURLY && e.endTime != null) {
                        val mins = ((e.endTime - e.startTime) / 60000L) - e.breakMinutes
                        ElamalUtils.formatDurationArabic(mins)
                    } else "-"
                    canvas.drawText(durStr, col2 - 8f, y + 17f, paint)

                    canvas.drawText(e.note.take(22), col3 - 8f, y + 17f, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = if (e.calculatedAmount < 0) Color.parseColor("#EF4444") else textColor
                    canvas.drawText(ElamalUtils.formatMoney(e.calculatedAmount), col4 - 8f, y + 17f, paint)

                    y += rowH
                }

                if (pageIndex == chunks.lastIndex) {
                    paint.style = Paint.Style.FILL
                    paint.color = headerBg
                    canvas.drawRect(leftX, y, rightX, y + rowH, paint)
                    paint.style = Paint.Style.STROKE
                    paint.color = gridColor
                    canvas.drawRect(leftX, y, rightX, y + rowH, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = textColor
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 11f
                    canvas.drawText("المجموع النهائي", rightX - 8f, y + 17f, paint)
                    canvas.drawText(ElamalUtils.formatDurationArabic(totalHourlyMinutes), col2 - 8f, y + 17f, paint)
                    canvas.drawText(ElamalUtils.formatMoney(netTotal), col4 - 8f, y + 17f, paint)
                }

                paint.color = subTextColor
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(
                    "تطبيق «العمل» (com.nanomks.elamal) - تاريخ الإنشاء: ${dfDateTime.format(Date())}",
                    rightX,
                    pageHeight - 28f,
                    paint
                )

                doc.finishPage(page)
            }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val pdfFile = File(reportsDir, "Elamal_Report_${System.currentTimeMillis()}.pdf")
            FileOutputStream(pdfFile).use { doc.writeTo(it) }
            doc.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("PDF Report", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ أثناء إنشاء PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportAndShareCsv(context: Context, entries: List<WorkEntry>) {
        try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val csvFile = File(reportsDir, "Elamal_Export_${System.currentTimeMillis()}.csv")
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            FileOutputStream(csvFile).use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                val sb = StringBuilder()
                sb.append("المعرف,النوع,البداية,النهاية,الاستراحة(د),المدة,الأجرة(DH),الملاحظة\n")
                for (e in entries) {
                    val mins = if (e.endTime != null) ((e.endTime - e.startTime) / 60000L) - e.breakMinutes else 0L
                    val dur = if (e.type == EntryType.HOURLY) ElamalUtils.formatDurationArabic(mins) else "-"
                    val endStr = e.endTime?.let { df.format(Date(it)) } ?: ""
                    val cleanNote = e.note.replace(",", " ")
                    sb.append("${e.id},${e.type},${df.format(Date(e.startTime))},$endStr,${e.breakMinutes},$dur,${e.calculatedAmount},$cleanNote\n")
                }
                fos.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("CSV Export", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "تصدير CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ أثناء تصدير CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
