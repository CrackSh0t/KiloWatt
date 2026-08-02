package com.example.kilowatt

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.databinding.ActivityConfiguracionBinding
import kotlinx.coroutines.launch

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos la instancia de Room
        database = AppDatabase.getDatabase(this)

        binding.btnGuardarInquilino.setOnClickListener {
            guardarDatos()
        }
    }

    private fun guardarDatos() {
        val nombre = binding.etNombreInquilino.text.toString().trim()
        val telefono = binding.etTelefono.text.toString().trim()
        val nombreEspacio = binding.etNombreEspacio.text.toString().trim()
        val esAreaComun = binding.cbEsAreaComun.isChecked

        if (nombreEspacio.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del espacio o departamento", Toast.LENGTH_SHORT).show()
            return
        }

        // Ejecutamos la inserción en segundo plano con Corrutinas
        lifecycleScope.launch {
            if (esAreaComun) {
                // Si es área común, guardamos solo el submedidor sin inquilino
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

                // 1. Guardamos el Inquilino
                val nuevoInquilino = Inquilino(
                    nombreCompleto = nombre,
                    telefonoWhatsapp = telefono
                )
                database.inquilinoDao().insertarInquilino(nuevoInquilino)

                // 2. Guardamos el Submedidor asignado
                val nuevoSubmedidor = Submedidor(
                    nombreEspacio = nombreEspacio,
                    esAreaComun = false,
                    idInquilinoTitular = null // Aquí podrías relacionar los IDs
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