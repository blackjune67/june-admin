# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
./gradlew build          # Full build + tests
./gradlew bootRun        # Run the application
./gradlew test           # Run tests only
./gradlew check          # Run all checks
./gradlew clean          # Clean build outputs
./gradlew bootJar        # Build executable JAR
```

## Tech Stack

- **Language:** Kotlin 2.2.21 targeting JVM (Java 24)
- **Framework:** Spring Boot 4.0.2
- **Build:** Gradle 9.3.0 with Kotlin DSL (`build.gradle.kts`)
- **Key dependencies:** Spring MVC (REST), Spring Security, Spring Data JPA, Jackson Kotlin module

## Architecture

Early-stage Spring Boot admin system. Entry point: `src/main/kotlin/com/dh/admin/DhAdminApplication.kt`.

Expected layered structure as the project grows:
- **Controllers** — REST endpoints under `com.dh.admin.controller`
- **Services** — Business logic under `com.dh.admin.service`
- **Repositories** — JPA repositories under `com.dh.admin.repository`
- **Entities** — JPA-mapped classes under `com.dh.admin.entity` (Kotlin JPA all-open plugin is configured)

Configuration lives in `src/main/resources/application.yaml`. No database is configured yet — when adding one, configure datasource properties there.

Spring Security is on the classpath but not yet customized. When implementing security, create a `SecurityConfig` class in `com.dh.admin.config`.

## Java Version Note

The project targets Java 24. The current environment may have Java 25 installed; Kotlin 2.2.21 has a compatibility note around this. If Gradle toolchain auto-provisioning fails, ensure JDK 24 is available locally.
