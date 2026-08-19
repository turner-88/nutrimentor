package com.sebaya.dm.ui

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@Composable
fun EducationScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    LaunchedEffect(Unit) { vm.loadArticles() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Edukasi Diabetes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (vm.articles.isEmpty()) Text("Belum ada artikel.", color = MaterialTheme.colorScheme.outline)
        LazyColumn {
            items(vm.articles) { a ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        if (a.category.isNotEmpty()) Text(a.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(a.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onOpen(a.slug) }) { Text("Baca") }
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(vm: AppViewModel, slug: String, onBack: () -> Unit) {
    LaunchedEffect(slug) { vm.loadArticle(slug) }
    val a = vm.currentArticle
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Kembali") }
        Text(a?.title ?: "…", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val html = a?.bodyHtml.orEmpty()
        AndroidView(
            factory = { ctx -> TextView(ctx) },
            update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
