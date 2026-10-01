package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inquilinos")
data class Inquilino(
    @PrimaryKey val idInquilino: String = "",
    val nombreCompleto: String = "",
    val telefonoWhatsapp: String = ""
)
