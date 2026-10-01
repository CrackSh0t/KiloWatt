package com.example.kilowatt.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "submedidores")
data class Submedidor(
    @PrimaryKey val idSubmedidor: String = "",
    val nombreEspacio: String = "",
    val esAreaComun: Boolean = false,
    val pagaAreaComun: Boolean = true,
    val idInquilinoTitular: String? = null
)
