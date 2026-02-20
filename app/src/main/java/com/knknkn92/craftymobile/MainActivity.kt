package com.knknkn92.craftymobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.knknkn92.craftymobile.ui.login.LoginScreen
import com.knknkn92.craftymobile.ui.main.MainScreen
import com.knknkn92.craftymobile.ui.theme.CraftyMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CraftyMobileTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var token   by remember { mutableStateOf<String?>(null) }
    var userId  by remember { mutableStateOf<String?>(null) }
    var baseUrl by remember { mutableStateOf<String?>(null) }

    if (token != null && baseUrl != null) {
        MainScreen(
            baseUrl  = baseUrl!!,
            token    = token!!,
            onLogout = {
                token   = null
                userId  = null
                baseUrl = null
            },
        )
    } else {
        LoginScreen(
            onLoginSuccess = { t, u, url ->
                token   = t
                userId  = u
                baseUrl = url
            }
        )
    }
}
