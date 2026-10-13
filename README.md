# Kilowatt ⚡ - Gestión y Prorrateo de Consumo Eléctrico

**Kilowatt** es una aplicación móvil Android diseñada para automatizar la gestión, cálculo y prorrateo del recibo de luz general entre múltiples inquilinos y submedidores independientes.

---

## 🚀 Características Principales

* **Registro de Lecturas:** Captura de lecturas anteriores y actuales (kWh) por cada submedidor o espacio independiente.
* **Cálculo Exacto de Tarifas:** Prorrateo proporcional según el consumo real de cada submedidor respecto al recibo de la empresa eléctrica (Luz del Sur / Pluz).
* **Gestión de Áreas Comunes:** Distribución equitativa de consumos compartidos (ej. baños comunes, pasillos, bombas de agua) entre los inquilinos designados.
* **Generación de Reportes PDF:** 
  * Comprobante individual por inquilino listo para compartir.
  * Reporte general consolidado con gráficos de distribución de consumo.
* **Notificación por WhatsApp:** Envío de resúmenes detallados directamente al chat del inquilino con un solo clic.

---

## 🛠️ Tecnologías y Arquitectura

* **Lenguaje:** Kotlin
* **Arquitectura:** MVVM (Model-View-ViewModel) / Clean Architecture
* **UI & Componentes:** Android Jetpack, ViewBinding, RecyclerView, MPAndroidChart (para gráficos)
* **Backend & Base de Datos:** Firebase Firestore (Sincronización en tiempo real)
* **Autenticación:** Firebase Auth
* **Concurrencia:** Kotlin Coroutines & Flow
* **Exportación:** Android PdfDocument / FileProvider

---

## 📊 Lógica de Cálculo

La app calcula el cobro individual basándose en la proporción exacta de consumo sobre el total medido:

1. **Porcentaje de Consumo Individual:**
   $$\% \text{ Consumo} = \frac{\text{kWh Submedidor}}{\sum \text{kWh de todos los Submedidores}}$$

2. **Pago Base (Soles):**
   $$\text{Pago Base} = \% \text{ Consumo} \times \text{Monto Total del Recibo General}$$

3. **Cálculo de Área Común:**
   El costo del área común se calcula con la misma fórmula proporcional y se divide en partes iguales únicamente entre los submedidores configurados para pagar cuota común.

---

## ⚙️ Configuración del Proyecto

1. **Clonar el repositorio:**
   ```bash
   git clone [https://github.com/TU_USUARIO/kilowatt.git](https://github.com/TU_USUARIO/kilowatt.git)
