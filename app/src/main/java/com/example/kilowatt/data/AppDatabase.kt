package com.example.kilowatt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Inquilino::class, Submedidor::class, FacturaGeneral::class, Lectura::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inquilinoDao(): InquilinoDao
    abstract fun submedidorDao(): SubmedidorDao
    abstract fun facturaGeneralDao(): FacturaGeneralDao
    abstract fun lecturaDao(): LecturaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kilowatt_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}