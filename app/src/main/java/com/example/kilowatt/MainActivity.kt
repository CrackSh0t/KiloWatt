package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kilowatt.data.IAuthRepository
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.databinding.ActivityMainBinding
import com.example.kilowatt.databinding.DialogPerfilPropietarioBinding
import com.example.kilowatt.util.ISettingsManager
import com.example.kilowatt.util.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var authRepository: IAuthRepository

    @Inject
    lateinit var inquilinoRepository: IInquilinoRepository

    @Inject
    lateinit var settingsManager: ISettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = authRepository.usuarioActual
        if (user == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user.email?.let { email ->
            binding.tvUsuarioEmail.text = getString(R.string.main_session_prefix, email)
        }

        binding.btnPerfilAvatar.setOnClickListener {
            mostrarDialogoPerfilPropietario(user.email ?: "")
        }

        // 1. Abrir Registrador de Lecturas
        binding.cardRegistrarLecturas.setOnClickListener {
            val intent = Intent(this, LecturasActivity::class.java)
            startActivity(intent)
        }

        // 2. Abrir Resumen y Cobros
        binding.cardResumenCobros.setOnClickListener {
            val intent = Intent(this, ResumenCobrosActivity::class.java)
            startActivity(intent)
        }

        // 3. Abrir Configuración de Inquilinos
        binding.cardConfiguracion.setOnClickListener {
            val intent = Intent(this, ConfiguracionActivity::class.java)
            startActivity(intent)
        }

        // 4. Abrir Historial y Gráficos
        binding.cardHistorialGraficos.setOnClickListener {
            val intent = Intent(this, HistorialConsumoActivity::class.java)
            startActivity(intent)
        }
    }

    private fun mostrarDialogoPerfilPropietario(email: String) {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogPerfilPropietarioBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvCuentaEmail.text = "Cuenta: $email"
        dialogBinding.etNombrePropietario.setText(settingsManager.nombrePropietario)
        dialogBinding.etNumeroPago.setText(settingsManager.numeroPago)
        dialogBinding.etDiaLimitePago.setText(settingsManager.diaLimitePago.toString())

        val opcionesProrrateo = arrayOf(
            getString(R.string.option_prorrateo_equity),
            getString(R.string.option_prorrateo_weighted),
        )
        val adapterProrrateo = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            opcionesProrrateo,
        )
        dialogBinding.actvTipoProrrateo.setAdapter(adapterProrrateo)

        val currentTipo = settingsManager.tipoProrrateoComun
        val indexInicial = if (currentTipo == "WEIGHTED_PERSONS") 1 else 0
        dialogBinding.actvTipoProrrateo.setText(opcionesProrrateo[indexInicial], false)

        // Sincronización en tiempo real desde Firestore para el usuario activo
        lifecycleScope.launch {
            inquilinoRepository.obtenerPerfilPropietarioFlow().collect { datos ->
                if (datos != null && dialog.isShowing) {
                    val nombre = datos["nombrePropietario"]?.toString() ?: ""
                    val numero = datos["numeroPago"]?.toString() ?: ""
                    val diaLimite = (datos["diaLimitePago"] as? Long)?.toInt()
                        ?: (datos["diaLimitePago"] as? Int) ?: 5

                    val tipoProrrateo = datos["tipoProrrateoComun"]?.toString() ?: "EQUITY"

                    (settingsManager as? SettingsManager)?.let {
                        it.nombrePropietario = nombre
                        it.numeroPago = numero
                        it.diaLimitePago = diaLimite
                        it.tipoProrrateoComun = tipoProrrateo
                    }

                    dialogBinding.etNombrePropietario.setText(nombre)
                    dialogBinding.etNumeroPago.setText(numero)
                    dialogBinding.etDiaLimitePago.setText(diaLimite.toString())

                    val indexUpdate = if (tipoProrrateo == "WEIGHTED_PERSONS") 1 else 0
                    dialogBinding.actvTipoProrrateo.setText(opcionesProrrateo[indexUpdate], false)
                }
            }
        }

        dialogBinding.btnGuardarPerfil.setOnClickListener {
            val nombre = dialogBinding.etNombrePropietario.text.toString().trim()
            val numero = dialogBinding.etNumeroPago.text.toString().trim()
            val diaStr = dialogBinding.etDiaLimitePago.text.toString().trim()
            val selectedProrrateoText = dialogBinding.actvTipoProrrateo.text.toString()

            if (nombre.isEmpty() || numero.isEmpty() || diaStr.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val diaLimite = diaStr.toIntOrNull() ?: 5
            val tipoProrrateo = if (selectedProrrateoText == opcionesProrrateo[1]) "WEIGHTED_PERSONS" else "EQUITY"

            (settingsManager as? SettingsManager)?.let {
                it.nombrePropietario = nombre
                it.numeroPago = numero
                it.diaLimitePago = diaLimite
                it.tipoProrrateoComun = tipoProrrateo
            }

            dialogBinding.btnGuardarPerfil.isEnabled = false
            lifecycleScope.launch {
                try {
                    val (exito, error) = inquilinoRepository.guardarPerfilPropietario(
                        nombre = nombre,
                        numero = numero,
                        diaLimite = diaLimite,
                        tipoProrrateoComun = tipoProrrateo,
                    )
                    if (exito) {
                        Toast.makeText(this@MainActivity, "Perfil de cobro actualizado", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this@MainActivity, "Guardado local. Error nube: $error", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    dialogBinding.btnGuardarPerfil.isEnabled = true
                }
            }
        }

        dialogBinding.btnCerrarSesionDialog.setOnClickListener {
            dialogBinding.btnCerrarSesionDialog.isEnabled = false
            lifecycleScope.launch {
                try {
                    authRepository.cerrarSesion()
                } catch (_: Exception) {}

                if (!isFinishing && !isDestroyed) {
                    dialog.dismiss()
                    val intent = Intent(this@MainActivity, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        dialog.show()
    }
}
