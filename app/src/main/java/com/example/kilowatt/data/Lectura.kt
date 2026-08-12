package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecturas",
    indices = [Index(value = ["idSubmedidor", "mesPeriodo"], unique = true)])
data class Lectura(
    @PrimaryKey(autoGenerate = true)
    val idLectura: Int = 0,
    val idSubmedidor: Int,
    val mesPeriodo: String,
    val lecturaAnterior: Double,
    val lecturaActual: Double,
    val consumoKwh: Double,
    val montoPagarSoles: Double
)
