# Roadmap y Hoja de Ruta - App Kilowatt ⚡

**Fecha de última revisión:** Noviembre 2026
**Responsable de Mantenimiento:** Agente Arquitecto (`@SKILL_ARQUITECTO.md`) / Agente Desarrollo (`@SKILL_DESARROLLO.md`)

---

## 🎯 Estado General del Roadmap

```
[Fase 1: MVVM & DI] ──► [Fase 2: SSOT Offline-First] ──► [Fase 3: Sellos & PDF] ──► [Fase 4: Rediseño M3] ──► [Fase 5: Prorrateo Personas] ──► [Fase 6: Métricas & Proyecciones]
     (100% ✅)                   (100% ✅)                    (100% ✅)                 (100% ✅)                    (100% ✅)                   (Planificada ⏳)
```

---

## 🚀 Fases e Iteraciones

### Fase 1: Refactorización Arquitectónica MVVM + Dagger Hilt (100% Completada)
- [x] **Iteración 1.1:** Configuración de Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `AppModule`, `RepositoryModule`).
- [x] **Iteración 1.2:** Migración de `HistorialConsumoActivity` a `HistorialConsumoViewModel` + `StateFlow`.
- [x] **Iteración 1.3:** Migración de `ResumenCobrosActivity` a `ResumenCobrosViewModel` + `StateFlow`.
- [x] **Iteración 1.4:** Migración de `LecturasActivity` a `LecturasViewModel` + `StateFlow`.
- [x] **Iteración 1.5:** Migración de `AuthActivity` y `ConfiguracionActivity` a ViewModels dedicados.
- [x] **Testing:** Cobertura de ViewModels con unit tests aislados (JUnit 4 + Coroutines Test).

---

### Fase 2: Arquitectura Offline-First SSOT (Room + Firestore + WorkManager) (100% Completada)
- [x] **Iteración 2.1:** Configuración de `DatabaseModule` inyectando `AppDatabase` y DAOs en Hilt.
- [x] **Iteración 2.2:** Refactorización Single Source of Truth (SSOT) en `LecturaRepository` (Lectura local en Room + Sincronización remota Firestore).
- [x] **Iteración 2.3:** Refactorización SSOT en `InquilinoRepository` y sincronización diferida en background con `SyncWorker` (WorkManager).
- [x] **Iteración 2.4:** Aislamiento de datos Multi-Tenant y purga atómica de Room y `SettingsManager` al cerrar sesión.
- [x] **DT-06 / Iteración 1:** Contrato `OperationResult<T>`, abortar Firestore ante fallo de Room, propagación de errores de Flow a la UI, logs estructurados y pruebas de `PendingSync`/`Failed`/fallo de lectura.
- [x] **DT-07 / Iteración 2:** Outbox persistente Room v7, transacciones atómicas, `@HiltWorker`, recuperación de `PROCESSING` y reintentos WorkManager con red conectada.
- [x] **DT-08 / Iteración 3:** Reconciliación Firestore–Room para `ADDED`, `MODIFIED` y `REMOVED`, protección de conflictos con la outbox y aislamiento por UID.

---

### Fase 3: Personalización de Comprobantes PDF y Sellos Visuales (100% Completada)
- [x] **Iteración 3.1:** Refactorización de `PdfGenerator` inyectando `ISettingsManager` para incluir perfil del propietario, QR/cuentas de pago y día límite.
- [x] **Iteración 3.2:** Implementación de sellos visuales emocionales de estado de pago (`PAGADO ✅`, `VENCIDO 🔴`, `PENDIENTE 🟡`) en los recibos PDF (A6/A5).
- [x] **Iteración 3.3:** Implementación de tarjetas distintivas con sellos de estado y detalles de auditoría en la UI de la app (`item_cobro.xml` / `CobroAdapter.kt`).

---

### Fase 4: Rediseño de Experiencia de Usuario M3 (100% Completada)
- [x] **Iteración 4.1:** Centro de Perfil del Propietario en `MainActivity` mediante `BottomSheetDialog` Material 3 y simplificación de `ConfiguracionActivity` enfocada en "Inquilinos y Medidores".
- [x] **Iteración 4.2:** Estandarización de `TopAppBar` M3 unificada con navegación `←` en todas las pantallas secundarias.
- [x] **Iteración 4.3:** Homogeneización de formularios a `TextInputLayout.OutlinedBox` y optimización de márgenes en `MPAndroidChart`.

---

### Fase 5: Prorrateo de Área Común por Ocupación / Personas (100% Completada)
- [x] **Iteración 5.1:** Dominio y Motor de Cálculo Ponderado (`TipoProrrateoComun`, `Inquilino.numeroPersonas`, `CobroCalculator.calcularResumenMensual` y pruebas en `CobroCalculatorTest`).
- [x] **Iteración 5.2:** Persistencia, Migración Room v6 (`MIGRATION_5_6`) y Sincronización Firestore (`InquilinoRepository`, `SettingsManager.tipoProrrateoComun`).
- [x] **Iteración 5.3:** UI de Configuración y Gestión de Inquilinos (`activity_configuracion`, `dialog_editar_inquilino`, `item_inquilino` con badges de ocupantes, y selector de modalidad en perfil).
- [x] **Iteración 5.4:** Visualización en Resumen de Cobros y Desglose en Comprobantes PDF (`ResumenCobrosViewModel`, `item_cobro`, `CobroAdapter` y `PdfGenerator`).

---

### Fase 6: Métricas Avanzadas, Proyecciones y Exportaciones (Preparada ⏳)
- [ ] **Iteración 6.1:** Definir y probar la proyección estimada del recibo del mes en curso basada en consumo diario, periodo transcurrido y costo por kWh.
- [ ] **Iteración 6.2:** Diseñar la exportación de historial consolidado de cobros e ingresos a Excel/CSV, preservando importes y estados de pago.
- [ ] **Iteración 6.3:** Incorporar modo oscuro adaptativo Material 3 y validar contraste, navegación y generación de PDF.

---

## 📈 Resumen de Progreso

| Módulo / Fase | Tareas Totales | Completadas | Estado |
| :--- | :---: | :---: | :---: |
| **Fase 1: MVVM & Hilt** | 6 | 6 | 100% ✅ |
| **Fase 2: SSOT Offline-First** | 7 | 7 | 100% ✅ |
| **Fase 3: Sellos & Comprobantes** | 3 | 3 | 100% ✅ |
| **Fase 4: Rediseño M3** | 3 | 3 | 100% ✅ |
| **Fase 5: Prorrateo Personas** | 4 | 4 | 100% ✅ |
| **Fase 6: Métricas Avanzadas** | 3 | 0 | 0% ⏳ |
