# Consumer example

## Android

```kotlin
import com.questsecure.tamper.TamperDetector

val result = TamperDetector.check()
println("risk=${result.risk} integrity=${result.deviceIntegrity}")
```

## iOS

```swift
import SdkAntiTamper

let result = TamperDetector.shared.check()
print("risk=\(result.risk) signals=\(result.signals)")
```
