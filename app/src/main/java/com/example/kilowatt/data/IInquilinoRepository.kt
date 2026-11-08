package com.example.kilowatt.data

import com.example.kilowatt.domain.OperationResult
import kotlinx.coroutines.flow.Flow

interface IInquilinoRepository {
    fun obtenerInquilinosFlow(): Flow<List<Inquilino>>
    fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>>
    fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?>
    suspend fun guardarInquilinoYSubmedidor(
        nombreCompleto: String,
        telefono: String,
        nombreEspacio: String,
        esAreaComun: Boolean,
        pagaAreaComun: Boolean,
        numeroPersonas: Int = 1,
    ): OperationResult<Unit>
    suspend fun guardarPerfilPropietario(
        nombre: String,
        numero: String,
        diaLimite: Int,
        tipoProrrateoComun: String = "EQUITY",
    ): OperationResult<Unit>
    suspend fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
    ): OperationResult<Unit>
    suspend fun eliminarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
    ): OperationResult<Unit>
    fun cancelarListeners()
}
