package com.pulsechat.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.data.Repo
import com.pulsechat.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun UsernameScreen(repo: Repo, busy: Boolean, error: String?, onSubmit: (String) -> Unit) {
    val c = AppTheme.colors
    var name by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var taken by remember { mutableStateOf<Boolean?>(null) }

    val cleaned = name.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
    val valid = cleaned.length in 3..20

    LaunchedEffect(cleaned) {
        taken = null
        if (valid) {
            checking = true
            delay(450)
            taken = runCatching { withContext(Dispatchers.IO) { repo.usernameTaken(cleaned) } }.getOrNull()
            checking = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pick your username", style = MaterialTheme.typography.headlineMedium, color = c.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Instagram jaise — small letters, numbers, . aur _\nNo spaces. Unique hoga.", color = c.dim)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = cleaned,
            onValueChange = { name = it },
            label = { Text("Username") },
            prefix = { Text("@", color = c.primary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        when {
            !valid -> Text("3–20 characters, no spaces", color = c.dim, style = MaterialTheme.typography.bodySmall)
            checking -> Text("checking…", color = c.dim, style = MaterialTheme.typography.bodySmall)
            taken == true -> Text("@$cleaned is already taken", color = c.danger, style = MaterialTheme.typography.bodySmall)
            taken == false -> Text("@$cleaned is available", color = c.ok, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(cleaned) },
            enabled = !busy && valid && taken == false,
            colors = ButtonDefaults.buttonColors(containerColor = c.ok),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
        if (busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall)
        }
    }
}
