# Reglas y Convenciones de Desarrollo - Kilowatt

Este documento establece las directrices de codificación, convenciones de nomenclatura y buenas prácticas observadas y requeridas en el proyecto **Kilowatt**.

---

## 1. Idioma y Nomenclatura

* **Idioma del código:** El código base utiliza **español** para las entidades de dominio, variables, nombres de funciones, comentarios y textos de la interfaz.
* **Clases y Tipos:** Usar `PascalCase` descriptivo en español:
  * Entidades y modelos: `Inquilino`, `Submedidor`, `FacturaGeneral`, `Lectura`, `DetalleCobro`.
  * Componentes Android: `ConfiguracionActivity`, `CobroAdapter`, `AuthActivity`.
  * Clases de utilidad/dominio: `CobroCalculator`, `PdfGenerator`, `SettingsManager`.
* **Propiedades y Variables:** Usar `camelCase` en español:
  * Ejemplos: `nombreInquilino`, `telefonoWhatsapp`, `lecturaAnterior`, `montoAreaComunSoles`.
* **Nombres de Tablas y Base de Datos:**
  * Room: Nombres en minúscula y `snake_case` plural (`inquilinos`, `submedidores`, `factura_general`, `lecturas`).
  * Firestore: Nombres de subcolecciones en minúscula (`facturas`, `lecturas`, `inquilinos`, `submedidores`, `perfil`).
* **Formatos de ID:** Sanitizar cadenas utilizadas como identificadores de documentos para evitar caracteres incompatibles (`/`, `\`).

---

## 2. Capa de Presentación (UI)

* **ViewBinding:** Obligatorio para todas las Activities y ViewHolders. No utilizar `findViewById`.
* **Listas (RecyclerView):**
  * Utilizar siempre `ListAdapter<T, VH>` con un `DiffUtil.ItemCallback<T>` dedicado.
  * El `ViewHolder` debe encapsular la instancia del layout binding correspondiente.
* **Manejo de Diálogos:**
  * Usar `BottomSheetDialog` para formularios contextuales o de creación rápida (por ejemplo, registro de lecturas o inquilinos).
* **Navegación e Intent:** Manejar intents explícitos y comprobaciones de sesión activa antes de renderizar la UI (`authRepository.usuarioActual != null`).

---

## 3. Concurrencia y Reactividad

* **Corrutinas:**
  * Usar los dispatchers adecuados según la carga: `Dispatchers.IO` para lectura/escritura en disco o llamadas de red, y `Dispatchers.Main` para actualización de vistas y UI.
  * Uso de `lifecycleScope` en componentes con ciclo de vida para evitar fugas de memoria (*leaks*).
* **Flows:**
  * Emplear `Flow` y `callbackFlow` para exponer flujos reactivos desde Firestore o Room.
  * Combinar flujos con operadores estándar de Kotlin Coroutines (`combine`, `map`, etc.) para componer datos multidominio.

---

## 4. Modelado y Lógica de Negocio

* **Inmutabilidad:**
  * Preferir `val` sobre `var` en todas las clases y funciones salvo cuando la mutabilidad interna sea estrictamente necesaria.
  * Modelar entidades y resultados con `data class`.
* **Manejo de Resultados y Estados:**
  * Utilizar `sealed class` o `sealed interface` para representar estados de operaciones o validaciones (ejemplo: `ValidacionLecturaResult.Valido` e `Invalido(mensaje)`).
* **Aislamiento de la lógica pura:**
  * Toda regla de cálculo o validación matemática debe residir en objetos de utilidad puros (como `CobroCalculator`), desacoplados del framework de Android y fácilmente testeables con JUnit.

---

## 5. Control de Versiones (Git)

* **Formato de Commits:** Utilizar la convención *Conventional Commits* en español:
  * `feat: ...` para nuevas funcionalidades.
  * `fix: ...` para corrección de bugs.
  * `refactor: ...` para refactorizaciones de código sin cambio de comportamiento.
  * `docs: ...` para creación o actualización de documentación.
  * `test: ...` para incorporación o modificación de tests.
