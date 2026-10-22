package com.example.kilowatt.di

import com.example.kilowatt.data.AuthRepository
import com.example.kilowatt.data.IAuthRepository
import com.example.kilowatt.data.IInquilinoRepository
import com.example.kilowatt.data.ILecturaRepository
import com.example.kilowatt.data.InquilinoRepository
import com.example.kilowatt.data.LecturaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLecturaRepository(
        impl: LecturaRepository,
    ): ILecturaRepository

    @Binds
    @Singleton
    abstract fun bindInquilinoRepository(
        impl: InquilinoRepository,
    ): IInquilinoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepository,
    ): IAuthRepository
}
