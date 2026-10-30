package com.example.kilowatt.util

import android.content.Context
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

class PdfGenerator(
    private val context: Context,
    private val settingsManager: ISettingsManager = SettingsManager(context),
) {

    suspend fun generarComprobanteIndividual(detalle: DetalleCobro): File? = withContext(Dispatchers.IO) {
        // Formato: A6 Horizontal (420 x 300 pts)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(420, 300, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Fondo Blanco
        canvas.drawColor(Color.WHITE)

        // 1. Cabecera Estilizada del Propietario / Inmueble
        paint.color = Color.parseColor("#673AB7") // Púrpura KiloWatt
        paint.style = Paint.Style.FILL
        val headerCard = RectF(15f, 15f, 405f, 65f)
        canvas.drawRoundRect(headerCard, 8f, 8f, paint)

        val nombrePropietario = settingsManager.nombrePropietario.ifBlank { "Administración KiloWatt" }
        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("⚡ $nombrePropietario", 25f, 37f, paint)

        paint.textSize = 9f
        paint.isFakeBoldText = false
        val fechaEmision = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Periodo: ${detalle.mesPeriodo} | Emisión: $fechaEmision", 25f, 52f, paint)

        // Sello Visual de Estado de Pago en la Cabecera (PAGADO / VENCIDO / PENDIENTE)
        val estadoUpper = detalle.estadoPago.uppercase(Locale.getDefault())
        val (colorSello, textoSello) = when (estadoUpper) {
            "PAGADO" -> Pair(Color.parseColor("#2E7D32"), "PAGADO ✅")
            "VENCIDO" -> Pair(Color.parseColor("#C62828"), "VENCIDO 🔴")
            else -> Pair(Color.parseColor("#EF6C00"), "PENDIENTE 🟡")
        }

        val badgeRect = RectF(295f, 25f, 395f, 55f)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)

        paint.color = colorSello
        paint.textSize = 9f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(textoSello, badgeRect.centerX(), badgeRect.centerY() + 3f, paint)
        paint.textAlign = Paint.Align.LEFT

        // 2. Datos del Inquilino y Espacio (Tarjeta Gris Clara)
        val tenantCard = RectF(15f, 72f, 405f, 115f)
        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRoundRect(tenantCard, 6f, 6f, paint)

        paint.color = Color.parseColor("#212121")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Inquilino: ${detalle.nombreInquilino}", 25f, 89f, paint)
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#616161")
        canvas.drawText("Espacio / Medidor: ${detalle.nombreEspacio}", 25f, 105f, paint)

        // 3. Desglose de Consumo
        paint.color = Color.parseColor("#424242")
        paint.textSize = 9.5f
        var yPos = 132f

        canvas.drawText("Lectura Anterior:", 25f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.lecturaAnterior)} kWh", 310f, yPos, paint)
        yPos += 14f

        canvas.drawText("Lectura Actual:", 25f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.lecturaActual)} kWh", 310f, yPos, paint)
        yPos += 14f

        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#212121")
        canvas.drawText("Consumo del Mes:", 25f, yPos, paint)
        canvas.drawText("${"%.1f".format(detalle.consumoKwh)} kWh", 310f, yPos, paint)
        paint.isFakeBoldText = false
        yPos += 16f

        // Costos
        paint.color = Color.parseColor("#616161")
        canvas.drawText("Costo x kWh (S/ ${"%.2f".format(detalle.precioKwh)}):", 25f, yPos, paint)
        canvas.drawText("S/ ${"%.2f".format(detalle.consumoKwh * detalle.precioKwh)}", 310f, yPos, paint)
        yPos += 14f

        if (detalle.montoAreaComunSoles > 0) {
            val etiquetaComun = if (settingsManager.tipoProrrateoComun == "WEIGHTED_PERSONS") {
                "Cuota Área Común (${detalle.numeroPersonas} pers.):"
            } else {
                "Cuota Área Común:"
            }
            canvas.drawText(etiquetaComun, 25f, yPos, paint)
            canvas.drawText("S/ ${"%.2f".format(detalle.montoAreaComunSoles)}", 310f, yPos, paint)
            yPos += 14f
        }

        // 4. Línea Divisoria y Total a Pagar
        paint.strokeWidth = 1.2f
        paint.color = Color.parseColor("#BDBDBD")
        canvas.drawLine(15f, yPos, 405f, yPos, paint)

        yPos += 18f
        paint.textSize = 14f
        paint.color = Color.parseColor("#2E7D32")
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL A PAGAR:", 25f, yPos, paint)
        canvas.drawText("S/ ${"%.2f".format(detalle.montoPagarSoles)}", 310f, yPos, paint)

        // 5. Pie de Página con Información de Pago Configurable
        val footerCard = RectF(15f, yPos + 10f, 405f, 288f)
        paint.color = Color.parseColor("#FAFAFA")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(footerCard, 6f, 6f, paint)

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#424242")
        paint.isFakeBoldText = false
        var yFooter = yPos + 22f

        val numPago = settingsManager.numeroPago.ifBlank { "-" }
        canvas.drawText("💳 Medio de Pago (Yape / Plin / Transf): $numPago", 22f, yFooter, paint)
        yFooter += 11f

        paint.color = Color.parseColor("#C62828")
        paint.isFakeBoldText = true
        canvas.drawText("⏰ Límite de pago: Día ${settingsManager.diaLimitePago} de cada mes.", 22f, yFooter, paint)

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
        // A5 Vertical: 420 x 600 pts
        val pageInfo = PdfDocument.PageInfo.Builder(420, 600, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.WHITE)

        // 1. Cabecera Personalizada
        paint.color = Color.parseColor("#673AB7")
        val headerCard = RectF(15f, 15f, 405f, 65f)
        canvas.drawRoundRect(headerCard, 8f, 8f, paint)

        val nombrePropietario = settingsManager.nombrePropietario.ifBlank { "Administración KiloWatt" }
        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText("⚡ REPORTE GENERAL DE COBROS", 25f, 38f, paint)

        paint.textSize = 9.5f
        paint.isFakeBoldText = false
        canvas.drawText("Propietario: $nombrePropietario | Periodo: $mes", 25f, 53f, paint)

        // 2. Resumen Recibo Matriz
        paint.color = Color.parseColor("#F5F5F5")
        val resumenCard = RectF(15f, 72f, 405f, 125f)
        canvas.drawRoundRect(resumenCard, 6f, 6f, paint)

        paint.color = Color.parseColor("#212121")
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("Resumen Factura General:", 25f, 89f, paint)
        paint.isFakeBoldText = false
        canvas.drawText("Monto Recibo Matriz: S/ ${"%.2f".format(factura.montoTotalSoles)}", 25f, 104f, paint)
        val totalKwh = detalles.sumOf { it.consumoKwh }
        canvas.drawText("Consumo Total Submedidores: ${"%.1f".format(totalKwh)} kWh", 25f, 118f, paint)

        // 3. Distribución Visual
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#212121")
        canvas.drawText("Distribución de Consumo", 15f, 145f, paint)

        val colors = intArrayOf(
            Color.parseColor("#FF5252"), Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"), Color.parseColor("#FFC107"),
            Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"),
            Color.parseColor("#FF9800"), Color.parseColor("#795548"),
            Color.parseColor("#607D8B"), Color.parseColor("#E91E63"),
        )
        var startAngle = 0f
        val rectF = RectF(25f, 160f, 175f, 310f)

        if (totalKwh > 0) {
            val detallesConConsumo = detalles.filter { it.consumoKwh > 0 }
            val rowHeight = if (detallesConConsumo.size > 6) 18f else 21f

            detallesConConsumo.forEachIndexed { index, det ->
                val sweep = (det.consumoKwh / totalKwh * 360).toFloat()

                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.FILL
                canvas.drawArc(rectF, startAngle, sweep, true, paint)

                val xLegend = 195f
                val yLegend = 168f + (index * rowHeight)

                paint.color = colors[index % colors.size]
                canvas.drawRect(xLegend, yLegend - 6f, xLegend + 8f, yLegend + 2f, paint)

                paint.color = Color.parseColor("#212121")
                paint.textSize = if (detallesConConsumo.size > 6) 7.5f else 8.5f
                paint.isFakeBoldText = true

                val tieneInquilinoDiferente = det.nombreInquilino.isNotBlank() &&
                    !det.nombreInquilino.trim().equals(det.nombreEspacio.trim(), ignoreCase = true)

                val nombreInquilinoTxt = if (tieneInquilinoDiferente) det.nombreInquilino else det.nombreEspacio
                val nombreInquilinoCorto = if (nombreInquilinoTxt.length > 20) nombreInquilinoTxt.take(18) + ".." else nombreInquilinoTxt
                canvas.drawText(nombreInquilinoCorto, xLegend + 12f, yLegend, paint)

                paint.isFakeBoldText = false
                val consumoTexto = "${"%.2f".format(det.consumoKwh)} kWh"
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(consumoTexto, 405f, yLegend, paint)
                paint.textAlign = Paint.Align.LEFT

                startAngle += sweep
            }
        }

        // 4. Tabla Consolidada
        paint.style = Paint.Style.FILL
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#212121")
        canvas.drawText("Detalle Consolidado de Cobros", 15f, 335f, paint)

        paint.textSize = if (detalles.size > 8) 8f else 8.5f
        paint.isFakeBoldText = false
        var yPos = 355f

        // Cabecera Tabla
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(15f, yPos - 12f, 405f, yPos + 3f, paint)
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Inquilino / Espacio", 20f, yPos, paint)
        canvas.drawText("kWh", 240f, yPos, paint)
        canvas.drawText("Área C.", 290f, yPos, paint)
        canvas.drawText("Total", 350f, yPos, paint)
        paint.isFakeBoldText = false
        yPos += 16f

        detalles.forEach { det ->
            val tieneInquilinoDiferente = det.nombreInquilino.isNotBlank() &&
                !det.nombreInquilino.trim().equals(det.nombreEspacio.trim(), ignoreCase = true)
            val etiqueta = if (tieneInquilinoDiferente) "${det.nombreInquilino} (${det.nombreEspacio})" else det.nombreEspacio
            val etiquetaTabla = if (etiqueta.length > 34) etiqueta.take(32) + ".." else etiqueta

            canvas.drawText(etiquetaTabla, 20f, yPos, paint)
            canvas.drawText("%.1f".format(det.consumoKwh), 240f, yPos, paint)
            canvas.drawText("S/ %.2f".format(det.montoAreaComunSoles), 290f, yPos, paint)
            canvas.drawText("S/ %.2f".format(det.montoPagarSoles), 350f, yPos, paint)
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
