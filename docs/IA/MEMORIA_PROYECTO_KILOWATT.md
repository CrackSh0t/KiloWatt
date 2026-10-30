# Memoria del Proyecto - Kilowatt

Este documento registra los hitos, decisiones y resúmenes de implementación clave en la evolución del proyecto **Kilowatt**. Servirá como registro histórico y punto de referencia para desarrolladores e inteligencias artificiales que intervengan en el sistema.

---

## Resumen de la Implementación: Recuperación de Contraseña por Correo Electrónico

### 1. Objetivo y Contexto

Se diseñó e implementó la funcionalidad de recuperación de contraseña para los usuarios administradores de Kilowatt, resolviendo una deuda técnica identificada en el roadmap del proyecto y permitiendo el restablecimiento seguro de claves de acceso vía correo electrónico.

---

### 2. Alcance Arquitectónico

La solución se construyó respetando la arquitectura por capas existente en el proyecto y los estándares de **Firebase Authentication**:

* **Capa de Datos ([`AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt)):**
  - Se agregó el método `enviarCorreoRecuperacion(email: String, onResult: (Boolean, String?) -> Unit)`.
  - Encapsula la llamada asíncrona a `FirebaseAuth.getInstance().sendPasswordResetEmail(email)`.

* **Interfaz de Usuario (Layout y Recursos):**
  - **[`activity_auth.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_auth.xml):** Se incorporó un `TextView` interactivo (`@+id/tvForgotPassword`) con texto `¿Olvidaste tu contraseña?` y selector de retroalimentación táctil (`selectableItemBackground`).
  - **[`strings.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/values/strings.xml):** Se incluyeron las cadenas localizadas en español para el enlace de olvido, título y mensaje del diálogo, etiquetas de botones y mensajes de éxito/error.

* **Controlador de Presentación ([`AuthActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthActivity.kt)):**
  - Manejo del evento de clic sobre `tvForgotPassword`.
  - Construcción dinámica de un diálogo `MaterialAlertDialogBuilder` con un campo de texto de entrada de correo (`TextInputEditText`).
  - Precarga automática del correo si el usuario ya lo había escrito previamente en el formulario principal.
  - Notificación de resultados mediante avisos flotantes `Toast` con mensajes claros de confirmación o descripción detallada en caso de error.

---

### 3. Detalle de Archivos Involucrados

#### Archivos de Código y Recursos Modificados:
1. `app/src/main/java/com/example/kilowatt/data/AuthRepository.kt`
2. `app/src/main/java/com/example/kilowatt/AuthActivity.kt`
3. `app/src/main/res/layout/activity_auth.xml`
4. `app/src/main/res/values/strings.xml`

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md` - Actualización del módulo de autenticación a estado 100% operativo y eliminación de la limitación en deuda técnica.
2. `docs/ROADMAP.md` - Marcado de la tarea como completada (`[x]`) en la Fase 4.
3. `docs/CONTEXTO_IA.md` - Actualización de las secciones de contexto, características implementadas y roadmap.
4. `docs/CHANGELOG.md` - Registro detallado de cambios en la sección `[Sin Publicar]`.

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug` sin errores ni advertencias de sintaxis.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` obteniendo **11 pruebas pasadas, 0 falladas y 0 omitidas**.

---

## Resumen de la Implementación: Sistema de Cobranza Persistente (Fase 1)

### 1. Objetivo y Contexto

Se diseñó e implementó la primera versión del sistema de cobranza persistente en Kilowatt, transformando la etiqueta estática "Pendiente" en un estado dinámico y persistido que permite al arrendador cambiar el estado de cada cobro entre `PENDIENTE` y `PAGADO`.

---

### 2. Alcance Arquitectónico

* **Modelos de Datos y Dominio:**
  - **[`Lectura.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Lectura.kt):** Se añadió la propiedad `val estadoPago: String = "PENDIENTE"`.
  - **[`DetalleCobro.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt):** Se extendió con `idLectura`, `idSubmedidor` y `estadoPago`.
  - **[`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt):** Propaga el estado de pago almacenado en la lectura o asigna `"PENDIENTE"` por defecto.

