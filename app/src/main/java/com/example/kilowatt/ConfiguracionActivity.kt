package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kilowatt.data.AuthRepository
import com.example.kilowatt.data.InquilinoConMedidor
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.databinding.ActivityConfiguracionBinding
import com.example.kilowatt.databinding.DialogEditarInquilinoBinding
import com.example.kilowatt.util.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracionBinding
    private lateinit var adapter: InquilinoAdapter
    private lateinit var repository: InquilinoRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var settingsManager: SettingsManager

    private val viewModel: ConfiguracionViewModel by viewModels {
        ConfiguracionViewModel.Factory(repository, settingsManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = InquilinoRepository()
        authRepository = AuthRepository()
        settingsManager = SettingsManager(this)

        setupRecyclerView()
        observarEstadoViewModel()

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
            val nombre = binding.etNombreInquilino.text.toString()
            val telefono = binding.etTelefonoWhatsapp.text.toString()
            val espacio = binding.etNombreEspacio.text.toString()
            val esAreaComun = binding.cbEsAreaComun.isChecked
            val pagaAreaComun = binding.cbPagaAreaComun.isChecked

            viewModel.guardarDatosInquilino(nombre, telefono, espacio, esAreaComun, pagaAreaComun)
        }

        binding.btnGuardarDatosPropietario.setOnClickListener {
            val nombre = binding.etNombrePropietario.text.toString()
            val numero = binding.etNumeroPago.text.toString()
            val dia = binding.etDiaLimitePago.text.toString()

            viewModel.guardarDatosPropietario(nombre, numero, dia)
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

    private fun observarEstadoViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    actualizarUi(state)
                }
            }
        }
    }

    private fun actualizarUi(state: ConfiguracionUiState) {
        binding.btnGuardarInquilino.isEnabled = !state.estaGuardando
        binding.btnGuardarDatosPropietario.isEnabled = !state.estaGuardando

        // Actualizar RecyclerView
        adapter.actualizarLista(state.listaInquilinos)

        // Sincronizar campos de propietario si difieren
        if (binding.etNombrePropietario.text.toString() != state.nombrePropietario) {
            binding.etNombrePropietario.setText(state.nombrePropietario)
        }
        if (binding.etNumeroPago.text.toString() != state.numeroPago) {
            binding.etNumeroPago.setText(state.numeroPago)
        }
        if (binding.etDiaLimitePago.text.toString() != state.diaLimitePago.toString()) {
            binding.etDiaLimitePago.setText(state.diaLimitePago.toString())
        }

        // Notificaciones
        state.mensajeNotificacion?.let { msj ->
            Toast.makeText(this, msj, if (state.esError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

            if (!state.esError) {
                binding.etNombreInquilino.setText("")
                binding.etTelefonoWhatsapp.setText("")
                binding.etNombreEspacio.setText("")
                binding.cbEsAreaComun.isChecked = false
                binding.cbPagaAreaComun.isChecked = true

                binding.etNombreInquilino.clearFocus()
                binding.etTelefonoWhatsapp.clearFocus()
                binding.etNombreEspacio.clearFocus()
            }

            viewModel.consumirNotificacion()
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

            viewModel.actualizarInquilinoYSubmedidor(inqActualizado, subActualizado) { exito, error ->
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
            viewModel.eliminarInquilinoYSubmedidor(item.inquilino, item.submedidor) { exito, error ->
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
