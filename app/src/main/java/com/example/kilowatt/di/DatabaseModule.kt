package com.example.kilowatt.di

import android.content.Context
import androidx.room.Room
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.FacturaGeneralDao
import com.example.kilowatt.data.InquilinoDao
import com.example.kilowatt.data.LecturaDao
import com.example.kilowatt.data.MIGRATION_5_6
import com.example.kilowatt.data.SubmedidorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kilowatt_database",
        )
            .addMigrations(MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLecturaDao(database: AppDatabase): LecturaDao {
        return database.lecturaDao()
    }

    @Provides
    @Singleton
    fun provideFacturaGeneralDao(database: AppDatabase): FacturaGeneralDao {
        return database.facturaGeneralDao()
    }

    @Provides
    @Singleton
    fun provideInquilinoDao(database: AppDatabase): InquilinoDao {
        return database.inquilinoDao()
    }

    @Provides
    @Singleton
    fun provideSubmedidorDao(database: AppDatabase): SubmedidorDao {
        return database.submedidorDao()
    }
}
