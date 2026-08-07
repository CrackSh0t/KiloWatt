package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LecturaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLectura(lectura: Lectura)

    @Query("SELECT * FROM lecturas WHERE mesPeriodo = :mes")
    fun obtenerLecturasPorMes(mes: String): kotlinx.coroutines.flow.Flow<List<Lectura>>
}