* **Capa de Persistencia (Firestore & Room):**
  - **[`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt):** Método `actualizarEstadoPago(idLectura: String, nuevoEstado: String)` para actualizar el campo `estadoPago` en Cloud Firestore de manera atómica.
  - **[`LecturaDao.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaDao.kt) / [`AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt):** Actualización a versión 4 de la base de datos local y consulta `@Query("UPDATE lecturas SET estadoPago = :estadoPago WHERE idLectura = :idLectura")`.

* **Interfaz de Usuario (Presentación y Recursos):**
  - **[`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt):** Renderizado dinámico del chip de estado con estados `Pagado 🟢` (fondo y texto verdes) y `Pendiente 🟡` (fondo y texto ámbar). Evento de toque para activar el callback de cambio de estado.
  - **[`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt):** Lógica para conmutar de forma interactiva y asíncrona entre `PENDIENTE` y `PAGADO` guardando el cambio directamente en la base de datos.
  - **Recursos:** Adición de colores `green_success_bg` y textos localizados en `strings.xml`.

---

### 3. Detalle de Archivos Involucrados

#### Código y Recursos Modificados:
1. `app/src/main/java/com/example/kilowatt/data/Lectura.kt`
2. `app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt`
3. `app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt`
4. `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt`
5. `app/src/main/java/com/example/kilowatt/data/LecturaDao.kt`
6. `app/src/main/java/com/example/kilowatt/data/AppDatabase.kt`
7. `app/src/main/java/com/example/kilowatt/CobroAdapter.kt`
8. `app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt`
9. `app/src/main/res/values/strings.xml`
10. `app/src/main/res/values/colors.xml`

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/MODELO_DATOS.md`
4. `docs/CHANGELOG.md`
5. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **11 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Registro Auditable de Pago (Fase 2A)

### 1. Objetivo y Contexto

Se implementó la Fase 2A del sistema de cobranza en Kilowatt, transformando el marcado del estado `PAGADO` en un registro mínimo auditable mediante la captura automática de la fecha de transacción (`fechaPago`) y la selección del medio de pago utilizado (`metodoPago`).

---

### 2. Alcance Arquitectónico

* **Modelos de Datos y Dominio:**
  - **[`Lectura.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Lectura.kt):** Extendido con `val fechaPago: Long? = null` y `val metodoPago: String? = null`.
  - **[`DetalleCobro.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt):** Extendido con `fechaPago` y `metodoPago`.
  - **[`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt):** Propagación de los nuevos atributos de auditoría al mapear los detalles de cobro.

* **Capa de Persistencia (Firestore & Room):**
  - **[`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt):** Método `actualizarEstadoPago(idLectura, nuevoEstado, fechaPago, metodoPago)` para persistir de manera atómica los campos en Firestore.
  - **[`LecturaDao.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaDao.kt) / [`AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt):** Base de datos actualizada a versión 5 y consulta de actualización de campos en Room.

* **Interfaz de Usuario (Presentación y Recursos):**
  - **[`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt):** Al presionar para marcar como `PAGADO`, despliega un diálogo simple de selección de método de pago (Yape, Plin, Efectivo, Transferencia). Al confirmar, asigna automáticamente `System.currentTimeMillis()`. Si el cobro ya estaba `PAGADO`, se ofrece la opción de revertirlo a `PENDIENTE` (limpiando fecha y método).
  - **[`item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml) / [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt):** Muestra de la etiqueta de auditoría *"Pagado el 15/10/2026 vía Yape"* dentro de la tarjeta de cobro cuando el estado sea `PAGADO`.
  - **Recursos:** Adición de diálogos y etiquetas localizadas en `strings.xml`.

---

### 3. Detalle de Archivos Involucrados

#### Código y Recursos Modificados:
1. `app/src/main/java/com/example/kilowatt/data/Lectura.kt`
2. `app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt`
3. `app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt`
4. `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt`
5. `app/src/main/java/com/example/kilowatt/data/LecturaDao.kt`
6. `app/src/main/java/com/example/kilowatt/data/AppDatabase.kt`
7. `app/src/main/res/layout/item_cobro.xml`
8. `app/src/main/res/values/strings.xml`
9. `app/src/main/java/com/example/kilowatt/CobroAdapter.kt`
10. `app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt`

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/MODELO_DATOS.md`
4. `docs/CHANGELOG.md`
5. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **17 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Evaluación Dinámica del Estado VENCIDO (Fase 2B)

### 1. Objetivo y Contexto

Se implementó la Fase 2B del sistema de cobranza, incorporando la evaluación dinámica del estado `VENCIDO` para los cobros que no han sido marcados como `PAGADO` una vez superada la fecha límite del periodo, utilizando el parámetro `diaLimitePago` configurado por el usuario en `SettingsManager`.

---

### 2. Alcance Arquitectónico

* **Utilidades de Fecha y Negocio:**
  - **[`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt):** Utilitario en Kotlin encargado de parsear cadenas de `mesPeriodo` (formato en español o numérico) y calcular la fecha y hora exacta de vencimiento (día `diaLimitePago` del mes siguiente a la facturación a las 23:59:59).
  - **[`PeriodoDateUtilsTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/PeriodoDateUtilsTest.kt):** Suite de pruebas unitarias cubriendo escenarios de cálculo de límites y verificación de vencimiento.

* **Motor de Cálculo ([`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt)):**
  - Incorporación del parámetro `diaLimitePago` en `calcularResumenMensual(...)`.
  - Regla de negocio: Si el cobro no está `PAGADO` y la fecha actual supera el límite del periodo, se asigna dinámicamente el estado `"VENCIDO"`.

