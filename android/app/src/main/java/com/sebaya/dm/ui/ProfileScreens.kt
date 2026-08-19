package com.sebaya.dm.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sebaya.dm.data.ChangePasswordRequest
import com.sebaya.dm.data.UpdateProfileRequest
import com.sebaya.dm.data.User
import com.sebaya.dm.ui.components.Eyebrow
import com.sebaya.dm.ui.components.ScreenScaffold
import com.sebaya.dm.ui.components.SebayaCard
import com.sebaya.dm.ui.components.StateHost

// ============================================================ Edit profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.error = null; vm.loadMe() }
    ScreenScaffold("Ubah Profil", onBack = onBack) { mod ->
        StateHost(vm.meState, onRetry = { vm.loadMe() }, modifier = mod.fillMaxSize()) { u ->
            EditProfileForm(vm, u, onBack)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileForm(vm: AppViewModel, u: User, onBack: () -> Unit) {
    var nama by remember(u.id) { mutableStateOf(u.namaLengkap) }
    var usia by remember(u.id) { mutableStateOf(if (u.usia > 0) u.usia.toString() else "") }
    var jk by remember(u.id) { mutableStateOf(u.jenisKelamin.ifBlank { "L" }) }
    var pendidikan by remember(u.id) { mutableStateOf(u.pendidikan) }
    var pekerjaan by remember(u.id) { mutableStateOf(u.pekerjaan) }
    var email by remember(u.id) { mutableStateOf(u.email) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(nama, { nama = it }, label = { Text("Nama Lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            usia, { usia = it.filter(Char::isDigit) }, label = { Text("Usia") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
        )

        Text("Jenis Kelamin", style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = jk == "L", onClick = { jk = "L" },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Laki-laki") }
            SegmentedButton(
                selected = jk == "P", onClick = { jk = "P" },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Perempuan") }
        }

        OutlinedTextField(pendidikan, { pendidikan = it }, label = { Text("Pendidikan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pekerjaan, { pekerjaan = it }, label = { Text("Pekerjaan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            email, { email = it }, label = { Text("Email") },
            supportingText = { Text("Untuk pemulihan password") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(
            onClick = {
                vm.updateProfile(
                    UpdateProfileRequest(nama.trim(), usia.toIntOrNull() ?: 0, jk, pendidikan.trim(), pekerjaan.trim(), email.trim()),
                ) { onBack() }
            },
            enabled = !vm.busy && nama.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Text("Simpan")
        }
    }
}

// ============================================================ Change password

@Composable
fun ChangePasswordScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.error = null }
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    ScreenScaffold("Ganti Password", onBack = onBack) { mod ->
        Column(
            mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                current, { current = it }, label = { Text("Password Saat Ini") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                new, { new = it }, label = { Text("Password Baru") }, singleLine = true,
                supportingText = { Text("Minimal 6 karakter") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                confirm, { confirm = it }, label = { Text("Konfirmasi Password Baru") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            (localError ?: vm.error)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = {
                    localError = null
                    if (new != confirm) {
                        localError = "Konfirmasi password tidak cocok."
                    } else {
                        vm.changePassword(ChangePasswordRequest(current, new)) { onBack() }
                    }
                },
                enabled = !vm.busy && current.isNotBlank() && new.isNotBlank() && confirm.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Simpan Password Baru")
            }
        }
    }
}

// ============================================================ History

@Composable
fun HistoryScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadHistory() }
    ScreenScaffold("Riwayat 30 Hari", onBack = onBack) { mod ->
        StateHost(
            vm.historyState, onRetry = { vm.loadHistory() },
            emptyText = "Belum ada riwayat catatan.", modifier = mod.fillMaxSize(),
        ) { h ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (h.medication.isNotEmpty()) item {
                    HistorySection("Konsumsi Obat", h.medication.map {
                        it.logDate.take(10) to buildString {
                            append(if (it.takenComplete) "Lengkap" else "Tidak lengkap")
                            if (it.takenOnTime) append(" · tepat waktu")
                        }
                    })
                }
                if (h.activity.isNotEmpty()) item {
                    HistorySection("Aktivitas Fisik", h.activity.map {
                        it.logDate.take(10) to buildString {
                            append(if (it.didActivity) "Aktif" else "Tidak aktif")
                            if (it.perDoctorAdvice) append(" · sesuai anjuran")
                        }
                    })
                }
                if (h.diet.isNotEmpty()) item {
                    HistorySection("Catatan Diet", h.diet.map {
                        it.logDate.take(10) to buildString {
                            append(if (it.perDoctorAdvice) "Sesuai anjuran" else "Belum sesuai")
                            if (it.onSchedule) append(" · tepat jadwal")
                        }
                    })
                }
                if (h.glucose.isNotEmpty()) item {
                    HistorySection("Gula Darah", h.glucose.map {
                        it.measuredAt.take(10) to "${it.valueMgdl} mg/dL · ${timingText(it.timing)}"
                    })
                }
            }
        }
    }
}

private fun timingText(t: String): String = when (t) {
    "before_meal" -> "sebelum makan"
    "after_meal" -> "sesudah makan"
    else -> t
}

@Composable
private fun HistorySection(title: String, rows: List<Pair<String, String>>) {
    Column {
        Eyebrow(title)
        Spacer(Modifier.height(8.dp))
        SebayaCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                rows.forEach { (date, detail) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
                        Text(detail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
