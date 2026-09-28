package com.customercounter.app.util

import android.content.Context
import android.net.Uri
import com.customercounter.app.data.Customer
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportManager(private val context: Context) {
    fun writeCsv(uri: Uri, customers: List<Customer>) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            BufferedWriter(OutputStreamWriter(output, StandardCharsets.UTF_8)).use { writer ->
                writer.write("Customer Number,Phone Number,First Call,Last Call,Total Calls")
                writer.newLine()
                customers.forEach { c ->
                    writer.write(listOf(c.customerNumber, c.displayPhone, Formatters.dateTime(c.firstCallTimestamp), Formatters.dateTime(c.lastCallTimestamp), c.totalCalls).joinToString(",") { csv(it.toString()) })
                    writer.newLine()
                }
            }
        } ?: error("Unable to open export destination")
    }

    fun writeXlsx(uri: Uri, customers: List<Customer>) {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                add(zip, "[Content_Types].xml", contentTypes())
                add(zip, "_rels/.rels", rels())
                add(zip, "xl/workbook.xml", workbook())
                add(zip, "xl/_rels/workbook.xml.rels", workbookRels())
                add(zip, "xl/worksheets/sheet1.xml", sheet(customers))
            }
        } ?: error("Unable to open export destination")
    }

    private fun csv(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
    private fun add(zip: ZipOutputStream, path: String, data: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(data.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
    private fun cell(row: Int, col: String, value: String): String = "<c r=\"$col$row\" t=\"inlineStr\"><is><t>${xml(value)}</t></is></c>"
    private fun sheet(customers: List<Customer>): String {
        val rows = StringBuilder()
        val headers = listOf("Customer Number", "Phone Number", "First Call", "Last Call", "Total Calls")
        rows.append("<row r=\"1\">")
        headers.forEachIndexed { i, h -> rows.append(cell(1, ('A'.code + i).toChar().toString(), h)) }
        rows.append("</row>")
        customers.forEachIndexed { index, c ->
            val r = index + 2
            rows.append("<row r=\"$r\">")
            listOf(c.customerNumber.toString(), c.displayPhone, Formatters.dateTime(c.firstCallTimestamp), Formatters.dateTime(c.lastCallTimestamp), c.totalCalls.toString()).forEachIndexed { i, v -> rows.append(cell(r, ('A'.code + i).toChar().toString(), v)) }
            rows.append("</row>")
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>$rows</sheetData></worksheet>"
    }
    private fun contentTypes() = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>"
    private fun rels() = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"
    private fun workbook() = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Customers\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
    private fun workbookRels() = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"
}
