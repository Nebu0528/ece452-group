package ca.uwaterloo.ece452.discoveruwaterloo.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen(
    email: String,
    onVerify: (code: String, onError: (String) -> Unit) -> Unit,
    onResend: (onError: (String) -> Unit) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Check your email", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            Text(
                text = "We sent a 6-digit code to $email",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = { Text("Verification code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (code.length != 6) {
                        scope.launch { snackbarHostState.showSnackbar("Enter the 6-digit code") }
                        return@Button
                    }
                    onVerify(code) { error ->
                        scope.launch { snackbarHostState.showSnackbar(error) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Verify") }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                onResend { error ->
                    scope.launch { snackbarHostState.showSnackbar(error) }
                }
                scope.launch { snackbarHostState.showSnackbar("Code resent!") }
            }) {
                Text("Didn't get it? Resend code")
            }
        }
    }
}
