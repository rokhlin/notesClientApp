package com.notes.client.crypto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.notes.client.biometrics.BiometricAuthManager
import com.notes.client.biometrics.BiometricAuthResult
import com.notes.client.biometrics.BiometricStatus
import com.notes.client.biometrics.SimulatedBiometricAuthManager

@Composable
fun VaultUnlockDialog(
    noteTitle: String = "Vault",
    biometricManager: BiometricAuthManager = remember { SimulatedBiometricAuthManager() },
    onDismiss: () -> Unit,
    onUnlocked: (ByteArray) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var passphrase by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(480.dp)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🔒 Unlock E2EE Vault",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Decrypting: $noteTitle (AES-GCM-256)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val canBio = remember(biometricManager) {
                    biometricManager.canAuthenticate() == BiometricStatus.AVAILABLE && biometricManager.isBiometricEnabled()
                }

                if (canBio) {
                    FilledTonalButton(
                        onClick = {
                            biometricManager.authenticate { result ->
                                when (result) {
                                    is BiometricAuthResult.Success -> {
                                        onUnlocked(result.key)
                                    }
                                    is BiometricAuthResult.Failure -> {
                                        errorMessage = result.reason
                                    }
                                    is BiometricAuthResult.Cancelled -> {
                                        errorMessage = "Biometric authentication cancelled"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics (Touch ID / Face ID)")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                        },
                        text = { Text("Passphrase") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                        },
                        text = { Text("12-Word Recovery") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            errorMessage = null
                        },
                        label = { Text("Master Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = recoveryPhrase,
                        onValueChange = {
                            recoveryPhrase = it
                            errorMessage = null
                        },
                        label = { Text("Paste 12-Word BIP-39 Seed") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val words = recoveryPhrase.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    val isValidWordCount = words.size == 12
                    Text(
                        text = if (isValidWordCount) "12 words detected (valid phrase)" else "${words.size} / 12 words",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isValidWordCount) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (passphrase.isBlank()) {
                                    errorMessage = "Please enter your master passphrase"
                                } else {
                                    val key = E2eeCryptoEngine.deriveKeyFromPassphrase(passphrase)
                                    onUnlocked(key)
                                }
                            } else {
                                val words = recoveryPhrase.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                                if (words.size != 12) {
                                    errorMessage = "A valid BIP-39 recovery phrase must contain exactly 12 words"
                                } else if (!Bip39RecoveryKit.isValidMnemonic(words)) {
                                    errorMessage = "Unrecognized BIP-39 dictionary word found in phrase"
                                } else {
                                    val key = Bip39RecoveryKit.deriveKeyFromMnemonic(words)
                                    onUnlocked(key)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock")
                    }
                }
            }
        }
    }
}
