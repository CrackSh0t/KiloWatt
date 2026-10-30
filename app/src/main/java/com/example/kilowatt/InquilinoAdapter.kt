package com.example.kilowatt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.databinding.ItemInquilinoBinding

class InquilinoAdapter(
    private val onItemClick: (InquilinoConMedidor) -> Unit
) : ListAdapter<InquilinoConMedidor, InquilinoAdapter.ViewHolder>(InquilinoDiffCallback) {

    class ViewHolder(val binding: ItemInquilinoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInquilinoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        val nombre = item.inquilino?.nombreCompleto?.ifEmpty { item.submedidor.nombreEspacio }
            ?: item.submedidor.nombreEspacio
        val telefono = item.inquilino?.telefonoWhatsapp?.ifEmpty { "N/A" } ?: "N/A"

        holder.binding.tvNombre.text = nombre
        holder.binding.tvEspacio.text = if (item.submedidor.esAreaComun) "🏢 Área Común" else item.submedidor.nombreEspacio
        holder.binding.tvTelefono.text = telefono

        if (item.submedidor.esAreaComun) {
            holder.binding.tvPersonas.visibility = android.view.View.GONE
        } else {
            holder.binding.tvPersonas.visibility = android.view.View.VISIBLE
            val numPersonas = item.inquilino?.numeroPersonas?.coerceAtLeast(1) ?: 1
            holder.binding.tvPersonas.text = "👥 $numPersonas pers."
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    fun actualizarLista(nuevaLista: List<InquilinoConMedidor>) {
        submitList(nuevaLista)
    }

    companion object {
        val InquilinoDiffCallback = object : DiffUtil.ItemCallback<InquilinoConMedidor>() {
            override fun areItemsTheSame(oldItem: InquilinoConMedidor, newItem: InquilinoConMedidor): Boolean {
                return oldItem.submedidor.idSubmedidor == newItem.submedidor.idSubmedidor
            }

            override fun areContentsTheSame(oldItem: InquilinoConMedidor, newItem: InquilinoConMedidor): Boolean {
                return oldItem == newItem
            }
        }
    }
}