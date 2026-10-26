# Quest Secure SDK Anti-Tamper KMP

Defensive Zero Trust attestation for Android (Horizon OS / Meta Quest 2/3/Pro) and iOS 14+.

## Overview

This repository contains a Kotlin Multiplatform SDK that detects tampering signals, classifies them into risk levels, and keeps the app experience non-blocking. The backend remains the source of truth for final attestation policy.

## Quick start

```bash
./gradlew :sdk-anti-tamper:allTests
```

## API

```kotlin
import com.questsecure.tamper.TamperDetector

val result = TamperDetector.check()
println("risk=${result.risk} signals=${result.signals}")
```

## Risk model

- LOW = 0
- WARNING_60 = 60
- DRIFT_75 = 75
- CRITICAL_90 = 90

## Status

This revision includes the core SDK API, anti-tamper signal classification, Android/iOS implementations, and CI scaffolding.
