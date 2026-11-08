# Resumen de implementación — DT-06 Iteración 1

## Objetivo

Eliminar la ambigüedad entre persistencia local, sincronización remota y errores en las operaciones de datos.

## Cambios realizados

- Se creó `OperationResult<T>` en la capa de dominio con los estados `Synced`, `PendingSync` y `Failed`.
- `IInquilinoRepository` e `ILecturaRepository` ahora devuelven `OperationResult<Unit>` en sus operaciones suspendidas.
- Los ViewModels distinguen sincronización completa, guardado local pendiente y fallo.
- Los `Flow` de Room ya no emiten `emptyList()` cuando falla la base de datos; propagan la excepción para que sea observable.
- Los consumidores de `Flow` capturan excepciones con `.catch { ... }` y actualizan estados de UI con feedback explícito.
- Las operaciones de actualización y eliminación abortan antes de Firestore cuando Room falla y notifican `Failed` mediante el callback existente.
- Las excepciones de sincronización de listeners se registran con entidad, operación e identificador lógico.
- Se añadieron pruebas unitarias para `PendingSync`, `Failed` y errores de Room propagados desde un `Flow`.
- Se añadió el hook `.github/hooks/run-unit-tests.json`, que ejecuta `./gradlew :app:testDebugUnitTest` después de cada uso de herramienta.

## Limitación temporal

`PendingSync` todavía no tiene durabilidad garantizada porque la outbox persistente y los reintentos aún no están implementados. Una operación pendiente puede perderse si la aplicación se cierra antes de la sincronización; esta capacidad corresponde a DT-07.

## Validación

La validación se ejecuta con:

```bash
./gradlew :app:testDebugUnitTest
```

Resultado: `BUILD SUCCESSFUL` ejecutando `./gradlew :app:testDebugUnitTest --rerun-tasks`; 40 tareas ejecutadas, sin pruebas fallidas. La orden exacta configurada por el hook (`./gradlew :app:testDebugUnitTest`) también finalizó con `BUILD SUCCESSFUL`.

## Subsanación de auditoría

- Los callbacks de actualización y eliminación de inquilinos abortan antes de Firestore si Room falla, registran entidad, operación e identificador y notifican el fallo.
- Los consumidores de Room (`LecturasViewModel`, `ConfiguracionViewModel` y `ResumenCobrosViewModel`) usan `.catch { exception -> ... }` para publicar feedback explícito en sus estados de UI.
- Se añadieron pruebas de `PendingSync`, `Failed` y de una excepción de Room propagada desde el Flow de submedidores hasta `LecturasUiState`.
- Se actualizaron `ESTADO_DEL_PROYECTO.md`, `CONTEXTO_IA.md`, `MEMORIA_PROYECTO_KILOWATT.md` y `ROADMAP.md`.
- La limitación temporal de `PendingSync` quedó documentada: la outbox persistente y los reintentos corresponden a DT-07.
