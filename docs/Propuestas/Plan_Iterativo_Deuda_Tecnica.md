# Plan Iterativo de Reducción de Deuda Técnica

**Proyecto:** KiloWatt  
**Tipo:** Propuesta técnica de mantenimiento evolutivo  
**Estado:** Propuesta  
**Alcance:** confiabilidad, sincronización, persistencia, privacidad, testabilidad y operación  
**Fecha:** Octubre de 2026

---

## 1. Objetivo

Reducir la deuda técnica que amenaza la promesa **Offline-First** de KiloWatt sin introducir una reescritura ni concentrar cambios de alto riesgo en una sola entrega. La propuesta divide el trabajo en iteraciones pequeñas, con contratos claros, pruebas específicas y puntos de validación antes de avanzar.

El objetivo no es cambiar la funcionalidad de negocio del cálculo de cobros, sino hacer que los datos locales y remotos sean confiables, observables y recuperables ante errores de red, cierre de sesión, actualización de esquema o restauración del dispositivo.

## 2. Diagnóstico ejecutivo

La aplicación cuenta con una base favorable: MVVM, Hilt, Room, StateFlow y pruebas unitarias para ViewModels y cálculos. Sin embargo, existen discrepancias entre la arquitectura documentada y el comportamiento actual:

| Área | Situación actual | Riesgo |
|---|---|---|
| Sincronización | `SyncWorker` devuelve `Result.success()` sin procesar datos ni estar programado | Los cambios locales pueden no llegar nunca a Firestore |
| Errores | Varios repositorios ignoran excepciones o emiten listas vacías | Un fallo puede parecer ausencia de datos |
| Resultado remoto | Los métodos devuelven éxito aunque Firestore falle, indicando solo un mensaje | La UI puede comunicar un éxito ambiguo |
| Eliminaciones | Los listeners importan documentos, pero no procesan `REMOVED` | Room conserva registros obsoletos |
| Migraciones | Se mantienen migraciones explícitas y `fallbackToDestructiveMigration()` | Una versión no cubierta puede borrar datos locales |
| Configuración | `AppDatabase` tiene un constructor alternativo además de la instancia Hilt | La configuración puede divergir entre entornos |
| Privacidad | Backup de Android y preferencias no tienen una política explícita | Riesgo de restaurar o exponer datos sensibles |
| Operación | PDF, release engineering y documentación tienen controles incompletos | Fallos de producción difíciles de detectar o reproducir |

## 3. Evidencia técnica

Las siguientes referencias corresponden al estado revisado:

- `app/src/main/java/com/example/kilowatt/worker/SyncWorker.kt:9-18`: el Worker no ejecuta sincronización real.
- `app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt:88,113,125-132,150,188,270-309`: excepciones ignoradas, éxito local representado como éxito global, errores convertidos en listas vacías y listeners que solo insertan.
- `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt:52-71,83-104,121-144,156-206`: mismo patrón para facturas, lecturas y estado de pago.
- `app/src/main/java/com/example/kilowatt/data/AppDatabase.kt:19-40` y `app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt:18-32`: fallback destructivo y dos rutas para construir la base.
- `app/src/main/res/xml/backup_rules.xml` y `app/src/main/res/xml/data_extraction_rules.xml:7-10`: reglas de backup sin política de inclusión/exclusión.
- `app/src/main/java/com/example/kilowatt/util/SettingsManager.kt:5-38`: datos de configuración de cobro almacenados en preferencias sin cifrado.
- `app/src/main/java/com/example/kilowatt/MainActivity.kt:151-157`: cast de la interfaz `ISettingsManager` a `SettingsManager`.
- `app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt:165-171,312-318`: uso de `printStackTrace()` y resultado `null` ante errores de generación.
- `app/build.gradle.kts:12-39`: SDK 34, JVM 1.8, versión fija `1.0` y R8 desactivado para release.
- `docs/ARQUITECTURA.md` y `docs/ROADMAP.md`: describen WorkManager como parte funcional de la sincronización, aunque el Worker actual es un placeholder; además, la arquitectura menciona Room v5 mientras la base está en versión 6.

## 4. Principios de implementación

1. **Room sigue siendo la fuente de lectura local.** La UI no leerá Firestore directamente.
2. **Toda escritura local debe tener un estado de sincronización explícito.**
3. **La sincronización debe ser idempotente.** Repetir una operación no puede duplicar ni corromper datos.
4. **Los errores no deben convertirse silenciosamente en estados válidos.**
5. **Cada iteración debe ser desplegable y reversible.**
6. **La migración de datos precede a la eliminación de mecanismos destructivos.**
7. **La seguridad se decide explícitamente.** No se confiará en los valores por defecto de Android.
8. **Los tests de repositorio y sincronización deben aislar red, reloj y almacenamiento.**

## 5. Matriz de prioridad

