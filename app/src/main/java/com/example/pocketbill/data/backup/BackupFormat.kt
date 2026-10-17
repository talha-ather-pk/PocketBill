package com.example.pocketbill.data.backup

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The backup file (MVP_SPEC 7, DATA_MODEL 7.4): one human-readable JSON file with every table, so
 * users own their data even without the app. [formatVersion] is separate from the database
 * version. Field names match the database columns and must never be renamed; add fields only
 * with defaults, and bump [BackupFormat.CURRENT_VERSION] with an upgrade step when the shape changes.
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val app: String = BackupFormat.APP,
    val appVersion: String = "",
    val createdAt: Long = 0,
    val databaseVersion: Int = 0,
    val businessProfile: BackupProfile? = null,
    val clients: List<BackupClient> = emptyList(),
    val invoices: List<BackupInvoice> = emptyList(),
    val lineItems: List<BackupLineItem> = emptyList(),
    val itemPresets: List<BackupPreset> = emptyList(),
    /** The current business logo as base64 PNG (F-14), or null. */
    val logo: String? = null,
)

@Serializable
data class BackupProfile(
    val uuid: String,
    val name: String,
    val contactLines: String = "",
    val address: String = "",
    val taxId: String? = null,
    val paymentInstructions: String = "",
    val defaultCurrency: String,
    val defaultTermsDays: Int = 14,
    val defaultTaxRateBp: Int = 0,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
    val paperSize: String = "A4",
    val lastBackupAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupClient(
    val id: Long,
    val uuid: String,
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val country: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    val preferredCurrency: String? = null,
    val preferredTermsDays: Int? = null,
    val archived: Boolean = false,
    val lastInvoicedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupInvoice(
    val id: Long,
    val uuid: String,
    val number: String? = null,
    val status: String,
    val clientId: Long? = null,
    val currency: String,
    val issueDate: Long,
    val dueDate: Long,
    val paidDate: Long? = null,
    val taxRateBp: Int = 0,
    val discountType: String? = null,
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

@Serializable
data class BackupLineItem(
    val id: Long,
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

@Serializable
data class BackupPreset(
    val id: Long,
    val uuid: String,
    val description: String,
    val unitPriceMinor: Long? = null,
    val currency: String? = null,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** What reading a file gave: a file to validate, or why it couldn't be read. */
sealed interface BackupRead {
    data class Ok(val file: BackupFile) : BackupRead

    /** "This backup was made by a newer PocketBill. Please update the app first." (DATA_MODEL 7.4) */
    data class Newer(val formatVersion: Int) : BackupRead

    /** Not a PocketBill backup, or damaged. */
    data object Unreadable : BackupRead
}

object BackupFormat {
    const val CURRENT_VERSION = 1
    const val APP = "PocketBill"

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    /**
     * Checks the version before decoding the rest, so a newer file gets the right message.
     * Older versions would be upgraded here step by step (v1 → v2 → …) before validation.
     */
    fun decode(text: String): BackupRead = try {
        val root = Json.parseToJsonElement(text).jsonObject
        val version = root["formatVersion"]?.jsonPrimitive?.int ?: return BackupRead.Unreadable
        when {
            version > CURRENT_VERSION -> BackupRead.Newer(version)
            version < 1 -> BackupRead.Unreadable
            else -> BackupRead.Ok(json.decodeFromString(BackupFile.serializer(), text))
        }
    } catch (e: SerializationException) {
        BackupRead.Unreadable
    } catch (e: IllegalArgumentException) {
        BackupRead.Unreadable
    }

    /** "PocketBill-backup-2026-10-15.json" (MVP_SPEC 7). */
    fun fileName(date: java.time.LocalDate): String = "PocketBill-backup-$date.json"

    // ---- Entity ↔ file ------------------------------------------------------------------------------

    fun from(
        profile: BusinessProfileEntity?,
        clients: List<ClientEntity>,
        invoices: List<InvoiceEntity>,
        lines: List<LineItemEntity>,
        presets: List<ItemPresetEntity>,
        appVersion: String,
        databaseVersion: Int,
        // Not "createdAt": inside run/with below, that name would hide each record's own createdAt.
        madeAt: Long,
    ) = BackupFile(
        formatVersion = CURRENT_VERSION,
        appVersion = appVersion,
        createdAt = madeAt,
        databaseVersion = databaseVersion,
        businessProfile = profile?.run {
            BackupProfile(
                uuid, name, contactLines, address, taxId, paymentInstructions, defaultCurrency, defaultTermsDays,
                defaultTaxRateBp, invoicePrefix, nextInvoiceNumber, paperSize.name, lastBackupAt, createdAt, updatedAt,
            )
        },
        clients = clients.sortedBy { it.id }.map {
            with(it) {
                BackupClient(
                    id, uuid, name, email, address, country, taxId, notes, preferredCurrency, preferredTermsDays,
                    archived, lastInvoicedAt, createdAt, updatedAt,
                )
            }
        },
        invoices = invoices.sortedBy { it.id }.map {
            with(it) {
                BackupInvoice(
                    id, uuid, number, status.name, clientId, currency, issueDate, dueDate, paidDate, taxRateBp,
                    discountType?.name, discountValue, notes, subtotalMinor, discountMinor, taxMinor, totalMinor,
                    fromSnapshot, toSnapshot, lastSentAt, lastSentVia, createdAt, updatedAt,
                )
            }
        },
        lineItems = lines.sortedWith(compareBy({ it.invoiceId }, { it.position })).map {
            with(it) { BackupLineItem(id, uuid, invoiceId, position, description, quantityMilli, unitPriceMinor, amountMinor, createdAt, updatedAt) }
        },
        itemPresets = presets.sortedBy { it.id }.map {
            with(it) { BackupPreset(id, uuid, description, unitPriceMinor, currency, useCount, lastUsedAt, createdAt, updatedAt) }
        },
    )

    /** Only call on a file that passed [BackupValidator]: enum names are trusted here. */
    fun toEntities(file: BackupFile): BackupEntities = BackupEntities(
        profile = file.businessProfile?.run {
            BusinessProfileEntity(
                uuid = uuid, name = name, contactLines = contactLines, address = address, taxId = taxId,
                paymentInstructions = paymentInstructions,
                // Set by the restore once the logo file from [BackupFile.logo] is written.
                logoFile = null,
                defaultCurrency = defaultCurrency, defaultTermsDays = defaultTermsDays, defaultTaxRateBp = defaultTaxRateBp,
                invoicePrefix = invoicePrefix, nextInvoiceNumber = nextInvoiceNumber, paperSize = PaperSize.valueOf(paperSize),
                lastBackupAt = lastBackupAt, createdAt = createdAt, updatedAt = updatedAt,
            )
        },
        clients = file.clients.map {
            with(it) {
                ClientEntity(
                    id, uuid, name, email, address, country, taxId, notes, preferredCurrency, preferredTermsDays,
                    archived, lastInvoicedAt, createdAt, updatedAt,
                )
            }
        },
        invoices = file.invoices.map {
            with(it) {
                InvoiceEntity(
                    id, uuid, number, InvoiceStatus.valueOf(status), clientId, currency, issueDate, dueDate, paidDate,
                    taxRateBp, discountType?.let(DiscountType::valueOf), discountValue, notes, subtotalMinor,
                    discountMinor, taxMinor, totalMinor, fromSnapshot, toSnapshot, lastSentAt, lastSentVia, createdAt, updatedAt,
                )
            }
        },
        lines = file.lineItems.map {
            with(it) { LineItemEntity(id, uuid, invoiceId, position, description, quantityMilli, unitPriceMinor, amountMinor, createdAt, updatedAt) }
        },
        presets = file.itemPresets.map {
            with(it) { ItemPresetEntity(id, uuid, description, unitPriceMinor, currency, useCount, lastUsedAt, createdAt, updatedAt) }
        },
    )
}

data class BackupEntities(
    val profile: BusinessProfileEntity?,
    val clients: List<ClientEntity>,
    val invoices: List<InvoiceEntity>,
    val lines: List<LineItemEntity>,
    val presets: List<ItemPresetEntity>,
)
