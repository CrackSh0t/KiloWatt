# Corrección de cierre inesperado (Crash) al iniciar la aplicación

La aplicación se cierra inmediatamente debido a cambios recientes en el esquema de la base de datos Room (`Submedidor` y `Lectura`) sin haber incrementado la versión de la base de datos ni proporcionado una migración. Esto causa un `IllegalStateException` al intentar acceder a los datos.

## Cambios Propuestos

### Componente de Datos (Room)

#### [MODIFY] [AppDatabase.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/AppDatabase.kt)
- Incrementar la versión de la base de datos de `1` a `2`.
- Agregar `.fallbackToDestructiveMigration()` al constructor de la base de datos para permitir que Room recree las tablas con el nuevo esquema (esto borrará los datos de prueba actuales, lo cual es normal en esta etapa de desarrollo).

#### [MODIFY] [InquilinoDao.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/data/InquilinoDao.kt)
- Cambiar el tipo de retorno de `insertarInquilino` de `Unit` a `Long` para poder obtener el ID del inquilino recién creado.

### Actividades

#### [MODIFY] [ConfiguracionActivity.kt](file:///D:/Usuarios/Fernando/AndroidStudioProjects/KiloWatt/app/src/main/java/com/example/kilowatt/ConfiguracionActivity.kt)
- Corregir la lógica de guardado para que el `Submedidor` se asocie correctamente con el `Inquilino` creado (usando el ID retornado por la base de datos).
- Mejorar la experiencia de usuario agregando un botón para ir a la pantalla de Lecturas una vez configurado.

## Plan de Verificación

### Pruebas Manuales
- Ejecutar la aplicación y verificar que ya no se cierra al iniciar.
- Registrar un nuevo inquilino y verificar que aparece en la lista.
- Verificar que el registro de inquilinos y submedidores funcione correctamente en la base de datos.
