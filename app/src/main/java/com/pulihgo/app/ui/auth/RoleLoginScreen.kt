package com.pulihgo.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pulihgo.app.data.model.Role
import com.pulihgo.app.data.model.User
import com.pulihgo.app.ui.theme.PulihGoTheme

/**
 * Login demo berbasis role. [onLogin] mengembalikan pesan error, atau null jika berhasil.
 * Layar ini tidak tahu apa pun tentang role. Pengalihan ke Home ditangani AppNavigation.
 */
@Composable
fun RoleLoginScreen(
    demoAccounts: List<User>,
    onLogin: (identifier: String, password: String) -> String?,
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PulihGo", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Masuk sebagai Patient atau Therapist menggunakan akun demo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it; error = null },
                label = { Text("Username atau email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("login_identifier"),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("login_password"),
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { error = onLogin(identifier, password) },
                modifier = Modifier.fillMaxWidth().testTag("login_submit"),
            ) { Text("Masuk") }

            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Akun demo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ketuk untuk mengisi otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    demoAccounts.forEach { account ->
                        OutlinedButton(
                            onClick = {
                                identifier = account.username
                                password = account.password
                                error = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${account.role.label()} · ${account.username} / ${account.password}")
                        }
                    }
                }
            }
        }
    }
}

fun Role.label(): String = when (this) {
    Role.PATIENT -> "Patient"
    Role.THERAPIST -> "Therapist"
}

@Preview(showBackground = true)
@Composable
private fun RoleLoginPreview() {
    PulihGoTheme(dynamicColor = false) {
        RoleLoginScreen(
            demoAccounts = listOf(
                User("U001", "therapist01", "demo123", Role.THERAPIST, "Demo Therapist", "t@demo", true),
                User("U002", "patient01", "demo123", Role.PATIENT, "Alya Putri", "a@demo", true),
            ),
            onLogin = { _, _ -> null },
        )
    }
}
