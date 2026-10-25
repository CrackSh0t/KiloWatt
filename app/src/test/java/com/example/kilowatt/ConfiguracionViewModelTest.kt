package com.example.kilowatt

import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Submedidor
import com.example.kilowatt.util.ISettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class ConfiguracionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeInquilinoRepository : IInquilinoRepository {
        var guardadoExitoso = false
        override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> = flowOf(emptyList())
        override fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> = flowOf(emptyList())
        override fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = flowOf(null)
        override suspend fun guardarInquilinoYSubmedidor(
            nombreCompleto: String,
            telefono: String,
            nombreEspacio: String,
            esAreaComun: Boolean,
            pagaAreaComun: Boolean,
        ): Pair<Boolean, String?> {
            guardadoExitoso = true
            return Pair(true, null)
        }

        override suspend fun guardarPerfilPropietario(nombre: String, numero: String, diaLimite: Int): Pair<Boolean, String?> = Pair(true, null)
        override fun actualizarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }

        override fun eliminarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }
        override fun cancelarListeners() {}
    }

    private class FakeSettingsManager : ISettingsManager {
        override val diaLimitePago: Int = 5
        override val nombrePropietario: String = "Juan Pérez"
        override val numeroPago: String = "987654321"
        override fun limpiarConfiguracion() {}
    }

    @Test
    fun guardarDatosInquilino_telefonoInvalido_emiteError() = runTest {
        val repo = FakeInquilinoRepository()
        val settings = FakeSettingsManager()
        val viewModel = ConfiguracionViewModel(repo, settings, testDispatcher)

        // Teléfono inválido
        viewModel.guardarDatosInquilino(
            nombre = "Carlos",
            telefono = "abc",
            nombreEspacio = "Depa 202",
            esAreaComun = false,
            pagaAreaComun = true,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.esError)
        assertEquals(false, repo.guardadoExitoso)
    }

    @Test
    fun guardarDatosInquilino_telefonoValido_ejecutaGuardado() = runTest {
        val repo = FakeInquilinoRepository()
        val settings = FakeSettingsManager()
        val viewModel = ConfiguracionViewModel(repo, settings, testDispatcher)

        // Teléfono válido E.164 o normalizable
        viewModel.guardarDatosInquilino(
            nombre = "Carlos",
            telefono = "987654321",
            nombreEspacio = "Depa 202",
            esAreaComun = false,
            pagaAreaComun = true,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.esError)
        assertEquals("¡Registrado en la nube con éxito!", state.mensajeNotificacion)
        assertTrue(repo.guardadoExitoso)
    }
}
