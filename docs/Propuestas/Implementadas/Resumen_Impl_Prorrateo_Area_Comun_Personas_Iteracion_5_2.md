# Resumen de Implementación: Persistencia, Migración Room v6 y Sincronización Firestore (Iteración 5.2)

**Fecha:** Octubre 2026  
**Responsable:** Agente Desarrollador (`@SKILL_DESARROLLO.md`)  
**Estado:** Implementado y Validado (100% Tests en Verde)  
**Propuesta Técnica Base:** [`Propuesta_Prorrateo_Area_Comun_Personas.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Propuesta_Prorrateo_Area_Comun_Personas.md)  
**Informe de Auditoría Previo:** [`Informe_Auditoria_Prorrateo_Area_Comun_Personas_Iteracion_5_1.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Informe_Auditoria_Prorrateo_Area_Comun_Personas_Iteracion_5_1.md)  

---

## 1. Objetivo y Alcance

Implementar la persistencia local y remota para la nueva propiedad de ocupación (`numeroPersonas`) en la entidad `Inquilino` y la preferencia global de prorrateo (`tipoProrrateoComun`), garantizando una migración explícita en SQLite sin destrucción de datos (`MIGRATION_5_6`), sincronización reactiva en Firestore y fallback defensivo ante documentos históricos.

---

## 2. Modificaciones Realizadas

### 2.1. Migración SQLite y Configuración de Room
1. **[`AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt) [MODIFICADO]:**
   - Definición de la migración explícita de esquema:
     ```kotlin
     val MIGRATION_5_6 = object : Migration(5, 6) {
         override fun migrate(db: SupportSQLiteDatabase) {
             db.execSQL("ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1")
         }
     }
     ```
   - Incremento de la versión de `@Database` a `6`.
   - Registro de `.addMigrations(MIGRATION_5_6)` en el companion object `AppDatabase.getDatabase`.
2. **[`DatabaseModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt) [MODIFICADO]:**
   - Registro de `.addMigrations(MIGRATION_5_6)` en el provider de Hilt `provideAppDatabase(...)`, asegurando que la inyección de dependencias ejecute la migración controlada.

### 2.2. Repositorio y Sincronización Remota (SSOT)
1. **[`IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt) [MODIFICADO]:**
   - Extensión de `guardarInquilinoYSubmedidor` con el parámetro `numeroPersonas: Int = 1`.
   - Extensión de `guardarPerfilPropietario` con el parámetro `tipoProrrateoComun: String = "EQUITY"`.
2. **[`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt) [MODIFICADO]:**
   - `guardarInquilinoYSubmedidor`: Persistencia de `numeroPersonas.coerceAtLeast(1)` tanto en `inquilinoDao.insertarInquilino` (Room) como en `inqRef.document().set(inqTemp)` (Firestore).
   - `actualizarInquilinoYSubmedidor`: Sanitización defensiva de `numeroPersonas` (`.coerceAtLeast(1)`) y guardado atómico en SQLite y Firestore.
   - `sincronizarInquilinosRemotos`: Mapeo de documentos desde Firestore aplicando `.copy(numeroPersonas = inq.numeroPersonas.coerceAtLeast(1))` para garantizar que documentos antiguos sin este campo o con valor 0 se normalicen a 1 habitante al guardarse en Room.
   - `guardarPerfilPropietario`: Inclusión de `"tipoProrrateoComun"` en el payload del documento `usuarios/{uid}/perfil/datos_pago`.

### 2.3. Preferencias de Configuración
1. **[`ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt) [MODIFICADO]:**
   - Adición de la propiedad `var tipoProrrateoComun: String`.
2. **[`SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt) [MODIFICADO]:**
   - Implementación respaldada en SharedPreferences con clave `"tipo_prorrateo_comun"` y valor por defecto `"EQUITY"`.
3. **[`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) [MODIFICADO]:**
   - Sincronización bidireccional en el diálogo de perfil con Firestore y SharedPreferences.

### 2.4. Pruebas Unitarias
1. **[`AppDatabaseMigrationTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt) [CREADO]:**
   - `migration5to6_versionesCorrectas`: Valida que `startVersion == 5` y `endVersion == 6`.
   - `migration5to6_ejecutaSentenciaAlterTableConColumnaNumeroPersonas`: Verifica mediante `Proxy` dinámico de `SupportSQLiteDatabase` que la sentencia DDL ejecutada sea exactamente la requerida.
   - `inquilino_valorPorDefectoNumeroPersonas_esUno`: Asegura el valor por defecto en la entidad.
2. **Actualización de Fakes de Test:**
   - Sincronización de `FakeInquilinoRepository` y `FakeSettingsManager` en [`ConfiguracionViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt), [`HistorialConsumoViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/HistorialConsumoViewModelTest.kt), [`LecturasViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt) y [`ResumenCobrosViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt).

---

## 3. Resultados de Compilación y Validación

- **Gradle Build (`:app:assembleDebug`):** `BUILD SUCCESSFUL` (0 errores).
- **Gradle Unit Tests (`:app:testDebugUnitTest`):** **44 pruebas pasadas, 0 falladas, 0 omitidas** (100% en verde).

---

## 4. Archivos Involucrados

| Operación | Ruta del Archivo |
| :--- | :--- |
| **Modificado** | `app/src/main/java/com/example/kilowatt/data/AppDatabase.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/util/SettingsManager.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/MainActivity.kt` |
| **Modificado** | `app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt` |
| **Creado** | `app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt` |
| **Modificado** | `app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt` |
| **Modificado** | `app/src/test/java/com/example/kilowatt/HistorialConsumoViewModelTest.kt` |
| **Modificado** | `app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt` |
| **Modificado** | `app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt` |
| **Creado** | `docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md` |
