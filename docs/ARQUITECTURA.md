# Arquitectura del Sistema - Kilowatt

Este documento describe las directrices arquitectónicas, los patrones de diseño y la estructura técnica de la aplicación **Kilowatt**.

---

## 1. Visión de Alto Nivel

La aplicación sigue un enfoque modular por capas centrado en el ecosistema Android y Firebase:

```
┌──────────────────────────────────────────────┐
│           Capa de Presentación               │
│  (Activities, ListAdapters, ViewBinding)     │
└──────────────────────┬───────────────────────┘
                       │
┌──────────────────────▼───────────────────────┐
│        Capa de Dominio y Utilidades          │
│    (CobroCalculator, PdfGenerator)           │
└──────────────────────┬───────────────────────┘
                       │
┌──────────────────────▼───────────────────────┐
│              Capa de Datos                   │
│   (AuthRepository, InquilinoRepository,      │
│    LecturaRepository, SettingsManager)       │
└──────────────────────┬───────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
  Firebase (Auth, Firestore)    Almacenamiento Local
                                (Room / SharedPreferences)
```

---

## 2. Descripción de Capas

### 2.1. Capa de Presentación (UI)
* **Tecnología:** Actividades clásicas (`AppCompatActivity`) con `ViewBinding` para manipulación tipada y segura de vistas.
* **Componentes clave:**
  * `AuthActivity`: Flujos de ingreso y alta de usuarios.
  * `MainActivity`: Orquestador principal de navegación.
  * `ConfiguracionActivity`: Administración de inquilinos, submedidores y datos de cobro del propietario.
  * `LecturasActivity`: Ingreso de datos de facturación general y tomas de submedidores.
  * `ResumenCobrosActivity`: Visualización de balances, cálculo mensual y acciones de exportación/compartido.
* **Listas y Renderizado:** Uso de `RecyclerView` con `ListAdapter` y `DiffUtil` (`InquilinoAdapter`, `CobroAdapter`).

### 2.2. Capa de Dominio y Lógica de Negocio
* **`CobroCalculator`:** Objeto puro en Kotlin encargado de:
  * Validación de reglas de negocio en lecturas (números no negativos, monotonicidad creciente).
  * Prorrateo del costo por kWh a partir del recibo general.
  * Cálculo y distribución equitativa de áreas comunes entre los submedidores participantes.
* **`PdfGenerator`:** Servicio de generación de documentos gráficos usando el API nativa de Android (`PdfDocument`), generando tanto comprobantes individuales (A6 aprox.) como reportes consolidados (A5).

### 2.3. Capa de Datos (Data Layer)
* **Patrón Repository:** Centraliza las operaciones de datos abstrayendo las fuentes remotas y locales.
  * `AuthRepository`: Encapsula `FirebaseAuth` para login, registro y estado de sesión.
  * `InquilinoRepository`: Gestiona subcolecciones en Firestore (`usuarios/{uid}/inquilinos`, `submedidores`, `perfil`).
  * `LecturaRepository`: Administra facturas maestras y lecturas por submedidor en Firestore (`usuarios/{uid}/facturas`, `lecturas`).
* **Concurrencia Reactiva:** Empleo de Kotlin Coroutines (`suspendCancellableCoroutine`) y Kotlin Flows (`callbackFlow`, `combine`) para observar cambios en tiempo real desde Firestore.
* **Almacenamiento Local:**
  * `SettingsManager`: Manejo de preferencias ligeras vía `SharedPreferences`.
  * `AppDatabase` (Room): Estructura relacional definida para funcionamiento offline.

---

## 3. Modelo de Datos Firestore

La base de datos NoSQL sigue un esquema multi-inquilino segregado por el UID del usuario:

* `usuarios/{uid}/perfil/datos_pago`: Datos bancarios y fecha de vencimiento del propietario.
* `usuarios/{uid}/inquilinos/{idInquilino}`: Información de contacto del inquilino.
* `usuarios/{uid}/submedidores/{idSubmedidor}`: Configuración del espacio y asignación de área común.
* `usuarios/{uid}/facturas/{mesPeriodo}`: Registro maestro de la empresa eléctrica matriz.
* `usuarios/{uid}/lecturas/{idLectura}`: Mediciones históricas asociadas a un submedidor y periodo.
