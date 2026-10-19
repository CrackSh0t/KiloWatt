# Decisiones Técnicas y de Arquitectura - Kilowatt

Este documento recopila las principales decisiones técnicas y arquitectónicas adoptadas durante el desarrollo de la aplicación Kilowatt, justificando los motivos de elección frente a otras alternativas disponibles en el ecosistema Android.

---

## 1. Firebase (BaaS) en lugar de Backend Propio

### Decisión
Adoptar **Firebase Authentication** y **Cloud Firestore** como infraestructura de backend como servicio (Backend as a Service - BaaS) en lugar de implementar una API REST/gRPC propia (Node.js, Spring Boot, Go, etc.) con base de datos relacional dedicada.

### Motivos
1. **Velocidad de iteración y Time-to-Market:** Elimina la necesidad de diseñar, hospedar, asegurar y mantener servidores y pipelines de despliegue para una API dedicada.
2. **Autenticación sin fricción:** Firebase Auth resuelve de forma nativa el ciclo de vida del usuario (registro, login, persistencia de tokens de sesión y recuperación de contraseña).
3. **Sincronización reactiva en tiempo real:** Firestore provee observabilidad en tiempo real sobre las colecciones (`addSnapshotListener`), permitiendo que los cambios en lecturas, facturas e inquilinos se reflejen instantáneamente en la UI.
4. **Modelo de datos multitenant por aislamiento de rutas:** La seguridad y separación de datos de cada arrendador se resuelve de manera transparente mediante rutas por UID (`usuarios/{uid}/...`) combinadas con reglas de seguridad en Firestore (`allow read, write: if request.auth.uid == userId`).
5. **Costos iniciales cero:** El nivel gratuito de Firebase cubre holgadamente la operativa habitual de un arrendador independiente o pequeñas administraciones inmobiliarias.

---

## 2. Vistas Clásicas (Activities y XML) en lugar de Jetpack Compose

### Decisión
Estructurar la interfaz de usuario mediante **Activities clásicas** (`MainActivity`, `AuthActivity`, `LecturasActivity`, `ResumenCobrosActivity`, `ConfiguracionActivity`) y layouts XML con widgets estándar de Android (`RecyclerView`, `Spinner`, `MaterialCardView`), en lugar de una interfaz 100% declarativa en Jetpack Compose.

### Motivos
1. **Madurez y estabilidad de componentes:** Los componentes del sistema de vistas tradicional cuentan con soporte nativo consolidado en la plataforma, facilitando integraciones con diálogos clásicos, menús contextuales y adaptadores (`ListAdapter` con `DiffUtil`).
2. **Menor sobrecarga de configuración de toolchain:** Evita la dependencia estricta de versiones acopladas entre el compilador de Kotlin, Gradle y el compilador de Compose en etapas de desarrollo rápido.
3. **Flujo de navegación estándar:** El desacoplamiento por Activities independientes simplifica el ciclo de vida de pantallas autónomas con barras de acción nativas (`ActionBar`), evitando la complejidad de configurar librerías de navegación declarativa para un flujo directo y acotado.

---

## 3. ViewBinding en lugar de DataBinding o findViewById

### Decisión
Utilizar **ViewBinding** (`binding = ActivityXBinding.inflate(layoutInflater)`) como mecanismo de enlace de vistas en todas las Activities y ViewHolder de adaptadores.

### Motivos
1. **Seguridad de tipos y de nulidad:** A diferencia de `findViewById`, ViewBinding genera referencias fuertemente tipadas en tiempo de compilación para cada ID de vista, evitando `NullPointerException` y `ClassCastException`.
2. **Eficiencia en compilación:** A diferencia de DataBinding, ViewBinding no requiere etiquetas especiales `<layout>`, no procesa expresiones embebidas en XML y no penaliza los tiempos de compilación de Gradle.
3. **Claridad del código:** Separa netamente la lógica de negocio y presentación en Kotlin del diseño visual en XML, facilitando el mantenimiento y las revisiones de código.

---

## 4. Room Preparado pero no Integrado (Coexistencia con Firestore)

### Decisión
Mantener la definición completa de base de datos local con **Room** (`AppDatabase`, entidades `@Entity` y DAOs) en el proyecto, mientras los repositorios operativos (`InquilinoRepository`, `LecturaRepository`) interactúan directamente con **Cloud Firestore**.

