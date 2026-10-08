# Contributing

The Golden Leaf is a portfolio project. Please keep changes reproducible and distinguish demonstrated behavior from production claims.

1. Open an issue describing the behavior, scope and acceptance criteria. Never include Firebase private keys, passwords, banking evidence, personal customer data or a token in a screenshot.
2. Create a focused branch (`codex/<scope>` for Codex-assisted work, otherwise `feat/<scope>` or `fix/<scope>`). Use Conventional Commits and avoid generated APK/JAR/test reports in source control.
3. Run the [local demo](docs/week-7-portfolio.md) with JDK 17. Keep the demo profile exclusive, loopback-only, in-memory and synthetic. Never weaken real authentication to make onboarding easier.
4. Backend: `The-Golden-Leaf-server/mvnw verify`. CI additionally runs MySQL suites against its disposable fixture schema; do not supply a real database URL to tests.
5. Android: `gradlew testDebugUnitTest assembleDebug lintDebug`. Device/Firebase testing is separate; never share account credentials in the PR.
6. Web journeys: package backend, then `cd tools/demo-browser`, `npm ci`, `npx playwright install chromium`, `npm test`. Review browser report and relevant screenshots after UI changes.
7. Update API docs/demo guide with changes. Add a new Flyway migration; never rewrite an applied migration. Financial/ownership changes need negative/idempotency/concurrency tests.

CI jobs are backend, android, infrastructure and demo-browser. Do not merge known failing checks. Image/dependency scanning, hosting and live operational acceptance are separate from the local demo.

Repository publication does not automatically supply an open-source license; the owner has not selected one in this milestone.