* **Interfaz de Usuario (Presentación y Recursos):**
  - **[`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt):** Renderizado del chip `Vencido 🔴` (fondo y texto rojos) acompañado del aviso de fecha límite incumplida (`"Venció el 05/09/2026"`).
  - **Recursos:** Adición de las cadenas localizadas `label_vencio_el` y colores de advertencia.

---

### 3. Detalle de Archivos Involucrados

#### Código, Pruebas y Recursos Modificados/Creados:
1. `app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt` [NUEVO]
2. `app/src/test/java/com/example/kilowatt/PeriodoDateUtilsTest.kt` [NUEVO]
3. `app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt`
4. `app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt`
5. `app/src/main/java/com/example/kilowatt/CobroAdapter.kt`
6. `app/src/main/res/values/strings.xml`

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/MODELO_DATOS.md`
4. `docs/CHANGELOG.md`
5. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **22 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Historial y Gráficos de Consumo (Fases 1, 2, 3 y 4)

### 1. Objetivo y Contexto

Se completó la implementación integral del módulo de Historial y Gráficos de Consumo en Kilowatt, construyendo la pantalla interactiva `HistorialConsumoActivity` alimentada en tiempo real desde Firestore con procesamiento asíncrono sobre `Dispatchers.Default`.

---

### 2. Alcance Arquitectónico

* **Librerías y Repositorio:**
  - Adición de `MPAndroidChart` (`v3.1.0`) en [`gradle/libs.versions.toml`](file:///home/fernando/StudioProjects/KiloWatt/gradle/libs.versions.toml) y repositorio JitPack en [`settings.gradle.kts`](file:///home/fernando/StudioProjects/KiloWatt/settings.gradle.kts).
  - Implementación del conector `obtenerTodasLasLecturasFlow()` en [`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt).

* **Modelos de Dominio y Procesador Asíncrono:**
  - **[`ConsumoHistorico.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/ConsumoHistorico.kt):** Estructuras `PuntoHistorico`, `HistoricoSubmedidor` y `ConsumoHistoricoResult`.
  - **[`ConsumoHistoricoCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ConsumoHistoricoCalculator.kt):** Calculador que ejecuta el agrupamiento y ordenamiento cronológico por fecha sobre `Dispatchers.Default` garantizando la fluidez de la UI.
  - **[`ConsumoHistoricoCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConsumoHistoricoCalculatorTest.kt):** Suite de pruebas unitarias verificando el ordenamiento por fecha, cálculo de promedios, meses con picos de consumo y agrupamiento por submedidor.

* **Interfaz de Usuario y Presentación:**
  - **[`HistorialConsumoActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoActivity.kt) / [`activity_historial_consumo.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_historial_consumo.xml):** Pantalla interactiva con `LineChart` para el inmueble matriz y `BarChart` con selector desplegable por submedidor/inquilino.
  - **Formateo Eje X:** Formateador `ValueFormatter` integrado con [`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt) para etiquetas limpias (ej. `"Ago 26"`).
  - **Acceso en Menú Principal:** Inclusión de la tarjeta de navegación en [`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) y declaración en `AndroidManifest.xml`.

---

### 3. Detalle de Archivos Involucrados

#### Código, Pruebas y Configuración Creados/Modificados:
1. `settings.gradle.kts`
2. `gradle/libs.versions.toml`
3. `app/build.gradle.kts`
4. `app/src/main/AndroidManifest.xml`
5. `app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt`
6. `app/src/main/java/com/example/kilowatt/data/ConsumoHistorico.kt` [NUEVO]
7. `app/src/main/java/com/example/kilowatt/util/ConsumoHistoricoCalculator.kt` [NUEVO]
8. `app/src/main/java/com/example/kilowatt/HistorialConsumoActivity.kt` [NUEVO]
9. `app/src/main/res/layout/activity_historial_consumo.xml` [NUEVO]
10. `app/src/main/java/com/example/kilowatt/MainActivity.kt`
11. `app/src/main/res/layout/activity_main.xml`
12. `app/src/main/res/values/strings.xml`
13. `app/src/test/java/com/example/kilowatt/ConsumoHistoricoCalculatorTest.kt` [NUEVO]

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/MODELO_DATOS.md`
4. `docs/CHANGELOG.md`
5. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **22 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Migración a MVVM (Iteración 2 - ResumenCobros)

### 1. Objetivo y Contexto

Se completó la Iteración 2 de la migración arquitectónica a MVVM, migrando la pantalla `ResumenCobrosActivity` para delegar la orquestación de datos, recálculo mensual de cobros y actualización de estados de pago a `ResumenCobrosViewModel` mediante `StateFlow`.

---

### 2. Alcance Arquitectónico

* **Modelos de Estado de UI:**
  - **[`ResumenCobrosUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosUiState.kt):** `data class` inmutable que retiene el estado de carga, lista de meses, mes seleccionado, factura activa, detalles de cobro e indicadores globales.

* **ViewModel y Lógica Asíncrona:**
  - **[`ResumenCobrosViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt):** Inyecta `ILecturaRepository`, `IInquilinoRepository` e `ISettingsManager`. Orquesta el cálculo del resumen en segundo plano (`Dispatchers.Default` inyectable) a través de `viewModelScope`.
  - **[`ResumenCobrosViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt):** Suite de pruebas unitarias verificando la carga correcta de meses, el cálculo de montos y la emisión de estados.

* **Refactorización de Vista:**
  - **[`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt):** Observa `uiState` utilizando `repeatOnLifecycle(Lifecycle.State.STARTED)` y delega Intents de WhatsApp y PDF en callbacks limpios.

---

### 3. Detalle de Archivos Involucrados

#### Código, Pruebas y Modificaciones:
1. `app/src/main/java/com/example/kilowatt/ResumenCobrosUiState.kt` [NUEVO]
2. `app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt` [NUEVO]
3. `app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt`
4. `app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt` [NUEVO]

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/CHANGELOG.md`
4. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **28 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Migración a MVVM (Iteración 3 - Lecturas)

### 1. Objetivo y Contexto

Se completó la Iteración 3 de la migración arquitectónica a MVVM, migrando la pantalla `LecturasActivity` para delegar la carga de lecturas previas, validación de monotonicidad (`lecturaActual >= lecturaAnterior`) y el guardado asíncrono de recibos base y lecturas a `LecturasViewModel` mediante `StateFlow`.

---

### 2. Alcance Arquitectónico

* **Modelos de Estado de UI:**
  - **[`LecturasUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasUiState.kt):** `data class` inmutable que retiene el periodo, submedidores disponibles, lectura previa del submedidor seleccionado, estado de progreso y mensajes de notificación.

* **ViewModel y Lógica Asíncrona:**
  - **[`LecturasViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasViewModel.kt):** Inyecta `ILecturaRepository` e `IInquilinoRepository`. Valida la monotonicidad coordinando con `CobroCalculator` y procesa el guardado en `viewModelScope`.
  - **[`LecturasViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt):** Suite de pruebas unitarias verificando la carga de submedidores, el rechazo de lecturas no monotónicas y el guardado exitoso.

* **Refactorización de Vista:**
  - **[`LecturasActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasActivity.kt):** Observa `uiState` utilizando `repeatOnLifecycle(Lifecycle.State.STARTED)` y gestiona avisos emergentes.

---

### 3. Detalle de Archivos Involucrados

#### Código, Pruebas y Modificaciones:
1. `app/src/main/java/com/example/kilowatt/LecturasUiState.kt` [NUEVO]
2. `app/src/main/java/com/example/kilowatt/LecturasViewModel.kt` [NUEVO]
3. `app/src/main/java/com/example/kilowatt/LecturasActivity.kt`
4. `app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt` [NUEVO]

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/CHANGELOG.md`
4. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **31 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Migración a MVVM (Iteración 4 y Cierre de Fase 1 - Auth & Configuracion)

### 1. Objetivo y Contexto

Se completó la Iteración 4 y el cierre de la Fase 1 de la migración arquitectónica a MVVM, migrando los módulos de Autenticación (`AuthActivity`) y Configuración (`ConfiguracionActivity`) hacia `AuthViewModel` y `ConfiguracionViewModel` con `StateFlow`, consolidando el 100% de las pantallas bajo MVVM y alcanzando 36 pruebas unitarias en verde.

---

### 2. Alcance Arquitectónico

* **Contratos y Modelos de Estado de UI:**
  - **[`IAuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IAuthRepository.kt) / [`ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt):** Interfaces para abstracción de autenticación y preferencias.
  - **[`AuthUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthUiState.kt) / [`ConfiguracionUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionUiState.kt):** Estados inmutables para auth y configuración.

* **ViewModels y Lógica Asíncrona:**
  - **[`AuthViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthViewModel.kt):** Inyecta `IAuthRepository`, gestionando login, registro, envío de correos de recuperación y verificación de sesión.
  - **[`ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt):** Inyecta `IInquilinoRepository` e `ISettingsManager`. Centraliza la validación E.164 de teléfonos (`PhoneUtils`) y la persistencia de perfiles y inquilinos.

* **Pruebas Unitarias:**
  - **[`AuthViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AuthViewModelTest.kt)** y **[`ConfiguracionViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt)** validando de forma aislada las interacciones y validaciones de ambos ViewModels.

---

### 3. Detalle de Archivos Involucrados

#### Código, Pruebas y Modificaciones:
1. `app/src/main/java/com/example/kilowatt/data/IAuthRepository.kt` [NUEVO]
2. `app/src/main/java/com/example/kilowatt/AuthUiState.kt` [NUEVO]
3. `app/src/main/java/com/example/kilowatt/AuthViewModel.kt` [NUEVO]
4. `app/src/main/java/com/example/kilowatt/AuthActivity.kt`
5. `app/src/test/java/com/example/kilowatt/AuthViewModelTest.kt` [NUEVO]
6. `app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt` [NUEVO]
7. `app/src/main/java/com/example/kilowatt/ConfiguracionUiState.kt` [NUEVO]
8. `app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt` [NUEVO]
9. `app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt`
10. `app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt` [NUEVO]

#### Documentación Sincronizada:
1. `docs/ESTADO_DEL_PROYECTO.md`
2. `docs/ROADMAP.md`
3. `docs/CHANGELOG.md`
4. `docs/IA/MEMORIA_PROYECTO_KILOWATT.md`

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **36 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Personalización de Comprobantes PDF (Fase 3 Iteración 1 - PdfGenerator)

### 1. Objetivo y Contexto

Se completó la Iteración 1 de la Fase 3, refactorizando [`PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt) para incorporar datos de perfil del propietario (`ISettingsManager`) y sellos de estado de pago en los comprobantes PDF.

---

### 2. Alcance Arquitectónico

* **Parametrización e Inyección en `PdfGenerator`:**
  - Integración de `ISettingsManager` por constructor con fallback seguro para testing o instanciación directa.
* **Diseño e Impronta Visual:**
  - Cabecera A6/A5 estilizada con nombre del inmueble o propietario.
  - Sello de estado de pago destacado (`PAGADO ✅`, `VENCIDO 🔴`, `PENDIENTE 🟡`).
  - Pie de página con información de contacto para pagos digitales (Yape / Plin / Transferencias) y advertencia de día límite.

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt)

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **36 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Sellos Visuales en UI (Fase 3 Iteración 2 y Cierre Fase 3 - CobroAdapter)

