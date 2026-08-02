package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inquilinos")
data class Inquilino(
    @PrimaryKey(autoGenerate = true)
    val idInquilino: Int = 0,
    val nombreCompleto: String,
    val telefonoWhatsapp: String
)
