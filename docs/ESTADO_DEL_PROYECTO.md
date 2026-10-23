# Estado del Proyecto - Kilowatt

**Fecha de última revisión:** Octubre 2026  
**Estado general:** En desarrollo activo / Versión funcional básica (MVP)

---

## 1. Visión General
**Kilowatt** es una aplicación móvil Android diseñada para propietarios o administradores de inmuebles que necesitan prorratear y cobrar el consumo eléctrico entre múltiples inquilinos o áreas comunes a partir de un recibo general y submedidores independientes.

> [!NOTE]
> **Arquitectura Actual:** La Fase 1 de refactorización arquitectónica se encuentra **100% completada**, implementando el patrón **MVVM + StateFlow + Dagger Hilt DI** en todas las pantallas de la aplicación, respaldado por 36 pruebas unitarias automatizadas en verde.

---

## 2. Componentes y Estado de Módulos

| Módulo / Funcionalidad | Estado | Descripción |
| :--- | :---: | :--- |
| **Autenticación (`AuthActivity`)** | ✅ Operativo | Registro, inicio de sesión y recuperación de contraseña por correo vía Firebase Auth (`sendPasswordResetEmail`). Manejo de sesión activa. |
| **Navegación Principal (`MainActivity`)** | ✅ Operativo | Menú de accesos directos hacia configuración, toma de lecturas y reportes de cobro. |
| **Configuración de Inquilinos (`ConfiguracionActivity`)** | ✅ Operativo | Alta, edición y eliminación de inquilinos y submedidores en Firestore. Validación robusta de teléfonos de WhatsApp (Formato E.164 vía `PhoneUtils`). Configuración de perfil de cobro (datos bancarios / día de corte). |
| **Registro de Lecturas (`LecturasActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`LecturasViewModel` + `StateFlow` + `ILecturaRepository`). Registro del recibo mensual principal (S/ y kWh) y lecturas por submedidor con validación de monotonicidad integrada. |
| **Cálculo de Consumos (`CobroCalculator`)** | ✅ Operativo | Validación de consistencia de lecturas, costo promedio por kWh y distribución de áreas comunes. |
| **Histórico y Gráficos (`HistorialConsumoActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`HistorialConsumoViewModel` + `StateFlow` + `ILecturaRepository`). Pantalla interactiva con gráficos MPAndroidChart (`LineChart` y `BarChart`) y procesador asíncrono `ConsumoHistoricoCalculator`. |
| **Resumen y Notificaciones (`ResumenCobrosActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`ResumenCobrosViewModel` + `StateFlow` + `ILecturaRepository`). Gestión completa de cobranza con auditoría (`PENDIENTE` / `PAGADO` / `VENCIDO`), envío de mensajes por WhatsApp y exportación a PDF. |
| **Autenticación (`AuthActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`AuthViewModel` + `StateFlow` + `IAuthRepository`). Inicio de sesión, registro, envío de correo de recuperación y gestión de sesión activa. |
| **Configuración y Perfil (`ConfiguracionActivity`)** | ✅ Operativo / MVVM | Migrado exitosamente al patrón MVVM (`ConfiguracionViewModel` + `StateFlow` + `IInquilinoRepository` + `ISettingsManager`). Validación E.164 de WhatsApp para inquilinos y gestión del perfil del propietario. |
| **Persistencia Local (`Room - AppDatabase`)** | ✅ Operativo / SSOT (Fase 2) | Implementación completa del patrón Single Source of Truth (SSOT) en `LecturaRepository` e `InquilinoRepository`. `DatabaseModule` inyecta `AppDatabase` y DAOs en Hilt. Soporte WorkManager integrado (`SyncWorker`). |

---

## 3. Limitaciones y Deuda Técnica Detectada
* **Fases 1, 2, 3 y 4 Completadas al 100%:** MVVM, StateFlow, Dagger Hilt DI, Offline-First SSOT (Room + Firestore + WorkManager), Material Design 3 TopAppBars unificadas, formularios OutlinedBox y gráficos MPAndroidChart optimizados. Respaldado por 36 pruebas unitarias pasadas al 100%.
