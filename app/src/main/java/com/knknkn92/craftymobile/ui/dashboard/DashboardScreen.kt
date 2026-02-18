package com.knknkn92.craftymobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knknkn92.craftymobile.ui.serverdetail.ServerDetailScreen
import com.knknkn92.craftymobile.ui.serverdetail.ServerDetailViewModel

private val OnlineGreen  = Color(0xFF4CAF50)
private val OfflineRed   = Color(0xFFF44336)
private val WarnAmber    = Color(0xFFFF9800)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    baseUrl: String,
    token: String,
    vm: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory(baseUrl, token)),
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 詳細画面に遷移するサーバー
    var detailServer by remember { mutableStateOf<ServerWithStats?>(null) }

    // Snackbar表示
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissSnackbar()
        }
    }

    // Error dialog
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissError() },
            title   = { Text("Error") },
            text    = { Text(state.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { vm.dismissError() }) { Text("OK") }
            }
        )
    }

    // サーバー詳細画面
    if (detailServer != null) {
        ServerDetailScreen(
            baseUrl    = baseUrl,
            token      = token,
            serverInfo = detailServer!!.info,
            onClose    = {
                detailServer = null
                vm.loadServers()   // 戻ったら更新
            },
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Dashboard", fontWeight = FontWeight.Medium)
                },
                actions = {
                    IconButton(onClick = { vm.loadServers() }, enabled = !state.isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (state.isLoading && state.servers.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- VPS Status Card ----
            item {
                VpsStatusCard(state = state)
            }

            // ---- Server List title ----
            item {
                Text(
                    text       = "Server List",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
            }

            // ---- Server rows ----
            if (state.servers.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        "No servers found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            } else {
                item {
                    Card(
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        state.servers.forEachIndexed { index, serverWithStats ->
                            ServerListRow(
                                serverWithStats  = serverWithStats,
                                actionInProgress = state.actionInProgress == serverWithStats.info.serverId,
                                onRowClick       = { detailServer = serverWithStats },
                                onAction         = { action ->
                                    vm.serverAction(serverWithStats.info.serverId, action)
                                },
                            )
                            if (index < state.servers.lastIndex) {
                                HorizontalDivider(
                                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    thickness = 1.dp,
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─────────────────────────────────────────────
// VPS Status Card
// ─────────────────────────────────────────────
@Composable
private fun VpsStatusCard(state: DashboardUiState) {
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Row 1: Servers count + Total Players
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatColumn(
                    icon  = Icons.Outlined.Dns,
                    label = "Servers",
                    value = "${state.servers.size}",
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    icon  = Icons.Outlined.Person,
                    label = "Total Players",
                    value = "${state.totalPlayers}/${state.totalMaxPlayers}",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Row 2: CPU + Memory
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ProgressStatColumn(
                    icon     = Icons.Outlined.Speed,
                    label    = "CPU",
                    percent  = state.avgCpu,
                    modifier = Modifier.weight(1f),
                )
                ProgressStatColumn(
                    icon     = Icons.Outlined.Memory,
                    label    = "Memory",
                    percent  = state.avgMem,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ProgressStatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    percent: Float,
    modifier: Modifier = Modifier,
) {
    val isHigh = percent > 80f
    val barColor = if (isHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("${"%.1f".format(percent)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress       = { (percent / 100f).coerceIn(0f, 1f) },
            modifier       = Modifier.fillMaxWidth().height(6.dp),
            color          = barColor,
            trackColor     = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            strokeCap      = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────
// Server list row
// ─────────────────────────────────────────────
@Composable
private fun ServerListRow(
    serverWithStats: ServerWithStats,
    actionInProgress: Boolean,
    onRowClick: () -> Unit,
    onAction: (String) -> Unit,
) {
    val info  = serverWithStats.info
    val stats = serverWithStats.stats
    val isOnline = stats?.running == true
    val isCrashed = stats?.crashed == true
    val dotColor = when {
        isCrashed -> OfflineRed
        isOnline  -> OnlineGreen
        else      -> OfflineRed
    }
    val statusLabel = when {
        isCrashed            -> "Crashed"
        stats?.updating == true -> "Updating"
        stats?.waitingStart == true -> "Starting"
        isOnline             -> "Online"
        stats != null        -> "Offline"
        else                 -> "Unknown"
    }
    val statusBgColor = if (isOnline && !isCrashed)
        OnlineGreen.copy(alpha = 0.15f) else OfflineRed.copy(alpha = 0.15f)
    val statusTextColor = if (isOnline && !isCrashed) OnlineGreen else OfflineRed

    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(12.dp))

        // Name + badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = info.serverName,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Status chip
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusBgColor,
                ) {
                    Text(
                        text     = statusLabel,
                        color    = statusTextColor,
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (stats != null) {
                    Text(
                        "${stats.online}/${stats.max} Players",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // More menu
        if (actionInProgress) {
            CircularProgressIndicator(
                modifier    = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color       = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded        = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    // Stop (オンラインのみ)
                    DropdownMenuItem(
                        text = { Text("Stop") },
                        leadingIcon = {
                            Icon(Icons.Default.StopCircle, null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuExpanded = false; onAction("stop_server") },
                        enabled = isOnline,
                    )
                    // Restart (オンラインのみ)
                    DropdownMenuItem(
                        text = { Text("Restart") },
                        leadingIcon = {
                            Icon(Icons.Default.RestartAlt, null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        onClick = { menuExpanded = false; onAction("restart_server") },
                        enabled = isOnline,
                    )
                    // Kill (常に表示)
                    DropdownMenuItem(
                        text = { Text("Kill") },
                        leadingIcon = {
                            Icon(Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuExpanded = false; onAction("kill_server") },
                    )
                    // Start (オフラインのみ)
                    if (!isOnline) {
                        DropdownMenuItem(
                            text = { Text("Start") },
                            leadingIcon = {
                                Icon(Icons.Outlined.PlayArrow, null,
                                    tint = OnlineGreen)
                            },
                            onClick = { menuExpanded = false; onAction("start_server") },
                        )
                    }
                }
            }
        }
    }
}