| ID | Deuda | Prioridad | Dependencias | Resultado esperado |
|---|---|---:|---|---|
| DT-06 | Contrato de estados y errores ambiguo | P0 | Ninguna | La UI distingue guardado local, sincronizado, pendiente y fallido |
| DT-07 | Sincronización diferida inexistente | P0 | DT-06 | Outbox persistente y Worker real con reintentos |
| DT-08 | Reconciliación incompleta de eliminaciones | P0 | DT-06, DT-07 | Room refleja altas, cambios y bajas remotas |
| DT-09 | Migración destructiva y DB duplicada | P0 | Pruebas de esquema | Datos locales protegidos ante upgrades |
| DT-10 | Backup y preferencias sensibles sin política | P1 | Decisión de producto/privacidad | Restauración controlada y configuración protegida |
| DT-11 | Firebase instanciado directamente | P1 | DT-06 | Repositorios sustituibles y testeables |
| DT-12 | PDF sin paginación ni error explícito | P1 | Contrato de errores | Reportes completos y fallos visibles |
| DT-13 | Release engineering incompleto | P2 | DT-09 | Builds reproducibles y artefactos preparados para release |
| DT-14 | Documentación divergente | P2 | Iteraciones anteriores | Documentación alineada con el comportamiento real |

---

## 6. Plan por iteraciones

### Iteración 1 — Contrato de errores y observabilidad

**Objetivo:** eliminar la ambigüedad entre “guardado local”, “sincronizado” y “fallido” antes de implementar reintentos.

**Componentes afectados**

- `IInquilinoRepository`
- `ILecturaRepository`
- `AuthRepository`
- ViewModels y estados de UI
- Logging de aplicación

**Trabajo**

1. Sustituir `Pair<Boolean, String?>` por un resultado tipado, por ejemplo `OperationResult`, con estados explícitos:
   - `Synced`
   - `PendingSync`
   - `Failed`
2. Definir qué errores son recuperables, cuáles requieren intervención del usuario y cuáles deben reintentarse.
3. Registrar contexto mínimo: operación, entidad, identificador lógico, usuario actual y causa técnica. No registrar contraseñas, tokens ni datos de pago completos.
4. Eliminar `catch (_: Exception) {}` en rutas de persistencia y reemplazarlo por propagación controlada.
5. Evitar que un `Flow` emita `emptyList()` como sustituto de un error de Room.

**Criterios de aceptación**

- La UI no muestra “guardado en la nube” cuando solo se guardó localmente.
- Un fallo de lectura de Room produce un estado de error observable, no una lista vacía indistinguible.
- Los errores incluyen una acción definida: reintentar, continuar con datos locales o solicitar autenticación.
- Existen tests para éxito remoto, éxito local pendiente y fallo no recuperable.

**Validación**

- Tests unitarios de repositorios con Firestore simulado.
- Tests de ViewModel para cada estado.
- Prueba manual con red desactivada durante una escritura.

**Riesgo y mitigación:** el cambio de contrato puede afectar Activities existentes. Mantener adaptadores temporales en los ViewModels y migrar una interfaz por vez.

### Iteración 2 — Outbox local y sincronización con WorkManager

**Objetivo:** hacer real la sincronización diferida declarada por la arquitectura.

**Componentes afectados**

- Room: nueva entidad de operaciones pendientes y DAO.
- `SyncWorker`
- Repositorios.
- Configuración de WorkManager.

**Trabajo**

1. Crear una outbox con:
   - tipo de entidad;
   - operación (`CREATE`, `UPDATE`, `DELETE`);
   - identificador del usuario;
   - identificador del documento;
   - payload serializado/versionado;
   - fecha de creación y último intento;
   - número de intentos;
   - estado;
   - mensaje de error técnico sanitizado.
2. Hacer que la escritura local y el alta de la operación pendiente sean atómicas en Room.
3. Implementar `SyncWorker` con:
   - restricción de red;
   - procesamiento acotado por lote;
   - reintento con backoff;
   - `Result.retry()` solo para fallos recuperables;
   - `Result.failure()` para operaciones inválidas que requieren intervención.
4. Programar trabajo único por usuario con una política que evite Workers duplicados.
5. Usar IDs deterministas y operaciones idempotentes en Firestore.
6. Eliminar operaciones pendientes al cerrar sesión o asociarlas inequívocamente al UID para impedir fugas entre cuentas.

**Criterios de aceptación**

- Una escritura sin red aparece en Room y en la outbox.
- Al recuperar conectividad, el Worker publica la operación y la marca como sincronizada.
- Un fallo temporal se reintenta; un payload inválido no entra en un bucle infinito.
- Reiniciar la aplicación o ejecutar el Worker dos veces no duplica datos.
- El logout no deja operaciones capaces de escribir para la cuenta anterior.

