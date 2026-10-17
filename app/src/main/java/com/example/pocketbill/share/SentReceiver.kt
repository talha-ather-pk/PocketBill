package com.example.pocketbill.share

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.pocketbill.app.PocketBillApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The share sheet calls this with the app the user picked (UX_SPEC 4 "Recording that it was
 * sent"). Android doesn't say whether the message was actually sent, so the invoice shows
 * "Sent via WhatsApp" with Undo.
 */
class SentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val invoiceId = intent.getLongExtra(EXTRA_INVOICE_ID, -1)
        val chosen = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
        }
        if (invoiceId < 0 || chosen == null) return
        val container = (context.applicationContext as PocketBillApplication).container
        val via = ShareIntents.appLabel(context, chosen.packageName)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.invoices.recordSent(invoiceId, via, container.clock.nowMillis())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_INVOICE_ID = "com.example.pocketbill.INVOICE_ID"
    }
}
