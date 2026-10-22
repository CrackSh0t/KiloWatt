package com.example.kilowatt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kilowatt.data.IAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: IAuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CargarSesion)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        verificarSesionActiva()
    }

    fun verificarSesionActiva() {
        if (authRepository.usuarioActual != null) {
            _uiState.value = AuthUiState.Autenticado
        }
    }

    fun iniciarSesion(email: String, pass: String) {
        val emailLimpio = email.trim()
        val passLimpio = pass.trim()

        if (emailLimpio.isEmpty() || passLimpio.isEmpty()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos")
            return
        }

        _uiState.value = AuthUiState.Cargando
        authRepository.iniciarSesion(emailLimpio, passLimpio) { exito, error ->
            if (exito) {
                _uiState.value = AuthUiState.Autenticado
            } else {
                _uiState.value = AuthUiState.Error(error ?: "Error al iniciar sesión")
            }
        }
    }

    fun registrarUsuario(email: String, pass: String) {
        val emailLimpio = email.trim()
        val passLimpio = pass.trim()

        if (emailLimpio.isEmpty() || passLimpio.isEmpty()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos")
            return
        }

        _uiState.value = AuthUiState.Cargando
        authRepository.registrarUsuario(emailLimpio, passLimpio) { exito, error ->
            if (exito) {
                _uiState.value = AuthUiState.Autenticado
            } else {
                _uiState.value = AuthUiState.Error(error ?: "Error al registrar usuario")
            }
        }
    }

    fun enviarCorreoRecuperacion(email: String) {
        val emailLimpio = email.trim()
        if (emailLimpio.isEmpty()) {
            _uiState.value = AuthUiState.Error("Ingresa tu correo electrónico para recuperar la contraseña")
            return
        }
        authRepository.enviarCorreoRecuperacion(emailLimpio) { exito, error ->
            if (exito) {
                _uiState.value = AuthUiState.MensajeRecuperacion("Correo de recuperación enviado a $emailLimpio")
            } else {
                _uiState.value = AuthUiState.Error(error ?: "Error al enviar correo de recuperación")
            }
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _uiState.value = AuthUiState.CargarSesion
    }

    class Factory(private val authRepository: IAuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("ViewModel desconocido")
        }
    }
}
