package com.example.kilowatt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.databinding.ItemInquilinoBinding

class InquilinoAdapter(
    private val onItemClick: (InquilinoConMedidor) -> Unit
) : RecyclerView.Adapter<InquilinoAdapter.ViewHolder>() {

    private var lista = listOf<InquilinoConMedidor>()

    class ViewHolder(val binding: ItemInquilinoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInquilinoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        val nombre = item.inquilino?.nombreCompleto ?: item.submedidor.nombreEspacio
        val telefono = item.inquilino?.telefonoWhatsapp ?: "N/A"

        holder.binding.tvNombre.text = nombre
        holder.binding.tvEspacio.text = if (item.submedidor.esAreaComun) "🏢 Área Común" else item.submedidor.nombreEspacio
        holder.binding.tvTelefono.text = telefono

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = lista.size

    fun actualizarLista(nuevaLista: List<InquilinoConMedidor>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}