# Registro de Cambios (Changelog) - Kilowatt

Todas las modificaciones notables realizadas en el proyecto Kilowatt están documentadas en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y se adhiere a las directrices de versiones semánticas ([SemVer](https://semver.org/lang/es/)).

---

## [Sin Publicar]

### Arreglado
- **Purga de SharedPreferences / SettingsManager al Cerrar Sesión:** Adición de `limpiarConfiguracion()` en [`ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt) e invocación automática en [`AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt) al cerrar sesión, asegurando que los datos de cobro del propietario no persistan localmente al cambiar de cuenta.
- **Aislamiento Multi-Tenant y Purga Atómica de Room al Cerrar Sesión:** Invocación explícita de `database.clearAllTables()` en [`AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt) al cerrar sesión para garantizar la eliminación de la caché local del usuario saliente antes de desautenticar en Firebase Auth. Verificación de `currentUid` en listeners remotos de [`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt) e [`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt) para prevenir contaminación cruzada entre sesiones en un mismo dispositivo.
- **Compatibilidad con KSP2 y Room Compiler:** Actualización de la dependencia `androidx.room` de la versión `2.6.1` a la `2.8.5` en el catálogo de versiones ([`gradle/libs.versions.toml`](file:///home/fernando/StudioProjects/KiloWatt/gradle/libs.versions.toml#L12)). Soluciona el error de compilación `[ksp] java.lang.IllegalStateException: unexpected jvm signature V` producido por la incompatibilidad de KSP2 (`KspAAWorkerAction`) con métodos de suspensión en DAOs de Room.
- **Sincronización del Entorno de Construcción:** Configuración optimizada de Gradle para compatibilidad total con Kotlin 2.x y KSP2.

### Añadido
- **Estandarización de TopAppBar M3, Formularios OutlinedBox y Ajustes en MPAndroidChart (Fase 4 Iteración 2 y Cierre Fase 4):**
  - Implementación de `MaterialToolbar` unificada con botón de retorno `←` en `LecturasActivity`, `ResumenCobrosActivity` e `HistorialConsumoActivity`.
  - Homogeneización de todos los formularios de entrada en `activity_lecturas.xml` al estilo `TextInputLayout.OutlinedBox`.
  - Configuración de `extraBottomOffset = 15f` en los gráficos de `MPAndroidChart` (`HistorialConsumoActivity`) para evitar recortes en etiquetas de fecha.
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Centro de Perfil del Propietario y Simplificación de ConfiguracionActivity (Fase 4 Iteración 1):**
  - Incorporación de botón/ícono Avatar en la cabecera superior de [`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) que despliega el diálogo Material 3 [`dialog_perfil_propietario.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/dialog_perfil_propietario.xml).
  - Centralización del perfil de cobro (Titular, Yape/Plin, Día Límite), correo de cuenta y botón de Cierre de Sesión en el modal de Perfil.
  - Simplificación de [`ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt) y [`activity_configuracion.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_configuracion.xml) enfocándolas exclusivamente en "Inquilinos y Medidores".
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Sellos Visuales de Estado de Pago en UI (Fase 3 Iteración 2 y Cierre Fase 3 - CobroAdapter):**
  - Adición de tarjeta distintiva redondeada (`cardEstadoPago`) en [`item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml).
  - Renderizado dinámico con fondos de alto contraste y etiquetas emojis (`PAGADO ✅`, `VENCIDO 🔴`, `PENDIENTE 🟡`, `Sin Lectura ⚠️`) en [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt).
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Personalización de Comprobantes PDF y Sellos Visuales (Fase 3 Iteración 1 - PdfGenerator):**
  - Inyección de `ISettingsManager` en [`PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt).
  - Personalización de cabeceras en comprobantes A6 y reportes A5 con nombre del propietario, número Yape/Plin y día límite de pago.
  - Renderizado dinámico de distintivos gráficos de estado de pago (`PAGADO ✅`, `VENCIDO 🔴`, `PENDIENTE 🟡`) en los recibos impresos.
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Soporte Offline y Consistencia de Datos (Fase 2 Iteración 2 - InquilinoRepository SSOT & WorkManager):**
  - Refactorización SSOT en [`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt) inyectando `InquilinoDao` y `SubmedidorDao` vía Hilt, permitiendo lectura local y sincronización asíncrona en background con Firestore.
  - Integración de WorkManager mediante [`SyncWorker.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/worker/SyncWorker.kt) para tareas de sincronización diferida.
  - Actualización de [`ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt) con inyección de dependencias por campo mediante `@Inject`.
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Soporte Offline y Consistencia de Datos (Fase 2 Iteración 1 - LecturaRepository SSOT):**
  - Creación del módulo Hilt [`DatabaseModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/DatabaseModule.kt) para proveer `AppDatabase` y DAOs (`LecturaDao`, `FacturaGeneralDao`, `InquilinoDao`, `SubmedidorDao`).
  - Adición de consulta en [`LecturaDao.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaDao.kt) (`obtenerTodasLasLecturas`).
  - Refactorización SSOT en [`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt): lectura local directa desde SQLite (Room) con sincronización en segundo plano hacia Cloud Firestore.
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Inyección de Dependencias con Dagger Hilt v2.60.1 (Cierre de Fase 1):**
  - Configuración e integración completa de Dagger Hilt en el proyecto (`libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`).
  - Creación de la clase [`KilowattApp.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/KilowattApp.kt) anotada con `@HiltAndroidApp` y registrada en [`AndroidManifest.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/AndroidManifest.xml).
  - Implementación de los módulos de inyección [`RepositoryModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/RepositoryModule.kt) (`@Binds` para repositorios) y [`AppModule.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/di/AppModule.kt) (`@Provides` para ajustes y dispatchers).
  - Inyección por constructor (`@Inject constructor`) en repositorios y ViewModels (`@HiltViewModel`), junto con la anotación `@AndroidEntryPoint` en todas las actividades (`MainActivity`, `AuthActivity`, `ConfiguracionActivity`, `LecturasActivity`, `ResumenCobrosActivity`, `HistorialConsumoActivity`).
  - Verificación mediante ejecución exitosa de las 36 pruebas unitarias al 100%.
- **Lógica de "Cierre de Caja Exacto" en Resumen de Cobros:**
  - Inclusión de todos los submedidores e inquilinos registrados en la vista de resumen mensual ([`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt)), incluso si no registran lectura en el mes seleccionado.
  - Asignación automática de `0 kWh` de consumo propio a inquilinos sin lectura en el periodo, manteniendo la asignación proporcional de la cuota de área común.
  - Garantía de coincidencia matemática del 100% entre el monto total del recibo principal y la suma de cobros individuales emitidos mediante el reparto equitativo del saldo sobrante entre los pagadores autorizados.
- **Migración Arquitectónica a MVVM (Iteración 4 y Cierre Fase 1 - Auth & Configuracion):**
  - Creación de contratos de interfaz [`IAuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IAuthRepository.kt) e [`ISettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/ISettingsManager.kt).
  - Creación de modelos de estado [`AuthUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthUiState.kt) y [`ConfiguracionUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionUiState.kt).
  - Creación de [`AuthViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthViewModel.kt) y [`ConfiguracionViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionViewModel.kt) (centralizando validación E.164 de PhoneUtils, login, registro, recuperación y gestión de perfiles).
  - Refactorización completa de [`AuthActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthActivity.kt) y [`ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt) para observar `uiState` vía `repeatOnLifecycle(STARTED)`.
  - Cobertura con suites de pruebas unitarias aisladas en [`AuthViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/AuthViewModelTest.kt) y [`ConfiguracionViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConfiguracionViewModelTest.kt).
- **Migración Arquitectónica a MVVM (Iteración 3 - Lecturas):**
  - Creación del modelo de estado de UI [`LecturasUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasUiState.kt).
  - Creación de [`LecturasViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasViewModel.kt) inyectando `ILecturaRepository` e `IInquilinoRepository`, coordinando la validación de monotonicidad de lecturas y guardado asíncrono en `viewModelScope`.
  - Refactorización de [`LecturasActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasActivity.kt) para observar `uiState` con `repeatOnLifecycle(STARTED)` y manejar notificaciones de UI.
  - Cobertura con pruebas unitarias aisladas en [`LecturasViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/LecturasViewModelTest.kt).
- **Migración Arquitectónica a MVVM (Iteración 2 - ResumenCobros):**
  - Creación del modelo de estado de UI [`ResumenCobrosUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosUiState.kt).
  - Creación de [`ResumenCobrosViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosViewModel.kt) inyectando `ILecturaRepository`, `IInquilinoRepository` e `ISettingsManager`, procesando el cálculo de resumen mensual y actualización de pagos en `viewModelScope`.
  - Refactorización de [`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt) para observar `uiState` vía `repeatOnLifecycle(STARTED)` manteniendo desacoplados los adaptadores y acciones de WhatsApp/PDF.
  - Cobertura con pruebas unitarias aisladas en [`ResumenCobrosViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ResumenCobrosViewModelTest.kt).
- **Migración Arquitectónica a MVVM (Iteración 1 - HistorialConsumo):**
  - Creación de contratos de interfaz para la capa de datos ([`ILecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/ILecturaRepository.kt) e [`IInquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/IInquilinoRepository.kt)).
  - Definición del modelo de estados inmutables [`HistorialConsumoUiState.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoUiState.kt) (`Cargando`, `Exito`, `Error`).
  - Creación de [`HistorialConsumoViewModel.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoViewModel.kt) procesando flujos asíncronos en `viewModelScope` y exponiendo `StateFlow`.
  - Refactorización de [`HistorialConsumoActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoActivity.kt) instanciando el ViewModel vía `by viewModels()` y recolectando estados con `repeatOnLifecycle(STARTED)`.
  - Cobertura con pruebas unitarias aisladas en [`HistorialConsumoViewModelTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/HistorialConsumoViewModelTest.kt).
- **Validación E.164 de Teléfonos y Desacoplamiento de CobroAdapter:**
  - Creación del componente utilitario [`PhoneUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PhoneUtils.kt) para la limpieza y validación internacional de números telefónicos de WhatsApp (formato E.164 con prefijo por defecto `+51` para Perú).
  - Cobertura con suite de pruebas unitarias [`PhoneUtilsTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/PhoneUtilsTest.kt).
  - Desacoplamiento total de [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt) eliminando la generación de Intents de WhatsApp y PDFs, delegando dichas acciones en callbacks gestionados por [`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt).
- **Flujo de Recuperación de Contraseña:**
  - Opción "¿Olvidaste tu contraseña?" en [`activity_auth.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_auth.xml) y pantalla de autenticación ([`AuthActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthActivity.kt)).
  - Integración con Firebase Authentication (`sendPasswordResetEmail`) mediante la función `enviarCorreoRecuperacion` en [`AuthRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AuthRepository.kt).
  - Solicitud dinámica del correo electrónico vía diálogo interactivo (`MaterialAlertDialogBuilder`) con retroalimentación clara de éxito o error al usuario.
- **Gestión de Cobranza Persistente (Fase 1, 2A & 2B):**
  - Incorporación de `estadoPago` ("PENDIENTE" / "PAGADO" / "VENCIDO"), `fechaPago` y `metodoPago` en las entidades [`Lectura.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/Lectura.kt) y [`DetalleCobro.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/DetalleCobro.kt).
  - Evaluación dinámica del estado `VENCIDO` en [`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt) mediante [`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt) en base al día límite configurado (`diaLimitePago`).
  - Registro de auditoría mínima al marcar como `PAGADO`: selección de medio de pago (Yape, Plin, Efectivo, Transferencia) e inclusión automática del timestamp de cobro.
  - Muestra del detalle de pago o fecha límite incumplida dentro de la tarjeta de cobro ([`item_cobro.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/item_cobro.xml) / [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt)).
  - Persistencia en Cloud Firestore ([`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt)) y Room v5 ([`LecturaDao.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaDao.kt)).
- **Historial y Gráficos de Consumo (Fases 1, 2, 3 & 4):**
  - Implementación de la pantalla interactiva de gráficos [`HistorialConsumoActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoActivity.kt) y su layout [`activity_historial_consumo.xml`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/res/layout/activity_historial_consumo.xml) con MPAndroidChart (`LineChart` para la tendencia global del inmueble y `BarChart` por submedidor).
  - Integración reactiva en tiempo real consumiendo los flujos de Firestore vía `lifecycleScope` con `ConsumoHistoricoCalculator` sin bloquear el hilo principal.
  - Formateo limpio del eje X con etiquetas de periodo (ej. `"Ago 26"`) vía [`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt).
  - Adición de acceso directo desde la pantalla principal ([`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt)).
  - Adición de la librería `MPAndroidChart` (`v3.1.0`) en [`libs.versions.toml`](file:///home/fernando/StudioProjects/KiloWatt/gradle/libs.versions.toml) y conector global `obtenerTodasLasLecturasFlow()` en [`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt).
  - Suite de pruebas unitarias [`ConsumoHistoricoCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/ConsumoHistoricoCalculatorTest.kt).
- **Componente Utilitario `PeriodoDateUtils` (Fases 1 y 2):**
  - Implementación del componente reutilizable [`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt) para normalización, conversión a fecha cronológica ordenable (`convertirAFechaOrdenable`) y formateo para gráficos (`formatearParaGrafico`).
  - Cobertura con suite de pruebas unitarias [`PeriodoDateUtilsTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/PeriodoDateUtilsTest.kt) validando múltiples formatos de periodo (textuales en español y numéricos ISO).
- **Propuesta de Diseño Técnico para la Migración Arquitectónica a MVVM + StateFlow (Fase 1):**
  - Elaboración del documento de arquitectura [`Propuesta_Migracion_MVVM_Fase1.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/Propuestas/Propuesta_Migracion_MVVM_Fase1.md) definiendo la estrategia de migración incremental por pantallas, modelos de estado inmutables (`UiState`), recolección segura mediante `repeatOnLifecycle`, abstraimiento de interfaces de repositorio y plan de testing aislado para ViewModels.
- **Auditoría Técnica y Documentación del Sistema:**
  - Creación de guía de flujos y skills para asistentes IA ([`SKILL_DESARROLLO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_DESARROLLO.md), [`SKILL_ARQUITECTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_ARQUITECTO.md), [`SKILL_AUDITOR.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/SKILL_AUDITOR.md) y [`MEMORIA_PROYECTO_KILOWATT.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/IA/MEMORIA_PROYECTO_KILOWATT.md)).
  - Documentación arquitectónica y de diseño en la carpeta `docs/` ([`ARQUITECTURA.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ARQUITECTURA.md), [`MODELO_DATOS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/MODELO_DATOS.md), [`DECISIONES_TECNICAS.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/DECISIONES_TECNICAS.md), [`ESTADO_DEL_PROYECTO.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ESTADO_DEL_PROYECTO.md), [`ROADMAP.md`](file:///home/fernando/StudioProjects/KiloWatt/docs/ROADMAP.md)).
- **Suite de Pruebas Unitarias para Motor de Cálculo:**
  - Pruebas unitarias en [`CobroCalculatorTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/CobroCalculatorTest.kt) para verificar las reglas de validación de lecturas, cálculo de consumo individual, prorrateo de áreas comunes y consolidación mensual.

---

## [0.3.0] - 2026-10-15

### Añadido
- **Generación de Reportes PDF Dinámicos:**
  - Emisión de comprobantes individuales de cobro en formato A6 con desglose de consumos, lectura anterior/actual, cuota de áreas comunes y datos de pago ([`PdfGenerator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PdfGenerator.kt)).
  - Generación de reporte general mensual en formato vertical con gráfico circular (*pie chart*) de distribución porcentual de consumo sobre `Canvas` y tabla consolidada.
- **Distribución vía WhatsApp:** Integración de enlaces con `Intent.ACTION_VIEW` para enviar mensajes estructurados de cobro directamente al número de contacto de cada arrendatario.
- **Configuración de Cobranza:** Parámetros configurables de pago (titular, número Yape/Plin, día de corte mensual) persistidos localmente con [`SettingsManager.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/SettingsManager.kt) y respaldados en la nube.

### Modificado
- Refactorización de visualización en [`CobroAdapter.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/CobroAdapter.kt) con estados dinámicos ("Pendiente 🟡", "Sin Lectura ⚠️") y renderizado optimizado con `ListAdapter` y `DiffUtil`.

---

## [0.2.0] - 2026-10-01

### Añadido
- **Motor de Cálculo y Prorrateo ([`CobroCalculator.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/CobroCalculator.kt)):**
  - Validación de coherencia de lecturas (valores no negativos, lectura actual mayor o igual a la lectura previa).
  - Distribución proporcional del costo del recibo general (`montoTotalRecibo`) según el consumo neto en kWh registrado por cada submedidor.
  - Prorrateo equitativo del gasto generado por submedidores catalogados como áreas comunes (`esAreaComun = true`) únicamente entre los inquilinos configurados como pagadores (`pagaAreaComun = true`).
- **Pantalla de Resumen y Cobros ([`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt)):**
  - Selector desplegable de meses facturados.
  - Indicadores globales de costo total del recibo, kWh totales y precio promedio resultante por kWh.
  - Listado detallado por espacio con reactividad en tiempo real mediante `combine` de Kotlin Flows.

---

## [0.1.0] - 2026-09-15

### Añadido
- **Autenticación Multitenant:**
  - Inicio de sesión y registro de administradores/arrendadores con correo y contraseña vía `FirebaseAuth` ([`AuthActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/AuthActivity.kt)).
  - Navegación protegida en [`MainActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/MainActivity.kt) según el estado de sesión activa.
- **Gestión de Inquilinos y Submedidores:**
  - Registro, edición y eliminación de espacios físicos y arrendatarios ([`ConfiguracionActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt)).
  - Flags de configuración por submedidor: designación como área común y participación en el pago comunitario.
- **Registro y Captura de Lecturas:**
  - Formulario de ingreso de recibos principales de la empresa distribuidora de energía eléctrica ([`LecturasActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasActivity.kt)).
  - Registro de mediciones mensuales por submedidor con precarga automática de la última lectura registrada.
- **Capa de Persistencia e Infraestructura:**
  - Esquema relacional local preparado con Room ([`AppDatabase.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt), DAOs y entidades `Inquilino`, `Submedidor`, `FacturaGeneral`, `Lectura`).
  - Conectores en tiempo real con Cloud Firestore ([`InquilinoRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoRepository.kt), [`LecturaRepository.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/LecturaRepository.kt)) mediante `callbackFlow`.
