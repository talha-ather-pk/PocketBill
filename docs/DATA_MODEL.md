# Data Model (Phase 3)

_Started 2026-10-16. Refines [MVP_SPEC.md](MVP_SPEC.md) section 4 and is authoritative where they differ. Each Phase 3 task adds a section._

| Task | Section | Status |
|---|---|---|
| P3-T01 Room schema | 1–4 | Draft, awaiting founder review |
| P3-T02 Historical snapshots & numbering | 3 (outline) | Outline only; detailed next |
| P3-T03 Money representation & rounding | MVP_SPEC section 5 | Rules written; to be moved here with full test list |
| P3-T04 Layer responsibilities | ARCHITECTURE.md | Not started |
| P3-T05 Migrations | 4 (policy) | Policy only |

---

## 1. Room schema, version 1 (P3-T01)

**Library:** Room 2.8.5 + KSP (D-005). `@Database(version = 1, exportSchema = true)`, with schema JSON in `app/schemas/` committed to Git.

These are **design sketches** of the entity classes. P4-T03 writes, compiles and tests the real code, and may adjust the syntax but not the meaning.

### Shared conventions (from MVP_SPEC section 4)
| What | Type | Rule |
|---|---|---|
| Local ID | `id: Long` | Auto-generated. Used for joins on the device only. **Never written to backups.** |
| Global ID | `uuid: String` | Random UUID v4, set when the row is created, **never changed**, unique index. Backups and any future sync use it (D-010). |
| Money | `…Minor: Long` | Minor units of the invoice currency. Never `Double`. |
| Quantity | `quantityMilli: Long` | Thousandths (`1500` = 1.5) |
| Percent | `…Bp: Int` | Basis points (`750` = 7.5%) |
| Calendar date | `…Date: Long` | `LocalDate.toEpochDay()`. Not affected by time zones. |
| Timestamp | `createdAt`, `updatedAt: Long` | Epoch milliseconds, UTC. `updatedAt` is set on every write by the repository. |
| Enums | Kotlin `enum` | Stored by name (`"ISSUED"`). Renaming an enum value requires a migration. |

### Entities

```kotlin
@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Long = 1,                 // single row
    val uuid: String,
    val name: String,
    val contactLines: String = "",
    val address: String = "",
    val taxId: String? = null,
    val paymentInstructions: String = "",
    val logoPath: String? = null,                 // file in app-private storage (F-14)
    val defaultCurrency: String,                  // ISO 4217
    val defaultTermsDays: Int = 14,
    val defaultTaxRateBp: Int = 0,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
    val paperSize: PaperSize = PaperSize.A4,
    val lastBackupAt: Long? = null,               // drives the backup reminder (MVP_SPEC section 7)
    val createdAt: Long,
    val updatedAt: Long,
)

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
    val archived: Boolean = false,
    val lastInvoicedAt: Long? = null,             // sorts "recent clients" chips (UX_SPEC 1)
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "invoice",
    foreignKeys = [ForeignKey(
        entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["number"], unique = true), // NULLs allowed (drafts); SQLite treats them as distinct
        Index("clientId"), Index("status"), Index("dueDate"),
    ],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val number: String? = null,                   // assigned when issued
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val clientId: Long,
    val currency: String,
    val issueDate: Long,
    val dueDate: Long,
    val paidDate: Long? = null,
    val taxRateBp: Int = 0,
    val discountType: DiscountType? = null,       // F-15
    val discountValue: Long? = null,              // bp for PERCENT, minor units for AMOUNT
    val notes: String = "",
    val subtotalMinor: Long = 0,                  // stored results of MVP_SPEC section 5
    val discountMinor: Long = 0,
    val taxMinor: Long = 0,
    val totalMinor: Long = 0,
    val fromSnapshot: String? = null,             // JSON, frozen when issued (section 3)
    val toSnapshot: String? = null,
    val lastSentAt: Long? = null,                 // F-06
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "line_item",
    foreignKeys = [ForeignKey(
        entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoiceId"],
        onDelete = ForeignKey.CASCADE,
    )],
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

enum class InvoiceStatus { DRAFT, ISSUED, PAID, VOID }   // OVERDUE is calculated, never stored
enum class DiscountType { PERCENT, AMOUNT }
enum class PaperSize { A4, LETTER }
```

