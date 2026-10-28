package com.pandakode.bambumqtt.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BomComponentRow(
    val designator: String,
    val partName: String,
    val packageFootprint: String,
    val pinConnection: String,
    val estPriceIdr: Int
)

object BlueprintExportEngine {

    fun getDefaultBomList(): List<BomComponentRow> = listOf(
        BomComponentRow("U1", "ESP32-WROOM-32 DevKit V1", "MOD-30PIN-SMD", "3V3, GND, D2, D4, D18, D21, D22", 65000),
        BomComponentRow("R1", "Resistor Metal Film 220 Ω 1/4W", "AXIAL-0207", "P1 -> GPIO2 (D2) | P2 -> LED1_A+", 500),
        BomComponentRow("LED1", "LED Indikator 5mm High-Bright Red", "THT-5MM", "A+ -> R1_P2 | K- -> GND", 1000),
        BomComponentRow("U2", "Sensor Suhu & Lembab DHT22 / AM2302", "SIP-4-2.54MM", "VCC->3V3 | DATA->D4 | GND->GND", 45000),
        BomComponentRow("DISP1", "OLED Graphic Display 0.96\" SSD1306", "I2C-4PIN-MODULE", "VCC->3V3 | GND->GND | SCL->D22 | SDA->D21", 38000),
        BomComponentRow("SRV1", "Micro Servo Motor TowerPro SG90", "PWM-3PIN-HEADER", "VCC->5V | GND->GND | SIG->D18", 22000)
    )

