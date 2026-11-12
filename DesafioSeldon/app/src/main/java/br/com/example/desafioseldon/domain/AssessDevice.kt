package br.com.example.desafioseldon.domain

class AssessDevice {
    operator fun invoke(device: Device): Assessment {
        val capacity = device.batteryCapacityMah
        val minutes = device.screenOnMinutes
        val consumption = device.consumptionMah
        if (capacity == null || capacity <= 0 || minutes == null || minutes <= 0 || consumption == null) {
            val reasons = buildList {
                if (capacity == null || capacity <= 0) add("capacidade de bateria ausente ou inválida")
                if (minutes == null || minutes <= 0) add("tempo de tela ausente")
                if (consumption == null) add("consumo ausente ou sem tensão nominal para converter mWh")
            }
            return Assessment(Status.UNKNOWN, null, null, reasons)
        }
        val drain = consumption / (minutes / 60.0)
        val relative = drain / capacity * 100.0
        val crash = device.crashCount7d ?: 0
        val jank = device.jankFramesPct ?: 0.0
        val critical = relative >= 12.0 || crash >= 3 || jank >= 15.0
        val attention = relative >= 7.0 || crash >= 1 || jank >= 8.0
        val status = when { critical -> Status.CRITICAL; attention -> Status.ATTENTION; else -> Status.OK }
        val reasons = buildList {
            when (status) {
                Status.CRITICAL -> {
                    if (relative >= 12.0) add("Crítico: dreno em ${fmt(relative)}%/h. Está acima do limiar de 12%/h.")
                    if (crash >= 3) add("Crítico: $crash falhas nos últimos 7 dias (limiar: 3).")
                    if (jank >= 15.0) add("Crítico: ${fmt(jank)}% de quadros perdidos (limiar: 15%).")
                }
                Status.ATTENTION -> {
                    if (relative >= 7.0) add("Atenção: dreno em ${fmt(relative)}%/h. Atingiu o limiar de 7%/h.")
                    if (crash >= 1) add("Atenção: $crash falha(s) nos últimos 7 dias.")
                    if (jank >= 8.0) add("Atenção: ${fmt(jank)}% de quadros perdidos (limiar: 8%).")
                }
                Status.OK -> add("Saudável: dreno, falhas e quadros perdidos estão abaixo dos limiares de atenção.")
                Status.UNKNOWN -> Unit
            }
        }
        return Assessment(status, drain, relative, reasons)
    }

    private fun fmt(value: Double) = String.format(java.util.Locale("pt", "BR"), "%.1f", value)
}