Changes from MVP_SPEC section 4 (all additive): `client.lastInvoicedAt` (recent-client chips), `business_profile.lastBackupAt` (backup reminder), and `uuid`/`createdAt`/`updatedAt` written out on every table.

## 2. Invariants

Room can't express SQL `CHECK` constraints through annotations, so the **repository enforces these, and unit tests cover each one**:

| # | Rule |
|---|---|
| I-1 | `number` is null **if and only if** status = DRAFT. |
| I-2 | `paidDate` is non-null **if and only if** status = PAID. |
| I-3 | `fromSnapshot` and `toSnapshot` are non-null for every non-DRAFT invoice. |
| I-4 | `dueDate ≥ issueDate`. |
| I-5 | Every invoice has ≥ 1 line item before it can be issued. |
| I-6 | `quantityMilli > 0`, `unitPriceMinor ≥ 0`, `taxRateBp` in 0..100_000 (0–1000%), percent discount in 0..10_000. |
| I-7 | Stored totals equal a fresh recalculation from the line items (checked in tests and on backup restore). |
| I-8 | `currency` is a valid ISO 4217 code (`java.util.Currency.getInstance` succeeds). |
| I-9 | PAID and VOID invoices are read-only except for status changes (MVP_SPEC "Editing rules"). |
| I-10 | `business_profile` has exactly one row once setup is complete. |

## 3. Operations that must be atomic (one `@Transaction` each)

| Operation | Steps inside the transaction |
|---|---|
| **Save draft** | Upsert the invoice, replace its line items, recalculate and store the totals. |
| **Issue invoice** | Check I-4/I-5 → take `nextInvoiceNumber` → format `prefix + n.padStart(4,'0')` → if that number already exists (the prefix was changed back), increment until free → set number, status ISSUED, snapshots, totals → `nextInvoiceNumber = n + 1` → `client.lastInvoicedAt = now`. |
| **Edit issued invoice** | Same as Save draft, but keeps the number. Snapshots are **refreshed only if the user chooses** "Update client details on this invoice" (detailed in P3-T02). |
| **Mark paid / unpaid** | Status plus `paidDate`. |
| **Void** | Status VOID. The number stays reserved forever. |
| **Restore backup** | Validate the whole file **before** the transaction → delete all rows → insert everything, mapping backup UUIDs to new local IDs → recheck I-1…I-10 → commit. Any failure rolls back everything (MVP_SPEC section 7). |

**Snapshot JSON (outline for P3-T02):** `{"name","contactLines","address","taxId","paymentInstructions","logoPath"}` for the sender and `{"name","email","address","country","taxId"}` for the client. The format is versioned with a `"v": 1` field.

## 4. Queries the screens need (DAO outline)

| Screen | Query |
|---|---|
| Home: outstanding | `SELECT currency, SUM(totalMinor) FROM invoice WHERE status = 'ISSUED' GROUP BY currency` |
| Home: overdue count | `SELECT COUNT(*) FROM invoice WHERE status = 'ISSUED' AND dueDate < :today` |
| Home: recent | `ORDER BY updatedAt DESC LIMIT 5` |
| Invoice list | Filter by status (Overdue = ISSUED and `dueDate < :today`), search `number LIKE :q OR client.name LIKE :q`, `ORDER BY issueDate DESC, id DESC`, returned as `Flow` and paged if > 200 rows |
| Client list | Clients not archived, each with an outstanding sum per currency (aggregate join) |
| Recent clients | `WHERE archived = 0 ORDER BY lastInvoicedAt DESC LIMIT 3` |
| Preset suggestions | `WHERE description LIKE :q || '%' ORDER BY useCount DESC, lastUsedAt DESC LIMIT 5` |

All read queries return `Flow`, so screens update automatically after any write.

### Migration policy (P3-T05, summary)
- **Never** `fallbackToDestructiveMigration`.
- Every schema change bumps the version and gets a `Migration` plus a `MigrationTestHelper` test that opens the previous exported schema with real rows and checks they survive.
- Every release is tested by installing the old app version and updating to the new one (P5-T05).
- Backups record their `formatVersion`. Restore accepts older versions (upgrading them in memory) and **refuses newer** ones, with a message to update the app.
