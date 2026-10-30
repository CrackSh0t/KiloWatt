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

### 3.1. Arquitectura y Capa de Presentación
- [ ] **[Severidad: Baja] Migración paulatina de UI a Jetpack Compose (Fase Futura):**
  - *Origen:* Auditoría de Rediseño UI / Evaluación Estratégica.
  - *Descripción:* La interfaz utiliza Views clásicos XML. Aunque funciona con MVVM + ViewBinding, migrar progresivamente a Jetpack Compose reduciría el código boilerplate de layouts.
  - *Estado:* Planificada para evaluación en versiones futuras.

### 3.2. Capa de Datos y Persistencia
- [ ] **[Severidad: Media] Estrategia de Migración de Esquema Room (Migrations explícitas):**
  - *Origen:* Auditoría de Seguridad y Persistencia.
  - *Descripción:* `AppDatabase` cuenta con `fallbackToDestructiveMigration()`. Para entornos de producción con datos acumulados por años, se debe reemplazar por instancias de `Migration(from, to)` explícitas.
  - *Estado:* Pendiente previa a lanzamiento masivo en producción.

### 3.3. Dominio y Modelos de Negocio
- [ ] **[Severidad: Observación / Baja] Cardinalidad de Factor de Ocupación por Espacio vs Inquilino:**
  - *Origen:* Auditoría Técnica Iteración 5.1 (Prorrateo Ponderado).
  - *Descripción:* `numeroPersonas` se almacena a nivel de `Inquilino`. Si un inquilino es titular de múltiples submedidores con `pagaAreaComun = true`, computará su número de personas en cada uno.
  - *Estado:* Observación registrada. Se evaluará si en el futuro se requiere permitir una sobreescritura de ocupantes por submedidor individual.

---

## 4. Histórico de Deuda Técnica Saneada

| ID | Descripción | Severidad | Módulo Afectado | Fecha de Solución | Resumen / Solución Aplicada |
| :---: | :--- | :---: | :---: | :---: | :--- |
| DT-01 | Crash en Logout por listeners Firestore en ejecución | Crítica | `AuthRepository` | Oct 2026 | `cerrarSesion()` suspendida con cancelación previa de listeners y purga de Room. |
| DT-02 | Fuga de datos Multi-tenant en Room | Alta | `AuthRepository` | Oct 2026 | Purga atómica de SQLite con `database.clearAllTables()` y de `SettingsManager`. |
| DT-03 | Incompatibilidad KSP2 con Room Compiler | Alta | Build System | Oct 2026 | Actualización de Room a v2.8.5 en `libs.versions.toml`. |
| DT-04 | Duplicados temporales en UI de Inquilinos | Media | `InquilinoRepository` | Oct 2026 | Pre-generación de IDs unificados de Firestore previo al guardado en Room. |
| DT-05 | Formularios heterogéneos en UI | Baja | Layouts XML | Oct 2026 | Homogeneización a `TextInputLayout.OutlinedBox` y TopAppBars unificadas. |
