package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmedidorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSubmedidor(submedidor: Submedidor)

    @Query("SELECT * FROM submedidores")
    fun obtenerTodosLosSubmedidores(): Flow<List<Submedidor>>

    @androidx.room.Delete
    suspend fun eliminarSubmedidor(submedidor: Submedidor)
}