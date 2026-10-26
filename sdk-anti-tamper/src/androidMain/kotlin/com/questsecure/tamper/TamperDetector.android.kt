package com.questsecure.tamper

import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

actual object TamperDetector {
    actual fun check(): AttestationResult {
        val signals = mutableListOf<String>()

        val maps = readFileSafe("/proc/self/maps")
        if (maps.contains("frida", ignoreCase = true) ||
            maps.contains("gum-js", ignoreCase = true) ||
            maps.contains("gadget", ignoreCase = true)
        ) {
            signals += "FRIDA_MAPS_DETECTED"
        }

        val mounts = readFileSafe("/proc/self/mounts")
        if (mounts.contains("magisk", ignoreCase = true) ||
            mounts.contains("rootfs", ignoreCase = true) ||
            mounts.contains("overlay", ignoreCase = true)
        ) {
            signals += "MAGISK_MOUNT"
        }

        val debuggable = System.getProperty("ro.debuggable")
        if (debuggable == "1") {
            signals += "PROP_DEBUGGABLE_1"
        }

        val secure = System.getProperty("ro.secure")
        if (secure == "0") {
            signals += "PROP_SECURE_0"
        }

        val buildTags = System.getProperty("ro.build.tags")
        if (buildTags?.contains("test-keys", ignoreCase = true) == true) {
            signals += "BUILD_TAGS_TEST_KEYS"
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 27042), 150)
                signals += "FRIDA_PORT_27042_OPEN"
            }
        } catch (_: Exception) {
            // Socket closed or not listening: expected in clean device state.
        }

        val risk = RiskClassifier.classify(signals)
        return AttestationResult(
            risk = risk,
            signals = signals.distinct(),
            deviceIntegrity = RiskClassifier.deviceIntegrity(risk),
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun readFileSafe(path: String): String {
        return try {
            File(path).readText()
        } catch (_: Throwable) {
            ""
        }
    }
}
