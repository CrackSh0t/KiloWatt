# Auditoría técnica — DT-06 Iteración 1

**Documento solicitado:** `docs/Propuestas/Resumen_Impl_DT_Iteracion_1.md`  
**Documento encontrado y auditado:** `docs/Propuestas/Implementadas/Resumen_Impl_DT_Iteracion_1.md`  
**Fecha:** 2026-11-04  
**Resultado:** **NO APROBADA para cierre; queda un hallazgo alto pendiente.**

## Alcance

Se validó el resumen de implementación contra el código actual, las pruebas unitarias, los consumidores de `Flow`, las rutas Room/Firestore, la documentación actualizada y el hook `.github/hooks/run-unit-tests.json`.

La ruta solicitada bajo `docs/Propuestas/` no existe. La auditoría se realizó sobre la copia existente bajo `docs/Propuestas/Implementadas/`.

## Resumen ejecutivo

La subsanación corrige varios hallazgos de la auditoría anterior:

- `OperationResult<T>` existe con `Synced`, `PendingSync` y `Failed`.
- Los `Flow` de Room ya no sustituyen errores por `emptyList()` en los repositorios.
- Las operaciones de actualización y eliminación en `InquilinoRepository` no continúan hacia Firestore cuando falla Room.
- Se añadieron pruebas de `PendingSync`, `Failed` y error de Room en un consumidor.
- Se añadieron logs con entidad, operación e identificador lógico.
- La documentación reconoce que `PendingSync` todavía no es durable y depende de DT-07.

Sin embargo, el manejo de errores no llega a toda la interfaz. `ResumenCobrosViewModel` escribe los errores en `ResumenCobrosUiState.mensajeError`, pero `ResumenCobrosActivity` no lee ni muestra ese campo. En consecuencia, un fallo de lectura de facturas o del resumen puede quedar sin feedback visible para el usuario.

## Hallazgos actuales

| # | Severidad | Archivo / líneas | Hallazgo | Riesgo |
|---|---|---|---|---|
| 1 | **Alto** | `app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt:44-49,151-157` y `app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt:84-107` | El ViewModel captura los errores de los `Flow` y actualiza `mensajeError`, pero `ResumenCobrosActivity.actualizarUi` no consume ese campo ni muestra un `Toast`, diálogo o estado visual de error. | El requisito de feedback claro para errores de Room no se cumple en la pantalla de resumen; el usuario puede ver datos vacíos o anteriores sin saber que ocurrió un fallo. |
| 2 | **Medio** | `app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt:143-146,192-195` | Actualización y eliminación abortan correctamente cuando Room falla, pero mantienen el callback `(Boolean, String?)`; el resultado no es un `OperationResult.Failed` tipado como el resumen afirma. | Persisten contratos ambiguos en rutas de persistencia no suspendidas y se dificulta que consumidores futuros distingan `Failed`, `PendingSync` y `Synced` de forma exhaustiva. |
| 3 | **Medio** | `app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt:174-217` | Las pruebas cubren `PendingSync`, `Failed` y error de Room en `LecturasViewModel`, pero no prueban que actualización/eliminación aborten antes de Firestore al fallar Room ni que todos los consumidores (`ConfiguracionViewModel`, `ResumenCobrosViewModel`) publiquen errores visibles. | Una regresión en rutas callback o en otras pantallas podría reintroducir el defecto sin que la suite lo detecte. |
| 4 | **Medio** | `app/src/main/java/com/example/kilowatt/LecturasViewModel.kt:65-74` | El `catch` de la lectura previa registra el error de UI, pero luego emite `emptyList()` para completar el flujo. | Aunque no ocurre en la capa Room, el fallback puede ocultar la diferencia entre “sin lecturas” y “fallo de lectura” para lógica posterior; el estado de error queda mezclado con un dato vacío. |
| 5 | **Bajo** | `app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt:138-139` y `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt:165-180` | `.catch { throw it }` no añade comportamiento frente a retornar directamente el `Flow`. | Ruido de implementación y falsa apariencia de manejo adicional del error. |
| 6 | **Medio** | `app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt:120,182-184,228-230` y `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt:77,112,154` | El estado `PendingSync` se devuelve cuando Firestore falla, pero no existe outbox ni reintento durable. | La operación pendiente puede perderse al cerrar la aplicación. La limitación está documentada y corresponde a DT-07, pero sigue siendo un riesgo operativo abierto. |

## Hallazgos corregidos desde la auditoría anterior

| Hallazgo anterior | Estado | Evidencia |
|---|---|---|
| Pruebas de `PendingSync` y `Failed` inexistentes | Corregido parcialmente | `LecturasViewModelTest.kt:174-201` y `OperationResultTest.kt:8-27`. |
| Room fallaba pero el flujo continuaba a Firestore en actualización/eliminación | Corregido | `InquilinoRepository.kt:151-165` y `197-210` retornan tras notificar el fallo local. |
| Falta de `.catch` en consumidores de `Flow` | Corregido parcialmente | `LecturasViewModel.kt`, `ConfiguracionViewModel.kt`, `ResumenCobrosViewModel.kt` y `HistorialConsumoViewModel.kt` tienen manejo de excepciones. El feedback no está conectado completamente en `ResumenCobrosActivity`. |
| Logs sin entidad, operación e ID | Corregido en sincronización y callbacks auditados | Logs de `InquilinoRepository.kt` y `LecturaRepository.kt` incluyen esos campos. |
| Documentación desactualizada | Corregido | Se actualizaron `ESTADO_DEL_PROYECTO.md`, `CONTEXTO_IA.md`, `MEMORIA_PROYECTO_KILOWATT.md` y `ROADMAP.md`. |

