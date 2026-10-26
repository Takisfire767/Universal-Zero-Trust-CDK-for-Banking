# Architecture

This project is structured as a Kotlin Multiplatform SDK.

## Components

- `sdk-anti-tamper/` — library module
- `src/commonMain/` — shared API and logic
- `src/androidMain/` — Android and Horizon OS runtime checks
- `src/iosMain/` — iOS runtime checks
- `src/commonTest/` — shared tests

## Behavioral contract

- detect without blocking
- report signals with low overhead
- degrade gracefully instead of exiting the app
- keep backend verification authoritative
