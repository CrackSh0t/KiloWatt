package com.example.kilowatt.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InquilinoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarInquilino(inquilino: Inquilino): Long

    @Query("SELECT * FROM inquilinos ORDER BY nombreCompleto ASC")
    fun obtenerTodosLosInquilinos(): Flow<List<Inquilino>>
}