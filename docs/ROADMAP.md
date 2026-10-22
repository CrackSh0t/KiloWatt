# Roadmap de Desarrollo - Kilowatt

Este documento detalla las etapas de evolución, mejoras técnicas y nuevas funcionalidades planificadas para el proyecto **Kilowatt**.

---

## Fase 1: Estabilización y Refactorización Arquitectónica 🛠️
- [x] **Migración a MVVM (Model-View-ViewModel):**
  - [x] Iteración 1: Migración de `HistorialConsumoActivity` a `HistorialConsumoViewModel` + `StateFlow` + `ILecturaRepository`.
  - [x] Iteración 2: Migración de `ResumenCobrosActivity` a `ResumenCobrosViewModel` + `StateFlow` + `ResumenCobrosUiState`.
  - [x] Iteración 3: Migración de `LecturasActivity` a `LecturasViewModel` + `StateFlow` + `LecturasUiState`.
  - [x] Iteración 4: Migración de `AuthActivity` y `ConfiguracionActivity` a `AuthViewModel` & `ConfiguracionViewModel` + StateFlow.
- [/] **Inyección de Dependencias (Hilt):**
  - Integrar Hilt para la gestión automatizada del ciclo de vida de repositorios y ViewModels (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
- [x] **Desacoplamiento de Adapters:**
  - Mover la generación de PDFs y los Intents de WhatsApp fuera de `CobroAdapter` hacia listeners/callbacks manejados por la UI o Casos de Uso.

---

## Fase 2: Soporte Offline y Consistencia de Datos 💾
- [ ] **Integración Offline-First (Room + Firestore):**
  - Conectar los repositorios a `AppDatabase` para que Room funcione como Fuente Única de Verdad (*Single Source of Truth*).
  - Configurar políticas de sincronización bidireccional en segundo plano (usando WorkManager).
- [x] **Validaciones de Entrada Robustas:**
  - Validación internacional y formato E.164 para números telefónicos de WhatsApp (`PhoneUtils`).
  - Validación de periodos/meses para evitar duplicidad de facturas por errores tipográficos.

---

## Fase 3: Gestión de Cobros y Pagos 💳
- [x] **Estado de Pagos:**
  - [x] Cobranza persistente (`PENDIENTE` / `PAGADO`) en Firestore y Room.
  - [x] Fase 2A: Registro auditable de fecha de pago y medio de pago (Yape, Plin, Efectivo, Transferencia).
  - [x] Fase 2B: Evaluación dinámica del estado `VENCIDO` basada en el día límite de pago configurable (`diaLimitePago`).
- [x] **Historial y Gráficos de Consumo:**
  - [x] Fases 1 y 2: Integración de MPAndroidChart, DTOs de dominio (`ConsumoHistorico.kt`), conector global `obtenerTodasLasLecturasFlow` y calculador asíncrono `ConsumoHistoricoCalculator`.
  - [x] Fases 3 y 4: Pantalla `HistorialConsumoActivity` con componentes gráficos de líneas/barras, leyendas claras y vista de métricas clave.

---

## Fase 4: Experiencia de Usuario y Documentación 📄
- [x] **Recuperación de Contraseña:**
  - Agregar enlace y flujo para envío de correo de restablecimiento de contraseña vía Firebase Auth.
- [ ] **Plantillas de Comprobantes Personalizables:**
  - Permitir adjuntar logo o firma digital del propietario en los comprobantes PDF.
  - Opciones de personalización del mensaje de texto para WhatsApp.
- [ ] **Modo Oscuro y Accesibilidad:**
  - Revisión de contrastes y soporte completo para Material You / Dark Theme.
