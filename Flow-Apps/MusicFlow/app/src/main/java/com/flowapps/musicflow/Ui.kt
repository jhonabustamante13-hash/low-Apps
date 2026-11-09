package com.flowapps.musicflow

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private fun audioPerm() =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPerm(c: Context) =
    ContextCompat.checkSelfPermission(c, audioPerm()) == PackageManager.PERMISSION_GRANTED

private val onBg @Composable get() = MaterialTheme.colorScheme.onBackground
private val PINK = Color(0xFFFF3D81)
private val VIOLET = Color(0xFF7B2FF7)

@Composable
fun MusicApp(ps: PlayerState) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasAudioPerm(ctx)) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val notifReq = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permReq = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }
    LaunchedEffect(granted) {
        if (granted) {
            songs = withContext(Dispatchers.IO) { Repo.load(ctx) }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) notifReq.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(ps.playing, ps.mediaId) {
        while (true) {
            ps.controller?.let { ps.pos = it.currentPosition; ps.dur = it.duration.coerceAtLeast(0L) }
            if (!ps.playing) break
            delay(300)
        }
    }
    GlassBackground {
        if (!granted) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.Center) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("MusicFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = onBg)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Para mostrar y reproducir la música guardada en tu teléfono necesitamos permiso para leer tus archivos de audio. " +
                            "También te pediremos permiso de notificaciones para mostrar los controles de reproducción.",
                        color = onBg
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permReq.launch(audioPerm()) }) { Text("Permitir acceso a la música") }
                }
            }
        } else Library(ps, songs)
    }
}

