package com.example.kilowatt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1")
    }
}

@Database(
    entities = [Inquilino::class, Submedidor::class, FacturaGeneral::class, Lectura::class],
    version = 6,
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
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}