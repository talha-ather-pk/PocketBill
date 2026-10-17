package com.example.pocketbill.share

import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.edit
import androidx.core.net.toUri

/**
 * Builds the intents for sending (UX_SPEC 4, ARCHITECTURE "share"): read access is granted to the
 * chosen app only, and nothing here changes data. Recording "Sent" is the caller's job.
 */
object ShareIntents {
    private const val PDF = "application/pdf"
    private val MAILTO: Uri = "mailto:".toUri()

    /** An installed app that can send email. */
    data class EmailApp(val label: String, val packageName: String)

    /** Apps that handle mailto: links (Gmail, Outlook, …), sorted by name. */
    fun emailApps(context: Context): List<EmailApp> {
        val pm = context.packageManager
        return pm.queryIntentActivities(Intent(Intent.ACTION_SENDTO, MAILTO), PackageManager.MATCH_DEFAULT_ONLY)
            .map { EmailApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * ACTION_SEND sent straight to one email app, with address, subject, message and PDF filled in.
     * Explicit on purpose: Android 14's share sheet ignores a mailto: selector and shows every app
     * and contact, which is the confusion "Email to client" exists to avoid (UX_SPEC 4, C2).
     */
    fun email(pkg: SendPackage, app: EmailApp): Intent = send(pkg).apply {
        pkg.toEmail?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
        setPackage(app.packageName)
    }

    private const val PREFS = "send"
    private const val KEY_EMAIL_APP = "emailApp"

    /** The email app chosen last time when several are installed, so the next send is one tap. */
    fun rememberedEmailApp(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL_APP, null)

    fun rememberEmailApp(context: Context, packageName: String?) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_EMAIL_APP, packageName) }

    /** "Send another way": the standard share sheet, which tells [SentReceiver] which app was picked. */
    fun chooser(context: Context, pkg: SendPackage, title: String): Intent {
        val callback = Intent(context, SentReceiver::class.java).putExtra(SentReceiver.EXTRA_INVOICE_ID, pkg.invoiceId)
        // Mutable so the system can add the chosen app; the intent is explicit, so only our receiver gets it.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val pending = PendingIntent.getBroadcast(context, pkg.invoiceId.toInt(), callback, flags)
        return Intent.createChooser(send(pkg), title, pending.intentSender)
    }

    private fun send(pkg: SendPackage): Intent {
        val uri = pkg.fileUri.toUri()
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
