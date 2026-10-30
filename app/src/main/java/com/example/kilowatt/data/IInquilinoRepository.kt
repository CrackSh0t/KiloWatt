package com.example.kilowatt.data

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
    ): Pair<Boolean, String?>
    suspend fun guardarPerfilPropietario(
        nombre: String,
        numero: String,
        diaLimite: Int,
        tipoProrrateoComun: String = "EQUITY",
    ): Pair<Boolean, String?>
    fun actualizarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    )
    fun eliminarInquilinoYSubmedidor(
        inquilino: Inquilino?,
        submedidor: Submedidor,
        onComplete: (Boolean, String?) -> Unit,
    )
    fun cancelarListeners()
}
