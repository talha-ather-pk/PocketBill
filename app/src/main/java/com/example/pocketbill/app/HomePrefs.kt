package com.example.pocketbill.app

import android.content.Context
import androidx.core.content.edit

/** When Home's dismissible cards may show again (UX_SPEC 2). Phone preferences, not invoice data. */
interface HomeCardPrefs {
    var backupCardHiddenUntil: Long

    /** When payment details changed and unpaid invoices were updated (DATA_MODEL 5.5); 0 = no card. */
    var paymentDetailsChangedAt: Long
}

class HomePrefs(context: Context) : HomeCardPrefs {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    override var backupCardHiddenUntil: Long
        get() = prefs.getLong(KEY_BACKUP_HIDDEN_UNTIL, 0)
        set(value) = prefs.edit { putLong(KEY_BACKUP_HIDDEN_UNTIL, value) }

    override var paymentDetailsChangedAt: Long
        get() = prefs.getLong(KEY_PAYMENT_CHANGED_AT, 0)
        set(value) = prefs.edit { putLong(KEY_PAYMENT_CHANGED_AT, value) }

    private companion object {
        const val KEY_BACKUP_HIDDEN_UNTIL = "backupCardHiddenUntil"
        const val KEY_PAYMENT_CHANGED_AT = "paymentDetailsChangedAt"
    }
}
