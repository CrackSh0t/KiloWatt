package com.example.kilowatt

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityConfiguracionBinding
import com.example.kilowatt.databinding.DialogEditarInquilinoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var adapter: InquilinoAdapter
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()
        observarInquilinos()

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnGuardarInquilino.setOnClickListener {
            guardarDatos()
        }
    }

    private fun setupRecyclerView() {
        adapter = InquilinoAdapter { item ->
            mostrarDialogoEditar(item)
        }
        binding.rvInquilinos.apply {
            layoutManager = LinearLayoutManager(this@ConfiguracionActivity)
            adapter = this@ConfiguracionActivity.adapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observarInquilinos() {
        lifecycleScope.launch {
            combine(
                database.inquilinoDao().obtenerTodosLosInquilinos(),
                database.submedidorDao().obtenerTodosLosSubmedidores()
            ) { inquilinos, submedidores ->
                submedidores.map { sub ->
                    val inq = inquilinos.find { it.idInquilino == sub.idInquilinoTitular }
                    InquilinoConMedidor(inquilino = inq, submedidor = sub)
                }
            }.collect { lista ->
                adapter.actualizarLista(lista)
            }
        }
    }

    private fun guardarDatos() {
        val nombre = binding.etNombreInquilino.text.toString().trim()
        val telefono = binding.etTelefonoWhatsapp.text.toString().trim()
        val nombreEspacio = binding.etNombreEspacio.text.toString().trim()
        val esAreaComun = binding.cbEsAreaComun.isChecked
        val pagaAreaComun = binding.cbPagaAreaComun.isChecked

        if (nombreEspacio.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del espacio", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (esAreaComun) {
                val nuevoSubmedidor = Submedidor(
                    nombreEspacio = nombreEspacio,
                    esAreaComun = true,
                    pagaAreaComun = false,
                    idInquilinoTitular = null
                )
                database.submedidorDao().insertarSubmedidor(nuevoSubmedidor)
            } else {
                if (nombre.isEmpty()) {
                    Toast.makeText(this@ConfiguracionActivity, "Ingresa el nombre del inquilino", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nuevoInquilino = Inquilino(
                    nombreCompleto = nombre,
                    telefonoWhatsapp = telefono
                )
                val idInquilinoGenerado = database.inquilinoDao().insertarInquilino(nuevoInquilino)

                val nuevoSubmedidor = Submedidor(
                    nombreEspacio = nombreEspacio,
                    esAreaComun = false,
                    pagaAreaComun = pagaAreaComun,
                    idInquilinoTitular = idInquilinoGenerado.toInt()
                )
                database.submedidorDao().insertarSubmedidor(nuevoSubmedidor)
            }

            binding.etNombreInquilino.text?.clear()
            binding.etTelefonoWhatsapp.text?.clear()
            binding.etNombreEspacio.text?.clear()
            binding.cbEsAreaComun.isChecked = false
            binding.cbPagaAreaComun.isChecked = true

            Toast.makeText(this@ConfiguracionActivity, "Guardado con éxito", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoEditar(item: InquilinoConMedidor) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogEditarInquilinoBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        dialogBinding.etEditNombre.setText(item.inquilino?.nombreCompleto ?: "")
        dialogBinding.etEditTelefono.setText(item.inquilino?.telefonoWhatsapp ?: "")
        dialogBinding.etEditEspacio.setText(item.submedidor.nombreEspacio)
        dialogBinding.cbEditPagaAreaComun.isChecked = item.submedidor.pagaAreaComun

        if (item.submedidor.esAreaComun) {
            dialogBinding.etEditNombre.isEnabled = false
            dialogBinding.etEditTelefono.isEnabled = false
            dialogBinding.cbEditPagaAreaComun.isEnabled = false
        }

        dialogBinding.btnGuardarCambios.setOnClickListener {
            val nuevoNombre = dialogBinding.etEditNombre.text.toString().trim()
            val nuevoTelefono = dialogBinding.etEditTelefono.text.toString().trim()
            val nuevoEspacio = dialogBinding.etEditEspacio.text.toString().trim()
            val nuevoPagaComun = dialogBinding.cbEditPagaAreaComun.isChecked

            lifecycleScope.launch {
                val subActualizado = item.submedidor.copy(
                    nombreEspacio = nuevoEspacio,
                    pagaAreaComun = nuevoPagaComun
                )
                database.submedidorDao().insertarSubmedidor(subActualizado)

                if (item.inquilino != null) {
                    val inqActualizado = item.inquilino.copy(
                        nombreCompleto = nuevoNombre,
                        telefonoWhatsapp = nuevoTelefono
                    )
                    database.inquilinoDao().insertarInquilino(inqActualizado)
                }

                Toast.makeText(this@ConfiguracionActivity, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialogBinding.btnEliminar.setOnClickListener {
            lifecycleScope.launch {
                database.submedidorDao().eliminarSubmedidor(item.submedidor)
                if (item.inquilino != null) {
                    database.inquilinoDao().eliminarInquilino(item.inquilino)
                }
                Toast.makeText(this@ConfiguracionActivity, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}