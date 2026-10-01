package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecturas",
    indices = [Index(value = ["idSubmedidor", "mesPeriodo"], unique = true)])
data class Lectura(
    @PrimaryKey val idLectura: String = "",
    val idSubmedidor: String = "",
    val mesPeriodo: String = "",
    val lecturaAnterior: Double = 0.0,
    val lecturaActual: Double = 0.0,
    val consumoKwh: Double = 0.0,
    val montoPagarSoles: Double = 0.0
)
