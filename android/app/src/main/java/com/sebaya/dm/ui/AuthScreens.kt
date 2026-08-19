package com.sebaya.dm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sebaya.dm.data.RegisterRequest
import com.sebaya.dm.ui.components.BrandMark
import com.sebaya.dm.ui.components.ScreenScaffold
import com.sebaya.dm.ui.theme.Emerald
import com.sebaya.dm.ui.theme.EmeraldDark

@Composable
fun LoginScreen(vm: AppViewModel, onLoggedIn: () -> Unit, onRegister: () -> Unit, onForgot: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        BrandHero()
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text("Masuk", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Masuk untuk melanjutkan pemantauan harian", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                password, { password = it }, label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { vm.login(username.trim(), password) { onLoggedIn() } },
                enabled = !vm.busy && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Masuk")
            }
            TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Lupa password?")
            }
            TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Belum punya akun? Daftar")
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(vm: AppViewModel, onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sentMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Lupa Password", onBack = onBack) { mod ->
        Column(mod.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
            Text(
                "Masukkan email terdaftar Anda. Kami akan mengirim tautan untuk membuat password baru.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(20.dp))

            if (sentMessage != null) {
                Text(sentMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Kembali ke Masuk") }
            } else {
                OutlinedTextField(
                    email, { email = it }, label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { vm.forgotPassword(email) { msg -> sentMessage = msg } },
                    enabled = !vm.busy && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Kirim Tautan Reset")
                }
            }
        }
    }
}

@Composable
private fun BrandHero() {
    Box(
        Modifier.fillMaxWidth().height(220.dp)
            .background(Brush.linearGradient(listOf(Emerald, EmeraldDark))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(size = 64.dp, background = SolidColor(Color.White.copy(alpha = 0.18f)))
            Spacer(Modifier.height(14.dp))
            Text("SebayaDM", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Sehat bersama kelompok sebaya", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var nama by remember { mutableStateOf("") }
    var usia by remember { mutableStateOf("") }
    var jk by remember { mutableStateOf("L") }
    var pendidikan by remember { mutableStateOf("") }
    var pekerjaan by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val emailValid = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    ScreenScaffold(title = "Buat Akun", onBack = onBack) { mod ->
    Column(
        mod.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Daftarkan diri Anda untuk mulai memantau kesehatan", style = MaterialTheme.typography.bodySmall)

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
            email, { email = it }, label = { Text("Email (opsional)") }, singleLine = true,
            isError = !emailValid,
            supportingText = if (!emailValid) ({ Text("Format email tidak valid.") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password, { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
        )
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

        Button(
            onClick = {
                vm.register(
                    RegisterRequest(nama.trim(), usia.toIntOrNull() ?: 0, jk, pendidikan.trim(), pekerjaan.trim(), username.trim(), password, email.trim()),
                ) { onDone() }
            },
            enabled = !vm.busy && nama.isNotBlank() && username.isNotBlank() && password.isNotBlank() && emailValid,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (vm.busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Text("Daftar")
        }
    }
    }
}
