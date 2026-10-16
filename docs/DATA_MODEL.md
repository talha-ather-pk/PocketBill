# Data Model (Phase 3)

_Started 2026-10-16. Refines [MVP_SPEC.md](MVP_SPEC.md) section 4 and is authoritative where they differ. Each Phase 3 task adds a section._

| Task | Section | Status |
|---|---|---|
| P3-T01 Room schema | 1–4 | Draft, awaiting founder review |
| P3-T02 Historical snapshots & numbering | 5 | Draft, awaiting founder review |
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
    val logoFile: String? = null,                 // content-named file in app-private storage, never overwritten (F-14, section 5.3)
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
    val lastSentVia: String? = null,              // chosen app label, e.g. "Gmail" (UX_SPEC 4)
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

Changes from MVP_SPEC section 4: `client.lastInvoicedAt` (recent-client chips), `business_profile.lastBackupAt` (backup reminder), `invoice.lastSentVia` (UX_SPEC 4), `logoPath` renamed `logoFile` (content-named, section 5.3), and `uuid`/`createdAt`/`updatedAt` written out on every table.

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
| **Edit issued invoice** | Same as Save draft, but keeps the number. Snapshots are refreshed only by the rules in sections 5.4–5.5. |
| **Mark paid / unpaid** | Status plus `paidDate`. |
| **Void** | Status VOID. The number stays reserved forever. |
| **Restore backup** | Validate the whole file **before** the transaction → delete all rows → insert everything, mapping backup UUIDs to new local IDs → recheck I-1…I-10 → commit. Any failure rolls back everything (MVP_SPEC section 7). |

**Snapshot JSON:** defined in section 5.3.

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

---

## 5. Invoice numbers and frozen details (P3-T02)

### 5.1 Numbering rules

| # | Rule | Why |
|---|---|---|
| N-1 | A number is assigned **when the invoice is issued**, not when the draft is created. | Deleting drafts never leaves gaps. |
| N-2 | Format: `prefix` + number padded to 4 digits (`INV-0001` … `INV-9999`, then `INV-10000`). | Familiar, and sorts well up to 9,999. |
| N-3 | Numbers are **unique** (database index) and **never reused**, including numbers of voided invoices. | Clients and accountants rely on a number meaning one invoice. |
| N-4 | `nextInvoiceNumber` only increases. It is incremented in the same transaction that issues the invoice (section 3). | No two invoices can get the same number, even after a crash. |
| N-5 | Changing the **prefix** affects future invoices only. Existing numbers never change. | Numbers already sent to clients must stay valid. |
| N-6 | **Starting number** is set in Setup and Settings ("Next invoice number"), so freelancers moving from another tool can continue from, say, 58. | Continuity with their existing records |
| N-7 | If the next number is already taken (for example, the prefix was changed back, or the starting number was lowered), issuing **skips to the next free number**. Settings warns: "INV-0042 is already used. The next invoice will be INV-0058." | Numbers stay unique without blocking the user. |
| N-8 | A number can't be edited after issue in the MVP. To correct a mistake: void it and issue a new invoice. | Keeps the audit trail honest. |
| N-9 | **Duplicate** creates a draft **without** a number (N-1). | |
| N-10 | Restoring a backup restores `nextInvoiceNumber` as well. | Numbering continues after moving to a new phone. |

**Not in the MVP:** automatic year or date parts (`INV-2026-001`), or separate sequences per client. Users can type a year into the prefix themselves (`2026-`).

**Legal note:** some countries require invoice numbers without gaps or with specific formats. PocketBill keeps numbers sequential and never reuses them, but **doesn't claim legal compliance** for any country. The store listing and help text must not imply it (Phase 8).

### 5.2 What gets frozen, and when

An issued invoice is a record of what was sent. It must look the same in 3 years, even after the freelancer changes their address or bank, or the client renames their company.

