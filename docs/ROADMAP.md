# Roadmap y Hoja de Ruta - App Kilowatt ⚡

**Fecha de última revisión:** Octubre 2026
**Responsable de Mantenimiento:** Agente Arquitecto (`@SKILL_ARQUITECTO.md`) / Agente Desarrollo (`@SKILL_DESARROLLO.md`)

---

## 🎯 Estado General del Roadmap

```
[Fase 1: MVVM & DI] ────► [Fase 2: SSOT Offline-First] ────► [Fase 3: Sellos & PDF] ────► [Fase 4: Rediseño M3] ────► [Fase 5: Métricas & Proyecciones]
     (100% ✅)                    (100% ✅)                     (100% ✅)                  (100% ✅)                      (Planificada 🚀)
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

### Fase 5: Métricas Avanzadas, Proyecciones y Exportaciones (Próxima Fase / En Planificación)
- [ ] **Iteración 5.1:** Proyección estimada del recibo del mes en curso basada en la tasa de consumo diario actual.
- [ ] **Iteración 5.2:** Exportación de historial consolidado de cobros e ingresos a formato Excel/CSV para contabilidad del arrendador.
- [ ] **Iteración 5.3:** Mapeo de migraciones explícitas de Room (`Migration(from, to)`) para producción sin `fallbackToDestructiveMigration`.
- [ ] **Iteración 5.4:** Soporte para modo oscuro (Dark Theme) adaptativo Material 3.

---

## 📈 Resumen de Progreso

| Módulo / Fase | Tareas Totales | Completadas | Estado |
| :--- | :---: | :---: | :---: |
| **Fase 1: MVVM & Hilt** | 6 | 6 | 100% ✅ |
| **Fase 2: SSOT Offline-First** | 4 | 4 | 100% ✅ |
| **Fase 3: Sellos & Comprobantes** | 3 | 3 | 100% ✅ |
| **Fase 4: Rediseño M3** | 3 | 3 | 100% ✅ |
| **Fase 5: Métricas Avanzadas** | 4 | 0 | 0% ⏳ |
