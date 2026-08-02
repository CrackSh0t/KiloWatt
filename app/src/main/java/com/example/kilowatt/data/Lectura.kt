package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lecturas")
data class Lectura(
    @PrimaryKey(autoGenerate = true)
    val idLectura: Int = 0,
    val idMedidor: Int,           // Relación con el Submedidor
    val mesPeriodo: String,       // Ej: "2026-08"
    val lecturaAnterior: Double,  // Valor del contador mes pasado
    val lecturaActual: Double     // Valor del contador este mes
)
