# Reglas Globales del Proyecto KiloWatt

Este proyecto se rige por las especificaciones de arquitectura, datos y desarrollo definidas en la carpeta `docs/`. Todo agente o sugerencia de código DEBE consultar y respetar los siguientes documentos:

## 📚 Documentación de Referencia Obligatoria
- **Arquitectura y MVVM:** Ver `docs/ARQUITECTURA.md`
- **Estilo de Código y Buenas Prácticas:** Ver `docs/REGLAS_DE_DESARROLLO.md`
- **Persistencia (Room + Firestore):** Ver `docs/MODELO_DATOS.md`
- **Gestión de Deuda Técnica:** Ver `docs/DEUDA_TECNICA.md` y `docs/Propuestas/Plan_Iterativo_Deuda_Tecnica.md`
- **Ruta de Entregables:** Ver `docs/ROADMAP.md`

## 🛠️ Reglas Técnicas Invariables
1. **Contrato de Datos:** Usar obligatoriamente `OperationResult<T>` (`Synced`, `PendingSync`, `Failed`) para las operaciones de persistencia.
2. **Prohibición de Silenciar Errores:** Nunca usar `catch (_: Exception) {}` ni emitir `emptyList()` en `Flow` ante un fallo de Room o Firestore.
3. **Pruebas Automatizadas:** Todo cambio en el dominio o ViewModels debe contar con sus pruebas unitarias en `app/src/test/`.