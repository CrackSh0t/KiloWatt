# Registro y Control de Deuda Técnica - Kilowatt

**Fecha de creación:** Octubre 2026  
**Mantenido por:** Agente Auditor (`@SKILL_AUDITOR.md`)  
**Supervisado por:** Agente Arquitecto (`@SKILL_ARQUITECTO.md`)

---

## 1. Propósito
Este documento centraliza los hallazgos de deuda técnica, vulnerabilidades menores, refactorizaciones pendientes y oportunidades de optimización identificadas durante las auditorías técnicas. Sirve de insumo para que el Arquitecto planifique y priorice las futuras fases e iteraciones en el [`ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md).

---

## 2. Clasificación de Severidad
- **Crítica:** Compromete la estabilidad, seguridad o integridad de datos. Debe corregirse de inmediato.
- **Alta:** Afecta significativamente la experiencia del usuario o el rendimiento.
- **Media:** Riesgo moderado o falta de optimización; no bloquea el release.
- **Baja / Observación:** Mejora recomendada de estilo, legibilidad o arquitectura.

---

## 3. Inventario de Deuda Técnica Activa

La propuesta detallada de ejecución incremental está documentada en [`Plan_Iterativo_Deuda_Tecnica.md`](Propuestas/Plan_Iterativo_Deuda_Tecnica.md). Este archivo mantiene el índice resumido; el documento enlazado contiene evidencia, dependencias, criterios de aceptación y estrategia de despliegue.

### 3.1. Arquitectura y Capa de Presentación
- [ ] **[Severidad: Baja] Migración paulatina de UI a Jetpack Compose (Fase Futura):**
  - *Origen:* Auditoría de Rediseño UI / Evaluación Estratégica.
  - *Descripción:* La interfaz utiliza Views clásicos XML. Aunque funciona con MVVM + ViewBinding, migrar progresivamente a Jetpack Compose reduciría el código boilerplate de layouts.
  - *Estado:* Planificada para evaluación en versiones futuras.

### 3.2. Capa de Datos y Persistencia
- [ ] **[Severidad: Media] Estrategia de Migración de Esquema Room (Migrations explícitas):**
  - *Origen:* Auditoría de Seguridad y Persistencia.
  - *Descripción:* `AppDatabase` utiliza migraciones explícitas (`Migration(from, to)`) y no permite migraciones destructivas, protegiendo la outbox y los datos locales ante cambios de esquema.
  - *Estado:* Pendiente previa a lanzamiento masivo en producción.

- [ ] **[Severidad: Alta] Errores silenciados y contratos ambiguos:**
  - *Origen:* Repositorios, `AuthRepository` y ViewModels.
  - *Descripción:* Excepciones ignoradas, `Flow` que emite listas vacías ante fallos y `Pair<Boolean, String?>` que mezcla resultado local, remoto y error.
  - *Estado:* Prioridad P0; requiere resultados tipados, logging controlado y estados de UI explícitos.

### 3.3. Dominio y Modelos de Negocio
- [ ] **[Severidad: Observación / Baja] Cardinalidad de Factor de Ocupación por Espacio vs Inquilino:**
  - *Origen:* Auditoría Técnica Iteración 5.1 (Prorrateo Ponderado).
  - *Descripción:* `numeroPersonas` se almacena a nivel de `Inquilino`. Si un inquilino es titular de múltiples submedidores con `pagaAreaComun = true`, computará su número de personas en cada uno.
  - *Estado:* Observación registrada. Se evaluará si en el futuro se requiere permitir una sobreescritura de ocupantes por submedidor individual.

### 3.4. Seguridad, Operación y Mantenibilidad
- [ ] **[Severidad: Alta] Política de backup y protección de preferencias:**
  - *Descripción:* Las reglas de backup son plantillas y `SettingsManager` guarda datos de cobro en `SharedPreferences` sin una política de cifrado/restauración definida.
  - *Estado:* Prioridad P1; definir exclusiones, restauración por cuenta y almacenamiento protegido.

- [ ] **[Severidad: Media] Acoplamiento directo a Firebase y casts a implementaciones:**
  - *Descripción:* Los repositorios crean instancias globales de Firebase y `MainActivity` convierte `ISettingsManager` a `SettingsManager`, reduciendo la testabilidad.
  - *Estado:* Prioridad P1; migrar a dependencias provistas por Hilt y contratos de interfaz completos.

- [ ] **[Severidad: Media] Generación de PDF sin paginación ni error explícito:**
  - *Descripción:* `PdfGenerator` usa una sola página para el reporte general, trunca potencialmente el contenido y usa `printStackTrace()` ante fallos.
  - *Estado:* Prioridad P1; añadir paginación, límites de texto y resultados de error observables.

- [ ] **[Severidad: Media] Configuración de release y documentación divergentes:**
  - *Descripción:* Release no tiene minificación habilitada, el versionado permanece en `1.0`, JVM/SDK requieren revisión y la documentación declara capacidades o versiones que no coinciden con el código actual.
  - *Estado:* Prioridad P2; corregir después de estabilizar persistencia y sincronización.

---

## 4. Histórico de Deuda Técnica Saneada

| ID | Descripción | Severidad | Módulo Afectado | Fecha de Solución | Resumen / Solución Aplicada |
| :---: | :--- | :---: | :---: | :---: | :--- |
| DT-01 | Crash en Logout por listeners Firestore en ejecución | Crítica | `AuthRepository` | Oct 2026 | `cerrarSesion()` suspendida con cancelación previa de listeners y purga de Room. |
| DT-02 | Fuga de datos Multi-tenant en Room | Alta | `AuthRepository` | Oct 2026 | Purga atómica de SQLite con `database.clearAllTables()` y de `SettingsManager`. |
| DT-03 | Incompatibilidad KSP2 con Room Compiler | Alta | Build System | Oct 2026 | Actualización de Room a v2.8.5 en `libs.versions.toml`. |
| DT-04 | Duplicados temporales en UI de Inquilinos | Media | `InquilinoRepository` | Oct 2026 | Pre-generación de IDs unificados de Firestore previo al guardado en Room. |
| DT-05 | Formularios heterogéneos en UI | Baja | Layouts XML | Oct 2026 | Homogeneización a `TextInputLayout.OutlinedBox` y TopAppBars unificadas. |
| DT-06 | Contrato de estados y errores ambiguo | Crítica | Repositorios, ViewModels, `OperationResult` | Nov 2026 | Introducción de `OperationResult<T>` (`Synced`, `PendingSync`, `Failed`), propagación explícita de errores y logging contextual sin fallos silenciosos. |
| DT-07 | Sincronización offline-first incompleta | Crítica | Room, outbox, `SyncWorker` | Nov 2026 | Outbox persistente Room v7, transacciones atómicas, `@HiltWorker`, reintentos con WorkManager y recuperación de operaciones `PROCESSING`. |
| DT-08 | Reconciliación incompleta de bajas remotas | Alta | Listeners Firestore, Room, outbox | Nov 2026 | `RemoteChangeReconciler`, procesamiento de `ADDED`/`MODIFIED`/`REMOVED`, borrado por ID, validación multi-tenant y protección de operaciones locales pendientes. |
