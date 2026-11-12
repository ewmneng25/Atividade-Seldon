package br.com.example.desafioseldon

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import br.com.example.desafioseldon.data.FavoritesRepository
import br.com.example.desafioseldon.data.FrotaRepository
import br.com.example.desafioseldon.ui.FrotaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val devices = FrotaRepository {
            assets.open("frota.json").bufferedReader().use { it.readText() }
        }.loadDevices()
        val favoritesRepository = FavoritesRepository(
            readFile = { openFileInput("favoritos.txt").bufferedReader().use { it.readText() } },
            writeFile = { content ->
                openFileOutput("favoritos.txt", MODE_PRIVATE).bufferedWriter().use { it.write(content) }
            }
        )
        val savedFavorites = favoritesRepository.load()

        setContent {
            val favoriteIds = remember { mutableStateOf(savedFavorites) }
            FrotaApp(
                devices = remember { devices },
                favoriteIds = favoriteIds.value,
                onToggleFavorite = { id ->
                    val updated = if (id in favoriteIds.value) favoriteIds.value - id else favoriteIds.value + id
                    favoriteIds.value = updated
                    favoritesRepository.save(updated)
                }
            )
        }
    }
}

