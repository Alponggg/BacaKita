package com.bacakita.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.ReaderFonts
import com.bacakita.app.data.ReaderLimits
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.data.ReaderThemes
import com.bacakita.app.ui.components.ReaderPreview
import com.bacakita.app.ui.components.SimpleTopBar
import com.bacakita.app.ui.components.SliderSetting
import com.bacakita.app.ui.components.SwitchRow
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: ReaderPreferences,
    onChange: ((ReaderPreferences) -> ReaderPreferences) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { SimpleTopBar("Pengaturan Tampilan", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ReaderPreview(
                prefs = prefs,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).heightIn(max = 200.dp),
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SliderSetting(
                    label = "Ukuran huruf",
                    valueText = "${prefs.fontSize.roundToInt()} sp",
                    value = prefs.fontSize,
                    range = ReaderLimits.FONT_SIZE_MIN..ReaderLimits.FONT_SIZE_MAX,
                    steps = (ReaderLimits.FONT_SIZE_MAX - ReaderLimits.FONT_SIZE_MIN).roundToInt() - 1,
                    onValueChange = { v -> onChange { it.copy(fontSize = v.roundToInt().toFloat()) } },
                )
                SliderSetting(
                    label = "Jarak antarbaris",
                    valueText = "%.1f×".format(prefs.lineSpacing),
                    value = prefs.lineSpacing,
                    range = ReaderLimits.LINE_SPACING_MIN..ReaderLimits.LINE_SPACING_MAX,
                    steps = ((ReaderLimits.LINE_SPACING_MAX - ReaderLimits.LINE_SPACING_MIN) * 10).roundToInt() - 1,
                    onValueChange = { v -> onChange { it.copy(lineSpacing = (v * 10).roundToInt() / 10f) } },
                )
                SliderSetting(
                    label = "Jarak antarhuruf",
                    valueText = "%.1f sp".format(prefs.letterSpacing),
                    value = prefs.letterSpacing,
                    range = ReaderLimits.LETTER_SPACING_MIN..ReaderLimits.LETTER_SPACING_MAX,
                    steps = ((ReaderLimits.LETTER_SPACING_MAX - ReaderLimits.LETTER_SPACING_MIN) * 10).roundToInt() - 1,
                    onValueChange = { v -> onChange { it.copy(letterSpacing = (v * 10).roundToInt() / 10f) } },
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Jenis huruf", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderFonts.all.forEach { id ->
                            OptionChip(label = ReaderFonts.label(id), selected = prefs.fontFamily == id) {
                                onChange { it.copy(fontFamily = id) }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tema / kontras", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderThemes.all.forEach { id ->
                            OptionChip(label = ReaderThemes.label(id), selected = prefs.theme == id) {
                                onChange { it.copy(theme = id) }
                            }
                        }
                    }
                }

                SwitchRow(
                    title = "Mode fokus",
                    description = "Meredupkan paragraf lain sehingga hanya paragraf terpilih yang tampak jelas.",
                    checked = prefs.focusMode,
                    onCheckedChange = { checked -> onChange { it.copy(focusMode = checked) } },
                )

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Kembalikan ke Pengaturan Awal") }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = "Dipilih") }
        } else null,
        modifier = Modifier.heightIn(min = 48.dp),
    )
}
