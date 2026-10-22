# Informe de Auditoría Técnica: Componente `PeriodoDateUtils`

**Auditor:** Auditor Técnico Independiente de Software  
**Objeto de Auditoría:** `PeriodoDateUtils.kt` y `PeriodoDateUtilsTest.kt`  
**Alcance:** Funcionalidad, cobertura de pruebas, casos borde, rendimiento y retrocompatibilidad.

---

## 1. Resumen Ejecutivo

Se ha auditado la implementación del componente utilitario [`PeriodoDateUtils.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/util/PeriodoDateUtils.kt) y su correspondiente batería de pruebas unitarias [`PeriodoDateUtilsTest.kt`](file:///home/fernando/StudioProjects/KiloWatt/app/src/test/java/com/example/kilowatt/PeriodoDateUtilsTest.kt).

El componente cumple satisfactoriamente con la normalización de cadenas de periodo (formato en español e ISO numérico de 4 dígitos), el cálculo aritmético de fechas límite de pago (manejando adecuadamente el cambio de año en diciembre y el ajuste de días máximos en meses de 28/30 días) y el formateo de etiquetas para gráficos.

No se detectaron hallazgos **Críticos** ni **Altos**. Se han identificado 2 hallazgos de severidad **Media** relacionados con la omisión de soporte para años de 2 dígitos (`YY`) y re-creación interna de expresiones regulares/formateadores en bucles de alta frecuencia.

---

## 2. Hallazgos Críticos

*(Sin hallazgos en esta categoría)*  
No se detectaron fallos de seguridad, pérdida de datos ni vulnerabilidades que comprometan la integridad del sistema.

---

## 3. Hallazgos Altos

*(Sin hallazgos en esta categoría)*  
No existen fallos funcionales que impidan la ejecución correcta en condiciones operativas normales.

---

## 4. Hallazgos Medios

### HM-01: Falta de soporte para formato de año de 2 dígitos (`YY`)
- **Ubicación:** `PeriodoDateUtils.kt` line 44 (`Regex("\\b(20\\d{2})\\b")`) y `convertirAFechaOrdenable`.
- **Descripción:** La expresión regular utilizada para identificar el año filtra strictly valores de 4 dígitos que comiencen con `20` (ej. `"2026"`). Si un usuario ingresa manualmente el periodo como `"Agosto 26"` u `"08/26"`, la extracción de año falla y la función retorna `null`.
- **Impacto:** Si un usuario escribe el año abreviado en `LecturasActivity`, el sistema no podrá determinar su fecha ordenable ni evaluar su vencimiento.
- **Sugerencia:** Ampliar la expresión regular o agregar patrones de fallback con `yy` (ej. `"MM/yy"`, `"yy-MM"`).

---

## 5. Hallazgos Bajos

### HB-01: Re-instanciación de `Regex` en cada llamada a `convertirAFechaOrdenable`
- **Ubicación:** `PeriodoDateUtils.kt` line 44 (`val regexAnio = Regex(...)`).
- **Descripción:** La expresión regular para detectar años de 4 dígitos se compila e instancia en cada invocación del método.
- **Impacto:** En el futuro, cuando la pantalla de gráficos históricos ordene una lista de cientos de lecturas mediante `sortedBy { convertirAFechaOrdenable(...) }`, se generará una sobrecarga innecesaria en la asignación de memoria para el Garbage Collector.
- **Sugerencia:** Mover la `Regex` a una constante privada a nivel de objeto (`private val REGEX_ANIO = Regex(...)`).

---

## 6. Observaciones

1. **Ajuste Aritmético de Días en Meses Cortos:**
   - Se verificó que `calPeriodo.getActualMaximum(Calendar.DAY_OF_MONTH)` y `diaLimite.coerceIn(1, maxDiasEnMes)` funcionan correctamente cuando el mes siguiente es Febrero (ajusta automáticamente un límite del día 31 al día 28 o 29).
2. **Cambio de Año en Diciembre:**
   - Se comprobó que `Calendar.add(Calendar.MONTH, 1)` incrementa de manera nativa el año cuando el periodo es Diciembre (ej. Diciembre 2026 -> Enero 2027).
3. **Thread Safety de DateFormatters:**
   - Las instancias de `SimpleDateFormat` se crean como variables locales dentro de los métodos, lo que garantiza thread-safety de forma limpia sin compartir estado entre hilos.

---

## 7. Riesgos Futuros

- **Volumen en Ejes de Gráficos:** Cuando el histórico acumule más de 24 meses de lecturas, el método `formatearParaGrafico` retornará etiquetas cortas (`"Ago 26"`), lo cual requerirá un control de densidad en la UI para evitar solapamientos visuales en pantallas pequeñas.

---

## 8. Recomendación Final

### **APROBADO CON OBSERVACIONES ⚠️**

La implementación es altamente confiable, limpia y robusta para la operativa actual y futura. Se recomienda abordar los hallazgos `HM-01` (soporte de año en 2 dígitos) y `HB-01` (optimización de Regex) en iteraciones posteriores de mantenimiento.
