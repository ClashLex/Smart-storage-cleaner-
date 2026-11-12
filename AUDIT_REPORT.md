# Repository Audit Report

Audit date: 2026-11-09
Branch: audit/fix-2026-11-09

## Scope inspected
- Android application source, Gradle configuration, manifest, repositories, ViewModels, workers, and API models.
- Express backend, middleware, Mongoose models, route handlers, environment examples, and tests.
- React/Vite dashboard authentication and data pages.
- Repository tree and tracked generated artifacts.

## Critical findings
1. Administrative authentication bypass: admin middleware accepted a static default secret header. Removed the bypass; admin routes now require a valid application JWT and database role.
2. Mock Firebase authentication outside tests: restricted mock tokens to NODE_ENV=test.
3. JWT fallback secret: production startup now requires a configured 32-character JWT secret and valid Firebase service account JSON.
4. Billing mock fallback: production no longer grants premium without Google Play verification. Mock billing is test-only or explicitly enabled in non-production.
5. Unauthenticated Pub/Sub webhook: now validates a Google-signed OIDC token against configured audience and service-account email.
6. Android login/API token mismatch: sync now forwards the Firebase ID token as Authorization bearer and maps backend response DTOs explicitly.
7. Hardcoded dashboard mock login: removed simulated admin identities and local-storage mock authentication.
8. Tracked generated binaries and signing material: removed the debug APK and encoded debug keystore from this branch; moved the TFLite model into the Android app assets directory and expanded ignore rules. Rotate the debug key if it was used outside disposable development.

## Reliability and data-integrity findings
- Scan API now validates category names, item counts, safe integer byte sizes, and total-size overflow.
- Cleanup log API now validates category strings and safe integer byte counts.
- Subscription schema now permits pending state, which billing can return.
- Admin user listing uses aggregation and pagination, avoids N+1 log queries, and handles missing emails.
- Admin routes validate IDs and prevent self-demotion or self-deletion through those endpoints; subscription listing is paginated.
- Express limits JSON request bodies, disables x-powered-by, configures CORS, and avoids exposing production stack traces.
- Server validates production environment variables and handles SIGTERM/SIGINT shutdown.
- Android API URL and Google web client ID are configurable; release signing no longer depends on a checked-in/default keystore.
- Android HTTP logging redacts Authorization and avoids request-body logging.
- Fake Android purchase-token fallback was removed. The Paywall still needs a real BillingClient purchase flow.

## Important issues still requiring runtime validation
1. Android photo deletion currently removes Room index rows in the inspected repository path; it does not demonstrate deletion of original MediaStore image files. Do not claim photo cleanup is complete until a MediaStore deletion flow and Android 10+ consent handling are implemented and tested.
2. AutoCleanWorker can automatically delete APK files from Downloads. Replace this destructive behavior with review/confirmation before production.
3. Photo duplicate scanning contains synthetic bitmap fallbacks and metadata-derived hash values; these can produce misleading results. Replace with real image-derived perceptual hashes and avoid invented results when image access fails.
4. Verify TFLite model input/output tensor dimensions against the interpreter assumptions (224x224 RGB float input and 1024-float output).
5. Review MANAGE_EXTERNAL_STORAGE against Google Play policy and least-privilege storage access.
6. Funnel and retention dashboard pages contain hardcoded sample telemetry; verify all displayed analytics are clearly labelled or replaced with live API data.
7. Android DTO alignment has been updated but needs a full Gradle build and device login test.
8. Backend tests and dashboard TypeScript build must run in CI before merge. No local Android SDK, MongoDB, Firebase service account, or Play Console credentials were available in this GitHub-only session.

## Validation status
- Repository tree and source files inspected through GitHub.
- Changes are being staged as Git objects on the audit branch.
- GitHub Actions CI passed on the audit branch: backend Jest 4 suites / 14 tests passed; dashboard dependency installation and TypeScript/Vite production build passed.
- Backend npm install reported 16 dependency advisories (6 low, 10 moderate); run npm audit and review remediation before production release.
- Android Gradle build and device tests were not included in CI. Firebase login, Play purchase verification, and Pub/Sub OIDC still require configured credentials and integration tests.
- Do not merge until Android build/device validation and the remaining production integrations have been verified.