# Estado del Proyecto - Kilowatt

**Fecha de última revisión:** Octubre 2026  
**Estado general:** En desarrollo activo / Versión funcional básica (MVP)

---

## 1. Visión General
**Kilowatt** es una aplicación móvil Android diseñada para propietarios o administradores de inmuebles que necesitan prorratear y cobrar el consumo eléctrico entre múltiples inquilinos o áreas comunes a partir de un recibo general y submedidores independientes.

---

## 2. Componentes y Estado de Módulos

| Módulo / Funcionalidad | Estado | Descripción |
| :--- | :---: | :--- |
| **Autenticación (`AuthActivity`)** | ✅ Operativo | Registro e inicio de sesión con Firebase Auth (correo/contraseña). Manejo de sesión activa. |
| **Navegación Principal (`MainActivity`)** | ✅ Operativo | Menú de accesos directos hacia configuración, toma de lecturas y reportes de cobro. |
| **Configuración de Inquilinos (`ConfiguracionActivity`)** | ✅ Operativo | Alta, edición y eliminación de inquilinos y submedidores en Firestore. Configuración de perfil de cobro (datos bancarios / día de corte). |
| **Registro de Lecturas (`LecturasActivity`)** | ✅ Operativo | Registro del recibo mensual principal (S/ y kWh) y registro de lecturas actuales y anteriores por submedidor. |
| **Cálculo de Consumos (`CobroCalculator`)** | ✅ Operativo | Validación de consistencia de lecturas, costo promedio por kWh y distribución de áreas comunes. |
| **Resumen y Notificaciones (`ResumenCobrosActivity`)** | ✅ Operativo | Lista de consumos calculados por periodo, envío de mensajes detallados por WhatsApp y exportación de comprobantes individuales/consolidados a PDF. |
| **Persistencia Local (`Room - AppDatabase`)** | ⚠️ Parcial / Desacoplada | Entidades y DAOs definidos, pero los repositorios activos operan directamente contra Cloud Firestore. |

---

## 3. Limitaciones y Deuda Técnica Detectada
* **Falta de capa ViewModel:** La lógica de presentación y suscripción a corrutinas/Flows reside directamente en las `Activity`.
* **Inyección de Dependencias manual:** Las dependencias (`AuthRepository`, `InquilinoRepository`, `LecturaRepository`) se instancian manualmente en cada componente.
* **Manejo de estado de pagos:** Los cobros no cuentan con un mecanismo persistente para marcar recibos individuales como "pagados" o "pendientes".
* **Recuperación de contraseña:** La pantalla de autenticación no implementa flujo de reseteo de clave por correo.
