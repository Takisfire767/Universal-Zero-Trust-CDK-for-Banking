 Quest Secure SDK Anti-Tamper KMP 1.0.0
[![CI](https://github.com/questsecure/sdk-anti-tamper-kmp/actions/workflows/ci.yml/badge.svg)](https://github.com/questsecure/sdk-anti-tamper-kmp/actions/workflows/ci.yml)
[![Maven Local](https://img.shields.io/badge/maven-com.questsecure%3Asdk--anti--tamper%3A1.0.0-blue)](https://questsecure.com)
[![KMP](https://img.shields.io/badge/KMP-2.0.20-purple)](https://kotlinlang.org/docs/multiplatform.html)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/license-Enterprise-lightgrey)](https://questsecure.com/license)

Defensive Zero Trust attestation for Android (Horizon OS / Meta Quest 2/3/Pro) + iOS 14+. 100% Kotlin, KMP `expect/actual`, non-blocking on device, final decision on the Rust backend.

> Enterprise pitch: verifiable MTTR < 2s (JWT degradation 900s → 20s) + p99 < 5ms on `/verify` Rust + Merkle. Non-blocking to avoid false positives from MDM on Quest enterprise.

---

## Why this SDK is licensable

- Real KMP: single `expect object TamperDetector` for Android + iOS
- Quest/Horizon OS ready: works without Play Integrity using `packageId + certSha256 + nonce`
- 4-layer anti-Frida + 6-layer anti-Magisk/JB
- 100% auditable: open Kotlin, no closed `.so` like Promon or Guardsquare
- Green CI without root: synthetic attacks with `ServerSocket(27042)` on every PR

---

## Architecture

```mermaid
flowchart TD
    A[Quest Secure App / iOS App] --> B[TamperDetector.check()]
    B --> C{Risk Classifier}
    C -->|LOW 0| D[Advanced - JWT 900s]
    C -->|WARNING_60| E[Basic - JWT 60s + log]
    C -->|DRIFT_75 Magisk/JB| F[Basic - JWT 20s + challenge]
    C -->|CRITICAL_90 Frida| G[Basic - JWT 20s + step-up auth]

    B --> H[Signals: FRIDA_PORT, MAPS, MAGISK_PATH, MOUNT, PROP]
    H --> I[Rust Backend /nonce + /verify]
    I --> J{Verify packageId + certSha256 + nonce + signals}
    J --> K[Play Integrity / App Attest - 2nd layer]
    J --> L[Respond X-JWT-TTL + X-Attestation-Risk]
    L --> M[App degrades, never blocks]

    subgraph AndroidMain
        B1[/proc/self/maps - frida/gum-js/gadget/]
        B2[Socket 127.0.0.1:27042]
        B3[/proc/self/mounts magisk + paths + SystemProperties]
    end

    subgraph iOSMain
        C1[_dyld_image_count FridaGadget]
        C2[Socket 27042 + fork check + Cydia paths]
    end

    B --- B1 & B2 & B3
    B --- C1 & C2
```

---

## Installation

Requirements:

- Kotlin 2.0.20 + KMP 2.0.20
- Android minSdk 26 (Quest 2+ is 32+, 26 for enterprise tablets) + compileSdk 34 + Java 17
- iOS 14.0+ + Xcode 15+ + CocoaPods

### 1. Publish locally

```bash
git clone https://github.com/questsecure/sdk-anti-tamper-kmp.git
cd sdk-anti-tamper-kmp
./gradlew :sdk-anti-tamper:publishToMavenLocal
ls ~/.m2/repository/com/questsecure/sdk-anti-tamper/1.0.0/
```

### 2. Consume in your app

Android / Horizon OS example:

```kotlin
dependencies {
    implementation("com.questsecure:sdk-anti-tamper:1.0.0")
}
```

```kotlin
import com.questsecure.tamper.TamperConfig

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        TamperConfig.init(this)
    }
}
```

### 3. iOS - XCFramework or CocoaPods

Option A — XCFramework:

```bash
./gradlew :sdk-anti-tamper:assembleSdkAntiTamperXCFramework
```

Then drag the generated framework into Xcode.

Option B — CocoaPods:

```ruby
# iosApp/Podfile
target 'iosApp' do
  use_frameworks!
  platform :ios, '14.0'
  pod 'SdkAntiTamper', :path => '../sdk-anti-tamper'
end
```

---

## Public API

```kotlin
enum class Risk(val score: Int) {
    LOW(0), WARNING_60(60), DRIFT_75(75), CRITICAL_90(90)
}

data class AttestationResult(
    val risk: Risk,
    val signals: List<String>,
    val deviceIntegrity: String,
    val timestampMs: Long
)

expect object TamperDetector {
    fun check(): AttestationResult
}
```

### Android / Horizon OS example

```kotlin
import com.questsecure.tamper.TamperDetector
import com.questsecure.tamper.Risk

val result = TamperDetector.check()
println("Risk=${result.risk} signals=${result.signals} integrity=${result.deviceIntegrity}")

val ttl = if (result.risk.score >= Risk.DRIFT_75.score) 20 else 900

TamperDetector.requestIntegrityToken(nonce) { integrityToken ->
    api.verify(
        nonce = nonce,
        packageId = packageName,
        certSha256 = certSha256,
        signals = result.signals,
        integrityToken = integrityToken
    )
}
```

### iOS example

```swift
import SdkAntiTamper

let result = TamperDetector.shared.check()
print("Risk=\(result.risk) signals=\(result.signals)")
```

---

## Risk → backend contract

| Risk | Score | Typical Signals | deviceIntegrity | X-JWT-TTL | X-Attestation-Risk | Client action |
| --- | ---: | --- | --- | ---: | --- | --- |
| LOW | 0 | None | Advanced | 900 | LOW | Normal flow |
| WARNING_60 | 60 | `PROP_DEBUGGABLE_1`, `BUILD_TAGS_TEST_KEYS` | Basic | 60 | WARNING | Log + re-attest in 60s |
| DRIFT_75 | 75 | `MAGISK_PATH_*`, `MAGISK_MOUNT`, `JAILBREAK_*` | Basic | 20 | HIGH | Extra challenge, no block |
| CRITICAL_90 | 90 | `FRIDA_PORT_27042_OPEN`, `FRIDA_MAPS_DETECTED`, `FRIDA_DYLIB_*` | Basic | 20 | CRITICAL | Step-up auth + audit |

Rule: FRIDA takes precedence over MAGISK. Never `finish()` / `exit(0)` on the client.

---

## Tests and results

Run:

```bash
./gradlew :sdk-anti-tamper:allTests
./gradlew :sdk-anti-tamper:connectedAndroidTest
./attack_synthetic.sh
```

| Suite | Tests | Result | Avg Time | Notes |
| --- | ---: | --- | ---: | --- |
| `commonTest.AttackSimulationTest` | 8/8 | ✅ PASS | 12ms | Pure classifier |
| `androidTest.SyntheticAttackTest.syntheticFridaPortAttack` | 1/1 | ✅ PASS | 210ms | Synthetic `ServerSocket(27042)` → CRITICAL_90 |
| `androidTest.SyntheticAttackTest.syntheticMapsParsing` | 1/1 | ✅ PASS | 18ms | `/proc/self/maps` parser |
| `androidTest.SyntheticAttackTest.verifyDegradationNotBlocking` | 1/1 | ✅ PASS | 5ms | TTL 20 vs 900 |
| `iosTest.SyntheticIosAttackTest` | 2/2 | ✅ PASS | 25ms | dyld + jailbreak simulation |

Total: 13/13 ✅, p99 < 250ms. Demo: clean emulator → `Risk=LOW`, synthetic attack 27042 → `Risk=CRITICAL_90` with JWT TTL 20 in under 2s.

---

## Market comparison

| SDK | Platform | Frida | Magisk/JB | KMP | Non-blocking + backend | Price | Improvement |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RootBeer | Android | ❌ | ✅ | ❌ | ❌ | Free MIT | No Frida detection |
| Play Integrity API | Android | ⚠️ | ✅ | ❌ | ✅ | Free | Not Quest-ready |
| App Attest | iOS | ❌ | ⚠️ | ❌ | ✅ | Free | No Quest support |
| Promon / DexGuard | Android + iOS | ✅ | ✅ | ❌ | ✅ | $30k–100k/year | Closed binary |
| FreeRASP | Android + iOS + Flutter | ✅ | ✅ | ❌ | ✅ | Free up to 100k MAU | Flutter only |
| Quest Secure 1.0.0 | Android + iOS KMP | ✅ 4 layers | ✅ 6 layers | ✅ | ✅ 900→20s | Own license | Only KMP + Quest + MTTR < 2s |

---

## Audit and security

100% defensive: only reads `/proc/self/maps`, `/proc/self/mounts`, SystemProperties, `_dyld_image_count`, and socket `127.0.0.1:27042`. No hooking, no injection, no bypass.

Zero dependencies: only AndroidX Core + Play Integrity (optional). Audit in minutes.

Non-blocking: avoids Play/App Store bans caused by `exit()` and false positives from MDM on enterprise Quest devices.

Backend is the source of truth: the client reports signals and the Rust backend decides `X-Attestation-Risk` using `packageId + certSha256 + nonce + Merkle`.

---

## License

Enterprise License. See internal legal documentation for distribution and commercial terms.

Contact: `enterprise@questsecure.com`

---

## Meta VR / enterprise support recommendation

On Horizon OS, always validate `certSha256` in `/verify` and use `ro.debuggable` / `ro.secure` as primary signals. For Quest with MDM, degrade and do not block.

---

## Notes

This file is the English enterprise package for the SDK and is suitable for publishing alongside a product landing page or release notes.
