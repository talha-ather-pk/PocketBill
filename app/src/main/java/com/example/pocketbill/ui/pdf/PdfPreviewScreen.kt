package com.example.pocketbill.ui.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Longest side, in pixels, of a preview page image; keeps memory low on long invoices. */
private const val MAX_PAGE_WIDTH_PX = 1080

/** UX_SPEC 4: show a progress indicator only if generating takes longer than this. */
private const val PROGRESS_DELAY_MS = 300L

/**
 * PDF preview (MVP_SPEC 6): the real PDF's pages, generated from the saved invoice.
 * Send and Save PDF join this screen in P4-T09.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(invoiceId: Long, onBack: () -> Unit) {
    val c = appContainer()
    val widthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
        .coerceAtMost(MAX_PAGE_WIDTH_PX)
    val vm: PdfPreviewViewModel = viewModel {
        PdfPreviewViewModel(c.invoices, c.profile, c.clients, c.pdfRenderer, invoiceId, widthPx)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val ready = state as? PdfPreviewUiState.Ready

    Scaffold(
        contentWindowInsets = WindowInsets(0), // the app's outer Scaffold handles the status bar
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(ready?.title ?: stringResource(R.string.title_pdf_preview)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                PdfPreviewUiState.Loading -> DelayedProgress()
                PdfPreviewUiState.Failed -> Message(stringResource(R.string.pdf_failed)) {
                    Button(onClick = vm::onRetry) { Text(stringResource(R.string.pdf_try_again)) }
                }
                PdfPreviewUiState.Missing -> Message(stringResource(R.string.pdf_missing)) {}
                is PdfPreviewUiState.Ready -> Pages(s)
            }
        }
    }
}

@Composable
private fun Pages(state: PdfPreviewUiState.Ready) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.screen),
    ) {
        if (state.isDraft) {
            item {
                Text(
                    stringResource(R.string.pdf_draft_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.m),
                )
            }
        }
        itemsIndexed(state.pages) { index, page ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = if (index == 0 && !state.isDraft) Spacing.m else 0.dp)) {
                val label = stringResource(R.string.pdf_page_label, index + 1, state.pages.size)
                Image(
                    bitmap = page.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(page.width.toFloat() / page.height)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant),
                )
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Spacing.xs))
            }
        }
    }
}

@Composable
private fun DelayedProgress() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(PROGRESS_DELAY_MS)
        visible = true
    }
    if (!visible) return
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
        Text(stringResource(R.string.pdf_creating), modifier = Modifier.padding(top = Spacing.m))
    }
}

@Composable
private fun Message(text: String, action: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        action()
    }
}
