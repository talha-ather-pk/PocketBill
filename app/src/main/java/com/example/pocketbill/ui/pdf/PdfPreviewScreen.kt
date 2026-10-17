package com.example.pocketbill.ui.pdf

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.domain.invoice.SendKind
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.send.SendMessage
import com.example.pocketbill.ui.send.SendRequestHandler
import com.example.pocketbill.ui.send.sendMessageText
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Longest side, in pixels, of a preview page image; keeps memory low on long invoices. */
private const val MAX_PAGE_WIDTH_PX = 1080

/** UX_SPEC 4: show a progress indicator only if generating takes longer than this. */
private const val PROGRESS_DELAY_MS = 300L

/**
 * PDF preview (MVP_SPEC 6): the real PDF's pages, generated from the saved invoice, with Send as
 * the primary action and Save PDF beside it (UX_SPEC 4). Drafts are sent from invoice detail
 * ("Issue and send"), so their preview has no actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(invoiceId: Long, onBack: () -> Unit) {
    val c = appContainer()
    val widthPx = LocalWindowInfo.current.containerSize.width.coerceIn(1, MAX_PAGE_WIDTH_PX)
    val vm: PdfPreviewViewModel = viewModel {
        PdfPreviewViewModel(
            invoices = c.invoices, browser = c.invoices, sending = c.invoices, profile = c.profile, clients = c.clients,
            renderer = c.pdfRenderer, preparer = c.sharePreparer, clock = c.clock, invoiceId = invoiceId, pageWidthPx = widthPx,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val actions by vm.actions.collectAsStateWithLifecycle()
    val sendState by vm.send.state.collectAsStateWithLifecycle()
    val ready = state as? PdfPreviewUiState.Ready
    SendRequestHandler(sendState.request, vm.send)

    val snackbar = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.action_undo)
    val sendMessage = sendState.message
    val sendText = sendMessage?.let { sendMessageText(it) }
    LaunchedEffect(sendMessage) {
        if (sendMessage == null || sendText == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = sendText,
            actionLabel = if (sendMessage is SendMessage.Sent) undoLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.send.onUndoSent() else vm.send.onMessageShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0), // the app's outer Scaffold handles the status bar
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (ready != null && (actions.kind != null || actions.canSave)) {
                PreviewActionsBar(actions, busy = sendState.busy, onSend = vm::onSend, onSave = vm::onSave)
            }
        },
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

/** UX_SPEC 4: Send is the primary button on the preview, with Save PDF beside it. */
@Composable
private fun PreviewActionsBar(actions: PreviewActions, busy: Boolean, onSend: () -> Unit, onSave: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.s),
            ) {
                if (actions.canSave) {
                    OutlinedButton(onClick = onSave, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) {
                        Text(stringResource(R.string.send_save_pdf))
                    }
                }
                val kind = actions.kind
                if (kind != null) {
                    val label = when {
                        !actions.hasEmail -> R.string.send_generic
                        kind == SendKind.RECEIPT -> R.string.send_receipt
                        kind == SendKind.REMINDER -> R.string.send_reminder
                        actions.sentBefore -> R.string.send_email_again
                        else -> R.string.send_email_client
                    }
                    Button(onClick = onSend, enabled = !busy, modifier = Modifier.weight(1.4f).heightIn(min = Spacing.buttonHeight)) {
                        Text(stringResource(label))
                    }
                }
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
