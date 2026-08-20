package com.sebaya.dm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sebaya.dm.data.Dashboard
import com.sebaya.dm.data.GlucoseLog
import com.sebaya.dm.data.LeaderboardEntry
import com.sebaya.dm.data.PillarState
import com.sebaya.dm.data.User
import com.sebaya.dm.ui.components.Avatar
import com.sebaya.dm.ui.components.Eyebrow
import com.sebaya.dm.ui.components.PillarTile
import com.sebaya.dm.ui.components.ProgressRing
import com.sebaya.dm.ui.components.PullRefresh
import com.sebaya.dm.ui.components.SebayaCard
import com.sebaya.dm.ui.components.StateHost
import com.sebaya.dm.ui.components.StatPill
import com.sebaya.dm.ui.theme.sebaya

// ============================================================ Dashboard

@Composable
fun DashboardScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.loadDashboard(); vm.loadMe() }
    val name = (vm.meState.dataOrNull)?.namaLengkap.orEmpty()

    PullRefresh(
        isRefreshing = vm.refreshingDashboard,
        onRefresh = { vm.loadDashboard(refresh = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        StateHost(vm.dashboardState, onRetry = { vm.loadDashboard() }, modifier = Modifier.fillMaxSize()) { d ->
            DashboardContent(vm, d, name)
        }
    }
}

@Composable
private fun DashboardContent(vm: AppViewModel, d: Dashboard, name: String) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val s = MaterialTheme.sebaya

    val doneCount = listOf(d.medication.logged, d.activity.logged, d.diet.logged).count { it }
    val progress = doneCount / 3f

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // Greeting header
        Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            name.ifBlank { "Sahabat Sebaya" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        // Daily-progress hero card
        SebayaCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress = progress, ringSize = 96.dp, stroke = 11.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$doneCount/3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("pilar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 18.dp)) {
                    Eyebrow("Manajemen Harian")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (doneCount == 3) "Semua pilar hari ini selesai. Hebat!"
                        else "Lengkapi $doneCount dari 3 pilar harianmu.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    d.date.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Eyebrow("Catatan Hari Ini")
        Spacer(Modifier.height(8.dp))

        PillarTile(
            icon = Icons.Filled.Medication, accent = s.obat, title = "Konsumsi Obat",
            statusText = medicationStatus(d.medication), done = d.medication.logged,
            onClick = { dialog = "med" }, modifier = Modifier.padding(vertical = 5.dp),
        )
        PillarTile(
            icon = Icons.AutoMirrored.Filled.DirectionsRun, accent = s.aktivitas, title = "Aktivitas Fisik",
            statusText = activityStatus(d.activity), done = d.activity.logged,
            onClick = { dialog = "act" }, modifier = Modifier.padding(vertical = 5.dp),
        )
        PillarTile(
            icon = Icons.Filled.Restaurant, accent = s.diet, title = "Catatan Diet",
            statusText = dietStatus(d.diet), done = d.diet.logged,
            onClick = { dialog = "diet" }, modifier = Modifier.padding(vertical = 5.dp),
        )

        Spacer(Modifier.height(16.dp))
        Eyebrow("Gula Darah")
        Spacer(Modifier.height(8.dp))
        GlucoseCard(vm.latestGlucose, s.gula) { dialog = "glu" }

        when (dialog) {
            "med" -> TwoQuestionDialog(
                "Catatan Konsumsi Obat", "Obat sudah diminum lengkap semua?", "Apakah diminum tepat waktu?",
                onDismiss = { dialog = null }, onSave = { a, b -> vm.submitMedication(a, b); dialog = null })
            "act" -> ActivityDialog(
                onDismiss = { dialog = null },
                onSave = { a, b, days -> vm.submitActivity(a, b, days); dialog = null })
            "diet" -> ThreeQuestionDialog(
                "Catatan Diet",
                "Makanan sesuai anjuran dokter?", "Apakah makan sesuai jadwal?",
                "Sudah membatasi gula, garam, lemak, dan memperbanyak sayur?",
                onDismiss = { dialog = null },
                onSave = { a, b, c -> vm.submitDiet(a, b, c); dialog = null })
            "glu" -> GlucoseDialog(onDismiss = { dialog = null }, onSave = { t, v -> vm.submitGlucose(t, v); dialog = null })
        }

        // Out-of-range glucose alert (shown after a saved reading exceeds normal).
        vm.glucoseWarning?.let { msg ->
            AlertDialog(
                onDismissRequest = { vm.clearGlucoseWarning() },
                title = { Text("Perhatian") },
                text = { Text(msg) },
                confirmButton = { Button(onClick = { vm.clearGlucoseWarning() }) { Text("Mengerti") } },
            )
        }
    }
}

private fun medicationStatus(p: PillarState): String = when {
    !p.logged -> "Catat obat harian Anda."
    p.takenComplete && p.takenOnTime -> "Lengkap · tepat waktu"
    p.takenComplete -> "Lengkap"
    else -> "Tercatat"
}

private fun activityStatus(p: PillarState): String = when {
    !p.logged -> "Jalan, senam, atau bersepeda."
    p.didActivity && p.perDoctorAdvice -> "Sesuai anjuran · ${p.exerciseDaysPerWeek} hari/minggu"
    p.didActivity -> "Aktivitas tercatat · ${p.exerciseDaysPerWeek} hari/minggu"
    else -> "Tercatat"
}

private fun dietStatus(p: PillarState): String = when {
    !p.logged -> "Isi piring dengan menu seimbang."
    p.perDoctorAdvice && p.onSchedule && p.limitSugarSaltFat -> "Sesuai anjuran · batasi gula/garam/lemak"
    p.perDoctorAdvice && p.onSchedule -> "Sesuai anjuran · tepat jadwal"
    p.perDoctorAdvice -> "Sesuai anjuran dokter"
    else -> "Tercatat"
}

@Composable
private fun GlucoseCard(latest: GlucoseLog?, accent: Color, onClick: () -> Unit) {
    SebayaCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Opacity, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("Catatan Gula Darah", style = MaterialTheme.typography.titleSmall)
                if (latest != null) {
                    Text(
                        "Terakhir: ${latest.valueMgdl} mg/dL · ${timingLabel(latest.timing)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("Sebelum makan 80–130 · sesudah <180", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onClick) { Text("Tambah") }
        }
    }
}

private fun timingLabel(t: String): String = when (t) {
    "before_meal" -> "sebelum makan"
    "after_meal" -> "sesudah makan"
    else -> t
}

// ---- Dialogs (no biased default: answers start unselected) ---------------

@Composable
private fun TwoQuestionDialog(
    title: String, q1: String, q2: String,
    onDismiss: () -> Unit, onSave: (Boolean, Boolean) -> Unit,
) {
    var a by remember { mutableStateOf<Boolean?>(null) }
    var b by remember { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                YesNo(q1, a) { a = it }
                Spacer(Modifier.height(14.dp))
                YesNo(q2, b) { b = it }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(a == true, b == true) }, enabled = a != null && b != null) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ThreeQuestionDialog(
    title: String, q1: String, q2: String, q3: String,
    onDismiss: () -> Unit, onSave: (Boolean, Boolean, Boolean) -> Unit,
) {
    var a by remember { mutableStateOf<Boolean?>(null) }
    var b by remember { mutableStateOf<Boolean?>(null) }
    var c by remember { mutableStateOf<Boolean?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                YesNo(q1, a) { a = it }
                Spacer(Modifier.height(14.dp))
                YesNo(q2, b) { b = it }
                Spacer(Modifier.height(14.dp))
                YesNo(q3, c) { c = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(a == true, b == true, c == true) },
                enabled = a != null && b != null && c != null,
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ActivityDialog(
    onDismiss: () -> Unit, onSave: (Boolean, Boolean, Int) -> Unit,
) {
    var a by remember { mutableStateOf<Boolean?>(null) }
    var b by remember { mutableStateOf<Boolean?>(null) }
    var days by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan Aktivitas Fisik") },
        text = {
            Column {
                YesNo("Sudah melakukan aktivitas fisik?", a) { a = it }
                Spacer(Modifier.height(14.dp))
                YesNo("Apakah sesuai anjuran dokter?", b) { b = it }
                Spacer(Modifier.height(14.dp))
                DayStepper(
                    "Dalam seminggu terakhir, berapa hari aktivitas fisik sedang (min 30 menit/hari)?",
                    days,
                ) { days = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(a == true, b == true, days ?: 0) },
                enabled = a != null && b != null && days != null,
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

/** A 0–7 day selector with -/+ controls; null until the user picks a value. */
@Composable
private fun DayStepper(question: String, value: Int?, onChange: (Int) -> Unit) {
    Column {
        Text(question, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { onChange((value ?: 0).minus(1).coerceAtLeast(0)) },
                enabled = (value ?: 0) > 0,
            ) { Text("−") }
            Text(
                value?.let { "$it hari" } ?: "— hari",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
                onClick = { onChange((value ?: 0).plus(1).coerceAtMost(7)) },
                enabled = (value ?: 0) < 7,
            ) { Text("+") }
        }
    }
}

@Composable
private fun YesNo(question: String, value: Boolean?, onChange: (Boolean) -> Unit) {
    Column {
        Text(question, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text("Ya") })
            FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text("Tidak") })
        }
    }
}

@Composable
private fun GlucoseDialog(onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var timing by remember { mutableStateOf("before_meal") }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan Gula Darah") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(timing == "before_meal", { timing = "before_meal" }, label = { Text("Sebelum makan") })
                    FilterChip(timing == "after_meal", { timing = "after_meal" }, label = { Text("Sesudah makan") })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value, { value = it.filter(Char::isDigit) }, label = { Text("Nilai (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(timing, value.toIntOrNull() ?: 0) }, enabled = value.isNotEmpty()) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

// ============================================================ Leaderboard

@Composable
fun LeaderboardScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.loadLeaderboard() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Kelompok Sebaya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Peringkat kepatuhan 30 hari terakhir", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        PullRefresh(
            isRefreshing = vm.refreshingLeaderboard,
            onRefresh = { vm.loadLeaderboard(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            StateHost(
                vm.leaderboardState, onRetry = { vm.loadLeaderboard() },
                emptyText = "Belum ada data kelompok.", modifier = Modifier.fillMaxSize(),
            ) { entries ->
                LeaderboardList(entries)
            }
        }
    }
}

@Composable
private fun LeaderboardList(entries: List<LeaderboardEntry>) {
    val podium = entries.take(3)
    val rest = entries.drop(3)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (podium.isNotEmpty()) {
            item { Podium(podium) }
            item { Spacer(Modifier.height(4.dp)) }
        }
        itemsIndexed(rest) { _, e -> LeaderboardRow(e) }
    }
}

@Composable
private fun Podium(top: List<LeaderboardEntry>) {
    val s = MaterialTheme.sebaya
    // Order visually: 2nd, 1st, 3rd
    val ordered = listOfNotNull(
        top.getOrNull(1) to s.silver,
        top.getOrNull(0) to s.gold,
        top.getOrNull(2) to s.bronze,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        ordered.forEach { (e, medal) ->
            if (e == null) { Spacer(Modifier.weight(1f)); return@forEach }
            val tall = e.rank == 1
            SebayaCard(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = if (tall) 20.dp else 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Avatar(
                            e.namaLengkap, size = if (tall) 56.dp else 46.dp,
                            background = Brush.linearGradient(listOf(medal, medal.copy(alpha = 0.7f))),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("#${e.rank}", style = MaterialTheme.typography.labelLarge, color = medal, fontWeight = FontWeight.Bold)
                    Text(
                        if (e.isMe) "Anda" else e.namaLengkap.substringBefore(' '),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Text("${e.compliantDays} hari", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(e: LeaderboardEntry) {
    val container = if (e.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    SebayaCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().background(container).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "#${e.rank}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp),
            )
            Avatar(e.namaLengkap, size = 40.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    if (e.isMe) "${e.namaLengkap} (Anda)" else e.namaLengkap,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1,
                )
                LinearProgressIndicator(
                    progress = { e.scorePercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 5.dp).clip(RoundedCornerShape(999.dp)),
                )
            }
            Text("${e.compliantDays} hari", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

// ============================================================ Profile

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onLoggedOut: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onHistory: () -> Unit,
) {
    LaunchedEffect(Unit) { vm.loadMe() }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        StateHost(vm.meState, onRetry = { vm.loadMe() }, modifier = Modifier.fillMaxSize()) { u ->
            ProfileContent(
                u,
                onLogout = { vm.logout { onLoggedOut() } },
                onEditProfile = onEditProfile,
                onChangePassword = onChangePassword,
                onHistory = onHistory,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    u: User,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onHistory: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SebayaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(u.namaLengkap, size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text(u.namaLengkap.ifBlank { "-" }, style = MaterialTheme.typography.titleLarge)
                Text("@${u.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (u.usia > 0) StatPill("${u.usia} tahun")
                    StatPill(if (u.jenisKelamin == "P") "Perempuan" else "Laki-laki")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        SebayaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(4.dp)) {
                InfoRow("Pendidikan", u.pendidikan.ifBlank { "-" })
                InfoRow("Pekerjaan", u.pekerjaan.ifBlank { "-" })
                InfoRow("Email", u.email.ifBlank { "-" })
            }
        }
        Spacer(Modifier.height(14.dp))
        SebayaCard(Modifier.fillMaxWidth()) {
            Column {
                ProfileAction(Icons.Filled.Edit, "Ubah Profil", onEditProfile)
                ProfileAction(Icons.Filled.Lock, "Ganti Password", onChangePassword)
                ProfileAction(Icons.Filled.History, "Riwayat 30 Hari", onHistory)
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onLogout, modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Keluar")
        }
    }
}

@Composable
private fun ProfileAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
