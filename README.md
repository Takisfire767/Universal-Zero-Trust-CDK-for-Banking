# Quest Secure SDK Anti-Tamper KMP

Defensive Zero Trust attestation for Android (Horizon OS / Meta Quest 2/3/Pro) and iOS 14+.

## Overview

This repository contains a Kotlin Multiplatform SDK designed to detect signals of tampering and classify the risk without blocking the user experience. The app reports signals, while the backend remains the final decision authority.

## Modules

- `sdk-anti-tamper/` — KMP SDK module
- `docs/` — security and architecture docs
- `examples/` — sample code and integration snippets
- `scripts/` — validation automation

## Quick start

```bash
./gradlew :sdk-anti-tamper:allTests
```

## API example

```kotlin
val result = TamperDetector.check()
println("risk=${result.risk} signals=${result.signals}")
```

## Risk levels

- LOW — normal device state
- WARNING_60 — debug/test signals observed
- DRIFT_75 — Magisk/JB environment detected
- CRITICAL_90 — Frida or equivalent instrumentation attack signals detected
