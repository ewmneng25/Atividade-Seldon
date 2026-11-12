package br.com.example.desafioseldon.data

import br.com.example.desafioseldon.domain.Device

class FrotaRepository(private val readAsset: () -> String) {
    fun loadDevices(): List<Device> {
        val root = SimpleJsonParser(readAsset()).parse() as? JsonValue.ObjectValue
            ?: error("O arquivo frota.json precisa conter um objeto na raiz")
        val rows = root.values["devices"] as? JsonValue.ArrayValue
            ?: error("A lista devices não foi encontrada no arquivo frota.json")
        return rows.values.map { value ->
            val row = value as? JsonValue.ObjectValue ?: error("Dispositivo inválido no JSON")
            fun string(key: String) = (row.values[key] as? JsonValue.StringValue)?.value
            fun number(key: String) = (row.values[key] as? JsonValue.NumberValue)?.value
            Device(
                id = string("id") ?: "",
                model = string("model") ?: "",
                soc = string("soc"),
                ramMb = number("ramMb")?.toInt(),
                androidApi = number("androidApi")?.toInt(),
                batteryCapacityMah = number("batteryCapacityMah"),
                batteryLevelPct = number("batteryLevelPct"),
                screenOnMinutes = number("screenOnMinutes"),
                energyConsumed = number("energyConsumed"),
                energyUnit = string("energyUnit"),
                nominalVoltage = number("nominalVoltage"),
                jankFramesPct = number("jankFramesPct"),
                crashCount7d = number("crashCount7d")?.toInt(),
                lastSeen = string("lastSeen")
            )
        }
    }
}

private sealed interface JsonValue {
    data class ObjectValue(val values: Map<String, JsonValue>) : JsonValue
    data class ArrayValue(val values: List<JsonValue>) : JsonValue
    data class StringValue(val value: String) : JsonValue
    data class NumberValue(val value: Double) : JsonValue
    data object OtherValue : JsonValue
}

/** Parser pequeno, sem dependência ou import de biblioteca JSON. */
private class SimpleJsonParser(private val source: String) {
    private var index = 0

    fun parse(): JsonValue {
        val value = readValue()
        skipWhitespace()
        require(index == source.length) { "Conteúdo extra após o JSON" }
        return value
    }

    private fun readValue(): JsonValue {
        skipWhitespace()
        require(index < source.length) { "Fim inesperado do JSON" }
        return when (source[index]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> JsonValue.StringValue(readString())
            't' -> { readLiteral("true"); JsonValue.OtherValue }
            'f' -> { readLiteral("false"); JsonValue.OtherValue }
            'n' -> { readLiteral("null"); JsonValue.OtherValue }
            '-', in '0'..'9' -> readNumber()
            else -> error("Valor JSON inválido na posição $index")
        }
    }

    private fun readObject(): JsonValue.ObjectValue {
        expect('{')
        skipWhitespace()
        val values = linkedMapOf<String, JsonValue>()
        if (take('}')) return JsonValue.ObjectValue(values)
        while (true) {
            skipWhitespace()
            require(index < source.length && source[index] == '"') { "Chave JSON inválida" }
            val key = readString()
            skipWhitespace(); expect(':')
            values[key] = readValue()
            skipWhitespace()
            if (take('}')) break
            expect(',')
        }
        return JsonValue.ObjectValue(values)
    }

    private fun readArray(): JsonValue.ArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue>()
        if (take(']')) return JsonValue.ArrayValue(values)
        while (true) {
            values += readValue()
            skipWhitespace()
            if (take(']')) break
            expect(',')
        }
        return JsonValue.ArrayValue(values)
    }

    private fun readString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < source.length) {
            val char = source[index++]
            when (char) {
                '"' -> return result.toString()
                '\\' -> {
                    require(index < source.length) { "Escape incompleto" }
                    when (val escaped = source[index++]) {
                        '"', '\\', '/' -> result.append(escaped)
                        'b' -> result.append('\b')
                        'f' -> result.append('\u000C')
                        'n' -> result.append('\n')
                        'r' -> result.append('\r')
                        't' -> result.append('\t')
                        'u' -> {
                            require(index + 4 <= source.length) { "Escape Unicode incompleto" }
                            result.append(source.substring(index, index + 4).toInt(16).toChar())
                            index += 4
                        }
                        else -> error("Escape inválido")
                    }
                }
                else -> result.append(char)
            }
        }
        error("Texto JSON sem aspas de fechamento")
    }

    private fun readNumber(): JsonValue.NumberValue {
        val start = index
        if (source[index] == '-') index++
        while (index < source.length && source[index].isDigit()) index++
        if (index < source.length && source[index] == '.') {
            index++
            while (index < source.length && source[index].isDigit()) index++
        }
        if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
            index++
            if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
            while (index < source.length && source[index].isDigit()) index++
        }
        return JsonValue.NumberValue(source.substring(start, index).toDouble())
    }

    private fun readLiteral(literal: String) {
        require(source.startsWith(literal, index)) { "Valor JSON inválido na posição $index" }
        index += literal.length
    }

    private fun skipWhitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
    private fun expect(char: Char) { skipWhitespace(); require(take(char)) { "Esperado '$char' na posição $index" } }
    private fun take(char: Char): Boolean = if (index < source.length && source[index] == char) { index++; true } else false
}

