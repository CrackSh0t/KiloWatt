---
applyTo: '**'
description: 'description'
---
1. ARQUITECTURA:
    - Respetar Clean Architecture (Domain, Data, Presentation) con MVVM y Hilt.
    - La fuente única de verdad (SSOT) es Room, respaldada en tiempo real por Cloud Firestore.
    - Las operaciones asíncronas deben usar Kotlin Coroutines y Flow.

2. LÓGICA FINANCIERA:
    - Todo cálculo de cobro debe invocar o extender `CobroCalculator.kt`.
    - Garantizar la conservación exacta del dinero facturado sin desbalances por redondeo.
    - Las cantidades numéricas de personas o lecturas deben incluir sanitización defensiva mediante `.coerceAtLeast(1)`.

3. INTERFAZ Y REQUISITOS DE PRUEBAS:
    - Utilizar ViewBinding y componentes de Material Design 3.
    - Todo cambio en el dominio o ViewModels requiere acompañarse de sus pruebas unitarias en `app/src/test/` con JUnit 4 y Coroutines Test, verificando el 100% en verde con `./gradlew :app:testDebugUnitTest`.