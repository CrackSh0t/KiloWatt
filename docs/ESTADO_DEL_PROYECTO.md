# Estado del Proyecto - Kilowatt

**Fecha de última revisión:** Noviembre 2026
**Estado general:** En desarrollo activo / Versión funcional básica (MVP)

> **DT-06, DT-07 y DT-08 completados:** la reestructuración de Persistencia Offline-First SSOT está finalizada. `OperationResult<T>` distingue estados de persistencia, la outbox Room v7 garantiza publicación diferida mediante `SyncWorker`, y `RemoteChangeReconciler` mantiene Room alineado con altas, modificaciones y bajas remotas sin perder cambios locales pendientes.

---

## 1. Visión General
**Kilowatt** es una aplicación móvil Android diseñada para propietarios o administradores de inmuebles que necesitan prorratear y cobrar el consumo eléctrico entre múltiples inquilinos o áreas comunes a partir de un recibo general y submedidores independientes.

> [!NOTE]
> **Arquitectura Actual:** Las Fases 1 a 5 se encuentran implementadas funcionalmente (MVVM, StateFlow, Dagger Hilt DI, Offline-First SSOT Room+Firestore, M3, Prorrateo Ponderado por Ocupantes). La suite `./gradlew :app:testDebugUnitTest` pasa en verde y la persistencia `PendingSync` cuenta con outbox durable y reconciliación remota.

---

## 2. Componentes y Estado de Módulos

| Módulo / Funcionalidad | Estado | Descripción |
| :--- | :---: | :--- |
| **Autenticación (`AuthActivity`)** | ✅ Operativo | Registro, inicio de sesión y recuperación de contraseña por correo vía Firebase Auth (`sendPasswordResetEmail`). Manejo de sesión activa. |
| **Navegación Principal (`MainActivity`)** | ✅ Operativo | Menú de accesos directos hacia configuración, toma de lecturas y reportes de cobro. |
| **Configuración de Inquilinos (`ConfiguracionActivity`)** | ✅ Operativo | Alta, edición y eliminación de inquilinos y submedidores en Firestore y Room (v6 con `numeroPersonas`). Validación robusta de teléfonos de WhatsApp (Formato E.164 vía `PhoneUtils`). Configuración de perfil de cobro (datos bancarios / día de corte / modalidad de prorrateo). |
| **Registro de Lecturas (`LecturasActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`LecturasViewModel` + `StateFlow` + `ILecturaRepository`). Registro del recibo mensual principal (S/ y kWh) y lecturas por submedidor con validación de monotonicidad integrada. |
| **Cálculo de Consumos (`CobroCalculator`)** | ✅ Operativo | Validación de consistencia de lecturas, costo promedio por kWh y distribución de áreas comunes en modalidades `EQUITY` y `WEIGHTED_PERSONS` (por número de ocupantes). |
| **Histórico y Gráficos (`HistorialConsumoActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`HistorialConsumoViewModel` + `StateFlow` + `ILecturaRepository`). Pantalla interactiva con gráficos MPAndroidChart (`LineChart` y `BarChart`) y procesador asíncrono `ConsumoHistoricoCalculator`. |
| **Resumen y Notificaciones (`ResumenCobrosActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`ResumenCobrosViewModel` + `StateFlow` + `ILecturaRepository`). Gestión completa de cobranza con auditoría (`PENDIENTE` / `PAGADO` / `VENCIDO`), envío de mensajes por WhatsApp y exportación a PDF con desglose de prorrateo por personas. |
| **Autenticación (`AuthActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`AuthViewModel` + `StateFlow` + `IAuthRepository`). Inicio de sesión, registro, envío de correo de recuperación y gestión de sesión activa. |
| **Configuración y Perfil (`ConfiguracionActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`ConfiguracionViewModel` + `StateFlow` + `IInquilinoRepository` + `ISettingsManager`). Validación E.164 de WhatsApp para inquilinos y gestión del perfil del propietario con sincronización de `tipoProrrateoComun`. |
| **Persistencia Local (`Room - AppDatabase`)** | ✅ Operativo / SSOT (Fase 2 & 5) | Implementación completa del patrón Single Source of Truth (SSOT). Room v7 conserva entidades y outbox mediante migraciones explícitas; `DatabaseModule` inyecta `AppDatabase` y DAOs en Hilt. `SyncWorker` publica operaciones con reintentos y recuperación. |
| **Contrato de errores (`OperationResult`)** | ✅ DT-06 completado | Distingue sincronización remota, persistencia local pendiente y fallo no recuperable; los errores se propagan a la UI con contexto operativo. |
| **Outbox y sincronización (`PendingOperationEntity` / `SyncWorker`)** | ✅ Implementado / Integración pendiente | Operaciones locales y outbox se registran atómicamente; WorkManager exige red conectada, procesa por UID y elimina operaciones confirmadas. |
| **Reconciliación remota (`DocumentChange` / Room)** | ✅ DT-08 implementado | Listeners procesan `ADDED`, `MODIFIED` y `REMOVED`; las bajas verifican outbox y UID antes de eliminar Room. |

---

## 3. Limitaciones y Deuda Técnica Detectada
* **Fases 1, 2, 3, 4 y 5 Completadas al 100%:** MVVM, StateFlow, Dagger Hilt DI, Offline-First SSOT (Room + Firestore + WorkManager), Material Design 3 TopAppBars unificadas, formularios OutlinedBox, gráficos MPAndroidChart optimizados y Prorrateo Ponderado por Ocupantes (`WEIGHTED_PERSONS`).
* **Seguridad, Estabilidad y Cierre de Sesión Blindado:** Inmunidad multi-tenant con purga síncrona en `suspend fun cerrarSesion()` (`cancelarListeners()` ➔ `clearAllTables()` ➔ `limpiarConfiguracion()` ➔ `signOut()`). La reconciliación remota valida UID y conflictos con la outbox.
* **Próxima fase:** Fase 6 — proyección de consumo y cobro, exportación contable Excel/CSV y soporte de modo oscuro Material 3.
