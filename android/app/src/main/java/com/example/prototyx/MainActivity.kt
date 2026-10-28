package com.example.prototyx

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.security.BiometricAuthManager
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.EditorialHeading
import com.example.prototyx.ui.components.MetadataLabel
import com.example.prototyx.ui.components.PrimaryButton

import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner

class MainActivity : AppCompatActivity() {

    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PrototyxApp
        val repository = app.repository
        val authManager = app.authManager
        biometricAuthManager = BiometricAuthManager(this)

        setContent {
            PrototyxTheme {
                val navigationDispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides navigationDispatcherOwner) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val hasToken = authManager.getToken() != null
                        var isBiometricUnlocked by remember { mutableStateOf(true) }
                        var authErrorMessage by remember { mutableStateOf<String?>(null) }

                        // Trigger biometric on startup if user is logged in
                        LaunchedEffect(hasToken) {
                            if (hasToken && !isBiometricUnlocked) {
                                biometricAuthManager.promptBiometricAuth(
                                    activity = this@MainActivity,
                                    title = "Prototyx Secure Vault",
                                    subtitle = "Scan fingerprint or use device lock to open terminal",
                                    onSuccess = {
                                        isBiometricUnlocked = true
                                        authErrorMessage = null
                                    },
                                    onError = { error ->
                                        authErrorMessage = error
                                    }
                                )
                            }
                        }

                        if (hasToken && !isBiometricUnlocked) {
                            // Biometric Gate Screen
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    EditorialHeading(text = "Prototyx Vault Locked")
                                    MetadataLabel(text = "HARDWARE BIOMETRIC SECURITY GATE")

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "This terminal is protected by biometric encryption.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    authErrorMessage?.let { err ->
                                        Text(
                                            text = err,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    PrimaryButton(
                                        text = "Unlock with Biometrics",
                                        onClick = {
                                            authErrorMessage = null
                                            biometricAuthManager.promptBiometricAuth(
                                                activity = this@MainActivity,
                                                title = "Prototyx Secure Vault",
                                                subtitle = "Scan fingerprint to access terminal",
                                                onSuccess = {
                                                    isBiometricUnlocked = true
                                                    authErrorMessage = null
                                                },
                                                onError = { error ->
                                                    authErrorMessage = error
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )

                                    TextButton(onClick = {
                                        authManager.clearToken()
                                        isBiometricUnlocked = true
                                    }) {
                                        Text("Switch Account / Logout", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else {
                            MainNavigation(repository = repository, authManager = authManager)
                        }
                    }
                }
            }
        }
    }
}
