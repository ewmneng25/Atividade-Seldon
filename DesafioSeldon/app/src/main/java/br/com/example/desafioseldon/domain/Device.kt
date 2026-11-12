package br.com.example.desafioseldon.domain

data class Device(
    val id: String, val model: String, val soc: String?, val ramMb: Int?, val androidApi: Int?,
    val batteryCapacityMah: Double?, val batteryLevelPct: Double?, val screenOnMinutes: Double?,
    val energyConsumed: Double?, val energyUnit: String?, val nominalVoltage: Double?,
    val jankFramesPct: Double?, val crashCount7d: Int?, val lastSeen: String?
) {
    val consumptionMah: Double? get() = when (energyUnit?.lowercase()) {
        "mah" -> energyConsumed
        "mwh" -> if (nominalVoltage != null && nominalVoltage > 0) energyConsumed?.div(nominalVoltage) else null
        else -> null
    }
}

enum class Status { OK, ATTENTION, CRITICAL, UNKNOWN }
data class Assessment(val status: Status, val drainMahPerHour: Double?, val relativeDrainPctPerHour: Double?, val reasons: List<String>)

