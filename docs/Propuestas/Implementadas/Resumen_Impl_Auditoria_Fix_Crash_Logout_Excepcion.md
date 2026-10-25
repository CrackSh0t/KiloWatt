# Resumen de Implementación y Auditoría: Fix de Estabilidad en Cierre de Sesión (Crash Logout)

Este documento registra el resumen de la investigación, solución y auditoría técnica aplicada para corregir el crash al presionar "Salir" en el Perfil del Propietario.

---

## 1. Resumen de Causa Raíz e Implementación

1. **Causa Raíz:**
   - Hilt no inyectaba `ILecturaRepository` ni `IInquilinoRepository` en `AuthRepository` por tener valores por defecto `= null`.
   - Los listeners de Firestore continuaban activos e insertando en Room mientras `clearAllTables()` ejecutaba en paralelo, provocando una excepción de concurrencia en SQLite.
   - `AuthActivity` se iniciaba de forma asíncrona antes de que `auth.signOut()` finalizara, reconectando al usuario anterior.
2. **Solución:**
   - Conversión de `cerrarSesion()` a función suspendida `suspend fun cerrarSesion() = withContext(Dispatchers.IO)`.
   - Requisito explícito de inyección Hilt para `ILecturaRepository` e `IInquilinoRepository` en `AuthRepository`.
   - Desenganche síncrono de listeners de Firestore ➔ purga de tablas SQLite ➔ purga de SharedPreferences ➔ desautenticación en Firebase Auth.
   - `MainActivity.kt` aguarda a que `cerrarSesion()` finalice antes de navegar a `AuthActivity`.

---

## 2. Resultado de la Auditoría Técnica (`../../IA/SKILL_AUDITOR.md`)

- **Veredicto:** **APROBADO Y RESUELTO EXITOSAMENTE ✅**
- **Hallazgos:** Cero defectos críticos, altos o medios.
- **Conclusión:** Se garantiza el cierre de sesión síncrono, limpio y sin excepciones.

---

## 3. Resultados de Validación

- **Compilación Gradle:** Ejecución exitosa de `:app:assembleDebug`.
- **Pruebas Unitarias:** Ejecución de `:app:testDebugUnitTest` con **37 pruebas pasadas (0 falladas, 0 omitidas)**.
