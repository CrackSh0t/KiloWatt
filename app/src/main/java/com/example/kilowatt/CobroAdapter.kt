package com.example.kilowatt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.databinding.ItemCobroBinding
import com.example.kilowatt.util.PdfGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class CobroAdapter(
    private val onWhatsappClick: ((DetalleCobro) -> Unit)? = null,
    private val onPdfClick: ((DetalleCobro) -> Unit)? = null
) : ListAdapter<DetalleCobro, CobroAdapter.ViewHolder>(CobroDiffCallback) {

    class ViewHolder(val binding: ItemCobroBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCobroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.binding.tvNombreInquilino.text = item.nombreInquilino.ifEmpty { "Inquilino / Espacio" }
        holder.binding.tvEspacio.text = item.nombreEspacio
        holder.binding.tvLecturaAnterior.text = "%.1f kWh".format(item.lecturaAnterior)
        holder.binding.tvLecturaActual.text = "%.1f kWh".format(item.lecturaActual)
        holder.binding.tvConsumoKwh.text = "%.1f kWh".format(item.consumoKwh)
        holder.binding.tvMontoPagarSoles.text = "S/ %.2f".format(item.montoPagarSoles)

        // Estado: Si no hay lectura, advertir
        if (item.lecturaAnterior == 0.0 && item.lecturaActual == 0.0 && item.consumoKwh == 0.0) {
            holder.binding.tvEstadoPago.text = "Sin Lectura ⚠️"
            holder.binding.tvEstadoPago.setBackgroundColor(COLOR_BG_SIN_LECTURA)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_SIN_LECTURA)
        } else {
            holder.binding.tvEstadoPago.text = "Pendiente 🟡"
            holder.binding.tvEstadoPago.setBackgroundColor(COLOR_BG_PENDIENTE)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_PENDIENTE)
        }

        // Botón WhatsApp
        holder.binding.btnEnviarWhatsapp.setOnClickListener {
            if (onWhatsappClick != null) {
                onWhatsappClick.invoke(item)
            } else {
                enviarMensajeWhatsapp(holder.itemView.context, item)
            }
        }

        // Botón PDF (Asíncrono y seguro)
        holder.binding.btnExportarPdf.setOnClickListener {
            if (onPdfClick != null) {
                onPdfClick.invoke(item)
            } else {
                exportarPdfIndividual(holder.itemView.context, item, holder)
            }
        }
    }

    fun actualizarLista(nuevaLista: List<DetalleCobro>) {
        submitList(nuevaLista)
    }

    private fun enviarMensajeWhatsapp(context: Context, cobro: DetalleCobro) {
        val detalleAreaComun = if (cobro.montoAreaComunSoles > 0) {
            "\n🏢 *Cuota Área Común:* S/ ${"%.2f".format(cobro.montoAreaComunSoles)}"
        } else ""

        val mensaje = """
        ⚡ *RESUMEN DE CONSUMO DE LUZ - KILOWATT*
        📅 *Periodo:* ${cobro.mesPeriodo}
        👤 *Inquilino:* ${cobro.nombreInquilino} (${cobro.nombreEspacio})
        
        🔹 *Lectura Anterior:* ${"%.1f".format(cobro.lecturaAnterior)} kWh
        🔹 *Lectura Actual:* ${"%.1f".format(cobro.lecturaActual)} kWh
        ⚡ *Consumo Propio:* ${"%.1f".format(cobro.consumoKwh)} kWh (S/ ${"%.2f".format(cobro.consumoKwh * cobro.precioKwh)})$detalleAreaComun
        💵 *Precio x kWh:* S/ ${"%.2f".format(cobro.precioKwh)}
        
        💰 *TOTAL A PAGAR:* *S/ ${"%.2f".format(cobro.montoPagarSoles)}*
        
        Por favor realizar el Yape/Plin o transferencia al número acostumbrado. ¡Muchas gracias!
        """.trimIndent()

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=51${cobro.telefonoWhatsapp}&text=" + URLEncoder.encode(mensaje, "UTF-8")
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportarPdfIndividual(context: Context, cobro: DetalleCobro, holder: ViewHolder) {
        holder.binding.btnExportarPdf.isEnabled = false
        Toast.makeText(context, "Generando PDF...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val generator = PdfGenerator(context)
                val file = generator.generarComprobanteIndividual(cobro)

                if (file != null && file.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "application/pdf"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(intent, "Compartir Comprobante PDF"))
                } else {
                    Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                holder.binding.btnExportarPdf.isEnabled = true
            }
        }
    }

    companion object {
        private val COLOR_BG_SIN_LECTURA = Color.parseColor("#33F44336")
        private val COLOR_TEXT_SIN_LECTURA = Color.parseColor("#D32F2F")
        private val COLOR_BG_PENDIENTE = Color.parseColor("#33FFC107")
        private val COLOR_TEXT_PENDIENTE = Color.parseColor("#FF8F00")

        val CobroDiffCallback = object : DiffUtil.ItemCallback<DetalleCobro>() {
            override fun areItemsTheSame(oldItem: DetalleCobro, newItem: DetalleCobro): Boolean {
                return oldItem.nombreEspacio == newItem.nombreEspacio && oldItem.mesPeriodo == newItem.mesPeriodo
            }

            override fun areContentsTheSame(oldItem: DetalleCobro, newItem: DetalleCobro): Boolean {
                return oldItem == newItem
            }
        }
    }
}
