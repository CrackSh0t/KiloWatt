# Roadmap de Desarrollo - Kilowatt

Este documento detalla las etapas de evolución, mejoras técnicas y nuevas funcionalidades planificadas para el proyecto **Kilowatt**.

---

## Fase 1: Estabilización y Refactorización Arquitectónica 🛠️
- [ ] **Migración a MVVM (Model-View-ViewModel):**
  - Implementar `ViewModel` y `StateFlow` en cada pantalla (`AuthViewModel`, `ConfiguracionViewModel`, `LecturasViewModel`, `ResumenCobrosViewModel`).
  - Separar la lógica de negocio y corrutinas de las Activities para evitar pérdida de estado por rotación de pantalla o recreación.
- [ ] **Inyección de Dependencias:**
  - Integrar Hilt (o Koin) para la gestión del ciclo de vida de repositorios, DAOs y servicios.
- [ ] **Desacoplamiento de Adapters:**
  - Mover la generación de PDFs y los Intents de WhatsApp fuera de `CobroAdapter` hacia listeners/callbacks manejados por la UI o Casos de Uso.

---

## Fase 2: Soporte Offline y Consistencia de Datos 💾
- [ ] **Integración Offline-First (Room + Firestore):**
  - Conectar los repositorios a `AppDatabase` para que Room funcione como Fuente Única de Verdad (*Single Source of Truth*).
  - Configurar políticas de sincronización bidireccional en segundo plano (usando WorkManager).
- [ ] **Validaciones de Entrada Robustas:**
  - Validación internacional y formato E.164 para números telefónicos de WhatsApp.
  - Validación de periodos/meses para evitar duplicidad de facturas por errores tipográficos.

---

## Fase 3: Gestión de Cobros y Pagos 💳
- [ ] **Estado de Pagos:**
  - Agregar estado a cada cobro (`Pendiente`, `Pagado`, `Vencido`).
  - Botón para registrar fecha de pago y método de recepción (efectivo, transferencia).
- [ ] **Historial y Gráficos de Consumo:**
  - Gráfica de barras/líneas que muestre la evolución del consumo en kWh por inquilino a lo largo de los meses.
  - Comparativa de gasto energético del inmueble completo.

---

## Fase 4: Experiencia de Usuario y Documentación 📄
- [ ] **Recuperación de Contraseña:**
  - Agregar enlace y flujo para envío de correo de restablecimiento de contraseña vía Firebase Auth.
- [ ] **Plantillas de Comprobantes Personalizables:**
  - Permitir adjuntar logo o firma digital del propietario en los comprobantes PDF.
  - Opciones de personalización del mensaje de texto para WhatsApp.
- [ ] **Modo Oscuro y Accesibilidad:**
  - Revisión de contrastes y soporte completo para Material You / Dark Theme.
