# Propuesta de Diseño Técnico: Fase 4 - Rediseño Visual, UX Moderno y Polishing Material Design 3

**Autor:** Arquitecto de Software Senior  
**Fecha:** Octubre 2026  
**Proyecto:** Kilowatt Android  
**Estado:** Propuesta de Arquitectura Actualizada con Retroalimentación de Tech Lead y UX  

---

## 1. Resumen Ejecutivo

Con las **Fases 1, 2 y 3 completadas al 100%** (MVVM, StateFlow, Hilt DI, Offline-First SSOT con Room + Firestore, y comprobantes PDF con sellos visuales), el sistema Kilowatt posee una base de datos y negocio sólida y aislada.

La **Fase 4** tiene como propósito optimizar la **Arquitectura de Información (UX) e Interfaz Gráfica (UI)** mediante los lineamientos de **Material Design 3 (M3)**. 

A partir de la retroalimentación de UX y Tech Lead, el ajuste clave consiste en **desacoplar los datos de configuración del propietario fuera de `ConfiguracionActivity` y reubicarlos en el centro de "Perfil del Propietario" (al tocar el Avatar en `MainActivity`)**, permitiendo que `ConfiguracionActivity` quede dedicada al 100% al registro operativo de "Inquilinos y Medidores".

---

## 2. Alcance Técnico y Arquitectura de Información (UX)

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                             │
│  - TopAppBar con Título KiloWatt + Avatar de Propietario    │
└──────────────────────────────┬──────────────────────────────┘
                               │ (Clic en Avatar)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               Diálogo / Vista "Perfil del Propietario"       │
│  1. Datos de Cobro (Titular, Número Yape/Plin, Día Límite) │
│  2. Datos de Cuenta (Email de usuario activo)               │
│  3. Accionable "Cerrar Sesión"                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│             ConfiguracionActivity ("Inquilinos y Medidores") │
│  - Vista simplificada enfocada al 100% en la administración  │
│    operativa de inquilinos y submedidores.                 │
└─────────────────────────────────────────────────────────────┘
```

### A. Reubicación del Perfil del Propietario y Logout
1. **Centro de "Perfil del Propietario" (Avatar en `MainActivity`):**
   - Se incorporará un componente Avatar/Perfil en la `TopAppBar` de `MainActivity`.
   - Al tocar el Avatar, se desplegará un `BottomSheetDialog` / Diálogo M3 titulado **"Perfil del Propietario"**, donde convivirán:
     - **Datos de Cobro del Propietario:** Formulario para consultar y actualizar el Nombre del Titular, Número de Yape/Plin/Transferencia y Día Límite de Pago (utilizados para los recibos PDF).
     - **Información de la Cuenta:** Muestra del correo electrónico del usuario autenticado.
     - **Botón "Cerrar Sesión":** Acción centralizada para salir de la aplicación invocando `AuthViewModel` / `IAuthRepository`.

2. **Simplificación de `ConfiguracionActivity` ("Inquilinos y Medidores"):**
   - Se removerán los campos de perfil del propietario y el botón de logout.
   - La pantalla quedará dedicada al 100% al registro, edición y eliminación de inquilinos y submedidores, eliminando sobrecarga visual.

### B. Estandarización de `TopAppBar` M3 y Componentes
1. **`TopAppBar` Unificada:**
   - Tema base `Theme.Material3.DayNight.NoActionBar` en `themes.xml`.
   - `MaterialToolbar` unificada (`@id/topAppBar`) en las pantallas secundarias (`LecturasActivity`, `ResumenCobrosActivity`, `ConfiguracionActivity`, `HistorialConsumoActivity`) con botón de retorno `←` (`setDisplayHomeAsUpEnabled`).
2. **Formularios e Inputs (`TextInputLayout`):**
   - Estandarización al estilo `@style/Widget.Material3.TextInputLayout.OutlinedBox` en todos los campos de texto del sistema.
3. **Optimización de Gráficos `MPAndroidChart` (`HistorialConsumoActivity`):**
   - Ajuste de márgenes inferiores (`paddingBottom="24dp"`, `clipToPadding="false"`, `extraBottomOffset = 15f`) en `activity_historial_consumo.xml` para prevenir cualquier recorte de etiquetas de fecha (ej: `"Ago 26"`).

---

## 3. Plan de Iteraciones de Desarrollo

```
[Iteración 1] Centro de "Perfil del Propietario" (Avatar en MainActivity + Datos de Cobro + Logout)
              y Simplificación de ConfiguracionActivity ("Inquilinos y Medidores")
      │
      ▼
[Iteración 2] TopAppBar unificada M3, Formularios OutlinedBox y Ajustes en MPAndroidChart
```

---

## 4. Estrategia de Testing y Resguardo de Pruebas

- La reubicación de los datos del propietario reutiliza los contratos existentes de `ISettingsManager` e `IInquilinoRepository.guardarPerfilPropietario(...)`.
- La lógica de negocio (`CobroCalculator`, `ConsumoHistoricoCalculator`, `PhoneUtils`, `PeriodoDateUtils`) no sufrirá alteraciones.
- **La suite completa de 36 pruebas unitarias continuará ejecutándose y pasando al 100%.**

---

## 5. Recomendación Final

### **APROBAR PROPUESTA TÉCNICA AJUSTADA (FASE 4)**
Se presenta el documento actualizado con la retroalimentación de UX para la revisión final del Tech Lead previo al inicio del desarrollo.
