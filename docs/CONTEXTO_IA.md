# Contexto del Proyecto para IA - Kilowatt

Guía concisa y de alto nivel con el resumen operativo, arquitectónico y normativo del proyecto **Kilowatt**.

---

## 1. Propósito del Proyecto
**Kilowatt** es una aplicación móvil Android diseñada para propietarios y administradores de inmuebles compartidos (casas multifamiliares, quintas, departamentos) con un único suministro eléctrico matriz y múltiples submedidores individuales.

* **Problema:** Errores de cálculo manual, falta de transparencia hacia inquilinos y fricción en la cobranza.
* **Solución:** Automatiza el cálculo del costo real por kWh, distribuye el consumo de áreas comunes de forma equitativa, genera comprobantes PDF y emite recordatorios preformateados para WhatsApp.

---

## 2. Stack Tecnológico
* **Lenguaje:** Kotlin.
* **Plataforma:** Android Nativo (minSdk / targetSdk estándar, Gradle con Kotlin DSL / Groovy).
* **UI & Componentes:** Android Jetpack (`AppCompatActivity`, `ViewBinding`, `RecyclerView` con `ListAdapter`/`DiffUtil`, `BottomSheetDialog`).
* **Backend & Servicios:** Firebase Authentication (email/password) y Cloud Firestore.
* **Persistencia Local:** `SharedPreferences` (`SettingsManager`) y Room (`AppDatabase`, `Entity`, `Dao`).
* **Concurrencia & Asincronía:** Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Main`, `lifecycleScope`) y Kotlin Flows (`callbackFlow`, `combine`).
* **Generación de Documentos:** Android nativo `PdfDocument`.

---

## 3. Arquitectura Actual
Patrón por capas centrado en Android y Firebase:

```
[Presentación (Activities, ListAdapters, ViewBinding)]
       │
[Dominio / Utilidades (CobroCalculator, PdfGenerator)]
       │
[Capa de Datos (Repositories, SettingsManager)]
  ├── Firebase (Auth, Firestore: usuarios/{uid}/[perfil|inquilinos|submedidores|facturas|lecturas])
  └── Local (Room AppDatabase, SharedPreferences)
```

* **Firestore (Multi-inquilino por UID):**
  * `usuarios/{uid}/perfil/datos_pago`: Datos bancarios y día de corte.
  * `usuarios/{uid}/inquilinos`: Contacto del arrendatario.
  * `usuarios/{uid}/submedidores`: Datos del medidor y flag de área común.
  * `usuarios/{uid}/facturas`: Recibos de la empresa matriz por mes.
  * `usuarios/{uid}/lecturas`: Mediciones históricas por submedidor.

---

## 4. Funcionalidades Implementadas
* **Autenticación (`AuthActivity`):** Registro e inicio de sesión con Firebase Auth; control de sesión activa.
* **Navegación (`MainActivity`):** Enrutamiento principal del sistema.
* **Configuración (`ConfiguracionActivity`):** CRUD de inquilinos y submedidores en Firestore; guardado de datos de cobro en `SettingsManager`.
* **Registro de Lecturas (`LecturasActivity`):** Alta de recibo matriz mensual (S/ y kWh) y registro de lecturas por submedidor.
* **Cálculo de Consumos (`CobroCalculator`):** Validación de consistencia (no negativos, monotonía creciente), costo unitario prorrateado y reparto de áreas comunes.
* **Cobros y Notificaciones (`ResumenCobrosActivity`, `CobroAdapter`):** Visualización de balances mensuales, redacción y apertura de WhatsApp con desglose de cobro, y generación/compartido de PDFs individuales y consolidados (`PdfGenerator`).

---

## 5. Deuda Técnica y Limitaciones
* **Ausencia de MVVM:** Lógica de negocio, orquestación y corrutinas alojadas directamente en las `Activity`.
* **Inyección de Dependencias Manual:** Repositorios y managers se instancian a mano (`new Repository()`).
* **Desacople de Room:** Entidades y DAOs de Room existen en `AppDatabase`, pero los repositorios activos interactúan directamente con Firestore sin sincronización bidireccional.
* **Acoplamiento en Adapters:** `CobroAdapter` ejecuta generación de archivos PDF y dispatching de intents de Android.
* **Sin seguimiento de estado de pagos:** No se registra si un cobro mensual ha sido pagado, pendiente o vencido.
* **Sin recuperación de contraseña:** Falta flujo de reseteo de clave por correo.

---

## 6. Funcionalidades Pendientes y Roadmap
1. **Fase 1 (Arquitectura):** Migración a MVVM (`ViewModel` + `StateFlow`), integración de DI (Hilt/Koin) y desacoplamiento de adaptadores.
2. **Fase 2 (Offline-First):** Sincronización Room + Firestore (WorkManager) y validaciones telefónicas E.164.
3. **Fase 3 (Pagos y Métricas):** Estados de pago (`Pendiente`, `Pagado`, `Vencido`) y gráficos históricos de consumo.
4. **Fase 4 (UX):** Recuperación de contraseña, personalización de recibos (logos/firmas) y soporte para tema oscuro.

---

## 7. Reglas de Desarrollo Obligatorias
* **Idioma:** Todo el código (clases, funciones, variables, comentarios, logs, UI) debe estar en **español**.
* **Nomenclatura:**
  * Clases/Objetos: `PascalCase` (`DetalleCobro`, `CobroCalculator`).
  * Propiedades/Métodos: `camelCase` (`montoAreaComunSoles`, `validarLecturas`).
  * Tablas Room y Colecciones Firestore: Minúsculas y plural (`inquilinos`, `factura_general`).
* **UI:** Obligatorio `ViewBinding` (prohibido `findViewById`). Uso de `ListAdapter` con `DiffUtil.ItemCallback`. Formularios rápidos en `BottomSheetDialog`.
* **Concurrencia:** `Dispatchers.IO` para I/O y Firestore; `Dispatchers.Main` para UI. Vincular corrutinas al ciclo de vida (`lifecycleScope`).
* **Modelado & Pureza:**
  * Preferir inmutabilidad (`val` y `data class`).
  * Estados con `sealed class` o `sealed interface`.
  * Toda lógica matemática o de negocio debe ir en objetos puros sin acoplamiento a Android (`CobroCalculator`) para permitir pruebas unitarias con JUnit.
* **Control de Versiones:** *Conventional Commits* en español (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`).

---

## 8. Prioridades Actuales
1. Implementar la capa `ViewModel` (`ResumenCobrosViewModel`, `LecturasViewModel`, etc.) desacoplando lógica de las Activities.
2. Establecer inyección de dependencias.
3. Conectar persistencia local (Room) con Firestore para soporte offline robusto.
