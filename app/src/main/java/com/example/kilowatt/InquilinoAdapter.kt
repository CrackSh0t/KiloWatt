package com.example.kilowatt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.databinding.ItemInquilinoBinding

class InquilinoAdapter(
    private var lista: List<Inquilino> = emptyList()
) : RecyclerView.Adapter<InquilinoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemInquilinoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInquilinoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.binding.tvNombre.text = item.nombreCompleto
        holder.binding.tvEspacio.text = "Inquilino registrado"
        holder.binding.tvTelefono.text = if (item.telefonoWhatsapp.isNotEmpty()) "📱 ${item.telefonoWhatsapp}" else ""
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Inquilino>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}