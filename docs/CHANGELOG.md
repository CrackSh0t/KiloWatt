# Registro de Cambios (Changelog) - Kilowatt

Todas las modificaciones notables realizadas en el proyecto Kilowatt están documentadas en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y se adhiere a las directrices de versiones semánticas.

---

## [Sin Publicar] - 2026-10-19

### Añadido
- **Auditoría Técnica Integral (`docs/AUDITORIA_TECNICA.md`):** Diagnóstico de arquitectura, deuda técnica, bugs potenciales clasificados por severidad (Alta, Media, Baja) y hoja de ruta de remediación.
- **Documentación de Decisiones de Arquitectura (`docs/DECISIONES_TECNICAS.md`):** Justificación de elecciones clave (Firebase vs Backend propio, Views vs Compose, ViewBinding, Corrutinas/Flows y persistencia).
- **Especificación del Modelo de Datos (`docs/MODELO_DATOS.md`):** Detalle técnico de entidades Room, esquemas Firestore, Data Classes de negocio, relaciones, cardinalidades y restricciones.

---

## [0.3.0] - 2026-10-15

### Añadido
- **Generación de Reportes PDF Dinámicos:**
  - Emisión de comprobantes individuales de cobro en formato A6 con desglose de consumos, lectura anterior/actual, cuota de áreas comunes y datos de pago.
  - Generación de reporte general mensual en formato vertical con gráfico circular (*pie chart*) de distribución porcentual de consumo sobre `Canvas` y tabla consolidada.
- **Distribución vía WhatsApp:** Integración de enlaces con `Intent.ACTION_VIEW` para enviar mensajes estructurados de cobro directamente al número de contacto de cada arrendatario.
- **Configuración de Cobranza:** Parámetros configurables de pago (titular, número Yape/Plin, día de corte mensual) persistidos localmente con `SettingsManager` y respaldados en la nube.

### Modificado
- Refactorización de visualización en `CobroAdapter` con estados dinámicos ("Pendiente 🟡", "Sin Lectura ⚠️") y renderizado optimizado con `ListAdapter` y `DiffUtil`.

---

## [0.2.0] - 2026-10-01

### Añadido
- **Motor de Cálculo y Prorrateo (`CobroCalculator`):**
  - Validación de coherencia de lecturas (valores no negativos, lectura actual mayor o igual a la lectura previa).
  - Distribución proporcional del costo del recibo general (`montoTotalRecibo`) según el consumo neto en kWh registrado por cada submedidor.
  - Prorrateo equitativo del gasto generado por submedidores catalogados como áreas comunes (`esAreaComun = true`) únicamente entre los inquilinos configurados como pagadores (`pagaAreaComun = true`).
- **Pantalla de Resumen y Cobros (`ResumenCobrosActivity`):**
  - Selector desplegable de meses facturados.
  - Indicadores globales de costo total del recibo, kWh totales y precio promedio resultante por kWh.
  - Listado detallado por espacio con reactividad en tiempo real mediante `combine` de Kotlin Flows.

---

## [0.1.0] - 2026-09-15

### Añadido
- **Autenticación Multitenant:**
  - Inicio de sesión y registro de administradores/arrendadores con correo y contraseña vía `FirebaseAuth` (`AuthActivity`).
  - Navegación protegida en `MainActivity` según el estado de sesión activa.
- **Gestión de Inquilinos y Submedidores:**
  - Registro, edición y eliminación de espacios físicos y arrendatarios (`ConfiguracionActivity`).
  - Flags de configuración por submedidor: designación como área común y participación en el pago comunitario.
- **Registro y Captura de Lecturas:**
  - Formulario de ingreso de recibos principales de la empresa distribuidora de energía eléctrica (`LecturasActivity`).
  - Registro de mediciones mensuales por submedidor con precarga automática de la última lectura registrada.
- **Capa de Persistencia e Infraestructura:**
  - Esquema relacional local preparado con Room (`AppDatabase`, versión 3, DAOs y entidades `Inquilino`, `Submedidor`, `FacturaGeneral`, `Lectura`).
  - Conectores en tiempo real con Cloud Firestore (`InquilinoRepository`, `LecturaRepository`) mediante `callbackFlow`.
