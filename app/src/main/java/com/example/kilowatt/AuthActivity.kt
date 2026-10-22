package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kilowatt.data.AuthRepository
import com.example.kilowatt.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val authRepository = AuthRepository()
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observarEstadoViewModel()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            viewModel.iniciarSesion(email, pass)
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            viewModel.registrarUsuario(email, pass)
        }

        binding.tvForgotPassword.setOnClickListener {
            val emailIngresado = binding.etEmail.text.toString().trim()
            mostrarDialogoRecuperacion(emailIngresado)
        }
    }

    private fun observarEstadoViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Autenticado -> {
                            irAMainActivity()
                        }
                        is AuthUiState.Error -> {
                            Toast.makeText(this@AuthActivity, state.mensaje, Toast.LENGTH_LONG).show()
                        }
                        is AuthUiState.MensajeRecuperacion -> {
                            Toast.makeText(this@AuthActivity, state.mensaje, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun mostrarDialogoRecuperacion(emailInicial: String) {
        val inputEditText = com.google.android.material.textfield.TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = getString(R.string.email_hint)
            if (emailInicial.isNotBlank()) {
                setText(emailInicial)
            }
        }

        val container = android.widget.FrameLayout(this).apply {
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
            addView(inputEditText)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_forgot_password_title)
            .setMessage(R.string.dialog_forgot_password_msg)
            .setView(container)
            .setPositiveButton(R.string.btn_send) { _, _ ->
                val emailParaEnviar = inputEditText.text.toString()
                viewModel.enviarCorreoRecuperacion(emailParaEnviar)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun irAMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