### 1. Objetivo y Contexto

Se completó la Iteración 2 y el cierre de la Fase 3, refactorizando [`item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml) y [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt) para renderizar distintivos de estado con esquemas de color contrastantes y etiquetas emocionales (`PAGADO ✅`, `VENCIDO 🔴`, `PENDIENTE 🟡`, `Sin Lectura ⚠️`).

---

### 2. Alcance Arquitectónico

* **Diseño e Interfaz:**
  - `cardEstadoPago` (`MaterialCardView` con radio de 16dp) para el distintivo de estado.
  - Colores dinámicos y detalles de auditoría de pago o fecha de vencimiento.

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/res/layout/item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml)
2. [`app/src/main/java/com/example/kilowatt/CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt)

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **36 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Fix de Seguridad Multi-Tenant y Purga Atómica de Room y SharedPreferences al Cerrar Sesión

### 1. Objetivo y Contexto

Se corrigió la falla de aislamiento de datos al cambiar de usuario en el mismo dispositivo, implementando la purga atómica de la base de datos SQLite (`database.clearAllTables()`) y de las preferencias locales (`settingsManager.limpiarConfiguracion()`) en `AuthRepository.kt` al ejecutar el cierre de sesión, y filtrando la ejecución de listeners remotos en `LecturaRepository.kt` e `InquilinoRepository.kt` mediante la verificación de `currentUid`.