    // 1. GENERATE DOKUMEN PDF BLUEPRINT SKEMA & BOM (Downloads/BambuMQTT/*.pdf)
    fun exportCircuitBlueprintPdf(
        context: Context,
        projectName: String,
        boardModel: String,
        bomItems: List<BomComponentRow>
    ): String {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standar A4 (595 x 842 pt)
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 54, 34)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val headerFillPaint = Paint().apply {
            color = Color.rgb(10, 54, 34)
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 30, 25)
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 54, 34)
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        // Bingkai Blueprint Teknik A4
        canvas.drawRect(24f, 24f, 571f, 818f, borderPaint)
        canvas.drawRect(24f, 24f, 571f, 82f, headerFillPaint)

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("PANDAKODE ENGINEERING BLUEPRINT & BOM SCHEMATIC", 38f, 48f, titlePaint)
        titlePaint.textSize = 10f
        titlePaint.color = Color.rgb(255, 183, 3)
        canvas.drawText("Project: $projectName | Target MCU: $boardModel | Date: $dateStr WIB", 38f, 68f, titlePaint)

        // Gambar Kotak Blok Diagram Skema di Bagian Atas
        canvas.drawText("1. TOPOLOGICAL BLOCK & PINOUT DIAGRAM", 38f, 108f, boldPaint)
        val schPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(242, 245, 243)
            style = Paint.Style.FILL
        }
        canvas.drawRect(38f, 118f, 555f, 275f, schPaint)
        canvas.drawRect(38f, 118f, 555f, 275f, borderPaint)

        // Gambar Chip MCU di Tengah Diagram PDF
        canvas.drawRect(65f, 145f, 215f, 250f, headerFillPaint)
        titlePaint.color = Color.WHITE
        canvas.drawText(boardModel.take(16), 80f, 185f, titlePaint)
        canvas.drawText("3V3 | 5V | GND | GPIO", 78f, 210f, titlePaint)

        // Gambar Jalur Netlist ke Periferal di PDF
        val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 133, 0)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        bomItems.drop(1).take(4).forEachIndexed { idx, item ->
            val py = 145f + idx * 28f
            canvas.drawLine(215f, py + 8f, 310f, py + 8f, wirePaint)
            canvas.drawRect(310f, py - 4f, 535f, py + 18f, borderPaint)
            canvas.drawText("${item.designator}: ${item.partName.take(26)}", 318f, py + 10f, textPaint)
        }

        // Tabel 2: Bill of Materials (BOM) & Netlist
        canvas.drawText("2. BILL OF MATERIALS (BOM) & PIN NETLIST TABLE", 38f, 305f, boldPaint)
        var currentY = 325f
        canvas.drawRect(38f, currentY, 555f, currentY + 22f, headerFillPaint)
        titlePaint.textSize = 9.5f
        canvas.drawText("REF   COMPONENT DESCRIPTION              FOOTPRINT         NET / PIN CONNECTION", 45f, currentY + 15f, titlePaint)
        currentY += 28f

        var totalCost = 0
        bomItems.forEach { row ->
            totalCost += row.estPriceIdr
            val line = String.format(
                Locale.US,
                "%-5s %-34s %-17s %s",
                row.designator,
                row.partName.take(32),
                row.packageFootprint.take(15),
                row.pinConnection.take(28)
            )
            canvas.drawText(line, 44f, currentY + 10f, textPaint)
            canvas.drawLine(38f, currentY + 16f, 555f, currentY + 16f, borderPaint)
            currentY += 24f
        }

        currentY += 15f
        canvas.drawText("Estimasi Total Biaya Komponen (BOM): Rp ${String.format("%,d", totalCost)}", 38f, currentY, boldPaint)

        // Title Block Stempel Insinyur di Kanan Bawah PDF
        canvas.drawRect(320f, 730f, 555f, 805f, borderPaint)
        canvas.drawText("APPROVED BY: PANDAKODE LABS", 332f, 750f, boldPaint)
        canvas.drawText("Author : Muhammad Saputra (breto1)", 332f, 768f, textPaint)
        canvas.drawText("Origin : Pekanbaru, Riau, Indonesia", 332f, 784f, textPaint)
        canvas.drawText("Suite  : BambuMQTT IoT Studio v2.6", 332f, 798f, textPaint)

        pdfDoc.finishPage(page)

        val fileName = "BambuMQTT_Blueprint_${System.currentTimeMillis() / 1000}.pdf"
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BambuMQTT")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                val out: OutputStream? = resolver.openOutputStream(uri)
                out?.use { pdfDoc.writeTo(it) }
                pdfDoc.close()
                Toast.makeText(context, "✅ PDF Blueprint Tersimpan: Downloads/BambuMQTT/$fileName", Toast.LENGTH_LONG).show()
                "Downloads/BambuMQTT/$fileName"
            } else {
                pdfDoc.close()
                "Gagal membuat URI MediaStore"
            }
        } catch (e: Exception) {
            pdfDoc.close()
            "Error PDF: ${e.message}"
        }
    }

    // 2. GENERATE FILE GERBER RS-274X UNTUK CETAK PCB (Downloads/BambuMQTT/*.gbr)
    fun exportGerberTopCopper(context: Context, projectName: String): String {
        val gerberContent = """
            G04 ========================================================== *
            G04 BAMBUMQTT IOT STUDIO - RS-274X GERBER TOP COPPER LAYER     *
            G04 Designer: Muhammad Saputra (PandaKode - Pekanbaru, Riau)   *
            G04 Project : $projectName                                     *
            G04 ========================================================== *
            %FSLAX24Y24*%
            %MOMM*%
            %ADD10C,0.8000*%
            %ADD11C,1.6000*%
            %LPD*%
            G01*
            G04 --- Pad Header MCU ESP32 --- *
            D11*
            X100000Y100000D03*
            X100000Y125400D03*
            X100000Y150800D03*
            X100000Y176200D03*
            G04 --- Jalur Copper Trace ke Sensor & Aktuator --- *
            D10*
            X100000Y100000D02*
            X250000Y100000D01*
            X100000Y125400D02*
            X250000Y125400D01*
            X100000Y150800D02*
            X250000Y150800D01*
            M02*
        """.trimIndent()

        val fileName = "BambuPCB_TopCopper_${System.currentTimeMillis() / 1000}.gbr"
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BambuMQTT")
                }
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(gerberContent.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "✅ File Gerber PCB Tersimpan: Downloads/BambuMQTT/$fileName", Toast.LENGTH_LONG).show()
                gerberContent
            } else gerberContent
        } catch (_: Exception) {
            gerberContent
        }
    }
}