@Composable
fun Library(ps: PlayerState, songs: List<Song>) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var favs by remember { mutableStateOf(Prefs.favs(ctx)) }
    var showPlayer by remember { mutableStateOf(false) }
    val cur = ps.queue.firstOrNull { it.id.toString() == ps.mediaId }
    BackHandler(showPlayer) { showPlayer = false }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            Text(
                listOf("Canciones", "Favoritas", "Álbumes", "Cola")[tab],
                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = onBg,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            if (tab == 0) {
                OutlinedTextField(
                    query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar canción, artista o álbum") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            val q = query.trim()
            val base = when (tab) {
                0 -> songs.filter { q.isEmpty() || it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true) }
                1 -> songs.filter { it.id in favs }
                else -> songs
            }
            when (tab) {
                0, 1 -> if (base.isEmpty()) Empty(
                    if (tab == 0) (if (songs.isEmpty()) "No encontramos música en este teléfono." else "Sin resultados.")
                    else "Marca canciones con el corazón y aparecerán aquí."
                ) else LazyColumn(contentPadding = PaddingValues(bottom = 190.dp)) {
                    itemsIndexed(base, key = { _, s -> s.id }) { i, s ->
                        SongRow(s, s.id.toString() == ps.mediaId, s.id in favs,
                            onClick = { ps.play(base, i); showPlayer = true },
                            onFav = { favs = Prefs.toggleFav(ctx, s.id) })
                    }
                }
                2 -> {
                    val albums = songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
                    if (albums.isEmpty()) Empty("No hay álbumes todavía.") else LazyColumn(contentPadding = PaddingValues(bottom = 190.dp)) {
                        itemsIndexed(albums, key = { _, a -> a.first }) { _, (name, list) ->
                            GlassCard(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { ps.play(list, 0); showPlayer = true }) {
                                Text(name, fontWeight = FontWeight.SemiBold, color = onBg)
                                Text("${list.first().artist} · ${list.size} canciones", style = MaterialTheme.typography.bodySmall, color = onBg)
                            }
                        }
                    }
                }
                else -> if (ps.queue.isEmpty()) Empty("La cola está vacía. Reproduce una canción para llenarla.")
                else LazyColumn(contentPadding = PaddingValues(bottom = 190.dp)) {
                    itemsIndexed(ps.queue) { i, s ->
                        SongRow(s, s.id.toString() == ps.mediaId, s.id in favs,
                            onClick = { ps.controller?.seekTo(i, 0L); ps.controller?.play() },
                            onFav = { favs = Prefs.toggleFav(ctx, s.id) })
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)) {
            if (cur != null) {
                GlassCard(Modifier.fillMaxWidth().clickable { showPlayer = true }, tint = PINK) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Cover(cur, 44.dp, 12.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(cur.title, maxLines = 1, fontWeight = FontWeight.SemiBold, color = onBg)
                            Text(cur.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = onBg)
                        }
                        IconButton(onClick = { ps.toggle() }) {
                            Icon(if (ps.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir o pausar", tint = onBg)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf(Icons.Default.MusicNote, Icons.Default.Favorite, Icons.Default.Album, Icons.Default.QueueMusic)
                        .forEachIndexed { i, icon ->
                            IconButton(onClick = { tab = i }) {
                                Icon(icon, null, tint = if (tab == i) PINK else onBg)
                            }
                        }
                }
            }
        }

        AnimatedVisibility(
            showPlayer && cur != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            if (cur != null) NowPlaying(ps, cur, cur.id in favs,
                onFav = { favs = Prefs.toggleFav(ctx, cur.id) }, onClose = { showPlayer = false })
        }
    }
}

@Composable
fun Empty(msg: String) {
    GlassCard(Modifier.fillMaxWidth()) { Text(msg, color = onBg) }
}

@Composable
fun SongRow(s: Song, active: Boolean, fav: Boolean, onClick: () -> Unit, onFav: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(if (active) PINK.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(s, 48.dp, 12.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.title, maxLines = 1, fontWeight = FontWeight.SemiBold, color = if (active) PINK else onBg)
            Text("${s.artist} · ${fmtTime(s.durationMs)}", maxLines = 1, style = MaterialTheme.typography.bodySmall, color = onBg)
        }
        IconButton(onClick = onFav) {
            Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorita", tint = if (fav) PINK else onBg)
        }
    }
}

@Composable
fun Cover(song: Song?, size: Dp, radius: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, song?.id) {
        value = withContext(Dispatchers.IO) {
            if (song == null || Build.VERSION.SDK_INT < 29) null
            else try { ctx.contentResolver.loadThumbnail(song.uri, Size(512, 512), null).asImageBitmap() } catch (e: Exception) { null }
        }
    }
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius))
            .background(Brush.linearGradient(listOf(PINK, VIOLET))),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(size / 3))
    }
}

