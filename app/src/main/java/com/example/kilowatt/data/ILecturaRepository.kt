package com.example.kilowatt.data

import kotlinx.coroutines.flow.Flow

interface ILecturaRepository {
    fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>>
    fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>>
    fun obtenerTodasLasLecturasFlow(): Flow<List<Lectura>>
    suspend fun guardarFactura(factura: FacturaGeneral): Pair<Boolean, String?>
    suspend fun guardarLectura(lectura: Lectura): Pair<Boolean, String?>
    suspend fun actualizarEstadoPago(
        idLectura: String,
        nuevoEstado: String,
        fechaPago: Long? = null,
        metodoPago: String? = null,
    ): Pair<Boolean, String?>
}
