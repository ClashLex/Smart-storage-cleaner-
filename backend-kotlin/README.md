# Kotlin backend migration

This module is the first migration slice from the Express backend to Kotlin/JVM using Ktor.

## Run locally

Requires JDK 17 and the repository Gradle wrapper.

```bash
./gradlew :backend-kotlin:run
```

The service listens on port `3001` by default (override with `PORT`).

## Implemented endpoints

- `GET /health` — service health and migration stage.
- `POST /api/scan/analyze` — validates scan categories, calculates byte totals, formats storage sizes, and returns deterministic cleanup guidance.

Example request:

```json
{
  "categories": [
    { "category": "Cache", "sizeBytes": 50000000 },
    { "category": "Duplicates", "sizeBytes": 250000000 }
  ]
}
```

## Migration policy

The existing `backend/` Express service remains the active API during this first slice. Do not point the Android app or dashboard at this service yet. Authentication, MongoDB persistence, account deletion, admin analytics, billing verification/webhooks, rate limiting, and API contract tests must be ported and verified before cutover.

The React/Vite dashboard remains TypeScript because it runs in browsers. Kotlin/JVM cannot replace browser UI code directly. A Kotlin/Wasm or Compose Multiplatform web rewrite would be a separate frontend project.
