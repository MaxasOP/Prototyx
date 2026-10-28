package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.LoginRequest
import com.example.prototyx.data.model.RegisterRequest
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: DataRepository,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            EditorialHeading(text = "Prototyx")
            Spacer(modifier = Modifier.height(4.dp))
            MetadataLabel(text = "PRIVATE WEALTH OPERATING SYSTEM", color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = if (isLoginMode) "SIGN IN TO TERMINAL" else "CREATE PRIVATE ACCOUNT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            DoubleBezelCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isLoginMode) {
                        TerminalTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "FULL NAME",
                            placeholder = "Enter your full name",
                            leadingIcon = Icons.Outlined.Person,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    TerminalTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "EMAIL ADDRESS",
                        placeholder = "user@prototyx.wealth",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        MetadataLabel(text = "PASSWORD")
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            placeholder = {
                                Text(
                                    text = "••••••••••••",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                TerminalTextButton(
                                    text = if (passwordVisible) "HIDE" else "SHOW",
                                    onClick = { passwordVisible = !passwordVisible }
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        coroutineScope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            try {
                                                val response = if (isLoginMode) {
                                                    repository.login(LoginRequest(email, password))
                                                } else {
                                                    repository.register(RegisterRequest(email, password, name))
                                                }
                                                onLoginSuccess(response.accessToken)
                                            } catch (e: Exception) {
                                                errorMessage = e.message ?: "Authentication failed"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        StatusMessage(message = errorMessage!!, isError = true)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = if (isLoginMode) "LOGIN TO TERMINAL" else "REGISTER ACCOUNT",
                        onClick = {
                            coroutineScope.launch {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter valid email and password."
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                try {
                                    val response = if (isLoginMode) {
                                        repository.login(LoginRequest(email, password))
                                    } else {
                                        repository.register(RegisterRequest(email, password, name))
                                    }
                                    onLoginSuccess(response.accessToken)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Authentication action failed"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        isLoading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TerminalTextButton(
                text = if (isLoginMode) "New to Prototyx? Create account" else "Already registered? Sign in to terminal",
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(text = "AES-256 HARDWARE ENCRYPTION", type = StatusBadgeType.NEUTRAL)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PrototyxTheme {
        LoginScreen(
            repository = MockDataRepository(),
            onLoginSuccess = {}
        )
    }
}
