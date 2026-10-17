package com.example.pocketbill.share

import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Builds the intents for sending (UX_SPEC 4, ARCHITECTURE "share"): read access is granted to the
 * chosen app only, and nothing here changes data. Recording "Sent" is the caller's job.
 */
object ShareIntents {
    private const val PDF = "application/pdf"
    private val MAILTO: Uri = Uri.parse("mailto:")

    /** Apps that can send email. With exactly one (usually Gmail) it opens directly. */
    fun emailAppLabels(context: Context): List<String> {
        val pm = context.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_SENDTO, MAILTO), PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.loadLabel(pm).toString() }
            .distinct()
    }

    /** ACTION_SEND limited to email apps by a mailto: selector, with address, subject and message prefilled. */
    fun email(pkg: SendPackage): Intent = send(pkg).apply {
        pkg.toEmail?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
        selector = Intent(Intent.ACTION_SENDTO, MAILTO)
    }

    /** "Send another way": the standard share sheet, which tells [SentReceiver] which app was picked. */
    fun chooser(context: Context, pkg: SendPackage, title: String): Intent {
        val callback = Intent(context, SentReceiver::class.java).putExtra(SentReceiver.EXTRA_INVOICE_ID, pkg.invoiceId)
        // Mutable so the system can add the chosen app; the intent is explicit, so only our receiver gets it.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val pending = PendingIntent.getBroadcast(context, pkg.invoiceId.toInt(), callback, flags)
        return Intent.createChooser(send(pkg), title, pending.intentSender)
    }

    private fun send(pkg: SendPackage): Intent {
        val uri = Uri.parse(pkg.fileUri)
        return Intent(Intent.ACTION_SEND).apply {
            type = PDF
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(pkg.fileName, uri)
            if (pkg.subject.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, pkg.subject)
            if (pkg.body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, pkg.body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** "Gmail" for com.google.android.gm; the package name if the label can't be read. */
    fun appLabel(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.')
    }
}
