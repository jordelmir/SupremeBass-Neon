package com.supreme.network

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class NetworkDoctorEngineTest {

    private val engine = NetworkDoctorEngine()

    @Test
    fun `test diagnose returns diagnosis`() = runBlocking {
        val diagnosis = engine.diagnose()
        assertNotNull(diagnosis)
        assertTrue(diagnosis.checks.isNotEmpty())
    }

    @Test
    fun `test diagnosis has overall status`() = runBlocking {
        val diagnosis = engine.diagnose()
        assertNotNull(diagnosis.overallStatus)
        assertNotNull(diagnosis.summary)
    }

    @Test
    fun `test diagnosis has score`() = runBlocking {
        val diagnosis = engine.diagnose()
        assertTrue(diagnosis.score in 0..100)
    }

    @Test
    fun `test diagnosis records snapshot`() = runBlocking {
        engine.diagnose()
        engine.diagnose()
        val history = engine.getHistory(1)
        assertTrue(history.size >= 2)
    }

    @Test
    fun `test checks include wifi signal`() = runBlocking {
        val diagnosis = engine.diagnose()
        assertTrue(diagnosis.checks.any { it.name == "Wi-Fi Signal" })
    }

    @Test
    fun `test checks include latency`() = runBlocking {
        val diagnosis = engine.diagnose()
        assertTrue(diagnosis.checks.any { it.name == "Latency" })
    }
}
