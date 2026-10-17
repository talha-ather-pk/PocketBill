package com.example.pocketbill.ui.send

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.pocketbill.app.SafeLog
import com.example.pocketbill.R
import com.example.pocketbill.share.SendPackage
import com.example.pocketbill.share.ShareIntents
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Starts the Activity a [SendRequest] needs, then reports back to [flow] (UX_SPEC 4). */
@Composable
fun SendRequestHandler(request: SendRequest?, flow: InvoiceSendFlow) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooserTitle = request?.let { stringResource(R.string.send_chooser_title, it.pkg.title) }.orEmpty()
    var pendingSave by remember { mutableStateOf<SendPackage?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
        val pkg = pendingSave
        pendingSave = null
        if (uri == null || pkg == null) return@rememberLauncherForActivityResult // the user backed out
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out -> File(pkg.filePath).inputStream().use { it.copyTo(out) } } != null
                } catch (e: IOException) {
                    SafeLog.failure("SavePdf", "Saving the PDF failed", e)
                    false
                } catch (e: SecurityException) {
                    SafeLog.failure("SavePdf", "Saving the PDF failed", e)
                    false
                }
            }
            flow.onSaveResult(ok)
        }
    }

    // Several email apps and none remembered: ask once with our own short list (not the share sheet).
    var choose by remember { mutableStateOf<Pair<SendPackage, List<ShareIntents.EmailApp>>?>(null) }

    fun openEmail(pkg: SendPackage, app: ShareIntents.EmailApp) {
        try {
            context.startActivity(ShareIntents.email(pkg, app))
            flow.onEmailOpened(app.label)
            flow.onRequestHandled()
        } catch (e: ActivityNotFoundException) {
            ShareIntents.rememberEmailApp(context, null)
            flow.onNoEmailApp(pkg)
        }
    }

    choose?.let { (pkg, apps) ->
        EmailAppDialog(
            apps,
            onPick = { app ->
                choose = null
                ShareIntents.rememberEmailApp(context, app.packageName)
                openEmail(pkg, app)
            },
            onDismiss = {
                choose = null
                flow.onRequestHandled()
            },
        )
    }

    LaunchedEffect(request) {
        when (request) {
            null -> Unit
            is SendRequest.Email -> {
                val apps = ShareIntents.emailApps(context)
                val remembered = ShareIntents.rememberedEmailApp(context)
                val app = apps.singleOrNull() ?: apps.firstOrNull { it.packageName == remembered }
                when {
                    apps.isEmpty() -> flow.onNoEmailApp(request.pkg)
                    app != null -> openEmail(request.pkg, app)
                    else -> choose = request.pkg to apps
                }
            }
            is SendRequest.Other -> {
                try {
                    context.startActivity(ShareIntents.chooser(context, request.pkg, chooserTitle))
                } catch (e: ActivityNotFoundException) {
                    SafeLog.failure("Send", "No share sheet", e)
                }
                flow.onRequestHandled()
            }
            is SendRequest.Save -> {
                pendingSave = request.pkg
                flow.onRequestHandled()
                saveLauncher.launch(request.pkg.fileName)
            }
        }
    }
}

@Composable
private fun EmailAppDialog(apps: List<ShareIntents.EmailApp>, onPick: (ShareIntents.EmailApp) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_with_title)) },
        text = {
            Column {
                apps.forEach { app ->
                    TextButton(onClick = { onPick(app) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                        Text(app.label, modifier = Modifier.fillMaxWidth())
                    }
                }
                Text(stringResource(R.string.send_with_remember), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** The snackbar text for a [SendMessage]. */
@Composable
fun sendMessageText(message: SendMessage): String = when (message) {
    is SendMessage.Sent -> message.via?.let { stringResource(R.string.send_msg_sent_via, it) } ?: stringResource(R.string.send_msg_sent)
    SendMessage.NoEmailApp -> stringResource(R.string.send_msg_no_email_app)
    SendMessage.Saved -> stringResource(R.string.send_msg_saved)
    SendMessage.SaveFailed -> stringResource(R.string.send_msg_save_failed)
    SendMessage.PdfFailed -> stringResource(R.string.pdf_failed)
}
