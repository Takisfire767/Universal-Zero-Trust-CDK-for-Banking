package com.questsecure.tamper

import kotlin.system.getTimeMillis

public class TamperDetectorFactory {
    public fun create(): TamperDetector = TamperDetector
}

public object TamperDetector {
    public fun check(): AttestationResult {
        val signals = mutableListOf<String>()

        if (System.getProperty("ro.debuggable") == "1") {
            signals += "PROP_DEBUGGABLE_1"
        }

        if (System.getProperty("ro.secure") == "0") {
            signals += "PROP_SECURE_0"
        }

        if (System.getProperty("ro.build.tags")?.contains("test-keys", ignoreCase = true) == true) {
            signals += "BUILD_TAGS_TEST_KEYS"
        }

        val risk = RiskClassifier.classify(signals)
        return AttestationResult(
            risk = risk,
            signals = signals.distinct(),
            deviceIntegrity = RiskClassifier.deviceIntegrity(risk),
            timestampMs = getTimeMillis()
        )
    }
}
