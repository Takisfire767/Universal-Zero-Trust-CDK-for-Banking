package com.questsecure.tamper

public expect object TamperDetector {
    public fun check(): AttestationResult
}

public enum class Risk(val score: Int) {
    LOW(0),
    WARNING_60(60),
    DRIFT_75(75),
    CRITICAL_90(90)
}

public data class AttestationResult(
    val risk: Risk,
    val signals: List<String>,
    val deviceIntegrity: String,
    val timestampMs: Long
)

public object RiskClassifier {
    public fun classify(signals: List<String>): Risk {
        if (signals.any { it.contains("FRIDA", ignoreCase = true) || it.contains("GADGET", ignoreCase = true) }) {
            return Risk.CRITICAL_90
        }
        if (signals.any { it.contains("MAGISK", ignoreCase = true) || it.contains("JAILBREAK", ignoreCase = true) }) {
            return Risk.DRIFT_75
        }
        if (signals.any { it.contains("DEBUGGABLE", ignoreCase = true) || it.contains("TEST_KEYS", ignoreCase = true) }) {
            return Risk.WARNING_60
        }
        return Risk.LOW
    }

    public fun deviceIntegrity(risk: Risk): String = if (risk == Risk.LOW) "Advanced" else "Basic"
}