@Composable
fun Visualizer(active: Boolean) {
    val t = rememberInfiniteTransition(label = "viz")
    Row(
        Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(16) { i ->
            val h by t.animateFloat(
                0.15f, 1f,
                infiniteRepeatable(tween(480 + (i * 53) % 400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "b$i"
            )
            Box(
                Modifier.width(6.dp).fillMaxHeight(if (active) h else 0.15f)
                    .clip(RoundedCornerShape(3.dp)).background(Color(0xFFFF7BB0))
            )
        }
    }
}

@Composable
fun NowPlaying(ps: PlayerState, song: Song, fav: Boolean, onFav: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var view by remember { mutableIntStateOf(0) }
    val scale by animateFloatAsState(if (ps.playing) 1f else 0.9f, spring(dampingRatio = 0.6f), label = "cover")
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    GlassBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Cerrar", tint = onBg) }
                Spacer(Modifier.weight(1f))
                FilterChip(selected = view == 0, onClick = { view = 0 }, label = { Text("Portada") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = view == 1, onClick = { view = 1 }, label = { Text("Letra") })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onFav) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorita", tint = if (fav) PINK else onBg)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (view == 0) {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Cover(song, 280.dp, 32.dp, Modifier.scale(scale))
                    Spacer(Modifier.height(20.dp))
                    Text(song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = onBg, textAlign = TextAlign.Center, maxLines = 2)
                    Text(song.artist, color = onBg, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Visualizer(ps.playing)
                }
            } else {
                Box(Modifier.weight(1f)) {
                    LyricsView(song, ps.pos) { ps.controller?.seekTo(it) }
                }
            }
            Slider(
                value = ps.pos.toFloat().coerceAtMost(maxOf(ps.dur, 1L).toFloat()),
                onValueChange = { ps.pos = it.toLong() },
                onValueChangeFinished = { ps.controller?.seekTo(ps.pos) },
                valueRange = 0f..maxOf(ps.dur, 1L).toFloat()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtTime(ps.pos), style = MaterialTheme.typography.bodySmall, color = onBg)
                Text(fmtTime(ps.dur), style = MaterialTheme.typography.bodySmall, color = onBg)
            }
            GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp), tint = PINK) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { ps.controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) {
                        Icon(Icons.Default.Shuffle, "Aleatorio", tint = if (ps.shuffle) PINK else onBg)
                    }
                    IconButton(onClick = { ps.controller?.seekToPrevious() }) { Icon(Icons.Default.SkipPrevious, "Anterior", tint = onBg, modifier = Modifier.size(34.dp)) }
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(PINK).clickable { ps.toggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (ps.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproducir o pausar", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { ps.controller?.seekToNext() }) { Icon(Icons.Default.SkipNext, "Siguiente", tint = onBg, modifier = Modifier.size(34.dp)) }
                    IconButton(onClick = { ps.cycleRepeat() }) {
                        Icon(
                            if (ps.repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "Repetir", tint = if (ps.repeat == Player.REPEAT_MODE_OFF) onBg else PINK
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, null, tint = onBg)
                Slider(
                    value = vol,
                    onValueChange = { vol = it; am.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0) },
                    valueRange = 0f..am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                )
            }
        }
    }
}

@Composable
fun LyricsView(song: Song, pos: Long, onSeek: (Long) -> Unit) {
    val ctx = LocalContext.current
    var raw by remember(song.id) { mutableStateOf(Prefs.lyrics(ctx, song.id)) }
    var edit by remember { mutableStateOf(false) }
    val lines = remember(raw) { parseLrc(raw) }
    val synced = lines.isNotEmpty() && lines.all { it.t != null }
    val cur = if (synced) lines.indexOfLast { (it.t ?: 0L) <= pos } else -1
    val st = rememberLazyListState()
    LaunchedEffect(cur) { if (cur >= 0) st.animateScrollToItem((cur - 2).coerceAtLeast(0)) }

    GlassCard(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (synced) "Letra sincronizada" else "Letra", fontWeight = FontWeight.SemiBold, color = onBg, modifier = Modifier.weight(1f))
            TextButton(onClick = { edit = true }) { Text(if (raw.isBlank()) "Añadir" else "Editar") }
        }
        if (lines.isEmpty()) {
            Text(
                "Esta canción no tiene letra guardada. Pega la letra o importa un archivo .lrc o .txt. " +
                    "MusicFlow no descarga letras de Internet.",
                color = onBg
            )
        } else {
            LazyColumn(state = st, modifier = Modifier.weight(1f)) {
                itemsIndexed(lines) { i, l ->
                    val active = i == cur
                    Text(
                        l.text.ifBlank { "♪" },
                        fontSize = if (active) 22.sp else 18.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (!synced || active) onBg else onBg.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 6.dp).clickable(enabled = synced) { onSeek(l.t ?: 0L) }
                    )
                }
            }
        }
    }
    if (edit) LyricsDialog(raw, { edit = false }) { text ->
        raw = text
        Prefs.saveLyrics(ctx, song.id, text)
        edit = false
    }
}

@Composable
fun LyricsDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(initial) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val t = try { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() } catch (e: Exception) { null }
            if (t != null) text = t
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Letra de la canción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open.launch(arrayOf("*/*")) }) { Text("Importar archivo .lrc o .txt") }
                OutlinedTextField(text, { text = it }, minLines = 6, maxLines = 12, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pega aquí la letra") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
