# Informe de Auditoría Técnica: Persistencia, Migración Room v6 y Sincronización Firestore (Iteración 5.2)

**Fecha:** Octubre 2026  
**Auditor:** Agente Auditor Técnico Independiente (`@SKILL_AUDITOR.md`)  
**Fase Auditada:** Fase 5 - Iteración 5.2 (Persistencia Local, Migración SQLite y Sincronización Remota)  
**Propuesta Técnica Evaluada:** [`Propuesta_Prorrateo_Area_Comun_Personas.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Propuesta_Prorrateo_Area_Comun_Personas.md)  
**Resumen de Implementación:** [`Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md)  
**Dictamen:** **APROBADO ✅**

---

## 1. Resumen Ejecutivo

Se realizó la auditoría técnica independiente de la **Iteración 5.2**, correspondiente a la capa de persistencia local (Room v6), migración de esquema SQLite, sincronización bidireccional en Firestore y configuración global en `SettingsManager`.

La auditoría verificó:
1. La correcta definición y ejecución de la migración de esquema SQLite `MIGRATION_5_6` sin pérdida ni destrucción de datos.
2. El registro consistente de la migración tanto en el companion object de `AppDatabase` como en el proveedor de inyección de dependencias con Hilt (`DatabaseModule`).
3. La propagación defensiva del campo `numeroPersonas` (mínimo 1) y `tipoProrrateoComun` ("EQUITY") en el repositorio de inquilinos y las preferencias globales.
4. La normalización de documentos históricos en Firestore sin el campo `numeroPersonas` mediante fallbacks automáticos al deserializar y sincronizar con Room.
5. La creación y aprobación de pruebas unitarias automatizadas especificas para la migración de base de datos y sincronización de fakes (44 pruebas en verde).

La implementación cumple con todos los requisitos de estabilidad, arquitectura SSOT (Single Source of Truth) y seguridad defensiva.

---

## 2. Alcance y Archivos Auditados

| Componente | Archivo | Dictamen Parcial |
| :--- | :--- | :---: |
| **Migración SQLite y Room** | [`../../../app/src/main/java/com/example/kilowatt/data/AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt) | Conforme ✅ |
| **Inyección de Dependencias Hilt** | [`../../../app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt) | Conforme ✅ |
| **Contrato de Repositorio** | [`../../../app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt) | Conforme ✅ |
| **Repositorio SSOT** | [`../../../app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt) | Conforme ✅ |
| **Contrato de Preferencias** | [`../../../app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt) | Conforme ✅ |
| **Gestor de Preferencias** | [`../../../app/src/main/java/com/example/kilowatt/util/SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt) | Conforme ✅ |
| **Sincronización en UI Principal** | [`../../../app/src/main/java/com/example/kilowatt/MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) | Conforme ✅ |
| **Prueba de Migración** | [`../../../app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt) | Conforme ✅ |
| **Documentación** | [`Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md) | Conforme ✅ |

---

## 3. Verificación de Puntos de Auditoría

### 3.1. Migración de Esquema SQLite (v5 ➔ v6)
- La sentencia DDL definida en `MIGRATION_5_6`:
  ```sql
  ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1
  ```
  se ejecuta correctamente alterando la tabla local sin requerir destrucción de registros.
- Se verificó que tanto `AppDatabase.getDatabase(...)` como `DatabaseModule.provideAppDatabase(...)` incluyen `.addMigrations(MIGRATION_5_6)`.
- La prueba unitaria [`AppDatabaseMigrationTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt) valida el cambio de versión (5 a 6), la sentencia de migración interceptada por proxy y el valor por defecto en la entidad.

### 3.2. Sincronización Remota y Normalización Defensiva
- En [`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt), los métodos de guardado y actualización fuerzan la sanitización:
  ```kotlin
  numeroPersonas = numeroPersonas.coerceAtLeast(1)
  ```
- En `sincronizarInquilinosRemotos()`, la transformación `.map { inq -> inq.copy(numeroPersonas = inq.numeroPersonas.coerceAtLeast(1)) }` blinda la inserción en Room ante documentos JSON/Firestore históricos que no tengan la clave `numeroPersonas` o que registren valores $0$ o negativos.
- `guardarPerfilPropietario` persiste correctamente el atributo `"tipoProrrateoComun"` en `usuarios/{uid}/perfil/datos_pago`.

### 3.3. Suite de Pruebas Unitarias
Se ejecutó la suite completa de pruebas unitarias:
```bash
./gradlew :app:testDebugUnitTest --rerun-tasks
```
**Resultado:** **44 pruebas pasadas, 0 falladas, 0 omitidas (100% de éxito)**.

---

## 4. Hallazgos Críticos
* **Ninguno.**

---

## 5. Hallazgos Altos
* **Ninguno.**

---

## 6. Hallazgos Medios
* **Ninguno.**

---

## 7. Hallazgos Bajos
* **Ninguno.**

---

## 8. Observaciones Técnicas (No Bloqueantes)

* **OBS-01: Preparación para la UI de Edición de Ocupantes (Iteración 5.3):**
  - *Contexto:* La capa de datos e inyección ya soportan la lectura y escritura de `numeroPersonas`. La captura de este valor en los formularios de la UI (`dialog_editar_inquilino.xml`, `activity_configuracion.xml`) y el selector de modalidad en la pantalla de perfil/ajustes se habilitarán en la **Iteración 5.3**.
* **OBS-02: Lectura de Preferencia en ResumenCobrosViewModel (Iteración 5.4):**
  - *Contexto:* `ISettingsManager` expone `tipoProrrateoComun`. En la **Iteración 5.4**, `ResumenCobrosViewModel` consumirá esta propiedad para pasarla explícitamente a `CobroCalculator.calcularResumenMensual`.

---

## 9. Riesgos Futuros y Consideraciones para las Siguientes Iteraciones

1. **Iteración 5.3 (UI de Configuración e Inquilinos):** Se deberán agregar los controles de entrada numérica para `numeroPersonas` con validación `coerceAtLeast(1)` en la UI y el selector de modalidad (`EQUITY` / `WEIGHTED_PERSONS`) en los ajustes.
2. **Iteración 5.4 (UI de Resumen, Tarjetas y Recibo PDF):** Se deberá actualizar `ResumenCobrosViewModel` para leer `settingsManager.tipoProrrateoComun` y adaptar el desglose en `PdfGenerator.kt`.

---

## 10. Recomendación Final

### Dictamen: **APROBADO ✅**

La implementación de la **Iteración 5.2** es sólida, defensiva y cumple rigurosamente con los requisitos de migración de base de datos, sincronización remota en Firestore, inyección de dependencias con Hilt y mantenimiento del 100% de pruebas unitarias en verde. Se autoriza el avance a la **Iteración 5.3**.
