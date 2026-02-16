# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform (KMP) project targeting Android, iOS, and Desktop (JVM) using Compose Multiplatform for shared UI. Package: `dev.skrip.aichallenge`.

## Build Commands

```bash
# Desktop (JVM) - run
./gradlew :composeApp:run

# Android - build debug APK
./gradlew :composeApp:assembleDebug

# Run tests
./gradlew :composeApp:allTests

# iOS - open iosApp/ directory in Xcode and run from there
```

## Architecture

- **composeApp/src/commonMain/** - Shared Kotlin code for all platforms (UI, business logic)
- **composeApp/src/androidMain/** - Android-specific implementations
- **composeApp/src/iosMain/** - iOS-specific implementations
- **composeApp/src/jvmMain/** - Desktop JVM-specific implementations
- **iosApp/** - iOS app entry point (SwiftUI wrapper)
- Keep architecture simple and modern.
- No DI frameworks.
- No Clean Architecture overengineering.
- No unnecessary interfaces.
- Single module project.

## Style
- Minimalist UI
- Compose Multiplatform Desktop
- Material 3
- No unnecessary animations
- Clear and readable Kotlin

## API
- Anthropic Messages API
- API key must come from environment variable
- Never hardcode secrets

### Platform Abstraction Pattern

Platform-specific code uses Kotlin's expect/actual pattern:
- `Platform.kt` in commonMain declares `expect fun getPlatform(): Platform`
- Each platform (androidMain, iosMain, jvmMain) provides `actual fun getPlatform()` implementation

## Key Technologies

- Kotlin 2.3.0
- Compose Multiplatform 1.10.0
- Material3 for UI components
- Compose Hot Reload enabled for development