## Verificaciones realizadas

- Se confirmó que `docs/Propuestas/Resumen_Impl_DT_Iteracion_1.md` no existe.
- Se auditó `docs/Propuestas/Implementadas/Resumen_Impl_DT_Iteracion_1.md`.
- `OperationResult<T>` existe como `sealed interface` con los tres estados requeridos.
- `IInquilinoRepository` e `ILecturaRepository` ya no utilizan `Pair<Boolean, String?>` en sus operaciones suspendidas.
- Los repositorios de Room no emiten `emptyList()` al capturar errores.
- Los consumidores principales tienen operadores `.catch`.
- `HistorialConsumoViewModel` ya publica `HistorialConsumoUiState.Error`.
- `ResumenCobrosViewModel` publica `mensajeError`, pero su Activity no lo presenta.
- El hook configura `postToolUse` con `./gradlew :app:testDebugUnitTest`.
- Se ejecutó la suite completa con tareas forzadas:

```text
./gradlew :app:testDebugUnitTest --rerun-tasks
BUILD SUCCESSFUL
40 actionable tasks: 40 executed
```

La orden exacta configurada por el hook también había finalizado correctamente:

```text
./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
```

## Criterios de aceptación

| Criterio | Estado | Observación |
|---|---|---|
| La UI no muestra “guardado en la nube” cuando solo se guardó localmente | Parcialmente cumple | Las escrituras suspendidas distinguen `PendingSync`; las rutas callback abortan ante Room, pero siguen usando Boolean y no un resultado tipado. |
| Un fallo de Room produce un estado de error observable | Parcialmente cumple | Se cumple en Lecturas, Configuración e Historial; no se muestra en Resumen de Cobros porque la Activity ignora `mensajeError`. |
| Los errores incluyen una acción definida | Parcialmente cumple | Existe feedback en varios estados, pero no hay reintento durable; `PendingSync` queda pendiente de DT-07. |
| Existen tests para éxito remoto, éxito local pendiente y fallo no recuperable | Parcialmente cumple | Los estados se cubren mediante fakes/ViewModel; faltan pruebas de repositorios y de abortar Firestore en actualización/eliminación. |

## Riesgos adicionales

### Feedback incompleto en Resumen de Cobros

El estado `mensajeError` está definido en `ResumenCobrosUiState` y se actualiza desde `ResumenCobrosViewModel`, pero no aparece en la lógica de `ResumenCobrosActivity`. Esto convierte el manejo interno de error en una señal no observable por el usuario.

### Contratos mixtos

La iteración tipa las operaciones suspendidas, pero deja `actualizarInquilinoYSubmedidor` y `eliminarInquilinoYSubmedidor` con callbacks booleanos. Aunque se evita continuar hacia Firestore tras un fallo local, la superficie pública sigue permitiendo estados ambiguos.

### Pendientes no durables

`PendingSync` comunica correctamente que la nube no confirmó la operación, pero no conserva la operación en Room ni garantiza reintento después de reiniciar la aplicación. Esta limitación está documentada y debe permanecer abierta hasta DT-07.

## Deuda técnica

### Deuda crítica

- Conectar `ResumenCobrosUiState.mensajeError` con un feedback visible en `ResumenCobrosActivity`.

### Deuda inmediata

- Añadir tests de actualización/eliminación con DAO fallido y verificar que Firestore no se invoque.
- Añadir tests de error para `ConfiguracionViewModel` y `ResumenCobrosViewModel`.
- Unificar los callbacks restantes bajo un contrato tipado o documentar formalmente su compatibilidad temporal.

### Deuda aceptable

- Eliminar `.catch { throw it }` y retornar directamente los `Flow`.
- Sustituir el fallback `emit(emptyList())` de la lectura previa por una ruta que complete sin introducir un valor que parezca válido.
- Implementar la outbox persistente y los reintentos en DT-07.

## Recomendación

Corregir primero el hallazgo **1**, conectando `mensajeError` con la interfaz de Resumen de Cobros. Después añadir pruebas de los callbacks de actualización/eliminación y de los demás ViewModels. Repetir la auditoría una vez que el error sea visible en todas las pantallas afectadas.

## Dictamen final

La subsanación mejora sustancialmente la implementación y la suite pasa correctamente, pero el resumen auditado sobredeclara el cumplimiento: todavía existe una pantalla donde el error capturado no llega al usuario y permanecen contratos callback no tipados. La Iteración 1 queda **NO APROBADA para cierre** hasta resolver al menos el hallazgo alto de feedback en `ResumenCobrosActivity`.
