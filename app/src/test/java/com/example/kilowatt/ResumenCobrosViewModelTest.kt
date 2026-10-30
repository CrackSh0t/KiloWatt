package com.example.kilowatt

import com.example.kilowatt.data.FacturaGeneral
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.data.Inquilino
import com.example.kilowatt.data.Lectura
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResumenCobrosViewModelTest {

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

        override fun cancelarListeners() {}
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
            numeroPersonas: Int,
        ): Pair<Boolean, String?> = Pair(true, null)

        override suspend fun guardarPerfilPropietario(
            nombre: String,
            numero: String,
            diaLimite: Int,
            tipoProrrateoComun: String,
        ): Pair<Boolean, String?> = Pair(true, null)

        override fun actualizarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }

        override fun eliminarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
            onComplete(true, null)
        }

        override fun cancelarListeners() {}
    }

    @Test
    fun init_cargaMesesYCalculaResumenCorrectamente() = runTest {
        val facturas = listOf(
            FacturaGeneral(idFactura = "f1", mesPeriodo = "Agosto 2026", kwhTotalesRecibo = 100.0, montoTotalSoles = 100.0),
        )
        val submedidores = listOf(
            Submedidor(idSubmedidor = "sub1", nombreEspacio = "Depa 101", idInquilinoTitular = "inq1"),
        )
        val inquilinos = listOf(
            Inquilino(idInquilino = "inq1", nombreCompleto = "Ana Gómez"),
        )
        val lecturas = listOf(
            Lectura(idLectura = "l1", idSubmedidor = "sub1", mesPeriodo = "Agosto 2026", lecturaAnterior = 0.0, lecturaActual = 100.0, consumoKwh = 100.0, montoPagarSoles = 100.0),
        )

        val fakeLecturaRepo = FakeLecturaRepository(facturas = facturas, lecturas = lecturas)
        val fakeInquilinoRepo = FakeInquilinoRepository(submedidores = submedidores, inquilinos = inquilinos)
        val settingsManager = object : ISettingsManager {
            override val diaLimitePago: Int = 5
            override val nombrePropietario: String = "Juan Pérez"
            override val numeroPago: String = "987654321"
            override var tipoProrrateoComun: String = "EQUITY"
            override fun limpiarConfiguracion() {}
        }

        val viewModel = ResumenCobrosViewModel(fakeLecturaRepo, fakeInquilinoRepo, settingsManager, testDispatcher)

        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(1, estado.listaMesesDisponibles.size)
        assertEquals("Agosto 2026", estado.mesSeleccionado)
        assertNotNull(estado.facturaActual)
        assertEquals(100.0, estado.montoTotalRecibo, 0.001)
        assertEquals(1, estado.detalles.size)
        assertEquals("Ana Gómez", estado.detalles[0].nombreInquilino)
    }

    @Test
    fun init_calculaResumenConProrrateoWeightedPersons_calculaCuotasProporcionales() = runTest {
        val facturas = listOf(
            FacturaGeneral(idFactura = "f1", mesPeriodo = "Agosto 2026", kwhTotalesRecibo = 100.0, montoTotalSoles = 100.0),
        )
        val submedidores = listOf(
            Submedidor(idSubmedidor = "subComun", nombreEspacio = "Bomba Agua", esAreaComun = true, pagaAreaComun = false),
            Submedidor(idSubmedidor = "sub1", nombreEspacio = "Depa 101", idInquilinoTitular = "inq1", pagaAreaComun = true),
            Submedidor(idSubmedidor = "sub2", nombreEspacio = "Depa 102", idInquilinoTitular = "inq2", pagaAreaComun = true),
        )
        val inquilinos = listOf(
            Inquilino(idInquilino = "inq1", nombreCompleto = "Familia Pérez", numeroPersonas = 4),
            Inquilino(idInquilino = "inq2", nombreCompleto = "Juan Solo", numeroPersonas = 2),
        )
        val lecturas = listOf(
            Lectura(idLectura = "lComun", idSubmedidor = "subComun", mesPeriodo = "Agosto 2026", lecturaAnterior = 0.0, lecturaActual = 30.0, consumoKwh = 30.0),
            Lectura(idLectura = "l1", idSubmedidor = "sub1", mesPeriodo = "Agosto 2026", lecturaAnterior = 0.0, lecturaActual = 35.0, consumoKwh = 35.0),
            Lectura(idLectura = "l2", idSubmedidor = "sub2", mesPeriodo = "Agosto 2026", lecturaAnterior = 0.0, lecturaActual = 35.0, consumoKwh = 35.0),
        )

        val fakeLecturaRepo = FakeLecturaRepository(facturas = facturas, lecturas = lecturas)
        val fakeInquilinoRepo = object : IInquilinoRepository {
            override fun obtenerInquilinosFlow(): Flow<List<Inquilino>> = flowOf(inquilinos)
            override fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>> = flowOf(submedidores)
            override fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?> = flowOf(
                mapOf("tipoProrrateoComun" to "WEIGHTED_PERSONS", "diaLimitePago" to 5)
            )
            override suspend fun guardarInquilinoYSubmedidor(
                nombreCompleto: String,
                telefono: String,
                nombreEspacio: String,
                esAreaComun: Boolean,
                pagaAreaComun: Boolean,
                numeroPersonas: Int
            ): Pair<Boolean, String?> = Pair(true, null)

            override suspend fun guardarPerfilPropietario(
                nombre: String,
                numero: String,
                diaLimite: Int,
                tipoProrrateoComun: String
            ): Pair<Boolean, String?> = Pair(true, null)

            override fun actualizarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
                onComplete(true, null)
            }

            override fun eliminarInquilinoYSubmedidor(inquilino: Inquilino?, submedidor: Submedidor, onComplete: (Boolean, String?) -> Unit) {
                onComplete(true, null)
            }

            override fun cancelarListeners() {}
        }

        val settingsManager = object : ISettingsManager {
            override val diaLimitePago: Int = 5
            override val nombrePropietario: String = "Propietario"
            override val numeroPago: String = "987654321"
            override var tipoProrrateoComun: String = "WEIGHTED_PERSONS"
            override fun limpiarConfiguracion() {}
        }

        val viewModel = ResumenCobrosViewModel(fakeLecturaRepo, fakeInquilinoRepo, settingsManager, testDispatcher)

        testDispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(2, estado.detalles.size)

        val det1 = estado.detalles.find { it.nombreInquilino == "Familia Pérez" }
        val det2 = estado.detalles.find { it.nombreInquilino == "Juan Solo" }

        assertNotNull(det1)
        assertNotNull(det2)

        // Total común = 30.0 Soles. 4 + 2 = 6 personas total.
        // Familia Pérez (4 personas) -> 20.0 Soles de área común
        // Juan Solo (2 personas) -> 10.0 Soles de área común
        assertEquals(20.0, det1?.montoAreaComunSoles ?: 0.0, 0.001)
        assertEquals(4, det1?.numeroPersonas)

        assertEquals(10.0, det2?.montoAreaComunSoles ?: 0.0, 0.001)
        assertEquals(2, det2?.numeroPersonas)
    }
}
