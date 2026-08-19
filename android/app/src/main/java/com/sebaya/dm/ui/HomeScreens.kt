package com.sebaya.dm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sebaya.dm.data.PillarState

@Composable
fun DashboardScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.loadDashboard() }
    val d = vm.dashboard

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Manajemen Harian", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        d?.date?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))

        var dialog by remember { mutableStateOf<String?>(null) }

        PillarCard("Konsumsi Obat", "Catat obat harian Anda.", d?.medication?.logged == true) { dialog = "med" }
        PillarCard("Aktivitas Fisik", "Jalan, senam, atau bersepeda.", d?.activity?.logged == true) { dialog = "act" }
        PillarCard("Catatan Diet", "Isi piring dengan menu seimbang.", d?.diet?.logged == true) { dialog = "diet" }
        GlucoseCard { dialog = "glu" }

        when (dialog) {
            "med" -> TwoQuestionDialog(
                "Catatan Konsumsi Obat", "Obat sudah diminum lengkap semua?", "Apakah diminum tepat waktu?",
                onDismiss = { dialog = null }, onSave = { a, b -> vm.submitMedication(a, b); dialog = null })
            "act" -> TwoQuestionDialog(
                "Catatan Aktivitas Fisik", "Sudah melakukan aktivitas fisik?", "Apakah sesuai anjuran dokter?",
                onDismiss = { dialog = null }, onSave = { a, b -> vm.submitActivity(a, b); dialog = null })
            "diet" -> TwoQuestionDialog(
                "Catatan Diet", "Makanan sesuai anjuran dokter?", "Apakah makan sesuai jadwal?",
                onDismiss = { dialog = null }, onSave = { a, b -> vm.submitDiet(a, b); dialog = null })
            "glu" -> GlucoseDialog(onDismiss = { dialog = null }, onSave = { t, v -> vm.submitGlucose(t, v) {}; dialog = null })
        }
    }
}

@Composable
private fun PillarCard(title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (done) AssistChip(onClick = onClick, label = { Text("✓ Terisi") })
            else Button(onClick = onClick) { Text("Isi") }
        }
    }
}

@Composable
private fun GlucoseCard(onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Catatan Gula Darah", fontWeight = FontWeight.Bold)
                Text("Sebelum makan 80–130 · sesudah <180 mg/dL", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onClick) { Text("Tambah") }
        }
    }
}

@Composable
private fun TwoQuestionDialog(
    title: String, q1: String, q2: String,
    onDismiss: () -> Unit, onSave: (Boolean, Boolean) -> Unit,
) {
    var a by remember { mutableStateOf(true) }
    var b by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                YesNo(q1, a) { a = it }
                Spacer(Modifier.height(12.dp))
                YesNo(q2, b) { b = it }
            }
        },
        confirmButton = { Button(onClick = { onSave(a, b) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun YesNo(question: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Text(question, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = value, onClick = { onChange(true) }, label = { Text("Ya") })
            FilterChip(selected = !value, onClick = { onChange(false) }, label = { Text("Tidak") })
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
                OutlinedTextField(value, { value = it.filter(Char::isDigit) }, label = { Text("Nilai (mg/dL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(timing, value.toIntOrNull() ?: 0) }, enabled = value.isNotEmpty()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
fun LeaderboardScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.loadLeaderboard() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Kelompok Sebaya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Peringkat kepatuhan 30 hari terakhir", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        if (vm.leaderboard.isEmpty()) {
            Text("Belum ada data kelompok.", color = MaterialTheme.colorScheme.outline)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vm.leaderboard) { e ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${e.rank}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(0.dp).also { })
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(if (e.isMe) "${e.namaLengkap} (Anda)" else e.namaLengkap, fontWeight = FontWeight.Medium)
                            LinearProgressIndicator(progress = { e.scorePercent / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        }
                        Text("${e.compliantDays} hari", modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: AppViewModel, onLoggedOut: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadMe() }
    val u = vm.me
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(u?.namaLengkap ?: "-", style = MaterialTheme.typography.titleMedium)
                Text("Username: ${u?.username ?: "-"}")
                Text("Usia: ${u?.usia ?: 0} · JK: ${u?.jenisKelamin ?: "-"}")
                Text("Pendidikan: ${u?.pendidikan?.ifEmpty { "-" } ?: "-"}")
                Text("Pekerjaan: ${u?.pekerjaan?.ifEmpty { "-" } ?: "-"}")
                Text("Study arm: ${u?.studyArm?.ifEmpty { "-" } ?: "-"}")
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { vm.logout { onLoggedOut() } }, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
    }
}
