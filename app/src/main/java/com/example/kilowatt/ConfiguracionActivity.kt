package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityConfiguracionBinding
import kotlinx.coroutines.launch

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: InquilinoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()
        observarInquilinos()

        binding.btnGuardarInquilino.setOnClickListener {
            guardarDatos()
        }

        binding.btnIrALecturas.setOnClickListener {
            val intent = Intent(this, LecturasActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = InquilinoAdapter()
        binding.rvInquilinos.apply {
            layoutManager = LinearLayoutManager(this@ConfiguracionActivity)
            adapter = this@ConfiguracionActivity.adapter
        }
    }

    private fun observarInquilinos() {
        // Se ejecuta automáticamente cada vez que los datos en Room cambien
        lifecycleScope.launch {
            database.inquilinoDao().obtenerTodosLosInquilinos().collect { lista ->
                adapter.actualizarLista(lista)
            }
        }
    }

    private fun guardarDatos() {
        val nombre = binding.etNombreInquilino.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val nombreEspacio = binding.etNombreEspacio.text.toString().trim()
        val esAreaComun = binding.cbEsAreaComun.isChecked

        if (nombreEspacio.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del espacio", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (esAreaComun) {
                val nuevoSubmedidor = Submedidor(
                    nombreEspacio = nombreEspacio,
                    esAreaComun = true,
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
                val idInquilino = database.inquilinoDao().insertarInquilino(nuevoInquilino)

                val nuevoSubmedidor = Submedidor(
                    nombreEspacio = nombreEspacio,
                    esAreaComun = false,
                    idInquilinoTitular = idInquilino.toInt()
                )
                database.submedidorDao().insertarSubmedidor(nuevoSubmedidor)
            }

            Toast.makeText(this@ConfiguracionActivity, "¡Registrado con éxito!", Toast.LENGTH_SHORT).show()
            limpiarCampos()
        }
    }

    private fun limpiarCampos() {
        binding.etNombreInquilino.text.clear()
        binding.etTelefono.text.clear()
        binding.etNombreEspacio.text.clear()
        binding.cbEsAreaComun.isChecked = false
    }
}