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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsechat.app.ui.theme.Cyan
import com.pulsechat.app.ui.theme.Green
import com.pulsechat.app.ui.theme.Red
import com.pulsechat.app.ui.theme.TextDim

@Composable
fun UsernameScreen(busy: Boolean, error: String?, onSubmit: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val cleaned = name.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(20)
    val valid = cleaned.length in 3..20

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Pick your username", style = MaterialTheme.typography.headlineMedium, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Instagram jaise — sirf small letters, numbers, . aur _\nNo spaces. Ye unique hoga.", color = TextDim)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = cleaned,
            onValueChange = { name = it },
            label = { Text("Username") },
            prefix = { Text("@", color = Cyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (valid) "@$cleaned — available?" else "3–20 characters, no spaces",
            color = if (valid) Green else TextDim,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onSubmit(cleaned) },
            enabled = !busy && valid,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
        if (busy) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = Red, style = MaterialTheme.typography.bodySmall)
        }
    }
}
