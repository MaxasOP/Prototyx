package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.LoginRequest
import com.example.prototyx.data.model.RegisterRequest
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecovery by remember { mutableStateOf(false) }
    var useCode by remember { mutableStateOf(false) }
    var recoveryCode by remember { mutableStateOf("") }
    var recoveredPassword by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var recoveryMessage by remember { mutableStateOf<String?>(null) }
    var recoveryBusy by remember { mutableStateOf(false) }
    if(showRecovery) AlertDialog(onDismissRequest={if(!recoveryBusy) showRecovery=false},
        title={Text(if(useCode) "Reset password" else "Recover your account")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(if(useCode) "Paste the recovery code from your email. Codes expire after 30 minutes and can be used once." else "Enter your account email to receive a secure recovery link and code.")
            if(!useCode) OutlinedTextField(email,{email=it},label={Text("Account email")},singleLine=true)
            else {
                OutlinedTextField(recoveryCode,{recoveryCode=it},label={Text("Recovery code")},singleLine=true)
                OutlinedTextField(recoveredPassword,{recoveredPassword=it},label={Text("New password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
                OutlinedTextField(repeatedPassword,{repeatedPassword=it},label={Text("Confirm new password")},visualTransformation=PasswordVisualTransformation(),singleLine=true)
            }
            recoveryMessage?.let{Text(it,style=MaterialTheme.typography.bodySmall)}
        }},
        confirmButton={TextButton(enabled=!recoveryBusy,onClick={coroutineScope.launch {
            recoveryBusy=true
            try {
                val api=com.example.prototyx.data.network.RetrofitInstance.apiService
                if(useCode) {
                    require(recoveredPassword==repeatedPassword && recoveredPassword.length>=8 && recoveryCode.isNotBlank()){ "Enter the code and matching passwords of at least 8 characters." }
                    recoveryMessage=api.resetPassword(mapOf("code" to recoveryCode.trim(),"new_password" to recoveredPassword))["message"]
                    recoveryCode="";recoveredPassword="";repeatedPassword=""
                } else {
                    require(email.contains("@")){"Enter your account email."}
                    recoveryMessage=api.forgotPassword(mapOf("email" to email.trim().lowercase()))["message"]
                }
            } catch(e:kotlinx.coroutines.CancellationException){throw e}
            catch(e:Exception){recoveryMessage=e.message}
            finally{recoveryBusy=false}
        }}){Text(if(recoveryBusy) "Working…" else if(useCode) "Reset password" else "Send recovery email")}},
        dismissButton={Row {
            TextButton(enabled=!recoveryBusy,onClick={useCode=!useCode;recoveryMessage=null}){Text(if(useCode) "Request email" else "Use code")}
            TextButton(enabled=!recoveryBusy,onClick={showRecovery=false;recoveryCode="";recoveredPassword="";repeatedPassword=""}){Text("Close")}
        }})

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Text("P/  PROTOTYX", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            MetadataLabel(text = "INVESTMENT RESEARCH / PERSONAL LEDGER")
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(if (isLoginMode) "A considered view\nof your capital." else "Start your\ninvestment ledger.", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Portfolio allocation, market research and risk analysis in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (!isLoginMode) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("FULL NAME", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("EMAIL ADDRESS", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("PASSWORD", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PrimaryButton(
                text = if (isLoginMode) "Sign in" else "Create account",
                onClick = {
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
                            errorMessage = e.message ?: "Action failed"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            if(isLoginMode) TextButton(onClick={showRecovery=true;recoveryMessage=null}) {Text("Forgot password?")}
            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (isLoginMode) "New to Prototyx? Create account" else "Already have an account? Sign in",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            MetadataLabel(
                text = "PROTOTYX / INVESTMENT RESEARCH",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
