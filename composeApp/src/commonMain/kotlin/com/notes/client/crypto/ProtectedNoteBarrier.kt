package com.notes.client.crypto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.notes.common.crypto.ProtectedNoteCodec
import com.notes.common.models.Note

@Composable
fun ProtectedNoteBarrier(
    note: Note,
    passwordHint: String? = null,
    expectedCheckTagHex: String? = null,
    saltHex: String? = null,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🛡️", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Protected Note: ${note.title}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This note has individual password protection. Content is held in a self-contained container and unlocks only in the official client.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!passwordHint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("💡 Hint: $passwordHint") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = enteredPassword,
                onValueChange = {
                    enteredPassword = it
                    errorMessage = null
                },
                label = { Text("Note Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.width(320.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (expectedCheckTagHex != null && saltHex != null) {
                        val computed = ProtectedNoteCodec.deriveCheckTag(enteredPassword, saltHex)
                        if (computed.equals(expectedCheckTagHex, ignoreCase = true)) {
                            onUnlocked()
                        } else {
                            errorMessage = "Incorrect password. Please try again."
                        }
                    } else {
                        // Fallback: accept password if >= 4 chars
                        if (enteredPassword.length >= 4) {
                            onUnlocked()
                        } else {
                            errorMessage = "Password must be at least 4 characters."
                        }
                    }
                },
                enabled = enteredPassword.isNotBlank(),
                modifier = Modifier.width(200.dp)
            ) {
                Text("🔓 Unlock Note")
            }
        }
    }
}
