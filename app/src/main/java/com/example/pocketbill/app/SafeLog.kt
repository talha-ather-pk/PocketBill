package com.example.pocketbill.app

import android.util.Log

/**
 * Error logging without personal data (P5-T06, RELEASE_CHECKLIST "Privacy"): only the kind of
 * error is written, never its message, which can contain file names built from client names.
 */
object SafeLog {
    fun failure(tag: String, what: String, error: Throwable) {
        Log.e(tag, "$what: ${error.javaClass.simpleName}")
    }
}
