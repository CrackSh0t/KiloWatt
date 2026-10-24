# Arquitectura del Sistema - Kilowatt

Este documento describe las directrices arquitectónicas, los patrones de diseño y la estructura técnica actual de la aplicación **Kilowatt**.

---

## 1. Visión de Alto Nivel

La aplicación sigue un enfoque modular en capas basado en **Clean Architecture**, **MVVM**, inyección de dependencias con **Dagger Hilt**, y persistencia **Offline-First SSOT (Room + Firestore)**:

```
┌─────────────────────────────────────────────────────────────┐
│                   Capa de Presentación                      │
│      (Activities + ViewBinding + ViewModels + StateFlow)    │
└──────────────────────────────┬──────────────────────────────┘
                               │ (Inyección de Dependencias vía Hilt)
┌──────────────────────────────▼──────────────────────────────┐
│                Capa de Dominio y Utilidades                 │
│        (CobroCalculator, PdfGenerator, PhoneUtils)          │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                       Capa de Datos                         │
│        (Repositories: LecturaRepository, Inquilino, Auth)   │
└──────────────────────────────┬──────────────────────────────┘
                               │
         ┌─────────────────────┴─────────────────────┐
         ▼                                           ▼
  Cloud Firestore (Remoto)                  Room SQLite / SharedPreferences (Local SSOT)
```

---

## 2. Descripción de Capas

### 2.1. Capa de Presentación (UI & MVVM)
* **Tecnología:** Actividades (`AppCompatActivity`) con `ViewBinding`, ViewModels de Jetpack (`androidx.lifecycle.ViewModel`) y recolección reactiva de estados mediante `StateFlow` y `repeatOnLifecycle(STARTED)`.
* **Inyección de Dependencias:** Totalmente gestionada por **Dagger Hilt** (`@AndroidEntryPoint`, `@HiltViewModel`, `@Inject constructor`).
* **Componentes clave:**
  * `MainActivity`: Menú de navegación principal con acceso directo al Centro de **Perfil del Propietario** (BottomSheetDialog con datos de cobro, cuenta y cierre de sesión).
  * `AuthActivity`: Flujos de ingreso, alta de usuarios y recuperación de contraseña vía Firebase Auth.
  * `ConfiguracionActivity`: Administración operativa de inquilinos y submedidores ("Inquilinos y Medidores").
  * `LecturasActivity`: Ingreso de datos de facturación general y tomas de submedidores con validación de monotonicidad.
  * `ResumenCobrosActivity`: Visualización de balances, cálculo mensual, gestión de estados de pago (`PENDIENTE`, `PAGADO`, `VENCIDO`) y acciones de PDF / WhatsApp.
  * `HistorialConsumoActivity`: Pantalla interactiva con gráficos (`LineChart` y `BarChart`) impulsados por MPAndroidChart.

### 2.2. Capa de Dominio y Lógica de Negocio
* **`CobroCalculator`:** Objeto puro en Kotlin encargado de:
  * Validación de reglas de negocio en lecturas (números no negativos, monotonicidad creciente).
  * Prorrateo del costo por kWh a partir del recibo general.
  * Cálculo y distribución equitativa de áreas comunes entre los submedidores participantes.
* **`PdfGenerator`:** Servicio de generación de documentos gráficos usando el API nativa de Android (`PdfDocument`), generando comprobantes individuales (A6 aprox.) y reportes consolidados (A5) personalizados con datos del propietario y sellos visuales de estado de pago.

### 2.3. Capa de Datos (Data Layer & SSOT Offline-First)
* **Patrón Repository & SSOT:** Centraliza las operaciones de datos combinando persistencia local en SQLite (Room) como fuente única de verdad con sincronización asíncrona hacia Cloud Firestore.
  * `AuthRepository`: Encapsula `FirebaseAuth` para login, registro y estado de sesión (abstraído mediante `IAuthRepository`).
  * `InquilinoRepository`: Gestiona inquilinos y submedidores utilizando `InquilinoDao` y `SubmedidorDao` con sincronización en background.
  * `LecturaRepository`: Administra facturas maestras y lecturas por submedidor utilizando `LecturaDao` y `FacturaGeneralDao`.
* **Sincronización Diferida:** Empleo de **WorkManager** (`SyncWorker`) para reintentos de red.
* **Almacenamiento Local:**
  * `SettingsManager` (implementando `ISettingsManager`): Manejo de preferencias de cobro vía `SharedPreferences`.
  * `AppDatabase` (Room v5): Base de datos relacional local con soporte Offline-First.
