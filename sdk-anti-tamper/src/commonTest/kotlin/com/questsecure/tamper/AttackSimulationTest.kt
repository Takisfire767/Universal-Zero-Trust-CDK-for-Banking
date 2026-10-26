package com.questsecure.tamper

import kotlin.test.Test
import kotlin.test.assertEquals

class AttackSimulationTest {
    @Test
    fun `classifier returns low for clean state`() {
        assertEquals(Risk.LOW, RiskClassifier.classify(emptyList()))
    }

    @Test
    fun `classifier returns warning for debug build flags`() {
        assertEquals(Risk.WARNING_60, RiskClassifier.classify(listOf("PROP_DEBUGGABLE_1")))
    }

    @Test
    fun `classifier returns drift for magisk signals`() {
        assertEquals(Risk.DRIFT_75, RiskClassifier.classify(listOf("MAGISK_MOUNT")))
    }

    @Test
    fun `classifier returns critical for frida signals`() {
        assertEquals(Risk.CRITICAL_90, RiskClassifier.classify(listOf("FRIDA_PORT_27042_OPEN")))
    }
}
