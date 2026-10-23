package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LecturaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLectura(lectura: Lectura)

    @Query("SELECT * FROM lecturas")
    fun obtenerTodasLasLecturas(): Flow<List<Lectura>>

    @Query("SELECT * FROM lecturas WHERE mesPeriodo = :mes")
    fun obtenerLecturasPorMes(mes: String): Flow<List<Lectura>>

    @Query("UPDATE lecturas SET estadoPago = :estadoPago, fechaPago = :fechaPago, metodoPago = :metodoPago WHERE idLectura = :idLectura")
    suspend fun actualizarEstadoPago(idLectura: String, estadoPago: String, fechaPago: Long?, metodoPago: String?)
}
