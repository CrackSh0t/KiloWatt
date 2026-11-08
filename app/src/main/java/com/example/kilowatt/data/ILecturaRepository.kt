package com.example.kilowatt.data

import com.example.kilowatt.domain.OperationResult
import kotlinx.coroutines.flow.Flow

interface ILecturaRepository {
    fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>>
    fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>>
    fun obtenerTodasLasLecturasFlow(): Flow<List<Lectura>>
    suspend fun guardarFactura(factura: FacturaGeneral): OperationResult<Unit>
    suspend fun guardarLectura(lectura: Lectura): OperationResult<Unit>
    suspend fun actualizarEstadoPago(
        idLectura: String,
        nuevoEstado: String,
        fechaPago: Long? = null,
        metodoPago: String? = null,
    ): OperationResult<Unit>
    fun cancelarListeners()
}
