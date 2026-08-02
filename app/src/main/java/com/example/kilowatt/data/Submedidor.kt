package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "submedidores")
data class Submedidor(
    @PrimaryKey(autoGenerate = true)
    val idMedidor: Int = 0,
    val nombreEspacio: String, // Ej: "Depa 101", "Baño 2do piso"
    val esAreaComun: Boolean = false,
    val idInquilinoTitular: Int? = null // ID del inquilino (puede ser null si es área común)
)
