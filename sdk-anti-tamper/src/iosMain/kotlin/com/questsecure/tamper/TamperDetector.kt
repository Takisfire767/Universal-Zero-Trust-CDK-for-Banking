package com.questsecure.tamper

import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSDate

public actual object TamperDetector {
    public actual fun check(): AttestationResult {
        val signals = mutableListOf<String>()

        val fileManager = NSFileManager.defaultManager
        val jailbreakPaths = listOf(
            "/Applications/Cydia.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/etc/apt",
            "/private/var/lib/apt",
            "/private/var/stash"
        )

        jailbreakPaths.forEach { path ->
            if (fileManager.fileExistsAtPath(path)) {
                signals += when {
                    path.contains("Cydia", ignoreCase = true) -> "CYDIA_APP_DETECTED"
                    path.contains("MobileSubstrate", ignoreCase = true) -> "SUBSTRATE_DYLIB_DETECTED"
                    path.contains("apt", ignoreCase = true) -> "JAILBREAK_PATH_DETECTED"
                    else -> "JAILBREAK_ROOT_PATH_DETECTED"
                }
            }
        }

        val dyldInsert = NSProcessInfo.processInfo.environment["DYLD_INSERT_LIBRARIES"] as? String
        if (!dyldInsert.isNullOrBlank()) {
            signals += "FRIDA_DYLIB_INSERTED"
        }

        val risk = RiskClassifier.classify(signals)
        return AttestationResult(
            risk = risk,
            signals = signals.distinct(),
            deviceIntegrity = RiskClassifier.deviceIntegrity(risk),
            timestampMs = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
        )
    }
}
