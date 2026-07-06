package ca.uwaterloo.ece452.discoveruwaterloo.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String, onError: (String) -> Unit) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToVerify: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var showVerifyPrompt by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DiscoverUW", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false; showVerifyPrompt = false },
                label = { Text("UWaterloo Email") },
                isError = emailError,
                supportingText = { if (emailError) Text("Must be a @uwaterloo.ca email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!email.endsWith("@uwaterloo.ca")) { emailError = true; return@Button }
                    showVerifyPrompt = false
                    onLogin(email, password) { error ->
                        if (error.contains("verify", ignoreCase = true)) {
                            showVerifyPrompt = true
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(error) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log In") }

            if (showVerifyPrompt) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Your email isn't verified yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = { onNavigateToVerify(email) }) {
                    Text("Verify your email")
                }
            } else {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register") }
            }
        }
    }
}