**Validación**

- Tests de DAO para estados y transiciones.
- Tests del Worker con red disponible, no disponible y fallo permanente.
- Prueba de proceso: escribir offline, cerrar aplicación, recuperar red y verificar Firestore.

**Riesgo y mitigación:** la outbox introduce un nuevo esquema. Añadir una migración explícita y habilitar primero la lectura de la outbox antes de activar la publicación automática.

### Iteración 3 — Reconciliación Room–Firestore

**Objetivo:** reflejar también las bajas remotas y evitar datos obsoletos.

**Componentes afectados**

- `InquilinoRepository`
- `LecturaRepository`
- DAOs de Room
- Listeners de Firestore

**Trabajo**

1. Procesar `DocumentChange.Type.ADDED`, `MODIFIED` y `REMOVED`.
2. Añadir operaciones DAO para borrar por ID y, si es necesario, reemplazar un conjunto completo dentro de una transacción.
3. Verificar el UID capturado por cada listener antes de escribir.
4. Definir una política de conflicto entre cambios locales pendientes y cambios remotos.
5. Asegurar que cambios remotos no borren datos locales todavía pendientes de publicación sin registrarlos como conflicto.
6. Revisar la normalización de IDs y periodos para que la clave local coincida con la remota.

**Criterios de aceptación**

- Borrar un documento en Firestore elimina el registro correspondiente en Room.
- Un documento modificado remotamente actualiza la UI sin reiniciar la Activity.
- Un cambio de cuenta no mezcla datos ni listeners.
- Los conflictos quedan registrados y no se resuelven mediante sobrescritura silenciosa.

**Validación**

- Tests de cada tipo de `DocumentChange`.
- Test de logout durante una notificación remota.
- Prueba con dos clientes modificando y eliminando el mismo registro.

### Iteración 4 — Migraciones Room seguras y única composición de DB

**Objetivo:** evitar pérdida de datos al actualizar la aplicación.

**Componentes afectados**

- `AppDatabase`
- `DatabaseModule`
- Tests de migración
- Configuración de exportación de esquemas

**Trabajo**

1. Mantener una sola ruta de construcción: la instancia proporcionada por Hilt.
2. Retirar `AppDatabase.getDatabase()` si no tiene consumidores, o convertirlo en una única fuente compartida con Hilt.
3. Eliminar `fallbackToDestructiveMigration()` después de cubrir todas las rutas de migración soportadas.
4. Activar `exportSchema` y guardar esquemas versionados en el repositorio.
5. Añadir tests de migración desde cada versión soportada, incluyendo la outbox.
6. Documentar la política para instalaciones con versiones demasiado antiguas.

**Criterios de aceptación**

- Ninguna configuración de producción usa migración destructiva.
- Las migraciones preservan inquilinos, medidores, facturas, lecturas y outbox.
- La aplicación falla de forma explícita si encuentra una versión no soportada, sin borrar datos.
- Existe una única configuración de nombre, versión y migraciones.

### Iteración 5 — Privacidad, backups y configuración local

**Objetivo:** controlar qué información puede salir del dispositivo y proteger la configuración local.

**Componentes afectados**

- `AndroidManifest.xml`
- `backup_rules.xml`
- `data_extraction_rules.xml`
- `SettingsManager`
- Política de privacidad y restauración

**Trabajo**

1. Decidir si Room y preferencias se excluyen completamente del backup debido a que Firestore es la fuente remota autenticada.
2. Definir reglas equivalentes para Android 11 y Android 12+.
3. Evaluar `EncryptedSharedPreferences` o una solución respaldada por Android Keystore para los valores sensibles.
4. Revisar qué datos se necesitan localmente para operar offline.
5. Documentar el comportamiento esperado al restaurar la aplicación en otro dispositivo.

**Criterios de aceptación**

- Las reglas de backup no son plantillas sin decisión.
- Restaurar una instalación no mezcla datos entre cuentas.
- Los valores sensibles no aparecen en logs ni en artefactos de backup no autorizados.
- Logout y borrado de cuenta tienen una política consistente.

**Riesgo y mitigación:** cambiar el almacenamiento de preferencias puede invalidar valores existentes. Implementar lectura dual temporal y migración controlada antes de retirar el formato anterior.

### Iteración 6 — Desacoplamiento de Firebase y testabilidad

**Objetivo:** reducir dependencias globales y hacer testeable la capa de datos.

**Componentes afectados**

- Módulos Hilt.
- Repositorios.
- `MainActivity`.
- Tests unitarios.

**Trabajo**

1. Proveer `FirebaseAuth` y `FirebaseFirestore` mediante Hilt.
2. Inyectar referencias de colección o un gateway de Firestore para centralizar rutas y conversiones.
3. Eliminar casts de interfaces a implementaciones concretas, especialmente en `MainActivity`.
4. Extraer validaciones y normalización de documentos a componentes puros.
5. Añadir fakes para Firestore, autenticación, reloj y conectividad.

