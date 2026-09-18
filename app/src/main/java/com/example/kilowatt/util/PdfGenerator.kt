package com.example.kilowatt.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.data.FacturaGeneral
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val settingsManager = SettingsManager(context)

    fun generarComprobanteIndividual(detalle: DetalleCobro): File? {
        // Formato: 148 x 105 mm (aprox) -> A6 horizontal o similar
        // En puntos (1/72 inch): 148mm = 419 pts, 105mm = 297 pts
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(420, 300, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Dibujar Fondo Blanco
        canvas.drawColor(Color.WHITE)

        // Encabezado
        paint.color = Color.parseColor("#673AB7")
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("⚡ KiloWatt - Comprobante", 20f, 35f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val fechaEmision = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Periodo: ${detalle.mesPeriodo} | Emisión: $fechaEmision", 20f, 55f, paint)

        // Línea divisoria
        paint.strokeWidth = 1f
        canvas.drawLine(20f, 65f, 400f, 65f, paint)

        // Datos del Inquilino
        paint.textSize = 12f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Inquilino: ${detalle.nombreInquilino}", 20f, 85f, paint)
        paint.isFakeBoldText = false
        canvas.drawText("Espacio: ${detalle.nombreEspacio}", 20f, 100f, paint)

        // Desglose de Consumo (Tabla simple)
        paint.textSize = 10f
        var yPos = 125f
        canvas.drawText("Lectura Anterior:", 20f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.lecturaAnterior)} kWh", 300f, yPos, paint)
        yPos += 15f
        canvas.drawText("Lectura Actual:", 20f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.lecturaActual)} kWh", 300f, yPos, paint)
        yPos += 15f
        paint.isFakeBoldText = true
        canvas.drawText("Consumo del Mes:", 20f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.consumoKwh)} kWh", 300f, yPos, paint)
        paint.isFakeBoldText = false
        yPos += 20f

        // Costos
        canvas.drawText("Costo x kWh (S/ ${"%.2f".format(detalle.precioKwh)}):", 20f, yPos, paint)
        canvas.drawText("S/ ${"%.2f".format(detalle.consumoKwh * detalle.precioKwh)}", 300f, yPos, paint)
        yPos += 15f
        if (detalle.montoAreaComunSoles > 0) {
            canvas.drawText("Cuota Área Común:", 20f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(detalle.montoAreaComunSoles)}", 300f, yPos, paint)
            yPos += 15f
        }

        // Total
        paint.strokeWidth = 2f
        canvas.drawLine(20f, yPos - 5, 400f, yPos - 5, paint)
        paint.textSize = 16f
        paint.color = Color.parseColor("#2E7D32")
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL A PAGAR:", 20f, yPos + 15, paint)
        canvas.drawText("S/ ${"%.2f".format(detalle.montoPagarSoles)}", 300f, yPos + 15, paint)

        // Información de Pago
        paint.textSize = 9f
        paint.color = Color.DKGRAY
        paint.isFakeBoldText = false
        yPos += 45f
        canvas.drawText("Pagar vía Yape/Plin al: ${settingsManager.numeroPago}", 20f, yPos, paint)
        canvas.drawText("Titular: ${settingsManager.nombrePropietario}", 20f, yPos + 12, paint)
        paint.color = Color.RED
        canvas.drawText("Límite de pago: Día ${settingsManager.diaLimitePago} de cada mes.", 20f, yPos + 24, paint)

        document.finishPage(page)

        val fileName = "Comprobante_${detalle.nombreInquilino.replace(" ", "_")}_${detalle.mesPeriodo}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            document.writeTo(FileOutputStream(file))
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }

    fun generarReporteGeneral(mes: String, factura: FacturaGeneral, detalles: List<DetalleCobro>): File? {
        val document = PdfDocument()
        // A5 Vertical: 148mm x 210mm -> 420 x 595 pts
        val pageInfo = PdfDocument.PageInfo.Builder(420, 600, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        canvas.drawColor(Color.WHITE)

        // Encabezado Reporte General
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("REPORTE GENERAL DE COBROS", 20f, 40f, paint)
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Periodo: $mes", 20f, 60f, paint)

        // Resumen Recibo
        paint.color = Color.LTGRAY
        canvas.drawRect(20f, 75f, 400f, 130f, paint)
        paint.color = Color.BLACK
        paint.textSize = 11f
        canvas.drawText("Recibo Empresa Eléctrica:", 30f, 95f, paint)
        canvas.drawText("S/ ${"%.2f".format(factura.montoTotalSoles)}", 300f, 95f, paint)
        canvas.drawText("Total kWh Submedidores:", 30f, 115f, paint)
        val totalKwh = detalles.sumOf { it.consumoKwh }
        canvas.drawText("${"%.1f".format(totalKwh)} kWh", 300f, 115f, paint)

        // Gráfico de Torta (Simple)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Distribución de Consumo", 20f, 160f, paint)
        
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)
        var startAngle = 0f
        val rectF = RectF(110f, 180f, 310f, 380f)
        
        if (totalKwh > 0) {
            detalles.forEachIndexed { index, det ->
                val sweep = (det.consumoKwh / totalKwh * 360).toFloat()
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.FILL
                canvas.drawArc(rectF, startAngle, sweep, true, paint)
                
                // Leyenda pequeña
                paint.textSize = 8f
                canvas.drawRect(330f, 180f + (index * 15), 340f, 190f + (index * 15), paint)
                paint.color = Color.BLACK
                canvas.drawText("${det.nombreEspacio}: ${det.consumoKwh.toInt()}kWh", 345f, 190f + (index * 15), paint)
                
                startAngle += sweep
            }
        }

        // Tabla Consolidada
        paint.style = Paint.Style.FILL
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Detalle Consolidado", 20f, 420f, paint)
        
        paint.textSize = 9f
        paint.isFakeBoldText = false
        var yPos = 445f
        // Cabecera Tabla
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(20f, yPos - 12, 400f, yPos + 3, paint)
        paint.color = Color.BLACK
        canvas.drawText("Inquilino / Espacio", 25f, yPos, paint)
        canvas.drawText("kWh", 220f, yPos, paint)
        canvas.drawText("Área C.", 280f, yPos, paint)
        canvas.drawText("Total", 350f, yPos, paint)
        yPos += 20f

        detalles.forEach { det ->
            canvas.drawText(det.nombreInquilino.take(25), 25f, yPos, paint)
            canvas.drawText("${"%.1f".format(det.consumoKwh)}", 220f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(det.montoAreaComunSoles)}", 280f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(det.montoPagarSoles)}", 350f, yPos, paint)
            yPos += 15f
        }

        document.finishPage(page)

        val fileName = "Reporte_General_${mes.replace(" ", "_")}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            document.writeTo(FileOutputStream(file))
            document.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }
}
