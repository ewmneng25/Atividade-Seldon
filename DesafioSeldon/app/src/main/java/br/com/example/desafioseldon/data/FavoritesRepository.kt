package br.com.example.desafioseldon.data

/** Guarda os IDs como texto simples; não depende de Android nem de JSON. */
class FavoritesRepository(
    private val readFile: () -> String,
    private val writeFile: (String) -> Unit
) {
    fun load(): Set<String> = runCatching {
        readFile().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }.getOrDefault(emptySet())

    fun save(ids: Set<String>) {
        writeFile(ids.sorted().joinToString("\n"))
    }
}

