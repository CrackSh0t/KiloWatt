# Propuesta de Diseño Técnico: Integración de Hilt (Inyección de Dependencias)

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Propuesta de Arquitectura en Revisión para el Tech Lead  

---

## 1. Resumen Ejecutivo

Habiendo completado con éxito al 100% la migración de todas las pantallas de Kilowatt hacia el patrón **MVVM + StateFlow** (Fase 1), el siguiente paso lógico en la evolución arquitectónica es automatizar la provisión de dependencias mediante **Dagger Hilt**.

Actualmente, las interfaces de repositorio y servicios (`ILecturaRepository`, `IInquilinoRepository`, `IAuthRepository`, `ISettingsManager`) ya se encuentran completamente abstraídas, lo que facilita de forma directa la anotación con Hilt sin necesidad de alterar la lógica de negocio ni las 36 pruebas unitarias existentes.

---

## 2. Objetivos de la Integración

1. **Eliminar fábricas manuales (`ViewModelProvider.Factory`):** Reemplazar la instanciación manual de ViewModels y repositorios por inyección de constructores respaldada por el contenedor de dependencias de Hilt (`@HiltViewModel` y `@Inject`).
2. **Ciclo de Vida de Aplicación y Actividades:** Gestionar de manera centralizada la persistencia y alcance (*scopes*) de Singletons y dependencias vinculadas a `Activity`.
3. **Mantenibilidad y Escalabilidad:** Reducir el acoplamiento y simplificar la incorporación de nuevas dependencias en futuros módulos (como Room o WorkManager).

---

## 3. Configuración de Dependencias y Gradle

### A. Adición de Plugins y Librerías en `libs.versions.toml`
```toml
[versions]
hilt = "2.51.1"

[libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### B. Aplicación en `../../../app/build.gradle.kts`
- Aplicar el plugin `kotlin-kapt` (o KSP según configuración del proyecto).
- Registrar las dependencias:
  ```kotlin
  implementation(libs.hilt.android)
  kapt(libs.hilt.compiler)
  ```

---

## 4. Arquitectura de Módulos Hilt y Proveedores (@Module)

Dado que las clases concretas (`LecturaRepository`, `InquilinoRepository`, `AuthRepository`, `SettingsManager`) implementan interfaces, utilizaremos módulos Hilt con `@Binds` y `@Provides` en el componente `@SingletonComponent`.

### A. `RepositoryModule.kt`
Este módulo provee las implementaciones concretas para las interfaces de repositorio y autenticación:

```kotlin
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
        impl: LecturaRepository
    ): ILecturaRepository

    @Binds
    @Singleton
    abstract fun bindInquilinoRepository(
        impl: InquilinoRepository
    ): IInquilinoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepository
    ): IAuthRepository
}
```

### B. `AppModule.kt`
Este módulo provee dependencias del sistema como `ISettingsManager` que requieren el contexto de aplicación (`@ApplicationContext`):

```kotlin
package com.example.kilowatt.di

import android.content.Context
import com.example.kilowatt.util.ISettingsManager
import com.example.kilowatt.util.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    val bindSettingsManager(
        @ApplicationContext context: Context
    ): ISettingsManager {
        return SettingsManager(context)
    }
}
```

---

## 5. Anotación en Repositorios y ViewModels

### A. Repositorios (`@Inject constructor`)
Cada repositorio incorporará un constructor inyectable:
```kotlin
class LecturaRepository @Inject constructor() : ILecturaRepository { ... }
class InquilinoRepository @Inject constructor() : IInquilinoRepository { ... }
class AuthRepository @Inject constructor() : IAuthRepository { ... }
```

### B. ViewModels (`@HiltViewModel`)
Se eliminan las clases `Factory` manuales y se reemplazan por `@HiltViewModel`:
```kotlin
@HiltViewModel
class HistorialConsumoViewModel @Inject constructor(
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository
) : ViewModel() { ... }
```

### C. Application y Activities (`@HiltAndroidApp` y `@AndroidEntryPoint`)
1. **`KilowattApp.kt` (Application clase):**
   ```kotlin
   @HiltAndroidApp
   class KilowattApp : Application()
   ```
2. **Activities:**
   ```kotlin
   @AndroidEntryPoint
   class HistorialConsumoActivity : AppCompatActivity() {
       private val viewModel: HistorialConsumoViewModel by viewModels()
       ...
   }
   ```

---

## 6. Impacto en Pruebas Unitarias

La integración de Hilt **no afecta** las 36 pruebas unitarias existentes, ya que los ViewModels continuarán aceptando dependencias directas mediante sus constructores normales para tests aislados, mientras que Hilt se encarga de la instanciación en tiempo de ejecución de la aplicación Android.

---

## 7. Recomendación Final

### **APROBAR PROPUESTA PARA INICIAR IMPLEMENTACIÓN HILT**
Se solicita la revisión y visto bueno del Tech Lead para proceder con la adición de las dependencias de Hilt en el proyecto e iniciar la refactorización modular.
