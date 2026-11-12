# Auditoría Técnica — Fix de Persistencia de Estado de Pagos y Reconciliación Offline-First

**Fecha:** Noviembre 2026  
**Auditor:** Agente Auditor (`@SKILL_AUDITOR.md`)  
**Componentes auditados:** `RemoteChangeReconciler.kt`, `SyncWorker.kt`, `LecturasViewModel.kt`, `RemoteChangeReconcilerTest.kt`  
**Estado de la Suite de Pruebas:** 67 pasadas, 0 falladas (Gradle `:app:testDebugUnitTest`)  

---

## Resumen Ejecutivo

Se realizó la auditoría técnica independiente sobre los cambios introducidos para corregir el defecto en el estado de cobro de los inquilinos. Anteriormente, al marcar un cobro como `PAGADO` (vía Yape/Plin/Efectivo) en el periodo actual o anterior (ej. Setiembre), el estado revertía a `VENCIDO` al cerrar y reabrir la aplicación o al recalcular lecturas.

La evaluación confirmó que la solución ataca con precisión las tres causas raíz identificadas:
1. **Sobreescritura por Listener Firestore:** Corrección en `RemoteChangeReconciler.debeAplicarCambio` para bloquear cualquier cambio remoto (`ADDED`, `MODIFIED`, `REMOVED`) cuando existe una operación local pendiente en la outbox Room (`tieneOperacionLocal = true`).
2. **Fallos transitorios en Outbox Worker:** Migración en `SyncWorker` de `update(...)` a `set(..., SetOptions.merge())` para mutaciones de pago en `LECTURA`, evitando excepciones `NOT_FOUND` si el documento remoto no se había creado previamente.
3. **Pérdida de estado en recálculo de lecturas:** Preservación de los campos `estadoPago`, `fechaPago` y `metodoPago` en `LecturasViewModel` al modificar o guardar lecturas de submedidores existentes.

---

## Hallazgos Críticos

*Ninguno.*

---

## Hallazgos Altos

*Ninguno.*

---

## Hallazgos Medios

*Ninguno.*

---

## Hallazgos Bajos y Observaciones

### Observación 1: Firma del método `debeAplicarCambio` en `RemoteChangeReconciler`
- **Detalle:** Se preservó la firma `debeAplicarCambio(tipo: DocumentChange.Type, uidCoincide: Boolean, tieneOperacionLocal: Boolean)` utilizando `@Suppress("UNUSED_PARAMETER")` sobre `tipo`.
- **Evaluación:** Decisión arquitectónica correcta. Evita refactorizaciones invasivas en `InquilinoRepository` y `LecturaRepository` manteniendo el contrato de interfaz existente.

### Observación 2: Granularidad de reconciliación a nivel de documento
- **Detalle:** `existeOperacionPendiente` evalúa la presencia de operaciones locales por `documentId`. Si hay una operación local pendiente para esa lectura en la outbox, no se aplica ningún cambio remoto hasta que el `SyncWorker` confirme la sincronización.
- **Evaluación:** Adecuado para la arquitectura monousuario del arrendador en Kilowatt. Mantiene la base de datos Room como *Single Source of Truth* (SSOT) de forma determinista.

---

## Matriz de Verificación

| # | Criterio de Verificación | Evidencia de Código / Test | Resultado |
|:---:|:---|:---|:---:|
| 1 | **Protección de Outbox frente a Snapshots:** Los cambios remotos se descartan si hay operaciones pendientes locales. | `RemoteChangeReconciler.kt:9` | ✅ Cumple |
| 2 | **Actualización atómica en Firestore:** Se usa `SetOptions.merge()` para evitar fallos por documentos no creados. | `SyncWorker.kt:52` | ✅ Cumple |
| 3 | **Preservación de Estado de Pago:** Modificar lecturas conserva `estadoPago`, `fechaPago` y `metodoPago`. | `LecturasViewModel.kt:187-189` | ✅ Cumple |
| 4 | **Cobertura de Pruebas Unitarias:** Escenarios `ADDED`, `MODIFIED`, `REMOVED` cubiertos en tests. | `RemoteChangeReconcilerTest.kt` (67/67 tests verde) | ✅ Cumple |

---

## Riesgos Futuros

Si en fases posteriores se planea soportar edición colaborativa simultánea en tiempo real desde múltiples dispositivos en la misma cuenta de arrendador, se recomendaría evolucionar la resolución por `documentId` hacia un esquema de marcas de tiempo (*Last-Write-Wins* / *Field-Level Merge*). Para el alcance actual, la protección por `documentId` es óptima.

---

## Recomendación Final

### **APROBADO ✅**

Los cambios implementados resuelven de forma definitiva la causa raíz del defecto, mantienen la integridad del modelo SSOT offline-first y cuentan con respaldo completo en la suite de pruebas unitarias.
