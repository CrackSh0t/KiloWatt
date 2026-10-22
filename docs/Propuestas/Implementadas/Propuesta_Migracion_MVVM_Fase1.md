# Propuesta de Diseño Técnico: Migración ArquITECTÓNICA a MVVM + StateFlow (Fase 1)

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Propuesta de Arquitectura en Revisión  

---

## 1. Resumen Ejecutivo

El presente documento establece la estrategia técnica y el diseño arquitectónico para migrar la aplicación **Kilowatt** desde un enfoque centrado en actividades (`AppCompatActivity` manejando directamente corrutinas y estado) hacia el patrón **MVVM (Model-View-ViewModel)** impulsado por **Kotlin StateFlow / SharedFlow** y preparado para **Hilt (Inyección de Dependencias)**.

Esta migración resuelve de manera estructural la pérdida de estado por rotación de pantalla, el acoplamiento entre UI y lógica de datos, y sienta las bases para pruebas unitarias de presentación automatizadas sobre los ViewModels.

---

## 2. Análisis del Problema y Necesidad de Negocio

### A. Diagnóstico de la Arquitectura Actual
Actualmente, las actividades del sistema ([`ResumenCobrosActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ResumenCobrosActivity.kt), [`HistorialConsumoActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/HistorialConsumoActivity.kt), [`LecturasActivity.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/LecturasActivity.kt), etc.):
1. Instancian manualmente repositorios (`new LecturaRepository()`).
2. Recolectan flujos de Firestore directamente dentro de `lifecycleScope.launch`.
3. Ejecutan la orquestación de datos y manipulación directa de vistas en la misma clase.

### B. Impacto y Valor para el Producto
- **Mantenibilidad:** Separación clara entre renderizado gráfico (`Activity` + XML ViewBinding) y lógica de estado (`ViewModel`).
- **Resiliencia:** El estado de la UI sobrevive al ciclo de vida de la actividad (rotación, cambios de tema o recreación del sistema).
- **Testabilidad:** Habilita el testing unitario desacoplado del framework Android (JUnit + Kotlin Coroutines Test).

---

## 3. Arquitectura Propuesta: MVVM + StateFlow + Repository Interfaces

```
┌─────────────────────────────────────────────────────────────┐
│                       Capa de UI                            │
│           (AppCompatActivity + ViewBinding)                 │
│  - Observa uiState vía repeatOnLifecycle(STARTED)           │
│  - Emite acciones del usuario (Intents / User Events)       │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼ (Observa StateFlow / Llama métodos)
┌─────────────────────────────────────────────────────────────┐
│                     Capa ViewModel                          │
│               (androidx.lifecycle.ViewModel)                │
│  - Expone _uiState: MutableStateFlow<UiState> (privado)     │
│  - Expone uiState: StateFlow<UiState> (público e inmutable) │
│  - Maneja asincronía en viewModelScope                      │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼ (Inyección por interfaz)
┌─────────────────────────────────────────────────────────────┐
│                 Interfaces de Repositorio                   │
│   (ILecturaRepository, IInquilinoRepository, IAuthRepository)│
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Capa de Persistencia                      │
│                (Cloud Firestore / Room)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Análisis de Impacto y Estrategia Incremental

Para asegurar cero regresiones y garantizar que las 26 pruebas unitarias existentes se mantengan en verde al 100%, se propone una **migración pantalla por pantalla en 4 iteraciones secuenciales**:

```
[Iteración 1] HistorialConsumoActivity -> HistorialConsumoViewModel
      │
      ▼
[Iteración 2] ResumenCobrosActivity   -> ResumenCobrosViewModel
      │
      ▼
[Iteración 3] LecturasActivity        -> LecturasViewModel
      │
      ▼
[Iteración 4] AuthActivity & Config   -> AuthViewModel & ConfiguracionViewModel
```

### A. Definición de Modelos de Estado de UI (`UiState`)

Cada pantalla manejará un estado inmutable modelado con `sealed interface` o `data class`:

#### 1. `HistorialConsumoUiState`:
```kotlin
sealed interface HistorialConsumoUiState {
    object Cargando : HistorialConsumoUiState
    data class Exito(val resultado: ConsumoHistoricoResult) : HistorialConsumoUiState
    data class Error(val mensaje: String) : HistorialConsumoUiState
}
```

#### 2. `ResumenCobrosUiState`:
```kotlin
data class ResumenCobrosUiState(
    val estaCargando: Boolean = true,
    val listaMesesDisponibles: List<String> = emptyList(),
    val mesSeleccionado: String? = null,
    val resumenResult: ResumenCobroResult? = null,
    val mensajeError: String? = null
)
```

#### 3. `LecturasUiState`:
```kotlin
data class LecturasUiState(
    val mesPeriodo: String = "",
    val estaGuardandoReciboBase: Boolean = false,
    val estaGuardandoLectura: Boolean = false,
    val submedidoresDisponibles: List<Submedidor> = emptyList(),
    val ultimaLecturaSubmedidor: Lectura? = null,
    val mensajeNotificacion: String? = null
)
```

#### 4. Eventos Únicos (`SharedFlow` / `SingleLiveEvent`):
Para eventos que no deben repetirse al rotar (ej. Toasts de confirmación o navegación a otra pantalla), se utilizará `MutableSharedFlow<String>()`.

---

## 5. Gestión del Ciclo de Vida y Corrutinas

1. **`viewModelScope`:**
   - Todas las llamadas asíncronas a repositorios y procesamiento pesado (`ConsumoHistoricoCalculator`, `CobroCalculator`) se ejecutarán en corrutinas dentro de `viewModelScope`.
   - Se elimina la ejecución manual de `lifecycleScope.launch` para llamadas de red/datos dentro de las actividades.

2. **Recolección Segura en la Vista (`repeatOnLifecycle`):**
   - Las actividades recolectarán el `StateFlow` utilizando:
     ```kotlin
     lifecycleScope.launch {
         repeatOnLifecycle(Lifecycle.State.STARTED) {
             viewModel.uiState.collect { state ->
                 actualizarInterfaz(state)
             }
         }
     }
     ```
   - Esto evita fugas de memoria y desperdicio de recursos cuando la pantalla pasa a segundo plano (`STOPPED`).

3. **Inmunidad ante Rotación de Pantalla:**
   - La instancia de `ViewModel` sobrevive a la destrucción y recreación de la `Activity`. Al volver a crearse la vista, se conecta inmediatamente al último valor retenido por el `StateFlow` sin re-consultar a Firestore.

---

## 6. Preparación para Inyección de Dependencias (Hilt)

Para desacoplar las clases de datos y facilitar la inyección directa o mediante Hilt `@HiltViewModel`:

### A. Definición de Interfaces
Se abstraerán las clases concretas de repositorios en contratos de interfaz:

```kotlin
interface ILecturaRepository {
    fun obtenerFacturasFlow(): Flow<List<FacturaGeneral>>
    fun obtenerLecturasPorMesFlow(mes: String): Flow<List<Lectura>>
    fun obtenerTodasLasLecturasFlow(): Flow<List<Lectura>>
    suspend fun guardarFactura(factura: FacturaGeneral): Pair<Boolean, String?>
    suspend fun guardarLectura(lectura: Lectura): Pair<Boolean, String?>
    suspend fun actualizarEstadoPago(idLectura: String, nuevoEstado: String, fechaPago: Long?, metodoPago: String?): Pair<Boolean, String?>
}

interface IInquilinoRepository {
    fun obtenerInquilinosFlow(): Flow<List<Inquilino>>
    fun obtenerSubmedidoresFlow(): Flow<List<Submedidor>>
    fun obtenerPerfilPropietarioFlow(): Flow<Map<String, Any>?>
    suspend fun guardarInquilinoYSubmedidor(...): Pair<Boolean, String?>
    suspend fun guardarPerfilPropietario(...): Pair<Boolean, String?>
}
```

### B. Constructores Inyectables en ViewModels
Cada `ViewModel` recibirá la interfaz del repositorio por constructor:
```kotlin
class ResumenCobrosViewModel(
    private val lecturaRepository: ILecturaRepository,
    private val inquilinoRepository: IInquilinoRepository,
    private val settingsManager: SettingsManager
) : ViewModel()
```

---

## 7. Estrategia de Pruebas Unitarias para ViewModels

Para validar el comportamiento del estado en cada ViewModel de forma aislada sin dependencias de Android:

1. **Regla de Corrutinas (`MainCoroutineRule`):**
   - Utilizar `StandardTestDispatcher` y `Dispatchers.setMain` para reemplazar el `MainDispatcher` de Android durante las pruebas unitarias con JUnit.
2. **Repositorios Falsos (`FakeLecturaRepository`):**
   - Implementar los repositorios in-memory mediante listas o `MutableSharedFlow` sin invocar Firebase.
3. **Validación de Estados:**
   - Verificar la secuencia de estados emitidos por `StateFlow`:
     `Estado Inicial (Cargando) -> Estado Exitoso (Lista poblada)`.

---

## 8. Análisis de Riesgos y Mitigación

1. **Riesgo 1: Múltiples recolecciones de `StateFlow` al rotar:**
   - *Mitigación:* Usar `StateFlow` (que es *conflated* por naturaleza) combinado con `repeatOnLifecycle(STARTED)`.
2. **Riesgo 2: Regresiones en las 26 pruebas unitarias existentes:**
   - *Mitigación:* No se modificarán las utilidades puras (`CobroCalculator`, `PeriodoDateUtils`, `PhoneUtils`). Las 26 pruebas continuarán ejecutándose contra sus objetos de dominio.
3. **Riesgo 3: Alteración de esquemas en la nube:**
   - *Mitigación:* Se prohíbe explícitamente modificar las estructuras de documentos en Firestore o tablas de Room durante esta fase.

---

## 9. Recomendación Final

### **APROBAR PROPUESTA**

La estrategia técnica es sólida, garantiza la preservación de las 26 pruebas unitarias en verde y resuelve el acoplamiento actual de las actividades. Se recomienda enviar la propuesta al Agente Desarrollador para iniciar la **Iteración 1 (`HistorialConsumoViewModel`)**.
