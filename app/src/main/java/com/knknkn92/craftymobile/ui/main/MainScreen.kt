package com.knknkn92.craftymobile.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.knknkn92.craftymobile.ui.dashboard.DashboardScreen

@Composable
fun MainScreen(
    baseUrl: String,
    token: String,
    onLogout: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = "Servers",
                        )
                    },
                    label = { Text("Servers") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.AccountCircle
                            else Icons.Outlined.AccountCircle,
                            contentDescription = "Account",
                        )
                    },
                    label = { Text("Account") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = MaterialTheme.colorScheme.primary,
                        selectedTextColor   = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    baseUrl = baseUrl,
                    token   = token,
                )
                1 -> AccountScreen(
                    baseUrl  = baseUrl,
                    onLogout = onLogout,
                )
            }
        }
    }
}

@Composable
private fun AccountScreen(
    baseUrl: String,
    onLogout: () -> Unit,
) {
    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement   = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text       = "Account",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onBackground,
        )

        Card(
            shape     = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier  = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text  = "Server",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = baseUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onLogout,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor   = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text("Logout", style = MaterialTheme.typography.labelLarge)
        }
    }
}