---

### 2. Alcance Arquitectónico

* **Purga Atómica Completa:** Invocación de `database.clearAllTables()` y `settingsManager.limpiarConfiguracion()` en `Dispatchers.IO` dentro de `cerrarSesion()`.
* **Aislamiento por UID:** Validación de coincidencia de UID en los listeners en tiempo real de Firestore.
* **Pruebas Unitarias:** Cobertura añadida en `AuthViewModelTest.kt`, alcanzando **37 pruebas unitarias pasadas al 100%**.

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/data/AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt)
2. [`app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt)
3. [`app/src/main/java/com/example/kilowatt/util/SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt)
4. [`app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt)
5. [`app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt)
6. [`app/src/test/java/com/example/kilowatt/AuthViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AuthViewModelTest.kt)

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **37 pruebas pasadas, 0 falladas**.

---

## Resumen de la Implementación: Fix de Estabilidad en Logout/Login y Duplicación Temporal en Inquilinos

### 1. Objetivo y Contexto

Se corrigieron dos anomalías en pruebas extremas: la duplicación visual de inquilinos al guardar y el crash esporádico al cambiar de cuenta.

---

### 2. Alcance Arquitectónico

* **IDs Unificados de Clave Primaria:** Pre-generación de IDs desde la referencia de Firestore en `InquilinoRepository.kt`, garantizando reemplazo atómico por clave primaria `@PrimaryKey` en Room (`OnConflictStrategy.REPLACE`).
* **Desenganche de Listeners Remotos (`cancelarListeners()`):** Invocación explícita de `cancelarListeners()` en los repositorios antes de ejecutar la purga SQLite `database.clearAllTables()`, eliminando conflictos de concurrencia al cerrar sesión.

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt)
2. [`app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt)
3. [`app/src/main/java/com/example/kilowatt/data/AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt)
4. [`app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt)
5. [`app/src/main/java/com/example/kilowatt/data/ILecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/ILecturaRepository.kt)

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de la tarea `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **37 pruebas pasadas, 0 falladas**.

---

## Propuesta de Arquitectura: Integración de Hilt (Inyección de Dependencias)

Se elaboró y registró el documento técnico [`Propuesta_Integracion_Hilt.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Propuesta_Integracion_Hilt.md) definiendo la estrategia para integrar Dagger Hilt en Kilowatt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `RepositoryModule` y `AppModule`).

---

## Resumen de la Implementación: Prorrateo de Área Común por Ocupación / Personas (Fase 5 - Iteración 5.1)

### 1. Objetivo y Contexto

Se implementó la base de dominio y el motor matemático para el cálculo de costos de áreas comunes ponderado por cantidad de habitantes u ocupantes (`WEIGHTED_PERSONS`), permitiendo una facturación justa en inmuebles multifamiliares y preservando la retrocompatibilidad total con la modalidad equitativa tradicional (`EQUITY`).

---

### 2. Alcance Arquitectónico

* **Modelos de Dominio y Datos:**
  - **[`TipoProrrateoComun.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/TipoProrrateoComun.kt) [NUEVO]:** Enum con los valores `EQUITY` (partes iguales) y `WEIGHTED_PERSONS` (ponderado por ocupantes).
  - **[`Inquilino.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Inquilino.kt):** Extensión de la entidad con `val numeroPersonas: Int = 1`.

* **Motor de Cálculo:**
  - **[`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt):** Adición del parámetro `tipoProrrateoComun: TipoProrrateoComun = EQUITY` en `calcularResumenMensual`. Implementación de la fórmula proporcional:
    $$P_{total} = \sum \max(1, \text{Personas}_i), \quad C_{persona} = \frac{\text{TotalSoles}_{comun}}{P_{total}}, \quad \text{Cuota}_i = \text{Personas}_i \times C_{persona}$$
    Incluye sanitización defensiva mediante `.coerceAtLeast(1)` y protección contra división por cero si $P_{total} == 0$.

* **Pruebas Unitarias:**
  - **[`CobroCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt):** Cuatro nuevos casos de prueba unitaria validando proporcionalidad ponderada (4 vs 2 personas), fallbacks ante valores nulos o negativos, conservación de comportamiento equitativo y suma exacta al 100% frente al recibo general.

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/data/TipoProrrateoComun.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/TipoProrrateoComun.kt) [NUEVO]
2. [`app/src/main/java/com/example/kilowatt/data/Inquilino.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Inquilino.kt)
3. [`app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt)
4. [`app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt)
5. [`docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_1.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_1.md) [NUEVO]

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **41 pruebas pasadas, 0 falladas, 0 omitidas** (100% en verde).

---

## Resumen de la Implementación: Persistencia, Migración Room v6 y Sincronización Firestore (Fase 5 - Iteración 5.2)

### 1. Objetivo y Contexto

Se implementó la persistencia completa para la propiedad de ocupación `numeroPersonas` y la preferencia global `tipoProrrateoComun`, asegurando una migración SQLite sin pérdida de datos en Room (`MIGRATION_5_6`), sincronización reactiva en Cloud Firestore con normalización defensiva ante documentos históricos y consistencia entre `SettingsManager` y el backend.

---

### 2. Alcance Arquitectónico

* **Migración de Base de Datos Local (Room):**
  - **[`AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt):** Declaración de `MIGRATION_5_6` (`ALTER TABLE inquilinos ADD COLUMN numeroPersonas INTEGER NOT NULL DEFAULT 1`), incremento de versión a 6 y registro de `.addMigrations(MIGRATION_5_6)`.
  - **[`DatabaseModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt):** Registro de la migración en el provider de Hilt para la instancia inyectada de `AppDatabase`.

* **Capa de Datos y Repositorios:**
  - **[`IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt) / [`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt):** Soporte de `numeroPersonas` en operaciones `guardarInquilinoYSubmedidor` y `actualizarInquilinoYSubmedidor`. Normalización defensiva con `.coerceAtLeast(1)` en la escucha en tiempo real de Firestore (`sincronizarInquilinosRemotos`). Respaldo del parámetro `tipoProrrateoComun` en `guardarPerfilPropietario`.

* **Gestor de Preferencias (SettingsManager):**
  - **[`ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt) / [`SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt):** Adición de la propiedad `var tipoProrrateoComun: String` respaldada en `SharedPreferences` con valor por defecto `"EQUITY"`.
  - **[`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) / [`ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt):** Sincronización en tiempo real de la preferencia con el perfil de Firestore.

* **Pruebas Unitarias:**
  - **[`AppDatabaseMigrationTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt) [NUEVO]:** Verificación del DDL de migración y versiones inicial/final mediante proxy dinámico. Total de pruebas incrementado a 44 (100% éxito).

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/data/AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt)
2. [`app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt)
3. [`app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt)
4. [`app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt)
5. [`app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt)
6. [`app/src/main/java/com/example/kilowatt/util/SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt)
7. [`app/src/main/java/com/example/kilowatt/MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt)
8. [`app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt)
9. [`app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AppDatabaseMigrationTest.kt) [NUEVO]
10. [`docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_2.md) [NUEVO]

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **44 pruebas pasadas, 0 falladas, 0 omitidas** (100% en verde).

---

## Resumen de la Implementación: UI de Configuración y Gestión de Inquilinos (Fase 5 - Iteración 5.3)

### 1. Objetivo y Contexto

Se implementaron las interfaces de usuario para la captura del número de ocupantes en los formularios de registro y edición de inquilinos, la visualización del badge de personas en las tarjetas de inquilinos, el selector de modalidad de prorrateo (`EQUITY` vs `WEIGHTED_PERSONS`) en el perfil del propietario, y las validaciones correspondientes en el ViewModel respaldadas por pruebas unitarias.

---

### 2. Alcance Arquitectónico

* **Formularios e Interfaz de Usuario:**
  - **[`activity_configuracion.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_configuracion.xml) / [`dialog_editar_inquilino.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/dialog_editar_inquilino.xml):** Adición de los campos numéricos `etNumeroPersonas` y `etEditNumeroPersonas` ("Cantidad de Ocupantes / Personas").
  - **[`item_inquilino.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_inquilino.xml) / [`InquilinoAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/InquilinoAdapter.kt):** Adición y binding dinámico del badge `tvPersonas` (`👥 X pers.`), oculto en submedidores de área común.
  - **[`dialog_perfil_propietario.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/dialog_perfil_propietario.xml) / [`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt):** Integración de `MaterialAutoCompleteTextView` para la selección de modalidad de prorrateo de área común con sincronización en `SettingsManager` e `InquilinoRepository.guardarPerfilPropietario`.

* **Capa de Presentación y Validaciones:**
  - **[`ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt):** Extensión de `guardarDatosInquilino` aceptando `numeroPersonas: Int = 1` con sanitización `.coerceAtLeast(1)`.
  - **[`ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt):** Lógica interactiva con deshabilitación dinámica de campos cuando se activa el CheckBox de área común.

* **Pruebas Unitarias:**
  - **[`ConfiguracionViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt):** Pruebas unitarias para la captura y sanitización defensiva del número de ocupantes. Total de pruebas incrementado a 45 (100% éxito).

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/res/values/strings.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/values/strings.xml)
2. [`app/src/main/res/layout/activity_configuracion.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_configuracion.xml)
3. [`app/src/main/res/layout/dialog_editar_inquilino.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/dialog_editar_inquilino.xml)
4. [`app/src/main/res/layout/item_inquilino.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_inquilino.xml)
5. [`app/src/main/res/layout/dialog_perfil_propietario.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/dialog_perfil_propietario.xml)
6. [`app/src/main/java/com/example/kilowatt/InquilinoAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/InquilinoAdapter.kt)
7. [`app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt)
8. [`app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt)
9. [`app/src/main/java/com/example/kilowatt/MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt)
10. [`app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt)
11. [`docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_3.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_3.md) [NUEVO]

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **45 pruebas pasadas, 0 falladas, 0 omitidas** (100% en verde).