**Criterios de aceptación**

- Los repositorios no llaman directamente a `getInstance()`.
- Las Activities dependen de interfaces.
- Los tests no requieren una instancia real de Firebase.
- Las rutas Firestore están centralizadas y cubiertas por tests.

### Iteración 7 — PDF, release engineering y calidad operativa

**Objetivo:** hacer confiables los artefactos que llegan al usuario.

**Componentes afectados**

- `PdfGenerator`
- `app/build.gradle.kts`
- Pipeline de build y tests

**Trabajo**

1. Implementar paginación del reporte general y límites de texto medidos por `Paint`.
2. Sustituir `printStackTrace()` por un resultado de error utilizable y logging controlado.
3. Verificar cierre de `PdfDocument` también en errores de inicialización.
4. Revisar compatibilidad de `compileSdk`/`targetSdk` y documentar la política de actualización.
5. Definir `versionCode`/`versionName` desde una única fuente.
6. Activar R8 en release después de verificar reglas de Room, Hilt, Firebase, WorkManager y PDF.
7. Añadir una tarea reproducible de lint, tests unitarios, tests instrumentados y build release.

**Criterios de aceptación**

- Un reporte con más elementos que el espacio disponible genera varias páginas.
- Los errores de PDF llegan a la UI como estado explicable.
- El build release pasa con minificación y shrink activados.
- El artefacto tiene versión reproducible y trazable.

### Iteración 8 — Alineación documental y cierre

**Objetivo:** que la documentación refleje el sistema real y que la deuda restante sea visible.

**Trabajo**

1. Actualizar `docs/ARQUITECTURA.md` con el flujo real de outbox, WorkManager y reconciliación.
2. Actualizar `docs/ROADMAP.md` solo con capacidades implementadas y pendientes reales.
3. Corregir la versión de Room y la cantidad/metodología de pruebas documentadas.
4. Mantener `docs/DEUDA_TECNICA.md` como índice breve, enlazando documentos de implementación.
5. Registrar decisiones no implementadas como deuda explícita con responsable y criterio de salida.

**Criterios de aceptación**

- No se declara funcional un componente placeholder.
- Las cifras de pruebas se generan o verifican desde el repositorio.
- Cada deuda P0/P1 tiene enlace a una propuesta o issue de ejecución.

## 7. Estrategia de entrega y reversibilidad

1. **Feature flag de sincronización:** activar el Worker real por etapas, conservando lectura local estable.
2. **Migración antes de activación:** desplegar primero la tabla outbox y verificar su migración, luego activar escrituras pendientes.
3. **Telemetría mínima:** medir tamaño de outbox, errores por tipo, reintentos agotados y conflictos, sin datos personales.
4. **Rollback:** poder desactivar publicación automática sin eliminar la outbox ni los datos locales.
5. **Criterio de pausa:** detener la siguiente iteración si aumenta la pérdida de datos, la duplicación o el tiempo de apertura.
6. **Limpieza posterior:** retirar adaptadores temporales solo después de que todos los consumidores usen el contrato nuevo.

## 8. Estrategia de pruebas transversal

| Nivel | Cobertura requerida |
|---|---|
| Unitario | Estados de operación, normalización, claves, conflictos, cálculos y reglas de backup |
| Room | Migraciones, transacciones outbox, borrados y aislamiento por UID |
| Worker | Red ausente, reintento, fallo permanente, duplicación y cancelación |
| Integración | Firestore simulado con altas, modificaciones, bajas y cambio de cuenta |
| Instrumentado | Logout, restauración, navegación y generación/compartición de PDF |
| Manual | Escenarios offline prolongados, reinicio de proceso y dos dispositivos |

## 9. Definition of Done para cada iteración

- El cambio está implementado sin modificar reglas de negocio no relacionadas.
- Existe al menos una prueba automatizada para el riesgo principal.
- Los errores recuperables y no recuperables están diferenciados.
- La documentación del módulo fue actualizada.
- El build y las pruebas aplicables pasan.
- Existe una forma de desactivar o revertir la funcionalidad cuando el riesgo lo justifica.
- No se registran secretos, credenciales ni datos personales innecesarios.

## 10. Resultado esperado

Al finalizar las iteraciones, KiloWatt conservará su modelo MVVM/Room/Firestore, pero con una garantía operacional más precisa:

- los datos locales serán utilizables sin red;
- las escrituras pendientes tendrán una cola durable;
- la sincronización será reintentable e idempotente;
- las eliminaciones remotas se reflejarán localmente;
- las migraciones no destruirán datos;
- los errores serán visibles y accionables;
- la información local tendrá una política de backup y protección;
- los builds y documentos representarán el comportamiento real.

