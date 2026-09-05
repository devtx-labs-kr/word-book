# WordBook Web

A local flashcard app with SM-2 spaced repetition. Brownfield rewrite of the original Swift macOS
app as **Java 17 + Spring Boot + React (Vite, TypeScript)**, packaged as a single local `java -jar`
artifact. The Swift original is not part of this copy.

> Status: **U1 — Foundation & Walking Skeleton**. The minimal end-to-end slice (create deck → add
> card → start study → answer one card → SRS fields update) is implemented. Full CRUD, search,
> statistics, import/export, and settings UIs arrive in U2–U6.

## Prerequisites

- Java 17
- Node.js + npm (used by the Gradle build via the `node-gradle` plugin; uses the system Node)

## Build

```bash
./gradlew build
```

This single command lint-and-builds the React frontend (ESLint + Prettier → `vite build`), folds
the bundle into the Spring Boot static resources, compiles the backend, runs **both** test suites —
the JUnit backend suite including the SM-2 golden tests, and the frontend vitest suite, which is
wired into `check` so it cannot bypass the build gate — checks formatting (Spotless /
google-java-format), and produces the executable jar at `build/libs/wordbook.jar` with the frontend
embedded.

To run only the frontend unit tests, from the `frontend/` directory:

```bash
cd frontend && npm test
```

## Run

```bash
java -jar build/libs/wordbook.jar
```

Then open http://127.0.0.1:8080. The server binds to `127.0.0.1` only (local access). Data is
persisted to an H2 file store at `./data/wordbook.mv.db` next to where you run the jar, so it
survives restarts. Health: http://127.0.0.1:8080/actuator/health

## Development mode (optional)

For fast feedback you can run the backend and the Vite dev server separately:

```bash
./gradlew bootRun                 # backend on :8080
cd frontend && npm run dev        # Vite dev server, proxies /api to :8080
```

## Project layout

```
build.gradle / settings.gradle    Root Gradle build (Spring Boot + node-gradle + Spotless)
backend/src/main/java             Spring Boot application (Java 17)
backend/src/test/java             JUnit tests incl. the SM-2 golden suite
frontend/                         React + Vite + TypeScript SPA
```

## SM-2 fidelity

`SrsEngine` is an exact 1:1 port of the original Swift `SRSEngine`. The integer interval conversion
uses a Java `(int)` cast (truncation toward zero) to match the Swift `Int(Double)` behaviour — never
`Math.round`/`Math.floor`. The full golden table (including the interval=20/21 boundary, the EF
lower bound 1.3, the floating-point truncation case, and the AGAIN reset) is verified in
`SrsEngineGoldenTest`.
