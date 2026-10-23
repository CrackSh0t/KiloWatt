# Reglas y Convenciones de Desarrollo - Kilowatt

Este documento establece las directrices de codificación, convenciones de nomenclatura y buenas prácticas obligatorias en el proyecto **Kilowatt**.

---

## 1. Idioma y Nomenclatura

* **Idioma del código:** El código base utiliza **español** para las entidades de dominio, variables, nombres de funciones, comentarios y textos de la interfaz.
* **Clases y Tipos:** Usar `PascalCase` descriptivo en español:
  * Entidades y modelos: `Inquilino`, `Submedidor`, `FacturaGeneral`, `Lectura`, `DetalleCobro`.
  * Componentes Android: `ConfiguracionActivity`, `CobroAdapter`, `AuthActivity`.
  * ViewModels y UI States: `LecturasViewModel`, `LecturasUiState`, `ResumenCobrosViewModel`.
  * Clases de utilidad/dominio: `CobroCalculator`, `PeriodoDateUtils`, `PhoneUtils`, `PdfGenerator`, `SettingsManager`.
* **Propiedades y Variables:** Usar `camelCase` en español (`nombreInquilino`, `telefonoWhatsapp`, `lecturaAnterior`, `montoAreaComunSoles`).
* **Nombres de Tablas y Base de Datos:**
  * Room: Nombres en minúscula y `snake_case` plural (`inquilinos`, `submedidores`, `factura_general`, `lecturas`).
  * Firestore: Nombres de subcolecciones en minúscula (`facturas`, `lecturas`, `inquilinos`, `submedidores`, `perfil`).
* **Formatos de ID:** Sanitizar cadenas utilizadas como identificadores de documentos para evitar caracteres incompatibles (`/`, `\`).

---

## 2. Capa de Presentación (MVVM + ViewBinding)

* **Patrón de Arquitectura:** Todas las pantallas deben implementar el patrón **MVVM (Model-View-ViewModel)**.
* **Inyección en Vistas:** Las Activities deben anotarse con `@AndroidEntryPoint` e instanciar el ViewModel mediante `by viewModels()`.
* **ViewBinding:** Obligatorio para todas las Activities y ViewHolders. Prohibido usar `findViewById`.
* **Manejo de Estado (UiState):**
  * Toda Activity debe consumir el estado de la pantalla expuesto como un `StateFlow<UiState>` inmutable desde el ViewModel.
  * La recolección de flujos en la UI debe realizarse siempre dentro de `lifecycleScope.launch` con `repeatOnLifecycle(Lifecycle.State.STARTED)` para evitar fugas de memoria.
  * Los eventos de notificación única (Toasts/Snackbars) deben consumirse mediante métodos de limpieza en el ViewModel (ej. `consumirNotificacion()`).
* **Listas (RecyclerView):** Utilizar `ListAdapter<T, VH>` con un `DiffUtil.ItemCallback<T>` dedicado. El `ViewHolder` debe encapsular la instancia del layout binding.
* **Manejo de Diálogos:** Usar `BottomSheetDialog` para formularios contextuales o de creación rápida.

---

## 3. Concurrencia, Inyección y Reactividad

* **Corrutinas:**
  * Toda operación asíncrona, de datos o cálculo pesado debe ejecutarse en el `ViewModel` dentro de `viewModelScope`.
  * Prohibido ejecutar llamadas directas de Firestore/Room o lógica de negocio desde la `Activity`.
  * Usar `Dispatchers.IO` para lectura/escritura en disco o red, y `Dispatchers.Default` para procesamiento computacional pesado (`CobroCalculator`, `ConsumoHistoricoCalculator`).
* **Inyección de Dependencias (Hilt):**
  * Los ViewModels deben anotarse con `@HiltViewModel` e inyectar sus dependencias vía `@Inject constructor`.
  * Inyectar **siempre contratos de interfaz** (`ILecturaRepository`, `IInquilinoRepository`, `IAuthRepository`, `ISettingsManager`), nunca clases concretas.
* **Flows:**
  * Emplear `Flow` y `callbackFlow` para exponer flujos reactivos desde Firestore o Room hacia los repositorios y ViewModels.

---

## 4. Modelado, Lógica de Negocio y Pruebas Unitarias

* **Inmutabilidad:** Preferir `val` sobre `var`. Modelar entidades, resultados y estados con `data class` o `sealed interface`.
* **Aislamiento de Lógica pura:** Toda regla de cálculo o validación matemática debe residir en objetos de utilidad puros (como `CobroCalculator`, `PeriodoDateUtils`, `PhoneUtils`), desacoplados del framework Android.
* **Pruebas Unitarias (Unit Tests):**
  * Todo nuevo `ViewModel`, `Util` o `Calculator` DEBE contar con su suite de pruebas unitarias en JUnit 4.
  * Antes de dar por concluida cualquier tarea, la suite de pruebas (`:app:testDebugUnitTest`) debe ejecutar y pasar al 100% (0 falladas, 0 omitidas).

---

## 5. Control de Versiones (Git)

* **Formato de Commits:** Utilizar la convención *Conventional Commits* en español:
  * `feat: ...` para nuevas funcionalidades.
  * `fix: ...` para corrección de bugs.
  * `refactor: ...` para refactorizaciones de código.
  * `docs: ...` para creación o actualización de documentación.
  * `test: ...` para incorporación o modificación de tests.
* **Inclusión de Documentación:** Todo commit que introduzca cambios de arquitectura o funcionalidades debe incluir la actualización de los documentos correspondientes en `docs/`.