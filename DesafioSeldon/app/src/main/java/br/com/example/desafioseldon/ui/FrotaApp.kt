package br.com.example.desafioseldon.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.example.desafioseldon.domain.*
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun FrotaApp(
    devices: List<Device>,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    var selected by remember { mutableStateOf<Device?>(null) }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf<Status?>(null) }
    val listState = rememberLazyListState()
    val statusFilterScroll = rememberScrollState()
    val assessor = remember { AssessDevice() }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (selected == null) {
                DeviceList(
                    devices = devices,
                    assessor = assessor,
                    favoriteIds = favoriteIds,
                    showFavoritesOnly = showFavoritesOnly,
                    onFavoritesFilterChange = { showFavoritesOnly = it },
                    selectedStatus = selectedStatus,
                    onStatusFilterChange = { selectedStatus = it },
                    listState = listState,
                    statusFilterScroll = statusFilterScroll,
                    onSelect = { selected = it },
                    onToggleFavorite = onToggleFavorite
                )
            } else {
                val device = selected!!
                DeviceDetail(
                    device = device,
                    assessor = assessor,
                    isFavorite = device.id in favoriteIds,
                    onToggleFavorite = { onToggleFavorite(device.id) },
                    onBack = { selected = null }
                )
            }
        }
    }
}

@Composable private fun DeviceList(
    devices: List<Device>,
    assessor: AssessDevice,
    favoriteIds: Set<String>,
    showFavoritesOnly: Boolean,
    onFavoritesFilterChange: (Boolean) -> Unit,
    selectedStatus: Status?,
    onStatusFilterChange: (Status?) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    statusFilterScroll: androidx.compose.foundation.ScrollState,
    onSelect: (Device) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val assessments = remember(devices) { devices.associateWith { assessor(it) } }
    val visibleDevices = devices.filter { device ->
        (!showFavoritesOnly || device.id in favoriteIds) &&
            (selectedStatus == null || assessments[device]?.status == selectedStatus)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Seldon Fleet", style = MaterialTheme.typography.headlineMedium)
        Text("${visibleDevices.size} de ${devices.size} dispositivos", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(statusFilterScroll),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = selectedStatus == null, onClick = { onStatusFilterChange(null) }, label = { Text("Todos") })
            listOf(Status.OK, Status.ATTENTION, Status.CRITICAL, Status.UNKNOWN).forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusFilterChange(if (selectedStatus == status) null else status) },
                    label = { Text(statusFilterLabel(status)) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showFavoritesOnly, onClick = { onFavoritesFilterChange(false) }, label = { Text("Todos") })
            FilterChip(
                selected = showFavoritesOnly,
                onClick = { onFavoritesFilterChange(true) },
                label = { Text("★ Favoritos (${favoriteIds.size})") }
            )
        }
        Spacer(Modifier.height(8.dp))

        if (visibleDevices.isEmpty()) {
            Text(
                if (showFavoritesOnly && favoriteIds.isEmpty()) {
                    "Nenhum favorito ainda. Toque na estrela de um dispositivo para favoritar."
                } else {
                    "Nenhum dispositivo corresponde aos filtros selecionados."
                }
            )
        } else {
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleDevices, key = { it.id }) { device ->
                    val assessment = assessments.getValue(device)
                    Card(Modifier.fillMaxWidth().clickable { onSelect(device) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(device.id, style = MaterialTheme.typography.titleMedium)
                                Text(device.model)
                                Text("Bateria: ${device.batteryLevelPct?.let { "${it.roundToInt()}%" } ?: "—"}")
                            }
                            StatusChip(assessment.status)
                            IconButton(onClick = { onToggleFavorite(device.id) }) {
                                Text(
                                    if (device.id in favoriteIds) "★" else "☆",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun DeviceDetail(
    device: Device,
    assessor: AssessDevice,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit
) {
    val assessment = assessor(device)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Voltar à frota") }
        Text(device.id, style = MaterialTheme.typography.headlineMedium)
        Text(device.model, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onToggleFavorite) {
            Text(if (isFavorite) "★ Remover dos favoritos" else "☆ Favoritar dispositivo")
        }
        Spacer(Modifier.height(10.dp))
        StatusChip(assessment.status)
        Spacer(Modifier.height(16.dp))
        Text("Justificativa", style = MaterialTheme.typography.titleMedium)
        assessment.reasons.forEach { Text("• $it", Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        Text("Atributos", style = MaterialTheme.typography.titleMedium)
        Attribute("Identificador", device.id)
        Attribute("Modelo", device.model)
        Attribute("Tempo de tela", device.screenOnMinutes?.let { "${it.roundToInt()} min" })
        Attribute("SoC", device.soc)
        Attribute("Memória", device.ramMb?.let { "${it / 1024.0} GB (${it} MB)" })
        Attribute("Nível de API", device.androidApi?.toString())
        Attribute("Capacidade da bateria", device.batteryCapacityMah?.let { "${it.roundToInt()} mAh" })
        Attribute("Nível de bateria", device.batteryLevelPct?.let { "${it.roundToInt()}%" })
        Attribute("Consumo de bateria normalizado", device.consumptionMah?.let { "${it.roundToInt()} mAh" })
        Attribute("Dreno absoluto", assessment.drainMahPerHour?.let { "${num(it)} mAh/h" })
        Attribute("Dreno relativo", assessment.relativeDrainPctPerHour?.let { "${num(it)}%/h" })
        Attribute("Taxas de quadros perdidos", device.jankFramesPct?.let { "${num(it)}%" })
        Attribute("Falhas recentes (7 dias)", device.crashCount7d?.toString())
        Attribute("Último contato", device.lastSeen?.let(::formatDate))
    }
}

@Composable private fun Attribute(name: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value ?: "Sem dados", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}

@Composable private fun StatusChip(status: Status) {
    val (label, color) = when (status) {
        Status.OK -> "SAUDÁVEL" to Color(0xFF2E7D32)
        Status.ATTENTION -> "ATENÇÃO" to Color(0xFFEF6C00)
        Status.CRITICAL -> "CRÍTICO" to Color(0xFFC62828)
        Status.UNKNOWN -> "SEM DADOS" to Color(0xFF616161)
    }
    SuggestionChip(onClick = {}, label = { Text(label, color = color) })
}

private fun statusFilterLabel(status: Status): String = when (status) {
    Status.OK -> "SAUDÁVEL"
    Status.ATTENTION -> "ATENÇÃO"
    Status.CRITICAL -> "CRÍTICO"
    Status.UNKNOWN -> "SEM DADOS"
}

private fun num(value: Double): String = String.format(java.util.Locale("pt", "BR"), "%.1f", value)
private fun formatDate(value: String): String = runCatching {
    val instant = Instant.parse(value)
    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'")
        .withZone(java.time.ZoneOffset.UTC).format(instant)
}.getOrDefault(value)


