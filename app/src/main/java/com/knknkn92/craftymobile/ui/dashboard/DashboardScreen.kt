package com.knknkn92.craftymobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knknkn92.craftymobile.data.api.ServerStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    baseUrl: String,
    token: String,
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(baseUrl, token)),
) {
    val state by vm.uiState.collectAsState()

    // Error dialog
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissError() },
            title   = { Text("Error") },
            text    = { Text(state.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { vm.dismissError() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Servers",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { vm.loadServers() }) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint               = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color    = MaterialTheme.colorScheme.primary,
                    )
                }

                state.servers.isEmpty() -> {
                    Text(
                        text     = "No servers found.",
                        modifier = Modifier.align(Alignment.Center),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.servers) { serverWithStats ->
                            ServerCard(
                                name  = serverWithStats.info.serverName,
                                type  = serverWithStats.info.type ?: "minecraft-java",
                                stats = serverWithStats.stats,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    name: String,
    type: String,
    stats: ServerStats?,
) {
    val isOnline = stats?.running == true
    val statusColor = when {
        stats?.crashed == true  -> MaterialTheme.colorScheme.error
        isOnline                -> Color(0xFF4CAF50)
        else                    -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusText = when {
        stats?.crashed == true  -> "Crashed"
        stats?.updating == true -> "Updating"
        stats?.waitingStart == true -> "Starting"
        isOnline                -> "Online"
        stats != null           -> "Offline"
        else                    -> "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ---- Header row ----
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier             = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = name,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text  = type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text  = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                    )
                }
            }

            if (stats != null && isOnline) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ---- Stats row ----
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    StatItem(
                        icon  = Icons.Outlined.People,
                        label = "Players",
                        value = "${stats.online}/${stats.max}",
                    )
                    StatItem(
                        icon  = Icons.Outlined.Speed,
                        label = "CPU",
                        value = "${"%.1f".format(stats.cpu)}%",
                    )
                    StatItem(
                        icon  = Icons.Outlined.Memory,
                        label = "RAM",
                        value = "${"%.0f".format(stats.memPercent)}%",
                    )
                }

                // Version & world
                if (!stats.version.isNullOrBlank() || !stats.worldName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!stats.version.isNullOrBlank()) {
                            Text(
                                text  = "v${stats.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!stats.worldName.isNullOrBlank()) {
                            Text(
                                text  = stats.worldName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color    = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}
