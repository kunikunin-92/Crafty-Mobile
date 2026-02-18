package com.knknkn92.craftymobile.ui.serverdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knknkn92.craftymobile.data.api.ServerInfo

private val ConsoleBackground = Color(0xFF1E1E1E)
private val ConsoleForeground  = Color(0xFFD4D4D4)
private val OnlineGreen        = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailScreen(
    baseUrl: String,
    token: String,
    serverInfo: ServerInfo,
    onClose: () -> Unit,
    vm: ServerDetailViewModel = viewModel(
        factory = ServerDetailViewModel.Factory(baseUrl, token, serverInfo),
        key     = serverInfo.serverId,
    ),
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // BAN List dialog state
    var banListOpen by remember { mutableStateOf(false) }

    // Selected tab (0=Terminal 1=Log 2=Players 3=Update)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Players tab を選択したら自動で loadPlayers
    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) vm.loadPlayers()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text       = serverInfo.serverName,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                    )
                },
                actions = {
                    if (state.actionInProgress) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        // Stop
                        IconButton(onClick = { vm.serverAction("stop_server") }) {
                            Icon(
                                Icons.Default.StopCircle,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Restart
                        IconButton(onClick = { vm.serverAction("restart_server") }) {
                            Icon(
                                Icons.Default.RestartAlt,
                                contentDescription = "Restart",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Kill
                        IconButton(onClick = { vm.serverAction("kill_server") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kill",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            // ── Tab Bar ─────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.background,
                contentColor     = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Outlined.Terminal, contentDescription = "Terminal") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Outlined.Article, contentDescription = "Log") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    icon     = { Icon(Icons.Outlined.People, contentDescription = "Players") },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick  = { selectedTab = 3 },
                    icon     = { Icon(Icons.Outlined.SystemUpdate, contentDescription = "Update") },
                )
            }

            // ── Tab Content ──────────────────────────────
            when (selectedTab) {
                0 -> TerminalTab(state = state, onSend = { vm.sendCommand(it) }, onRefresh = { vm.loadLogs() })
                1 -> LogTab(state = state, onFilterChange = { vm.setLogFilter(it) }, onRefresh = { vm.loadLogs() })
                2 -> PlayersTab(
                    state        = state,
                    onKick       = { vm.kickPlayer(it) },
                    onBan        = { vm.banPlayer(it) },
                    onBanListOpen = { banListOpen = true },
                    onRefresh    = { vm.loadPlayers() },
                )
                3 -> UpdateTab()
            }
        }
    }

    // ── Ban List Dialog ──────────────────────────────
    if (banListOpen) {
        BanListDialog(
            onDismiss = { banListOpen = false },
            onUnban   = { vm.unbanPlayer(it) },
        )
    }
}

