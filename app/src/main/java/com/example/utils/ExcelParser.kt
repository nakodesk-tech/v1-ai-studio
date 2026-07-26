package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

data class SheetInfo(
    val name: String,
    val sheetPath: String
)

object ExcelParser {

    private const val TAG = "ExcelParser"

    fun getFileName(context: Context, uri: Uri): String {
        var name = "Imported_Sheet.xlsx"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving display name from Uri", e)
        }
        return name
    }

    fun listSheets(context: Context, uri: Uri): List<SheetInfo> {
        val sheets = mutableListOf<SheetInfo>()
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            var workbookXmlBytes: ByteArray? = null

            while (entry != null) {
                if (entry.name == "xl/workbook.xml") {
                    workbookXmlBytes = zip.readBytes()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            if (workbookXmlBytes == null) {
                Log.w(TAG, "xl/workbook.xml not found in zip archive")
                return emptyList()
            }

            val parser = Xml.newPullParser()
            parser.setInput(workbookXmlBytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var sheetIndex = 1

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet$sheetIndex"
                    // Standard location in POI / Excel output
                    val sheetPath = "xl/worksheets/sheet$sheetIndex.xml"
                    sheets.add(SheetInfo(name = name, sheetPath = sheetPath))
                    sheetIndex++
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing sheets from Excel file", e)
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }

        return sheets
    }

    fun parseHeaders(context: Context, uri: Uri, targetSheetPath: String): List<String> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
        var sharedStringsBytes: ByteArray? = null
        var targetSheetBytes: ByteArray? = null

        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry

            while (entry != null) {
                when {
                    entry.name == "xl/sharedStrings.xml" -> {
                        sharedStringsBytes = zip.readBytes()
                    }
                    entry.name == targetSheetPath || entry.name.endsWith(targetSheetPath.substringAfterLast("/")) -> {
                        targetSheetBytes = zip.readBytes()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            zip.close()

            if (targetSheetBytes == null) {
                Log.w(TAG, "Target worksheet XML not found: $targetSheetPath")
                return emptyList()
            }

            val sharedStrings = parseSharedStrings(sharedStringsBytes)
            return parseFirstNonEmptyRow(targetSheetBytes, sharedStrings)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing headers from sheet: $targetSheetPath", e)
            return emptyList()
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val sharedStrings = mutableListOf<String>()

        try {
            val parser = Xml.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var currentText = StringBuilder()
            var insideSi = false
            var insideT = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "si") {
                            insideSi = true
                            currentText = StringBuilder()
                        } else if (insideSi && parser.name == "t") {
                            insideT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideSi && insideT) {
                            currentText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            insideT = false
                        } else if (parser.name == "si") {
                            insideSi = false
                            sharedStrings.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shared strings", e)
        }

        return sharedStrings
    }

    private fun parseFirstNonEmptyRow(sheetBytes: ByteArray, sharedStrings: List<String>): List<String> {
        val foundHeaders = mutableListOf<String>()

        try {
            val parser = Xml.newPullParser()
            parser.setInput(sheetBytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var insideRow = false
            var insideCell = false
            var currentCellType = ""
            var cellText = StringBuilder()

            val currentRowCells = mutableListOf<String>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "row") {
                            insideRow = true
                            currentRowCells.clear()
                        } else if (insideRow && parser.name == "c") {
                            insideCell = true
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellText = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideCell) {
                            cellText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "c") {
                            insideCell = false
                            val raw = cellText.toString().trim()
                            val headerVal = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        sharedStrings[idx]
                                    } else ""
                                }
                                "inlineStr" -> raw
                                else -> raw
                            }.trim()

                            if (headerVal.isNotBlank()) {
                                currentRowCells.add(headerVal)
                            }
                        } else if (parser.name == "row") {
                            insideRow = false
                            if (currentRowCells.isNotEmpty()) {
                                foundHeaders.addAll(currentRowCells)
                                break // Found first non-empty header row!
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing first non-empty row", e)
        }

        return foundHeaders
    }
}
