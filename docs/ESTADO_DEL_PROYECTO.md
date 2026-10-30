# Estado del Proyecto - Kilowatt

**Fecha de última revisión:** Octubre 2026  
**Estado general:** En desarrollo activo / Versión funcional básica (MVP)

---

## 1. Visión General
**Kilowatt** es una aplicación móvil Android diseñada para propietarios o administradores de inmuebles que necesitan prorratear y cobrar el consumo eléctrico entre múltiples inquilinos o áreas comunes a partir de un recibo general y submedidores independientes.

> [!NOTE]
> **Arquitectura Actual:** Las Fases 1 a 5 se encuentran **100% completadas** (MVVM, StateFlow, Dagger Hilt DI, Offline-First SSOT Room+Firestore, M3, Prorrateo Ponderado por Ocupantes). El sistema cuenta con **46 pruebas unitarias automatizadas en verde**.

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
| **Persistencia Local (`Room - AppDatabase`)** | ✅ Operativo / SSOT (Fase 2 & 5) | Implementación completa del patrón Single Source of Truth (SSOT). Versión 6 de Room con migración explícita `MIGRATION_5_6` (`ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1`). `DatabaseModule` inyecta `AppDatabase` y DAOs en Hilt. Soporte WorkManager integrado (`SyncWorker`). |

---

## 3. Limitaciones y Deuda Técnica Detectada
* **Fases 1, 2, 3, 4 y 5 Completadas al 100%:** MVVM, StateFlow, Dagger Hilt DI, Offline-First SSOT (Room + Firestore + WorkManager), Material Design 3 TopAppBars unificadas, formularios OutlinedBox, gráficos MPAndroidChart optimizados y Prorrateo Ponderado por Ocupantes (`WEIGHTED_PERSONS`).
* **Seguridad, Estabilidad y Cierre de Sesión Blindado:** Inmunidad multi-tenant con purga síncrona en `suspend fun cerrarSesion()` (`cancelarListeners()` ➔ `clearAllTables()` ➔ `limpiarConfiguracion()` ➔ `signOut()`). Validados con 46 pruebas unitarias en verde.
* **Próximas Fases:** Fase 6 (Métricas Avanzadas, Proyecciones y Exportaciones Excel/CSV).