| Data | Draft | Issued, Paid, Void |
|---|---|---|
| Business details (name, contact, address, tax ID, payment instructions, logo) | **Live** from `business_profile` | **Frozen** in `fromSnapshot` |
| Client details (name, email, address, country, tax ID) | **Live** from `client` | **Frozen** in `toSnapshot` |
| Items, currency, dates, tax, discount, totals | Stored on the invoice (always) | Stored on the invoice (always) |

The PDF, the send email (UX_SPEC 4) and the invoice list all read frozen details for issued invoices and live details for drafts.

### 5.3 Snapshot format (version 1)

Stored as JSON text, written and read with kotlinx.serialization:

```json
// fromSnapshot
{"v":1,"name":"Your Studio","contactLines":"hello@yourstudio.example","address":"…",
 "taxId":null,"paymentInstructions":"IBAN DE89 …","logoFile":"logo_3f9a…c2.png"}

// toSnapshot
{"v":1,"name":"Müller Design GmbH","email":"billing@mueller.example","address":"…",
 "country":"Germany","taxId":"DE123456789"}
```
- Unknown fields are ignored and missing optional fields default to null, so later versions can add fields without breaking old invoices.
- If a snapshot can't be read (a corrupted row), the invoice still opens, shows "Some saved details couldn't be read", and falls back to live details **for display only**. It never silently rewrites the snapshot.

**Logo files are never overwritten.** Each logo is saved under a name derived from its content (`logo_<sha256-prefix>.png`), so an old invoice's snapshot still points to the logo it was sent with. A logo file is deleted only when neither the profile nor any invoice snapshot refers to it.

### 5.4 Editing a client after invoicing them

Saving changes to a client that has **issued, unpaid** invoices shows a choice:

```
┌──────────────────────────────────────┐
│ Update unpaid invoices too?          │
│                                      │
│ Müller Design GmbH has 2 unpaid      │
│ invoices showing the old details.    │
│                                      │
│ ( ) Only new invoices                │  ← default
│ ( ) Also update the 2 unpaid invoices│
│                                      │
│ Paid invoices always keep the        │
│ details they were sent with.         │
│                [Cancel]  [Save]      │
└──────────────────────────────────────┘
```
| Invoice state | Effect of editing the client |
|---|---|
| Draft | Always shows the new details (live) |
| Issued, unpaid | Keeps old details **unless** the user picks "Also update" |
| Paid, Void | **Never** changes |

- No unpaid invoices → no dialog, just save.
- Updated invoices get a new `updatedAt`. The next PDF and email use the new details. Nothing is re-sent automatically.
- **Archiving** a client hides them from pickers and the client list. All their invoices keep working.
- **Changing an unpaid invoice's client** (in the editor) takes a new snapshot of the new client.

### 5.5 Editing the business profile

Same dialog and rules as 5.4, applied to all unpaid invoices. **One difference:** if the **payment instructions** changed, the dialog says so and **pre-selects "Also update unpaid invoices"**:

> **Your payment details changed.** 3 unpaid invoices still show the old details. Clients paying from those invoices could send money to the old account.
> (•) Also update the 3 unpaid invoices  ( ) Only new invoices

Already-sent PDFs can't be changed. After updating, Home shows a dismissible card: "3 unpaid invoices were updated with new payment details. Send them again?", which opens a list with **Email again** on each.

### 5.6 Search and display
- Invoice lists show the **frozen** client name for issued invoices (what the client sees on the PDF).
- Search matches **both** the frozen name and the client's current name, so searching "Müller Design" still finds invoices issued before the client was renamed.

### 5.7 Tests (P5-T01 / P5-T02)
- Issue → edit client → issued invoice unchanged; draft updated.
- Edit client with "Also update" → only unpaid invoices change; paid ones identical byte for byte.
- Change payment instructions → dialog pre-selects update; the PDF shows the new details.
- Numbering: 1,000 issues → unique and strictly increasing; deleting drafts leaves no gap; a voided number is never reissued; a prefix change plus collision skips correctly; a crash during issue (transaction rolled back) leaves no number used.
- A snapshot with an unknown field still reads; a corrupted snapshot shows the warning and isn't rewritten.
- Replacing the logo → old invoice PDFs still use the old logo.