// ─────────────────────────────────────────────────────
// Terminal Tab
// ─────────────────────────────────────────────────────
@Composable
private fun TerminalTab(
    state: ServerDetailUiState,
    onSend: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var command by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 新しいログが来たら一番下へスクロール
    LaunchedEffect(state.terminalLines.size) {
        if (state.terminalLines.isNotEmpty()) {
            listState.animateScrollToItem(state.terminalLines.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Reload button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onRefresh, enabled = !state.isTerminalLoading) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Console area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ConsoleBackground, RoundedCornerShape(8.dp))
                .padding(8.dp),
        ) {
            if (state.isTerminalLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color    = MaterialTheme.colorScheme.primary,
                )
            } else {
                LazyColumn(state = listState) {
                    items(state.terminalLines) { line ->
                        Text(
                            text       = line,
                            color      = ConsoleForeground,
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Command input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value         = command,
                onValueChange = { command = it },
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("Enter command...", fontSize = 14.sp) },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    onSend(command)
                    command = ""
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
            FilledIconButton(
                onClick = {
                    onSend(command)
                    command = ""
                },
                enabled = command.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Log Tab
// ─────────────────────────────────────────────────────
@Composable
private fun LogTab(
    state: ServerDetailUiState,
    onFilterChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val filtered = remember(state.logLines, state.logFilter) {
        if (state.logFilter == "all") state.logLines
        else state.logLines.filter { it.level.equals(state.logFilter, ignoreCase = true) }
    }

    val warnColor  = Color(0xFFFF9800)
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Filter row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Filter chip group
            val filters = listOf("all", "info", "warn", "error")
            filters.forEach { f ->
                FilterChip(
                    selected = state.logFilter == f,
                    onClick  = { onFilterChange(f) },
                    label    = { Text(f.uppercase(), fontSize = 11.sp) },
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onRefresh, enabled = !state.isLogLoading) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Log list
        if (state.isLogLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered) { line ->
                    val levelColor = when (line.level) {
                        "ERROR", "FATAL" -> errorColor
                        "WARN"           -> warnColor
                        else             -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        if (line.time.isNotEmpty()) {
                            Text(
                                text     = line.time,
                                fontSize = 11.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(52.dp),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = levelColor.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text     = line.level,
                                fontSize = 10.sp,
                                color    = levelColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text     = line.message,
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Players Tab
// ─────────────────────────────────────────────────────
@Composable
private fun PlayersTab(
    state: ServerDetailUiState,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    onBanListOpen: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Banned Players button
        OutlinedButton(
            onClick  = onBanListOpen,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                width = 1.dp,
            ),
        ) {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint     = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.width(6.dp))
            Text("Banned Players", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))

        // Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onRefresh, enabled = !state.isPlayersLoading) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isPlayersLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (state.onlinePlayers.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No players online.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.onlinePlayers) { player ->
                    Card(
                        shape  = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text       = player.name,
                                    fontWeight = FontWeight.Medium,
                                    style      = MaterialTheme.typography.bodyLarge,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    modifier   = Modifier.weight(1f),
                                )
                                // Online chip
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = OnlineGreen.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        text     = "Online",
                                        color    = OnlineGreen,
                                        style    = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Kick
                                OutlinedButton(
                                    onClick  = { onKick(player.name) },
                                    modifier = Modifier.weight(1f),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("Kick", color = MaterialTheme.colorScheme.error)
                                }
                                // Ban
                                Button(
                                    onClick  = { onBan(player.name) },
                                    modifier = Modifier.weight(1f),
                                    colors   = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                    ),
                                ) {
                                    Text("Ban")
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Update Tab  (static info card — Crafty API では
// バージョン情報が限られるため stats から取得)
// ─────────────────────────────────────────────────────
@Composable
private fun UpdateTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // PaperMC Card
        UpdateCard(
            title          = "PaperMC",
            currentVersion = "—",
            latestVersion  = "—",
            upToDate       = false,
            onUpdate       = { /* TODO: trigger update via Crafty */ },
        )

        // GeyserMC Card
        UpdateCard(
            title          = "GeyserMC",
            currentVersion = "—",
            latestVersion  = "—",
            upToDate       = true,
            onUpdate       = {},
        )

        Text(
            text  = "Version information is fetched from the server. Use the Terminal tab to manually trigger updates.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateCard(
    title: String,
    currentVersion: String,
    latestVersion: String,
    upToDate: Boolean,
    onUpdate: () -> Unit,
) {
    Card(
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Current Version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = currentVersion,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Latest Version",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = latestVersion,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color      = if (upToDate) OnlineGreen
                                     else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onUpdate,
                enabled  = !upToDate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (upToDate) "Up to date" else "Download and Restart")
            }
        }
    }
}

// ─────────────────────────────────────────────────────
// Ban List Dialog
// ─────────────────────────────────────────────────────
@Composable
private fun BanListDialog(
    onDismiss: () -> Unit,
    onUnban: (String) -> Unit,
) {
    // Crafty API doesn't expose a ban list endpoint,
    // so we show a message directing the user to use console
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Banned Players") },
        text  = {
            Column {
                Text(
                    "The ban list is managed by the server. Use the Terminal tab to run 'banlist' and 'pardon <player>'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
