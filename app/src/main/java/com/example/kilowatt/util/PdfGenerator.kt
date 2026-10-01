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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val settingsManager = SettingsManager(context)

    suspend fun generarComprobanteIndividual(detalle: DetalleCobro): File? = withContext(Dispatchers.IO) {
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
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }

    suspend fun generarReporteGeneral(mes: String, factura: FacturaGeneral, detalles: List<DetalleCobro>): File? = withContext(Dispatchers.IO) {
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
        
        val colors = intArrayOf(
            Color.parseColor("#FF5252"), // Rojo
            Color.parseColor("#4CAF50"), // Verde
            Color.parseColor("#2196F3"), // Azul
            Color.parseColor("#FFC107"), // Ámbar
            Color.parseColor("#9C27B0"), // Púrpura
            Color.parseColor("#00BCD4"), // Cian
            Color.parseColor("#FF9800"), // Naranja
            Color.parseColor("#795548"), // Marrón
            Color.parseColor("#607D8B"), // Gris Azulado
            Color.parseColor("#E91E63")  // Rosa
        )
        var startAngle = 0f
        // Gráfico más pequeño y más a la izquierda para dar máximo espacio a la leyenda
        val rectF = RectF(30f, 180f, 190f, 340f)
        
        if (totalKwh > 0) {
            // Filtrar solo los que tienen consumo para no saturar la leyenda
            val detallesConConsumo = detalles.filter { it.consumoKwh > 0 }
            val rowHeight = if (detallesConConsumo.size > 6) 20f else 23f
            
            detallesConConsumo.forEachIndexed { index, det ->
                val sweep = (det.consumoKwh / totalKwh * 360).toFloat()
                
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.FILL
                canvas.drawArc(rectF, startAngle, sweep, true, paint)
                
                // Leyenda a la derecha del gráfico
                val xLegend = 210f
                val yLegend = 188f + (index * rowHeight)
                
                paint.color = colors[index % colors.size]
                canvas.drawRect(xLegend, yLegend - 7f, xLegend + 8f, yLegend + 1f, paint)
                
                // Línea 1: Nombre del Inquilino (o Espacio)
                paint.color = Color.BLACK
                paint.textSize = if (detallesConConsumo.size > 6) 7.5f else 8.5f
                paint.isFakeBoldText = true
                
                val tieneInquilinoDiferente = det.nombreInquilino.isNotBlank() && 
                        !det.nombreInquilino.trim().equals(det.nombreEspacio.trim(), ignoreCase = true)
                
                val nombreInquilinoTxt = if (tieneInquilinoDiferente) det.nombreInquilino else det.nombreEspacio
                val nombreInquilinoCorto = if (nombreInquilinoTxt.length > 20) nombreInquilinoTxt.take(18) + ".." else nombreInquilinoTxt
                canvas.drawText(nombreInquilinoCorto, xLegend + 12f, yLegend, paint)
                
                // Consumo a la derecha con 2 decimales
                paint.isFakeBoldText = false
                val consumoTexto = "${"%.2f".format(det.consumoKwh)} kWh"
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(consumoTexto, 400f, yLegend, paint)
                paint.textAlign = Paint.Align.LEFT
                
                // Línea 2: Área/Espacio con letra más pequeña (debajo del nombre)
                if (tieneInquilinoDiferente) {
                    paint.textSize = if (detallesConConsumo.size > 6) 6.5f else 7.5f
                    paint.color = Color.DKGRAY
                    val espacioCorto = if (det.nombreEspacio.length > 25) det.nombreEspacio.take(23) + ".." else det.nombreEspacio
                    canvas.drawText(espacioCorto, xLegend + 12f, yLegend + 9f, paint)
                }
                
                startAngle += sweep
            }
        }

        // Tabla Consolidada
        paint.style = Paint.Style.FILL
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Detalle Consolidado", 20f, 380f, paint)
        
        paint.textSize = if (detalles.size > 8) 8f else 9f
        paint.isFakeBoldText = false
        var yPos = 405f
        // Cabecera Tabla
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(20f, yPos - 12, 400f, yPos + 3, paint)
        paint.color = Color.BLACK
        canvas.drawText("Inquilino / Espacio", 25f, yPos, paint)
        canvas.drawText("kWh", 245f, yPos, paint)
        canvas.drawText("Área C.", 295f, yPos, paint)
        canvas.drawText("Total", 355f, yPos, paint)
        yPos += 18f

        detalles.forEach { det ->
            val tieneInquilinoDiferente = det.nombreInquilino.isNotBlank() && 
                    !det.nombreInquilino.trim().equals(det.nombreEspacio.trim(), ignoreCase = true)
            val etiqueta = if (tieneInquilinoDiferente) "${det.nombreInquilino} (${det.nombreEspacio})" else det.nombreEspacio
            val etiquetaTabla = if (etiqueta.length > 36) etiqueta.take(34) + ".." else etiqueta
            
            canvas.drawText(etiquetaTabla, 25f, yPos, paint)
            canvas.drawText("${"%.1f".format(det.consumoKwh)}", 245f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(det.montoAreaComunSoles)}", 295f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(det.montoPagarSoles)}", 355f, yPos, paint)
            yPos += if (detalles.size > 8) 12f else 15f
        }

        document.finishPage(page)

        val fileName = "Reporte_General_${mes.replace(" ", "_")}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            document.close()
        }
    }
}
