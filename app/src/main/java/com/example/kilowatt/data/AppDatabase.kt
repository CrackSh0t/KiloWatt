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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_operations (
                id TEXT NOT NULL PRIMARY KEY,
                uid TEXT NOT NULL,
                entityType TEXT NOT NULL,
                operationType TEXT NOT NULL,
                documentId TEXT NOT NULL,
                payloadJson TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'PENDING',
                errorReason TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_operations_uid_status ON pending_operations(uid, status)")
    }
}

@Database(
    entities = [Inquilino::class, Submedidor::class, FacturaGeneral::class, Lectura::class, PendingOperationEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inquilinoDao(): InquilinoDao
    abstract fun submedidorDao(): SubmedidorDao
    abstract fun facturaGeneralDao(): FacturaGeneralDao
    abstract fun lecturaDao(): LecturaDao
    abstract fun pendingOperationDao(): PendingOperationDao

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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}