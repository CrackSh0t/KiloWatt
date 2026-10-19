package com.example.kilowatt

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kilowatt.data.AuthRepository
import com.example.kilowatt.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Si ya hay usuario con sesión activa, saltar al MainActivity
        if (authRepository.usuarioActual != null) {
            irAMainActivity()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authRepository.iniciarSesion(email, pass) { exito, error ->
                if (exito) {
                    irAMainActivity()
                } else {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authRepository.registrarUsuario(email, pass) { exito, error ->
                if (exito) {
                    Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show()
                    irAMainActivity()
                } else {
                    Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val emailIngresado = binding.etEmail.text.toString().trim()
            mostrarDialogoRecuperacion(emailIngresado)
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
                val emailParaEnviar = inputEditText.text.toString().trim()
                if (emailParaEnviar.isEmpty()) {
                    Toast.makeText(this, getString(R.string.msg_recovery_email_empty), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                authRepository.enviarCorreoRecuperacion(emailParaEnviar) { exito, error ->
                    if (exito) {
                        Toast.makeText(
                            this,
                            getString(R.string.msg_recovery_email_sent, emailParaEnviar),
                            Toast.LENGTH_LONG,
                        ).show()
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun irAMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}