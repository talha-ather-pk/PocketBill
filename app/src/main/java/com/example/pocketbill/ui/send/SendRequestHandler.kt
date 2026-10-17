package com.example.pocketbill.ui.send

import android.content.ActivityNotFoundException
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.pocketbill.R
import com.example.pocketbill.share.SendPackage
import com.example.pocketbill.share.ShareIntents
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
                    Log.e("SavePdf", "Saving the PDF failed", e)
                    false
                } catch (e: SecurityException) {
                    Log.e("SavePdf", "Saving the PDF failed", e)
                    false
                }
            }
            flow.onSaveResult(ok)
        }
    }

    LaunchedEffect(request) {
        when (request) {
            null -> Unit
            is SendRequest.Email -> {
                val apps = ShareIntents.emailAppLabels(context)
                if (apps.isEmpty()) {
                    flow.onNoEmailApp(request.pkg)
                    return@LaunchedEffect
                }
                try {
                    context.startActivity(ShareIntents.email(request.pkg))
                    flow.onEmailOpened(apps.singleOrNull())
                    flow.onRequestHandled()
                } catch (e: ActivityNotFoundException) {
                    flow.onNoEmailApp(request.pkg)
                }
            }
            is SendRequest.Other -> {
                try {
                    context.startActivity(ShareIntents.chooser(context, request.pkg, chooserTitle))
                } catch (e: ActivityNotFoundException) {
                    Log.e("Send", "No share sheet", e)
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

/** The snackbar text for a [SendMessage]. */
@Composable
fun sendMessageText(message: SendMessage): String = when (message) {
    is SendMessage.Sent -> message.via?.let { stringResource(R.string.send_msg_sent_via, it) } ?: stringResource(R.string.send_msg_sent)
    SendMessage.NoEmailApp -> stringResource(R.string.send_msg_no_email_app)
    SendMessage.Saved -> stringResource(R.string.send_msg_saved)
    SendMessage.SaveFailed -> stringResource(R.string.send_msg_save_failed)
    SendMessage.PdfFailed -> stringResource(R.string.pdf_failed)
}
