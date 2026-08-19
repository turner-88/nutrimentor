package com.sebaya.dm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sebaya.dm.data.RegisterRequest

@Composable
fun LoginScreen(vm: AppViewModel, onLoggedIn: () -> Unit, onRegister: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SebayaDM", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Masuk untuk melanjutkan", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        vm.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.login(username.trim(), password) { onLoggedIn() } },
            enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            if (vm.busy) CircularProgressIndicator(Modifier.height(18.dp)) else Text("Masuk")
        }
        TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Belum punya akun? Daftar")
        }
    }
}

@Composable
fun RegisterScreen(vm: AppViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    var nama by remember { mutableStateOf("") }
    var usia by remember { mutableStateOf("") }
    var jk by remember { mutableStateOf("L") }
    var pendidikan by remember { mutableStateOf("") }
    var pekerjaan by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Daftar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(nama, { nama = it }, label = { Text("Nama Lengkap") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(usia, { usia = it.filter(Char::isDigit) }, label = { Text("Usia") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Row {
            listOf("L" to "Laki-laki", "P" to "Perempuan").forEach { (v, lbl) ->
                TextButton(onClick = { jk = v }) {
                    Text(if (jk == v) "● $lbl" else "○ $lbl")
                }
            }
        }
        OutlinedTextField(pendidikan, { pendidikan = it }, label = { Text("Pendidikan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pekerjaan, { pekerjaan = it }, label = { Text("Pekerjaan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                vm.register(
                    RegisterRequest(nama.trim(), usia.toIntOrNull() ?: 0, jk, pendidikan.trim(), pekerjaan.trim(), username.trim(), password),
                ) { onDone() }
            },
            enabled = !vm.busy, modifier = Modifier.fillMaxWidth(),
        ) { Text("Daftar") }
        TextButton(onClick = onBack) { Text("Kembali") }
    }
}

@Composable
private fun Row(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() },
    )
}
