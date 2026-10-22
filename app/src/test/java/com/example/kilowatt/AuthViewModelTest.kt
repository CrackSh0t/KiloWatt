package com.example.kilowatt

import com.example.kilowatt.data.IAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAuthRepository(
        private val shouldSucceed: Boolean = true,
    ) : IAuthRepository {
        override val usuarioActual: FirebaseUser? = null

        override fun registrarUsuario(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
            onResult(shouldSucceed, if (shouldSucceed) null else "Error de registro")
        }

        override fun iniciarSesion(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
            onResult(shouldSucceed, if (shouldSucceed) null else "Credenciales inválidas")
        }

        override fun cerrarSesion() {}

        override fun enviarCorreoRecuperacion(email: String, onResult: (Boolean, String?) -> Unit) {
            onResult(shouldSucceed, if (shouldSucceed) null else "Error al enviar correo")
        }
    }

    @Test
    fun iniciarSesion_exitoso_emiteAutenticado() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = true)
        val viewModel = AuthViewModel(repo)

        viewModel.iniciarSesion("test@kilowatt.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Autenticado)
    }

    @Test
    fun iniciarSesion_fallido_emiteError() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = false)
        val viewModel = AuthViewModel(repo)

        viewModel.iniciarSesion("test@kilowatt.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Credenciales inválidas", (state as AuthUiState.Error).mensaje)
    }

    @Test
    fun enviarCorreoRecuperacion_exitoso_emiteMensajeRecuperacion() = runTest {
        val repo = FakeAuthRepository(shouldSucceed = true)
        val viewModel = AuthViewModel(repo)

        viewModel.enviarCorreoRecuperacion("test@kilowatt.com")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.MensajeRecuperacion)
    }
}