### Motivos
1. **Evolución arquitectónica hacia Offline-First:** Dejar preparados los DAOs (`InquilinoDao`, `SubmedidorDao`, `FacturaGeneralDao`, `LecturaDao`) y modelos de entidades locales permite integrar en fases futuras un patrón de sincronización bidireccional local-remoto (Single Source of Truth en Room respaldado por Firestore vía workers de WorkManager o NetworkBoundResource).
2. **Definición formal del esquema relacional:** El modelo de Room sirvió para tipar rigurosamente los campos, índices únicos y restricciones relacionales antes de la persistencia semi-estructurada en NoSQL.
3. **Resiliencia de migración:** La presencia de `fallbackToDestructiveMigration()` en `AppDatabase` evita inconsistencias si se decide activar la persistencia local en SQLite sin alterar la fuente remota de Firestore.

---

## 5. Coroutines y Kotlin Flow

### Decisión
Utilizar **Kotlin Coroutines** para tareas asíncronas y operaciones de E/S, y **Kotlin Flow** (específicamente `callbackFlow`) para la transmisión reactiva de colecciones de datos.

### Motivos
1. **Concurrencia estructurada y legibilidad:** Permite escribir código asíncrono con sintaxis secuencial y manejo directo de excepciones (`try-catch`), eliminando el anidamiento de callbacks (*callback hell*).
2. **Puente reactivo Firestore - UI:** El uso de `callbackFlow` en `LecturaRepository` e `InquilinoRepository` permite transformar la API orientada a listeners de Firestore (`addSnapshotListener`) en un flujo frío de Kotlin (`Flow<List<T>>`), desacoplando la capa de datos de la capa de presentación.
3. **Control de hilos explícito:** Despacho claro entre hilos de cálculo o I/O (`Dispatchers.IO`) y el hilo de interfaz de usuario (`Dispatchers.Main`) para tareas pesadas como la generación de archivos PDF o consultas de red.

---

## 6. Generación de Comprobantes PDF en el Dispositivo Móvil

### Decisión
Generar los comprobantes individuales de cobro localmente mediante la API nativa de Android **`PdfDocument`** y `Canvas` en la clase utilitaria `PdfGenerator`, en lugar de delegar la generación a servicios externos o bibliotecas pesadas de terceros (como iText o servicios en la nube).

### Motivos
1. **Disponibilidad inmediata:** El recibo se crea en milisegundos en el almacenamiento del dispositivo, sin latencia de red ni consumo de ancho de banda.
2. **Cero costos y dependencias externas:** No requiere claves de API, suscripciones de renderizado en la nube ni librerías con licenciamiento restrictivo.
3. **Control tipográfico y visual exacto:** El uso directo de `android.graphics.Canvas` y `Paint` permite diseñar formatos a medida (ej. formato compacto A6 horizontal / 420x300 pts) apto para visualización en pantallas móviles o impresión térmica.

---

## 7. Notificación y Cobro mediante Intent de WhatsApp

### Decisión
Implementar el envío de recibos y recordatorios de pago mediante **Intents implícitos hacia WhatsApp** (`Intent.ACTION_VIEW` con URI `https://api.whatsapp.com/send?phone=...`), en lugar de contratar un gateway de mensajería empresarial (Twilio, WhatsApp Business Cloud API) o SMS masivos.

### Motivos
1. **Canal preferido por los usuarios:** En el contexto local de arrendamiento independiente, WhatsApp es la vía de comunicación predominante y aceptada.
2. **Sin coste por mensaje:** Al apoyarse en la aplicación cliente instalada en el dispositivo del arrendador, no se incurre en costes por mensaje transaccional.
3. **Supervisión humana antes del envío:** El arrendador puede revisar y personalizar el mensaje pre-cargado antes de pulsar el botón de envío, lo que evita errores de cobro involuntarios.

---

## 8. Persistencia Híbrida de Configuración (SettingsManager + Firestore)

### Decisión
Almacenar los parámetros operativos del arrendador (nombre, número de pago y día de corte) de forma inmediata en `SharedPreferences` mediante `SettingsManager`, respaldándolos en paralelo en Firestore.

### Motivos
1. **Acceso síncrono ultra-rápido:** Evita retardos en la carga de vistas o en la generación dinámica de comprobantes PDF al no requerir llamadas asíncronas para leer un número telefónico o nombre de cobro.
2. **Respaldo en la nube:** Garantiza que si el usuario cambia de dispositivo móvil, sus datos de contacto y cobro se recuperen al iniciar sesión con su cuenta.
