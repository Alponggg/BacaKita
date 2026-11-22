package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.DocumentLimits
import com.bacakita.app.ui.components.SimpleTopBar

@Composable
fun InputTextScreen(
    text: String,
    onTextChange: (String) -> Unit,
    onOpen: () -> Unit,
    onBack: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val tooLong = text.length > DocumentLimits.MAX_MANUAL_CHARS

    Scaffold(topBar = { SimpleTopBar("Masukkan Teks", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = { Text("Ketik atau tempel teks di sini") },
                isError = tooLong,
                supportingText = {
                    Text(
                        if (tooLong) "Teks terlalu panjang (${text.length} / ${DocumentLimits.MAX_MANUAL_CHARS} karakter). Kurangi isinya."
                        else "${text.length} / ${DocumentLimits.MAX_MANUAL_CHARS} karakter"
                    )
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { clipboard.getText()?.text?.let { onTextChange(text + it) } },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Tempel") }
                OutlinedButton(
                    onClick = { onTextChange("") },
                    enabled = text.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Hapus") }
            }
            Button(
                onClick = onOpen,
                enabled = text.isNotBlank() && !tooLong,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("Buka di Pembaca") }
        }
    }
}
