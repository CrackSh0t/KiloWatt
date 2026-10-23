# Propuesta de Diseño Técnico: Soporte Offline y Consistencia de Datos (Fase 2 - Room + Firestore + SSOT)

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Propuesta de Arquitectura en Revisión para el Tech Lead  

---

## 1. Resumen Ejecutivo

Con la **Fase 1 completada al 100%** (MVVM + StateFlow + Dagger Hilt DI + 36 pruebas unitarias en verde), Kilowatt cuenta con una arquitectura de presentación robusta y desacoplada. Sin embargo, actualmente los repositorios dependen en tiempo real y exclusivamente de Cloud Firestore, lo que genera interrupciones en zonas con conectividad limitada o nula.

El objetivo de la **Fase 2** es implementar el patrón **Single Source of Truth (SSOT)** utilizando **Room** como caché local de persistencia primaria y **Cloud Firestore** como fuente remota de sincronización, orquestado mediante **Dagger Hilt** y respaldado por **WorkManager** para operaciones pendientes en modo offline.

---

## 2. Arquitectura Single Source of Truth (SSOT)

### A. Flujo de Datos Híbrido (Room + Firestore)
Para cada repositorio (`LecturaRepository`, `InquilinoRepository`), la lectura de datos hacia la UI se realizará primordialmente desde la base de datos local Room a través de flujos reactivos (`Flow<List<T>>`), garantizando disponibilidad instantánea sin red.

```
┌─────────────────────────────────────────────────────────────┐
│                        ViewModel                            │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼ (Observa Flow)
┌─────────────────────────────────────────────────────────────┐
│                          Repositorio                        │
│             (ILecturaRepository / IInquilinoRepository)     │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
                ▼ (Fuente Local - SSOT)       ▼ (Fuente Remota)
        ┌───────────────┐             ┌───────────────┐
        │   Room DAO    │◄──Sincroniza──│  Cloud        │
        │  (SQLite)     │             │  Firestore    │
        └───────────────┘             └───────────────┘
```

1. **Lectura de Datos:** La UI se suscribe al `Flow` expuesto por el DAO de Room.
2. **Sincronización Remota (Network Bound Resource):** El repositorio inicia un listener o fetch de Firestore para actualizar la base de datos local en segundo plano, la cual propagará automáticamente el cambio a la UI de forma transparente.
3. **Escritura (Escritura Offline-First):** Las operaciones de escritura (`guardarLectura`, `guardarFactura`, `actualizarEstadoPago`) se ejecutan primero en Room localmente (marcando estado de sincronización si es necesario) y luego se intentan sincronizar con Firestore. Si no hay red, WorkManager encola la tarea para reintentar al recuperar conexión.

---

## 3. Inyección de Dependencias con Hilt (`DatabaseModule.kt`)

Se proveerá `AppDatabase` y los DAOs a través de un nuevo módulo Hilt instalado en `SingletonComponent`:

```kotlin
package com.example.kilowatt.di

import android.content.Context
import androidx.room.Room
import com.example.kilowatt.data.AppDatabase
import com.example.kilowatt.data.InquilinoDao
import com.example.kilowatt.data.LecturaDao
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
            "kilowatt_db",
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideLecturaDao(database: AppDatabase): LecturaDao {
        return database.lecturaDao()
    }

    @Provides
    @Singleton
    fun provideInquilinoDao(database: AppDatabase): InquilinoDao {
        return database.inquilinoDao()
    }
}
```

---

## 4. Estrategia de Mapeo y Sincronización

1. **Entidades Room (`@Entity`) vs Modelos de Dominio:**
   - Se mantendrán las entidades existentes en Room y se crearán funciones de extensión de mapeo (`toEntity()`, `toDomain()`) para convertir sin acoplamiento entre los modelos de base de datos local y los objetos serializados en Firestore.
2. **WorkManager para Sincronización Diferida:**
   - Se implementará un `SyncWorker` inyectado mediante Hilt (`@HiltWorker`) que escuche cambios de conectividad y ejecute las peticiones pendientes hacia Firestore cuando el dispositivo recupere internet.

---

## 5. Plan de Pruebas Unitarias

1. **Pruebas de Repositorio SSOT:**
   - Se validará que los repositorios respondan correctamente con datos locales cuando Firestore no esté accesible.
2. **Preservación de las 36 Pruebas Existentes:**
   - Las pruebas unitarias actuales (`ConsumoHistoricoCalculatorTest`, `PeriodoDateUtilsTest`, `PhoneUtilsTest`, ViewModels Tests) utilizan repositorios falsos (*Fake Repositories*), por lo que operan de manera totalmente aislada y continuarán pasando al 100% sin modificaciones en la capa de UI.

---

## 6. Recomendación Final

### **APROBAR PROPUESTA TÉCNICA (FASE 2)**
Se solicita la revisión y aprobación del Tech Lead para iniciar la implementación de `DatabaseModule`, la incorporación de DAOs en los repositorios actuales y el soporte offline.
