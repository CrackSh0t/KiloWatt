# Propuesta de Diseño Técnico: Fase 3 - Gestión de Cobros, Pagos y Comprobantes PDF

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Propuesta de Arquitectura en Revisión para el Tech Lead  

---

## 1. Resumen Ejecutivo

Con la **Fase 1 (MVVM + StateFlow + Hilt DI)** y la **Fase 2 (Offline-First SSOT con Room + Firestore + WorkManager)** completadas al 100% y validadas con 36 pruebas unitarias en verde, Kilowatt cuenta con una arquitectura robusta, reactiva y resiliente.

La **Fase 3** se enfoca en perfeccionar la **Experiencia de Cobranza y Reportes**, incorporando la personalización avanzada de comprobantes PDF (encabezados profesionales, datos de contacto del propietario) y sellos visuales de estado de pago ("PAGADO ✅", "VENCIDO 🔴") tanto en la interfaz de usuario como en los documentos impresos o exportados.

---

## 2. Alcance Técnico de la Fase 3

### A. Personalización Avanzada de Comprobantes PDF (`PdfGenerator.kt`)
1. **Encabezado del Propietario:**
   - Inyección automática del nombre del propietario, número de contacto (Yape/Plin) y día límite de pago configurados en `SettingsManager` en la cabecera de cada recibo PDF generado.
2. **Mejoras de Estética y Tipografía (`PdfPTable` / `Paragraph`):**
   - Inclusión de bordes limpios, separadores de sección y un diseño tabular optimizado para impresión térmica o A4.

### B. Sellos Visuales de Estado de Pago
1. **Visualización en Pantalla (`item_cobro.xml` / `CobroAdapter.kt`):**
   - Incorporación de sellos gráficos dinámicos que indican claramente si un recibo se encuentra `PAGADO` (con fecha y medio de pago), `VENCIDO` (con la fecha exacta en que venció) o `PENDIENTE`.
2. **Sello Gráfico en PDF:**
   - Impresión de una marca de agua o etiqueta destacada de estado de pago en el comprobante PDF individual y general.

---

## 3. Plan de Implementación (Iteraciones)

```
[Iteración 1] Personalización de Encabezados y Datos del Propietario en PdfGenerator
      │
      ▼
[Iteración 2] Integración de Sellos Visuales de Pago en UI (CobroAdapter) y PDF
```

---

## 4. Plan de Pruebas Unitarias

- Se mantendrán intactas las **36 pruebas unitarias existentes**.
- Se añadirán pruebas de validación para asegurar la correcta incorporación de los metadatos del propietario y estados de pago en el generador de reportes.

---

## 5. Recomendación Final

### **APROBAR PROPUESTA TÉCNICA (FASE 3)**
Se solicita la revisión y aprobación del Tech Lead para proceder con la implementación de las mejoras visuales y de exportación PDF descritas en esta propuesta.
