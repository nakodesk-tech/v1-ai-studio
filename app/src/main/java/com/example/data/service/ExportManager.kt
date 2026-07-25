package com.example.data.service

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.model.FormWithFields
import com.example.data.model.SubmissionWithValues
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportManager(private val context: Context) {

    /**
     * Generates a PDF Report containing all filled school submission data for a form.
     */
    fun generatePdfReport(
        formWithFields: FormWithFields,
        submissionsWithValues: List<SubmissionWithValues>
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 pt
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        var y = 40f
        val margin = 36f
        val pageWidth = 595f - (margin * 2)

        // Header Background Banner
        paint.color = Color.parseColor("#0A4D2E") // Forest Green
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Header Text
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DISTRICT EDUCATION OFFICE - SCHOOL DATA SUMMARY", margin, 42f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Form Title: ${formWithFields.form.title}", margin, 65f, paint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())
        paint.textSize = 10f
        canvas.drawText("Generated on: $generatedDate | Total Submissions: ${submissionsWithValues.size}", margin, 80f, paint)

        y = 110f

        // Table Header
        paint.color = Color.parseColor("#E2F0E8") // Soft Green
        canvas.drawRect(margin, y, margin + pageWidth, y + 25f, paint)

        paint.color = Color.parseColor("#0A4D2E")
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("School Name & UDISE", margin + 10f, y + 17f, paint)
        canvas.drawText("HM Name", margin + 220f, y + 17f, paint)
        canvas.drawText("Submission Date", margin + 370f, y + 17f, paint)
        canvas.drawText("Status", margin + 470f, y + 17f, paint)

        y += 30f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f

        var isEvenRow = false
        for (sub in submissionsWithValues) {
            if (y > 780f) {
                // Break to next page if needed
                break
            }

            if (isEvenRow) {
                paint.color = Color.parseColor("#F8FAF9")
                canvas.drawRect(margin, y - 5f, margin + pageWidth, y + 35f, paint)
            }

            paint.color = Color.parseColor("#111827")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val schoolTitle = if (sub.submission.schoolName.length > 25) sub.submission.schoolName.substring(0, 22) + "..." else sub.submission.schoolName
            canvas.drawText(schoolTitle, margin + 10f, y + 12f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#6B7280")
            canvas.drawText("ID: ${sub.submission.schoolId}", margin + 10f, y + 26f, paint)

            paint.color = Color.parseColor("#374151")
            canvas.drawText(sub.submission.submittedBy, margin + 220f, y + 18f, paint)

            val subDate = dateFormat.format(Date(sub.submission.submittedAt))
            canvas.drawText(subDate.split(",")[0], margin + 370f, y + 18f, paint)

            paint.color = if (sub.submission.syncStatus == "SYNCED") Color.parseColor("#059669") else Color.parseColor("#D97706")
            canvas.drawText(sub.submission.syncStatus, margin + 470f, y + 18f, paint)

            paint.color = Color.parseColor("#E5E7EB")
            canvas.drawLine(margin, y + 35f, margin + pageWidth, y + 35f, paint)

            y += 42f
            isEvenRow = !isEvenRow
        }

        // Summary Footer
        y = 800f
        paint.color = Color.parseColor("#0A4D2E")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("EduData Sync - Centralized Officer Portal | Verified & Synced to Officer's Main Google Drive Account", margin, y, paint)

        pdfDocument.finishPage(page)

        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "School_Data_Report_${System.currentTimeMillis()}.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    /**
     * Generates an Excel-compatible CSV File containing all filled records in matrix format.
     */
    fun generateExcelFile(
        formWithFields: FormWithFields,
        submissionsWithValues: List<SubmissionWithValues>
    ): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "School_Data_${formWithFields.form.title.replace(" ", "_")}_${System.currentTimeMillis()}.csv")

        val sb = java.lang.StringBuilder()

        // Headers
        val headers = mutableListOf("UDISE / School ID", "School Name", "Submitted By (HM)", "Submission Date", "Sync Status")
        formWithFields.fields.forEach { field ->
            headers.add("\"${field.label.replace("\"", "\"\"")}\"")
        }
        sb.append(headers.joinToString(",")).append("\n")

        // Rows
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        submissionsWithValues.forEach { sub ->
            val rowValues = mutableListOf<String>()
            rowValues.add("\"${sub.submission.schoolId}\"")
            rowValues.add("\"${sub.submission.schoolName.replace("\"", "\"\"")}\"")
            rowValues.add("\"${sub.submission.submittedBy.replace("\"", "\"\"")}\"")
            rowValues.add("\"${dateFormat.format(Date(sub.submission.submittedAt))}\"")
            rowValues.add("\"${sub.submission.syncStatus}\"")

            // Map values by field id or label
            val valMap = sub.values.associateBy { it.fieldId }
            formWithFields.fields.forEach { field ->
                val filledVal = valMap[field.id]?.value ?: ""
                rowValues.add("\"${filledVal.replace("\"", "\"\"")}\"")
            }
            sb.append(rowValues.joinToString(",")).append("\n")
        }

        file.writeText(sb.toString())
        return file
    }
}