---

## Resumen de la Implementación: Visualización en Resumen de Cobros y Desglose en PDF (Fase 5 - Iteración 5.4 - Cierre Fase 5)

### 1. Objetivo y Contexto

Se completó la Fase 5 conectando la preferencia global `tipoProrrateoComun` con `ResumenCobrosViewModel`, reflejando la cuota de área común y el número de habitantes en las tarjetas de la UI (`item_cobro.xml` / `CobroAdapter.kt`), en los comprobantes en PDF (`PdfGenerator.kt`) y en las notificaciones de WhatsApp.

---

### 2. Alcance Arquitectónico

* **Inyección de Preferencia y Cálculo Reactivo:**
  - **[`ResumenCobrosViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt):** Integración de `obtenerPerfilPropietarioFlow()` en el operador `combine` de `cargarDatosDelMes`. Recálculo automático del resumen de cobros al cambiar de modalidad (`EQUITY` vs `WEIGHTED_PERSONS`) o cantidad de ocupantes.

* **Dominio y Motor de Cálculo:**
  - **[`DetalleCobro.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt) / [`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt):** Inclusión de `numeroPersonas` en `DetalleCobro` y asignación automática desde la entidad `Inquilino`.

* **Interfaz de Usuario y Exportaciones:**
  - **[`item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml) / [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt):** Renderizado de `tvCuotaComunDetalle` mostrando la cuota de área común y cantidad de ocupantes.
  - **[`PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt) / [`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt):** Personalización de la cuota común en comprobantes PDF A6 y mensajes de WhatsApp.

* **Pruebas Unitarias:**
  - **[`ResumenCobrosViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt):** Extensión de suite validando prorrateo ponderado con múltiples departamentos y habitantes. Total de pruebas incrementado a 46 (100% éxito).

---

### 3. Detalle de Archivos Involucrados

1. [`app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt)
2. [`app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt)
3. [`app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt)
4. [`app/src/main/res/layout/item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml)
5. [`app/src/main/java/com/example/kilowatt/CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt)
6. [`app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt)
7. [`app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt)
8. [`app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt)
9. [`docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_4.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Implementadas/Resumen_Impl_Prorrateo_Area_Comun_Personas_Iteracion_5_4.md) [NUEVO]

---

### 4. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **46 pruebas pasadas, 0 falladas, 0 omitidas** (100% en verde).



