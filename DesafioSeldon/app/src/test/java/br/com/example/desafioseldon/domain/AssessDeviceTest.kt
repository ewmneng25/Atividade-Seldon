package br.com.example.desafioseldon.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssessDeviceTest {
    private val assess = AssessDevice()

    private fun device(
        capacity: Double? = 1000.0,
        screenMinutes: Double? = 60.0,
        energy: Double? = 60.0,
        unit: String? = "mAh",
        voltage: Double? = 4.0,
        jank: Double? = 0.0,
        crashes: Int? = 0
    ) = Device(
        id = "TEST-1",
        model = "Test device",
        soc = null,
        ramMb = null,
        androidApi = null,
        batteryCapacityMah = capacity,
        batteryLevelPct = 50.0,
        screenOnMinutes = screenMinutes,
        energyConsumed = energy,
        energyUnit = unit,
        nominalVoltage = voltage,
        jankFramesPct = jank,
        crashCount7d = crashes,
        lastSeen = null
    )

    @Test fun `mWh é convertido para mAh pela tensão nominal`() {
        assertEquals(100.0, device(energy = 400.0, unit = "mWh", voltage = 4.0).consumptionMah!!, 0.001)
    }

    @Test fun `mAh permanece sem conversão`() {
        assertEquals(100.5, device(energy = 100.5, unit = "mAh", voltage = 4.0).consumptionMah!!, 0.001)
    }

    @Test fun `dreno relativo de 12 por cento por hora é crítico`() {
        assertEquals(Status.CRITICAL, assess(device(energy = 120.0)).status)
    }

    @Test fun `dreno relativo de 7 por cento por hora é atenção`() {
        assertEquals(Status.ATTENTION, assess(device(energy = 70.0)).status)
    }

    @Test fun `três falhas recentes são críticas`() {
        assertEquals(Status.CRITICAL, assess(device(crashes = 3)).status)
    }

    @Test fun `uma falha recente exige atenção`() {
        assertEquals(Status.ATTENTION, assess(device(crashes = 1)).status)
    }

    @Test fun `15 por cento de quadros perdidos é crítico`() {
        assertEquals(Status.CRITICAL, assess(device(jank = 15.0)).status)
    }

    @Test fun `8 por cento de quadros perdidos exige atenção`() {
        assertEquals(Status.ATTENTION, assess(device(jank = 8.0)).status)
    }

    @Test fun `valores abaixo dos limites são saudáveis`() {
        assertEquals(Status.OK, assess(device(energy = 69.0, jank = 7.9, crashes = 0)).status)
    }

    @Test fun `status crítico prevalece sobre atenção`() {
        assertEquals(Status.CRITICAL, assess(device(jank = 15.0, crashes = 1)).status)
    }

    @Test fun `capacidade ausente ou zero resulta em sem dados`() {
        assertEquals(Status.UNKNOWN, assess(device(capacity = 0.0)).status)
    }

    @Test fun `tempo de tela zero resulta em sem dados`() {
        assertEquals(Status.UNKNOWN, assess(device(screenMinutes = 0.0)).status)
    }

    @Test fun `mWh sem tensão nominal resulta em sem dados`() {
        val result = assess(device(energy = 400.0, unit = "mWh", voltage = null))
        assertEquals(Status.UNKNOWN, result.status)
        assertTrue(result.reasons.any { it.contains("tensão nominal") })
    }
}
