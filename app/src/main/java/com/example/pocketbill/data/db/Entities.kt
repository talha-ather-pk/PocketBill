package com.example.pocketbill.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize

// Room schema version 1 (DATA_MODEL.md section 1). Conventions: money in minor units (Long),
// quantity in thousandths, percentages in basis points, calendar dates as epoch days,
// timestamps as epoch ms (UTC). Every table has a uuid that never changes (D-010: sync-ready).
// Enums are stored by name. Invariants I-1…I-10 are enforced by the repositories.

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Long = SINGLE_ROW_ID,
    val uuid: String,
    val name: String,
    val contactLines: String = "",
    val address: String = "",
    val taxId: String? = null,
    val paymentInstructions: String = "",
    val logoFile: String? = null,
    val defaultCurrency: String,
    val defaultTermsDays: Int = 14,
    val defaultTaxRateBp: Int = 0,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
    val paperSize: PaperSize = PaperSize.A4,
    val lastBackupAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        /** The profile is a single row (invariant I-10). */
        const val SINGLE_ROW_ID = 1L
    }
}

@Entity(
    tableName = "client",
    indices = [Index(value = ["uuid"], unique = true), Index("archived"), Index("name")],
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val country: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    /** Optional: currency for new invoices to this client (D-015). Null = no preference. */
    val preferredCurrency: String? = null,
    /** Optional: payment terms in days for new invoices to this client (D-015). */
    val preferredTermsDays: Int? = null,
    val archived: Boolean = false,
    val lastInvoicedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "invoice",
    foreignKeys = [
        ForeignKey(
            entity = ClientEntity::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["number"], unique = true), // NULL for drafts; SQLite allows many NULLs
        Index("clientId"),
        Index("status"),
        Index("dueDate"),
    ],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val number: String? = null,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    /** Null only while DRAFT: autosaved drafts may not have a client yet (invariant I-11). */
    val clientId: Long? = null,
    val currency: String,
    val issueDate: Long,
    val dueDate: Long,
    val paidDate: Long? = null,
    val taxRateBp: Int = 0,
    val discountType: DiscountType? = null,
    val discountValue: Long? = null,
    val notes: String = "",
    val subtotalMinor: Long = 0,
    val discountMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val fromSnapshot: String? = null,
    val toSnapshot: String? = null,
    val lastSentAt: Long? = null,
    val lastSentVia: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "line_item",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["uuid"], unique = true), Index("invoiceId")],
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val invoiceId: Long,
    val position: Int,
    val description: String,
    val quantityMilli: Long,
    val unitPriceMinor: Long,
    val amountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "item_preset",
    indices = [Index(value = ["uuid"], unique = true), Index("description")],
)
data class ItemPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val description: String,
    val unitPriceMinor: Long? = null,
    val currency: String? = null,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** An invoice with its line items in display order. */
data class InvoiceWithLines(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "invoiceId")
    val unorderedLines: List<LineItemEntity>,
) {
    val lines: List<LineItemEntity> get() = unorderedLines.sortedBy { it.position }
}

/** What one client owes in one currency, and how many unpaid invoices make it up (UX_SPEC 6). */
data class ClientOwed(val clientId: Long, val currency: String, val totalMinor: Long, val invoiceCount: Int)

/** A count per client, e.g. overdue invoices. */
data class ClientCount(val clientId: Long, val count: Int)

/** Sum of amounts in one currency (outstanding totals are never added across currencies). */
data class CurrencyTotal(val currency: String, val totalMinor: Long)
