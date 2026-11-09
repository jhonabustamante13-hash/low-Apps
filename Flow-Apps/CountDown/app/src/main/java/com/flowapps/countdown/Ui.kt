package com.flowapps.countdown

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val PALETTE = listOf(0xFFE63E7A, 0xFFFF8A3D, 0xFF7B5CFF, 0xFF14B8A6, 0xFF3B82F6).map { it.toInt() }
val CATS = listOf("Cumpleaños", "Viaje", "Vacaciones", "Boda", "Evento", "Personalizado")
private val zone: ZoneId get() = ZoneId.systemDefault()
private val fmt = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")

fun parts(diff: Long): List<Long> {
    val s = diff.coerceAtLeast(0L) / 1000
    return listOf(s / 86400, s % 86400 / 3600, s % 3600 / 60, s % 60)
}

fun copyPhoto(c: Context, uri: Uri): String? = try {
    val f = File(c.filesDir, "photo_${System.currentTimeMillis()}.jpg")
    c.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    f.absolutePath
} catch (e: Exception) {
    null
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf(Store.load(ctx)) }
    var filter by remember { mutableIntStateOf(0) }
    var form by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Cd?>(null) }
    var deleting by remember { mutableStateOf<Cd?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    fun commit(l: List<Cd>) { list = l; Store.save(ctx, l) }
    val shown = list.filter { filter == 0 || it.favorite }.sortedBy { it.targetMillis }

    GlassBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
            Text(
                "CountDown", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("Todas") })
                FilterChip(selected = filter == 1, onClick = { filter = 1 }, label = { Text("Favoritas") })
            }
            Spacer(Modifier.height(12.dp))
            if (shown.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        if (filter == 0) "Crea tu primera cuenta regresiva" else "Aún no tienes favoritas",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (filter == 0) "Toca el botón + para añadir un cumpleaños, viaje, boda o cualquier fecha."
                        else "Marca una cuenta con la estrella. La favorita más próxima se muestra en el widget.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(shown, key = { it.id }) { c ->
                        CdCard(
                            c, now,
                            onFav = { commit(list.map { if (it.id == c.id) it.copy(favorite = !it.favorite) else it }) },
                            onEdit = { editing = c; form = true },
                            onDelete = { deleting = c }
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { editing = null; form = true },
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.Add, "Nueva cuenta regresiva") }
    }

    if (form) EditDialog(editing, { form = false }) { saved ->
        commit(if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved)
        form = false
    }
    deleting?.let { d ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar cuenta regresiva?") },
            text = { Text(d.title) },
            confirmButton = { TextButton(onClick = { commit(list.filter { it.id != d.id }); deleting = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun CdCard(c: Cd, now: Long, onFav: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accent = Color(c.color)
    val on = MaterialTheme.colorScheme.onBackground
    val bmp = remember(c.photo) {
        c.photo?.let { p -> BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }
    }
    GlassCard(Modifier.fillMaxWidth(), tint = accent) {
        if (bmp != null) {
            Image(
                bmp, null,
                Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = on)
                Text(
                    c.category + " · " + Instant.ofEpochMilli(c.targetMillis).atZone(zone).toLocalDateTime().format(fmt),
                    style = MaterialTheme.typography.bodySmall, color = on
                )
            }
            IconButton(onClick = onFav) {
                Icon(if (c.favorite) Icons.Default.Star else Icons.Default.StarBorder, "Favorito", tint = accent)
            }
        }
        Spacer(Modifier.height(10.dp))
        val diff = c.targetMillis - now
        if (diff <= 0) {
            Text("¡Llegó el día!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = on)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("días", "horas", "min", "seg").zip(parts(diff)).forEach { (l, v) ->
                    Digit(v, l, accent, Modifier.weight(1f))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar", tint = on) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar", tint = on) }
        }
    }
}

@Composable
fun Digit(v: Long, label: String, accent: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.6f), shape).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = v,
            transitionSpec = { (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut()) },
            label = "digit"
        ) { n ->
            Text("%02d".format(n), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun EditDialog(initial: Cd?, onDismiss: () -> Unit, onSave: (Cd) -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var cat by remember { mutableStateOf(initial?.category ?: "Evento") }
    var target by remember { mutableLongStateOf(initial?.targetMillis ?: (System.currentTimeMillis() + 7 * 86_400_000L)) }
    var color by remember { mutableIntStateOf(initial?.color ?: PALETTE[0]) }
    var photo by remember { mutableStateOf(initial?.photo) }
    var notify by remember { mutableStateOf(initial?.notify ?: true) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = copyPhoto(ctx, uri)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val dt = Instant.ofEpochMilli(target).atZone(zone).toLocalDateTime()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva cuenta regresiva" else "Editar cuenta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CATS.forEach { c -> FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        DatePickerDialog(ctx, { _, y, m, d ->
                            target = LocalDateTime.of(y, m + 1, d, dt.hour, dt.minute).atZone(zone).toInstant().toEpochMilli()
                        }, dt.year, dt.monthValue - 1, dt.dayOfMonth).show()
                    }) { Text("%02d/%02d/%d".format(dt.dayOfMonth, dt.monthValue, dt.year)) }
                    OutlinedButton(onClick = {
                        TimePickerDialog(ctx, { _, h, mi ->
                            target = LocalDateTime.of(dt.toLocalDate(), java.time.LocalTime.of(h, mi)).atZone(zone).toInstant().toEpochMilli()
                        }, dt.hour, dt.minute, true).show()
                    }) { Text("%02d:%02d".format(dt.hour, dt.minute)) }
                }
                Text("Color")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PALETTE.forEach { p ->
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(p))
                                .border(if (p == color) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = p }
                        )
                    }
                }
                OutlinedButton(onClick = {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (photo == null) "Añadir fotografía" else "Cambiar fotografía")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Notificar al llegar")
                        Text("Se pide permiso de notificaciones solo para avisarte cuando llegue la fecha.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = notify, onCheckedChange = {
                        notify = it
                        if (it && Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(Cd(initial?.id ?: System.currentTimeMillis(), title.trim(), cat, target, color, initial?.favorite ?: false, photo, notify))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
