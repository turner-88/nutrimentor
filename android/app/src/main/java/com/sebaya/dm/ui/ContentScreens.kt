package com.sebaya.dm.ui

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.sebaya.dm.data.ArticleSummary
import com.sebaya.dm.ui.components.PullRefresh
import com.sebaya.dm.ui.components.ScreenScaffold
import com.sebaya.dm.ui.components.SebayaCard
import com.sebaya.dm.ui.components.StatPill
import com.sebaya.dm.ui.components.StateHost

@Composable
fun EducationScreen(vm: AppViewModel, onOpen: (String) -> Unit, onOpenSingle: (String) -> Unit) {
    LaunchedEffect(Unit) { vm.loadArticles() }
    // With exactly one article, skip the single-card list and open it directly.
    val state = vm.articlesState
    LaunchedEffect(state) {
        (state as? UiState.Success)?.data?.singleOrNull()?.let { onOpenSingle(it.slug) }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Edukasi Diabetes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Bacaan untuk hidup sehat bersama diabetes", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        PullRefresh(
            isRefreshing = vm.refreshingArticles,
            onRefresh = { vm.loadArticles(refresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            StateHost(
                vm.articlesState, onRetry = { vm.loadArticles() },
                emptyText = "Belum ada artikel.", modifier = Modifier.fillMaxSize(),
            ) { articles ->
                EducationList(articles, onOpen)
            }
        }
    }
}

@Composable
private fun EducationList(articles: List<ArticleSummary>, onOpen: (String) -> Unit) {
    val categories = remember(articles) { articles.map { it.category }.filter { it.isNotBlank() }.distinct() }
    var filter by remember { mutableStateOf<String?>(null) }
    val shown = remember(articles, filter) { filter?.let { f -> articles.filter { it.category == f } } ?: articles }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (categories.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Semua") })
                    categories.forEach { c ->
                        FilterChip(selected = filter == c, onClick = { filter = c }, label = { Text(c) })
                    }
                }
            }
        }
        items(shown) { a -> ArticleCard(a, onOpen) }
    }
}

@Composable
private fun ArticleCard(a: ArticleSummary, onOpen: (String) -> Unit) {
    SebayaCard(Modifier.fillMaxWidth(), onClick = { onOpen(a.slug) }) {
        Column {
            if (a.coverImagePath.isNotBlank()) {
                AsyncImage(
                    model = imageUrl(a.coverImagePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
            Column(Modifier.padding(16.dp)) {
                if (a.category.isNotBlank()) {
                    StatPill(a.category)
                    Spacer(Modifier.height(8.dp))
                }
                Text(a.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (a.excerpt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        a.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun ArticleScreen(vm: AppViewModel, slug: String, onBack: () -> Unit) {
    LaunchedEffect(slug) { vm.loadArticle(slug) }
    ScreenScaffold(title = "Artikel", onBack = onBack) { m ->
        StateHost(vm.articleState, onRetry = { vm.loadArticle(slug) }, modifier = m.fillMaxSize()) { a ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (a.coverImagePath.isNotBlank()) {
                    // Full width, height from the cover's real pixel ratio (sent by the
                    // API) so the whole image is visible, never cropped.
                    val ratio = if (a.coverWidth > 0 && a.coverHeight > 0) {
                        a.coverWidth.toFloat() / a.coverHeight
                    } else {
                        16f / 9f
                    }
                    AsyncImage(
                        model = imageUrl(a.coverImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                    )
                }
                Column(Modifier.padding(16.dp)) {
                    if (a.category.isNotBlank()) {
                        StatPill(a.category)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(a.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    HtmlBody(a.bodyHtml)
                }
            }
        }
    }
}

/** Renders sanitized article HTML in a themed TextView. */
@Composable
private fun HtmlBody(html: String) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = 16f
                setLineSpacing(0f, 1.35f)
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) },
        modifier = Modifier.fillMaxWidth(),
    )
}
