package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.AuthRepository
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.databinding.ActivityConfiguracionBinding
import com.example.kilowatt.databinding.DialogEditarInquilinoBinding
import com.example.kilowatt.util.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var adapter: InquilinoAdapter
    private lateinit var repository: InquilinoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = InquilinoRepository()
        authRepository = AuthRepository()
        settingsManager = SettingsManager(this)

        setupRecyclerView()
        observarInquilinos()
        observarPerfilPropietario()

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnCerrarSesion.setOnClickListener {
            authRepository.cerrarSesion()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnGuardarInquilino.setOnClickListener {
            guardarDatos()
        }

        binding.btnGuardarDatosPropietario.setOnClickListener {
            guardarDatosPropietario()
        }
    }

    private fun observarPerfilPropietario() {
        // Cargar primero de SharedPreferences por defecto
        binding.etNombrePropietario.setText(settingsManager.nombrePropietario)
        binding.etNumeroPago.setText(settingsManager.numeroPago)
        binding.etDiaLimitePago.setText(settingsManager.diaLimitePago.toString())

        // Luego sincronizar desde Firestore
        lifecycleScope.launch {
            repository.obtenerPerfilPropietarioFlow().collect { datos ->
                if (datos != null) {
                    val nombre = datos["nombrePropietario"]?.toString() ?: ""
                    val numero = datos["numeroPago"]?.toString() ?: ""
                    val diaLimite = (datos["diaLimitePago"] as? Long)?.toInt()
                        ?: (datos["diaLimitePago"] as? Int) ?: 5

                    if (nombre.isNotEmpty()) {
                        settingsManager.nombrePropietario = nombre
                        binding.etNombrePropietario.setText(nombre)
                    }
                    if (numero.isNotEmpty()) {
                        settingsManager.numeroPago = numero
                        binding.etNumeroPago.setText(numero)
                    }
                    settingsManager.diaLimitePago = diaLimite
                    binding.etDiaLimitePago.setText(diaLimite.toString())
                }
            }
        }
    }

    private fun guardarDatosPropietario() {
        val nombre = binding.etNombrePropietario.text.toString().trim()
        val numero = binding.etNumeroPago.text.toString().trim()
        val diaStr = binding.etDiaLimitePago.text.toString().trim()

        if (nombre.isEmpty() || numero.isEmpty() || diaStr.isEmpty()) {
            Toast.makeText(this, "Por favor completa los datos del propietario", Toast.LENGTH_SHORT).show()
            return
        }

        val diaLimite = diaStr.toIntOrNull() ?: 5
        settingsManager.nombrePropietario = nombre
        settingsManager.numeroPago = numero
        settingsManager.diaLimitePago = diaLimite

        binding.btnGuardarDatosPropietario.isEnabled = false

        lifecycleScope.launch {
            try {
                val (exito, error) = repository.guardarPerfilPropietario(nombre, numero, diaLimite)
                if (exito) {
                    Toast.makeText(this@ConfiguracionActivity, "Datos del propietario guardados con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ConfiguracionActivity, "Guardado localmente. Error en nube: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, "Guardado localmente. Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnGuardarDatosPropietario.isEnabled = true
            }
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
                repository.obtenerInquilinosFlow(),
                repository.obtenerSubmedidoresFlow(),
            ) { inquilinos, submedidores ->
                submedidores.map { sub ->
                    val inq = if (sub.esAreaComun) null else inquilinos.find { it.idInquilino == sub.idInquilinoTitular }
                    InquilinoConMedidor(inquilino = inq, submedidor = sub)
                }
            }.collect { lista ->
                adapter.actualizarLista(lista)
            }
        }
    }

    private fun guardarDatos() {
        val nombre = binding.etNombreInquilino.text.toString().trim()
        // Nos aseguramos de leer correctamente el campo de WhatsApp/Teléfono
        val telefono = binding.etTelefonoWhatsapp.text.toString().trim()
        val nombreEspacio = binding.etNombreEspacio.text.toString().trim()
        val esAreaComun = binding.cbEsAreaComun.isChecked
        val pagaAreaComun = binding.cbPagaAreaComun.isChecked

        if (nombreEspacio.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el nombre del espacio", Toast.LENGTH_SHORT).show()
            return
        }

        if (!esAreaComun && nombre.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre del inquilino", Toast.LENGTH_SHORT).show()
            return
        }

        // Deshabilitar botón durante el envío a la nube
        binding.btnGuardarInquilino.isEnabled = false

        lifecycleScope.launch {
            try {
                val (exito, error) = repository.guardarInquilinoYSubmedidor(
                    nombreCompleto = if (esAreaComun) "Área Común" else nombre,
                    telefono = if (esAreaComun) "" else telefono,
                    nombreEspacio = nombreEspacio,
                    esAreaComun = esAreaComun,
                    pagaAreaComun = if (esAreaComun) false else pagaAreaComun,
                )

                if (exito) {
                    // 1. Limpieza explícita de todos los componentes
                    binding.etNombreInquilino.setText("")
                    binding.etTelefonoWhatsapp.setText("")
                    binding.etNombreEspacio.setText("")
                    binding.cbEsAreaComun.isChecked = false
                    binding.cbPagaAreaComun.isChecked = true

                    // 2. Limpiar el foco del teclado
                    binding.etNombreInquilino.clearFocus()
                    binding.etTelefonoWhatsapp.clearFocus()
                    binding.etNombreEspacio.clearFocus()

                    Toast.makeText(this@ConfiguracionActivity, "¡Registrado en la nube con éxito!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ConfiguracionActivity, "Error al guardar en Firestore: $error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ConfiguracionActivity, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                // 3. Garantizar que el botón siempre vuelva a estar activo
                binding.btnGuardarInquilino.isEnabled = true
            }
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

            val subActualizado = item.submedidor.copy(
                nombreEspacio = nuevoEspacio,
                pagaAreaComun = nuevoPagaComun,
            )

            val inqActualizado = item.inquilino?.copy(
                nombreCompleto = nuevoNombre,
                telefonoWhatsapp = nuevoTelefono,
            )

            repository.actualizarInquilinoYSubmedidor(inqActualizado, subActualizado) { exito, error ->
                if (!isFinishing && !isDestroyed) {
                    if (exito) {
                        Toast.makeText(this, "Actualizado en la nube", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialogBinding.btnEliminar.setOnClickListener {
            repository.eliminarInquilinoYSubmedidor(item.inquilino, item.submedidor) { exito, error ->
                if (!isFinishing && !isDestroyed) {
                    if (exito) {
                        Toast.makeText(this, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Error al eliminar: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
}