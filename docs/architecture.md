# Architecture

This project is structured as a Kotlin Multiplatform anti-tamper SDK.

## Layout

- `sdk-anti-tamper/` — shared SDK module
- `src/commonMain/` — public API and cross-platform classification logic
- `src/androidMain/` — Android / Horizon OS implementations
- `src/iosMain/` — iOS implementations
- `src/commonTest/` — shared unit tests

## Detection model

The SDK collects runtime signals, classifies them, and returns a risk value without terminating the application.

## Risk policy

- `LOW` — normal flow
- `WARNING_60` — log and re-attest
- `DRIFT_75` — challenge + shorter TTL
- `CRITICAL_90` — step-up auth + audit
