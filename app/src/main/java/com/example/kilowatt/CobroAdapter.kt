package com.example.kilowatt

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kilowatt.data.DetalleCobro
import com.example.kilowatt.databinding.ItemCobroBinding

class CobroAdapter(
    private val onSendWhatsAppClicked: ((DetalleCobro) -> Unit)? = null,
    private val onGeneratePdfClicked: ((DetalleCobro) -> Unit)? = null,
    private val onEstadoChange: ((DetalleCobro) -> Unit)? = null,
    private val onWhatsappClick: ((DetalleCobro) -> Unit)? = null,
    private val onPdfClick: ((DetalleCobro) -> Unit)? = null,
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

        if (item.montoAreaComunSoles > 0.0) {
            holder.binding.tvCuotaComunDetalle.visibility = android.view.View.VISIBLE
            val labelPers = if (item.numeroPersonas > 1) " (${item.numeroPersonas} pers.)" else ""
            holder.binding.tvCuotaComunDetalle.text = "+ S/ %.2f área común%s".format(item.montoAreaComunSoles, labelPers)
        } else {
            holder.binding.tvCuotaComunDetalle.visibility = android.view.View.GONE
        }

        // Estado: Renderizado dinámico del distintivo gráfico
        if (item.lecturaAnterior == 0.0 && item.lecturaActual == 0.0 && item.consumoKwh == 0.0) {
            holder.binding.tvEstadoPago.text = "Sin Lectura ⚠️"
            holder.binding.cardEstadoPago.setCardBackgroundColor(COLOR_BG_SIN_LECTURA)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_SIN_LECTURA)
            holder.binding.tvDetallePago.visibility = android.view.View.GONE
        } else if (item.estadoPago.equals("PAGADO", ignoreCase = true)) {
            holder.binding.tvEstadoPago.text = "Pagado ✅"
            holder.binding.cardEstadoPago.setCardBackgroundColor(COLOR_BG_PAGADO)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_PAGADO)

            val contexto = holder.itemView.context
            val fechaTexto = if (item.fechaPago != null && item.fechaPago > 0) {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(item.fechaPago))
            } else {
                "Recientemente"
            }
            val metodoTexto = item.metodoPago ?: "Efectivo"
            holder.binding.tvDetallePago.text = contexto.getString(R.string.label_pagado_el, fechaTexto, metodoTexto)
            holder.binding.tvDetallePago.setTextColor(COLOR_TEXT_PAGADO)
            holder.binding.tvDetallePago.visibility = android.view.View.VISIBLE
        } else if (item.estadoPago.equals("VENCIDO", ignoreCase = true)) {
            holder.binding.tvEstadoPago.text = "Vencido 🔴"
            holder.binding.cardEstadoPago.setCardBackgroundColor(COLOR_BG_VENCIDO)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_VENCIDO)

            val contexto = holder.itemView.context
            val diaLimite = com.example.kilowatt.util.SettingsManager(contexto).diaLimitePago
            val fechaLimiteStr = com.example.kilowatt.util.PeriodoDateUtils.formatearFechaLimite(item.mesPeriodo, diaLimite) ?: ""
            holder.binding.tvDetallePago.text = contexto.getString(R.string.label_vencio_el, fechaLimiteStr)
            holder.binding.tvDetallePago.setTextColor(COLOR_TEXT_VENCIDO)
            holder.binding.tvDetallePago.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.tvEstadoPago.text = "Pendiente 🟡"
            holder.binding.cardEstadoPago.setCardBackgroundColor(COLOR_BG_PENDIENTE)
            holder.binding.tvEstadoPago.setTextColor(COLOR_TEXT_PENDIENTE)
            holder.binding.tvDetallePago.visibility = android.view.View.GONE
        }

        // Clic en la tarjeta/badge de estado para alternar cobro
        holder.binding.cardEstadoPago.setOnClickListener {
            onEstadoChange?.invoke(item)
        }

        // Botón WhatsApp (Delegado mediante callback)
        holder.binding.btnEnviarWhatsapp.setOnClickListener {
            onSendWhatsAppClicked?.invoke(item) ?: onWhatsappClick?.invoke(item)
        }

        // Botón PDF (Delegado mediante callback)
        holder.binding.btnExportarPdf.setOnClickListener {
            onGeneratePdfClicked?.invoke(item) ?: onPdfClick?.invoke(item)
        }
    }

    fun actualizarLista(nuevaLista: List<DetalleCobro>) {
        submitList(nuevaLista)
    }

    companion object {
        private val COLOR_BG_SIN_LECTURA = Color.parseColor("#FFEBEE")
        private val COLOR_TEXT_SIN_LECTURA = Color.parseColor("#C62828")
        private val COLOR_BG_PENDIENTE = Color.parseColor("#FFF8E1")
        private val COLOR_TEXT_PENDIENTE = Color.parseColor("#EF6C00")
        private val COLOR_BG_PAGADO = Color.parseColor("#E8F5E9")
        private val COLOR_TEXT_PAGADO = Color.parseColor("#2E7D32")
        private val COLOR_BG_VENCIDO = Color.parseColor("#FFEBEE")
        private val COLOR_TEXT_VENCIDO = Color.parseColor("#C62828")

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
