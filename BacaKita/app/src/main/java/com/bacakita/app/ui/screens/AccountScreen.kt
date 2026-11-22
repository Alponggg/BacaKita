package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.User
import com.bacakita.app.ui.AuthUiState
import com.bacakita.app.ui.components.AppLogo
import com.bacakita.app.ui.components.InfoCard
import com.bacakita.app.ui.components.SimpleTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    user: User?,
    authState: AuthUiState,
    savedCount: Int,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onLogout: () -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { SimpleTopBar("Akun", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo(size = 96.dp)

            if (user != null) {
                Text("Masuk sebagai", style = MaterialTheme.typography.bodyMedium)
                Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                InfoCard(
                    "Dokumen tersimpan di riwayat: $savedCount\n\n" +
                        "Akun dan riwayat disimpan di perangkat ini saja dan belum tersinkron ke perangkat lain."
                )
                OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Keluar") }
            } else {
                var registerMode by rememberSaveable { mutableStateOf(false) }
                var username by rememberSaveable { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var showPassword by remember { mutableStateOf(false) }
                val loading = authState is AuthUiState.Loading
                val submit = {
                    if (registerMode) onRegister(username, password) else onLogin(username, password)
                }

                Text(
                    "Masuk untuk menyimpan riwayat dokumen. Tanpa akun, aplikasi tetap bisa dipakai penuh, hanya riwayatnya tidak tersimpan setelah aplikasi ditutup.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !registerMode,
                        onClick = { registerMode = false; onClearError() },
                        label = { Text("Masuk", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = registerMode,
                        onClick = { registerMode = true; onClearError() },
                        label = { Text("Daftar", style = MaterialTheme.typography.bodyLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; onClearError() },
                    label = { Text("Nama pengguna") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; onClearError() },
                    label = { Text("Kata sandi") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Sembunyikan" else "Tampilkan")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (registerMode) {
                    Text(
                        "Nama pengguna 3–30 karakter (huruf, angka, titik, garis bawah, strip). Kata sandi minimal 6 karakter.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (authState is AuthUiState.Error) InfoCard(authState.message, isError = true)

                Button(
                    onClick = { submit() },
                    enabled = !loading && username.isNotBlank() && password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(if (loading) "Memproses…" else if (registerMode) "Daftar" else "Masuk") }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
