package com.bacakita.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bacakita.app.data.ReaderPreferences
import com.bacakita.app.tts.PlaybackStatus
import com.bacakita.app.tts.TtsState
import com.bacakita.app.ui.ReaderContent
import com.bacakita.app.ui.components.readerTextStyle
import com.bacakita.app.ui.theme.ReaderPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    content: ReaderContent?,
    paragraphs: List<String>,
    prefs: ReaderPreferences,
    ttsState: TtsState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAi: () -> Unit,
    onPlay: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val palette = ReaderPalette.of(prefs.theme)
    val listState = rememberLazyListState()
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val safeSelected = selected.coerceIn(0, (paragraphs.size - 1).coerceAtLeast(0))
    val speaking = ttsState.playback != PlaybackStatus.IDLE
    val activeIndex = if (speaking && ttsState.currentIndex >= 0) ttsState.currentIndex else safeSelected

    val notice = content?.notice
    val headerCount = if (notice != null) 2 else 1

    // Ikuti paragraf yang sedang dibacakan (item awal: petunjuk + catatan impor bila ada).
    LaunchedEffect(ttsState.currentIndex, speaking) {
        if (speaking && ttsState.currentIndex in paragraphs.indices) {
            listState.animateScrollToItem(ttsState.currentIndex + headerCount)
        }
    }
    // Sumber daya TTS dilepas/dihentikan saat meninggalkan Reader.
    DisposableEffect(Unit) { onDispose { onStop() } }

    Scaffold(
        containerColor = palette.background,
        contentColor = palette.foreground,
        topBar = {
            TopAppBar(
                title = { Text(content?.title ?: "Pembaca", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") }
                },
                actions = {
                    IconButton(onClick = onOpenAi) { Icon(Icons.Default.AutoAwesome, "Rekomendasi AI") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Tune, "Pengaturan tampilan") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                    titleContentColor = palette.foreground,
                    navigationIconContentColor = palette.foreground,
                    actionIconContentColor = palette.foreground,
                ),
            )
        },
        bottomBar = {
            if (paragraphs.isNotEmpty()) {
                TtsBar(
                    state = ttsState,
                    palette = palette,
                    onPlay = { onPlay(safeSelected) },
                    onPause = onPause,
                    onResume = onResume,
                    onStop = onStop,
                )
            }
        },
    ) { padding ->
        if (paragraphs.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Belum ada teks untuk dibaca.", style = MaterialTheme.typography.titleMedium, color = palette.foreground)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                ) { Text("Kembali") }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (notice != null) {
                    item {
                        Surface(
                            color = palette.surface,
                            contentColor = palette.foreground,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, palette.foreground.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        ) {
                            Text("Catatan: $notice", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                item {
                    Text(
                        "Ketuk paragraf untuk memilih titik mulai membaca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.foreground,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                itemsIndexed(paragraphs, key = { index, _ -> index }) { index, text ->
                    ParagraphItem(
                        text = text,
                        isActive = index == activeIndex,
                        prefs = prefs,
                        palette = palette,
                        onClick = {
                            selected = index
                            when (ttsState.playback) {
                                PlaybackStatus.PLAYING -> onPlay(index)   // lompat ke paragraf ini
                                PlaybackStatus.PAUSED -> onStop()         // kembali siaga di paragraf baru
                                PlaybackStatus.IDLE -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ParagraphItem(
    text: String,
    isActive: Boolean,
    prefs: ReaderPreferences,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    val dimmed = prefs.focusMode && !isActive
    val alpha by animateFloatAsState(if (dimmed) 0.3f else 1f, label = "focusAlpha")
    val style = remember(prefs, palette) { readerTextStyle(prefs, palette) }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(onClickLabel = "Pilih paragraf ini", onClick = onClick)
                .padding(vertical = if (prefs.focusMode) 14.dp else 8.dp)
                .alpha(alpha),
        ) {
            // Penanda posisi berupa bentuk (bukan hanya warna) agar tidak bergantung pada warna saja.
            Box(
                Modifier.width(4.dp).fillMaxHeight()
                    .background(if (isActive) palette.accent else Color.Transparent)
            )
            Spacer(Modifier.width(12.dp))
            Text(text, style = style, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TtsBar(
    state: TtsState,
    palette: ReaderPalette,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    Surface(color = palette.surface, contentColor = palette.foreground) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.message?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = palette.foreground)
                if (state.needsEngineSettings) {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                            } catch (e: ActivityNotFoundException) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                } catch (e2: ActivityNotFoundException) { /* tidak ada aktivitas pengaturan */ }
                            }
                        },
                        border = BorderStroke(2.dp, palette.foreground),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.foreground),
                    ) { Text("Buka Pengaturan Suara") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (state.playback) {
                    PlaybackStatus.IDLE -> TtsButton("Baca", Icons.Default.PlayArrow, palette, true, onPlay, Modifier.weight(1f))
                    PlaybackStatus.PLAYING -> {
                        TtsButton("Jeda", Icons.Default.Pause, palette, true, onPause, Modifier.weight(1f))
                        TtsButton("Berhenti", Icons.Default.Stop, palette, false, onStop, Modifier.weight(1f))
                    }
                    PlaybackStatus.PAUSED -> {
                        TtsButton("Lanjutkan", Icons.Default.PlayArrow, palette, true, onResume, Modifier.weight(1f))
                        TtsButton("Berhenti", Icons.Default.Stop, palette, false, onStop, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TtsButton(
    label: String,
    icon: ImageVector,
    palette: ReaderPalette,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val content: @Composable () -> Unit = {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 56.dp),
            border = BorderStroke(2.dp, palette.foreground),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.foreground),
        ) { content() }
    }
}
