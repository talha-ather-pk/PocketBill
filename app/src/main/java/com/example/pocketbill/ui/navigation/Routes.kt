package com.example.pocketbill.ui.navigation

import kotlinx.serialization.Serializable

// Type-safe navigation routes from UX_SPEC.md section 1.
// Optional IDs use NO_ID (0) for "none" instead of null: Room IDs start at 1, and plain Long
// arguments are the most reliable kind in Navigation's type-safe API.

/** Means "no ID given" in an optional ID argument. */
const val NO_ID: Long = 0

/** First launch only, while no business profile exists (P4-T04). */
@Serializable data object Setup

// The four tab start screens.
@Serializable data object Home
@Serializable data class InvoiceList(val filter: InvoiceFilter = InvoiceFilter.ALL)
@Serializable data object ClientList
@Serializable data object Settings

@Serializable data class InvoiceDetail(val invoiceId: Long)

/** New invoice (all IDs NO_ID), edit [invoiceId], duplicate [duplicateOf], or new for [clientId]. */
@Serializable data class InvoiceEditor(
    val invoiceId: Long = NO_ID,
    val duplicateOf: Long = NO_ID,
    val clientId: Long = NO_ID,
)

@Serializable data class PdfPreview(val invoiceId: Long)
@Serializable data class ClientDetail(val clientId: Long)

/** New client ([clientId] = NO_ID) or edit. [returnToEditor]: select the client in the invoice editor afterwards. */
@Serializable data class ClientEditor(val clientId: Long = NO_ID, val returnToEditor: Boolean = false)

@Serializable data object BusinessProfile
@Serializable data object Presets
@Serializable data object Backup
@Serializable data class RestoreConfirm(val fileUri: String)

/** Invoice list filters (UX_SPEC.md section 5). */
enum class InvoiceFilter { ALL, DRAFTS, UNPAID, OVERDUE, PAID, VOID }
