package com.knknkn92.craftymobile.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knknkn92.craftymobile.ui.theme.CraftyMobileTheme

@Composable
fun LoginScreen(
    onLoginSuccess: (token: String, userId: String) -> Unit = { _, _ -> },
    vm: LoginViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    // ログイン成功ダイアログ
    if (state.loginSuccess) {
        AlertDialog(
            onDismissRequest = { vm.dismissSuccess() },
            title   = { Text("ログイン成功") },
            text    = { Text("Crafty Controller に接続しました。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissSuccess()
                    onLoginSuccess(state.token ?: "", state.userId ?: "")
                }) {
                    Text("OK")
                }
            }
        )
    }

    // エラーダイアログ
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissError() },
            title   = { Text("エラー") },
            text    = { Text(state.errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { vm.dismissError() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ---- アイコン ----
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = "ログイン",
                tint               = MaterialTheme.colorScheme.background,
                modifier           = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---- タイトル ----
        Text(
            text  = "ログイン",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text  = "アカウント情報を入力してください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- サーバーアドレス ----
        OutlinedTextField(
            value         = state.serverAddress,
            onValueChange = { vm.onServerAddressChange(it) },
            label         = { Text("サーバーアドレス") },
            placeholder   = { Text("https://example.com:8443") },
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Outlined.Dns,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction    = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- ユーザー名 ----
        OutlinedTextField(
            value         = state.username,
            onValueChange = { vm.onUsernameChange(it) },
            label         = { Text("ユーザー名") },
            placeholder   = { Text("username") },
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction    = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- パスワード ----
        OutlinedTextField(
            value         = state.password,
            onValueChange = { vm.onPasswordChange(it) },
            label         = { Text("パスワード") },
            placeholder   = { Text("••••••••") },
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Default.Lock,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon  = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff
                                      else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "パスワードを隠す" else "パスワードを表示",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction    = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- MFAコード ----
        OutlinedTextField(
            value         = state.mfaCode,
            onValueChange = { vm.onMfaCodeChange(it) },
            label         = { Text("Authenticator Code (If MFA is Enabled)") },
            placeholder   = { Text("123456") },
            leadingIcon   = {
                Icon(
                    imageVector        = Icons.Outlined.Security,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction    = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ---- ログインボタン ----
        Button(
            onClick  = { vm.login() },
            enabled  = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.background,
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color    = MaterialTheme.colorScheme.background,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text     = "ログイン",
                    fontSize = 16.sp,
                    style    = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    CraftyMobileTheme {
        LoginScreen()
    }
}
