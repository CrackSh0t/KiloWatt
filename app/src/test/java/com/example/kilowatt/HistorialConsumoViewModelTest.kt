package com.example.kilowatt

import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
import com.example.kilowatt.data.Submedidor
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
class HistorialConsumoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeLecturaRepository(
        private val facturas: List<FacturaGeneral> = emptyList(),
        private val lecturas: List<Lectura> = emptyList(),
    ) : ILecturaRepository {
        var estadoActualizado: String? = null
        override fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>> = flowOf(facturas)
        override fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>> = flowOf(lecturas)
        override fun obtenerTodasLasLecturasFlow(): Flow<List<Lectura>> = flowOf(lecturas)
        override suspend fun guardarFactura(factura: FacturaGeneral): Pair<Boolean, String?> = Pair(true, null)
        override suspend fun guardarLectura(lectura: Lectura): Pair<Boolean, String?> = Pair(true, null)
        override suspend fun actualizarEstadoPago(
            idLectura: String,
            nuevoEstado: String,
            fechaPago: Long?,
            metodoPago: String?,
        ): Pair<Boolean, String?> {
            estadoActualizado = nuevoEstado
            return Pair(true, null)
        }
    }

    private class FakeInquilinoRepository(
        private val submedidores: List<Submedidor> = emptyList(),
        private val inquilinos: List<Inquilino> = emptyList(),
    ) : IInquilinoRepository {
        override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> = flowOf(inquilinos)
        override fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> = flowOf(submedidores)
        override fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = flowOf(null)
        override suspend fun guardarInquilinoYSubmedidor(
            nombreCompleto: String,
            telefono: String,
            nombreEspacio: String,
            esAreaComun: Boolean,
            pagaAreaComun: Boolean,
        ): Pair<Boolean, String?> = Pair(true, null)

        override suspend fun guardarPerfilPropietario(nombre: String, numero: String, diaLimite: Int): Pair<Boolean, String?> = Pair(true, null)
        override fun actualizarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }

        override fun eliminarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }
    }

    @Test
    fun init_emiteEstadoExitoConResultadoProcesado() = runTest {
        val facturas = listOf(
            FacturaGeneral(idFactura = "f1", mesPeriodo = "Agosto 2026", kwhTotalesRecibo = 100.0, montoTotalSoles = 100.0),
            FacturaGeneral(idFactura = "f2", mesPeriodo = "Septiembre 2026", kwhTotalesRecibo = 200.0, montoTotalSoles = 200.0),
        )

        val fakeLecturaRepo = FakeLecturaRepository(facturas = facturas)
        val fakeInquilinoRepo = FakeInquilinoRepository()

        val viewModel = HistorialConsumoViewModel(fakeLecturaRepo, fakeInquilinoRepo, testDispatcher)

        testDispatcher.scheduler.advanceUntilIdle()

        val estadoActual = viewModel.uiState.value
        assertTrue(estadoActual is HistorialConsumoUiState.Exito)

        val exito = estadoActual as HistorialConsumoUiState.Exito
        assertEquals(150.0, exito.resultado.promedioKwhMensual, 0.001)
        assertEquals("Septiembre 2026", exito.resultado.mesMayorConsumo)
        assertEquals(2, exito.resultado.puntosInmuebleMatriz.size)
    }
}
