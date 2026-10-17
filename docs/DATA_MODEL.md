# Data Model (Phase 3)

_Started 2026-10-16. Refines [MVP_SPEC.md](MVP_SPEC.md) section 4 and is authoritative where they differ. Each Phase 3 task adds a section._

| Task | Section | Status |
|---|---|---|
| P3-T01 Room schema | 1–4 | Draft, awaiting founder review |
| P3-T02 Historical snapshots & numbering | 5 | Draft, awaiting founder review |
| P3-T03 Money representation & rounding | 6 | Draft; **implemented in P4-T02** (`domain/money`, 45 unit tests pass) |
| P3-T04 Layer responsibilities | ARCHITECTURE.md | Draft, awaiting founder review |
| P3-T05 Migrations | 7 | Draft, awaiting founder review |

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
    val clientId: Long? = null,                   // null only while DRAFT (I-11)
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

Room can't express SQL `CHECK` constraints through annotations, so the **repository enforces these, and tests cover each one**:

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
| I-11 | `clientId` may be null **only** while DRAFT. Autosaved drafts can exist before a client is chosen (UX_SPEC 3); issuing requires a client. *(Added in P4-T03.)* |

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
| Home: issued, not sent | `WHERE status = 'ISSUED' AND lastSentAt IS NULL ORDER BY issueDate` (UX_SPEC 2) |
| Home: drafts | `WHERE status = 'DRAFT' ORDER BY updatedAt DESC LIMIT 3` |
| Invoice search | Number, client name (current **and** frozen in `toSnapshot`) and `line_item.description`. Case- and accent-insensitive: search on a normalized copy (lower case, accents removed), e.g. a `searchText` column kept up to date on save. Decide the exact approach in P4-T11. |
| Preset suggestions | `WHERE description LIKE :q || '%' ORDER BY useCount DESC, lastUsedAt DESC LIMIT 5` |

All read queries return `Flow`, so screens update automatically after any write.

### Migrations
See section 7 (P3-T05).

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

---

## 6. Money (P3-T03)

_Authoritative. It replaces MVP_SPEC section 5 and keeps all its rules._ Code: the pure-Kotlin `domain/money` package (ARCHITECTURE.md), unit tested on the computer with no phone needed.

### 6.1 Representation
| Rule | Detail |
|---|---|
| **M-1** Amounts are whole **minor units** in a `Long` | Cents for USD, yen for JPY, fils (1/1000) for KWD. Never `Double` or `Float` anywhere money is stored, calculated or passed around. |
| **M-2** Each amount travels with its currency | In code: `Money(minor: Long, currency: CurrencyCode)`. Adding amounts in different currencies is an error the code refuses, not a conversion. |
| **M-3** Quantity is `Long` thousandths | `1500` = 1.5. At most 3 decimal places, must be > 0. |
| **M-4** Percentages are `Int` basis points | `750` = 7.5%. At most 2 decimal places when typed. |
| **M-5** No negative amounts | No credit notes in the MVP. Prices ≥ 0; discounts are their own field (F-15). |
| **M-6** Upper limit | Any single amount (price, line, subtotal, total) ≤ **999,999,999,999,999 minor units** (9,999,999,999,999.99 in a 2-decimal currency). Larger values are refused with "This amount is too large." The limit keeps every amount exact in a JSON backup file too (it is below 2⁵³). Quantity ≤ 999,999.999. |
| **M-7** Exchange rates never | PocketBill never converts between currencies (UX_SPEC 2, 7.2). |

### 6.2 Supported currencies
A **fixed list in code** (`CurrencyCode`), with decimal places (ISO 4217) **and display symbols** (Unicode CLDR, US English) written out rather than read from the phone's Java or ICU data. That data differs between Android versions, and between Android and the computer that runs the tests:

| Decimals | Currencies (MVP list) |
|---|---|
| 2 | USD, EUR, GBP, CAD, AUD, NZD, CHF, SEK, NOK, DKK, PLN, CZK, INR, PKR, BDT, LKR, NPR, NGN, KES, GHS, ZAR, EGP, AED, SAR, QAR, PHP, IDR, MYR, SGD, HKD, CNY, THB, TRY, BRL, MXN, ARS, COP |
| 0 | JPY, KRW, VND, CLP, UGX, ISK |
| 3 | KWD, BHD, OMR, JOD, TND |

- A unit test checks this table against ISO 4217 (P5-T01).
- Currencies that ISO lists with decimals but people rarely use with cents (such as IDR) keep their ISO decimals. Users just type whole numbers.
- More currencies are added by extending the list and its test. No database change is needed.

### 6.3 Reading typed amounts
One parser handles prices, discount amounts and quantities. Spaces, currency symbols and codes are ignored.

| Typed | Result | Rule |
|---|---|---|
| `450` | 450.00 | Whole number |
| `450.5` / `450,5` | 450.50 | One separator followed by 1–2 digits is the decimal point, whether `.` or `,` |
| `1,200` | 1,200.00 | A comma followed by **exactly 3** digits is a thousands separator |
| `1,200.50` | 1,200.50 | Both present: the **last** one is the decimal point |
| `1.200,50` | 1,200.50 | Same rule, so European-style typing also works |
| `1.200` | **1.20** | A single dot is always a decimal point (English convention, D-007) |
| `€ 60` | 60.00 | Symbols and spaces ignored |
| `10.555` in EUR | **Error**: "EUR allows 2 decimal places." | Too many decimals is refused, **never silently rounded** |
| `10.5` in JPY | **Error**: "JPY has no decimal places." | Same |
| `-5`, `abc`, empty | **Error**: "Enter a price, like 450 or 450.00." | |

**Changing an invoice's currency** keeps the numbers as typed (450.00 EUR becomes 450.00 USD, with **no conversion**). If a price has more decimals than the new currency allows (JPY), that field shows the decimals error until fixed.

### 6.4 Calculation order and rounding
Rounding is **half-up** (0.5 goes up) to the invoice currency's minor unit. All amounts are non-negative, so this equals "round half away from zero". Multiplications use exact integer maths (`BigInteger`), with half-up division computed as ⌊(2n + d) ÷ 2d⌋, and are checked against M-6 before converting back to `Long`.

1. **Line amount** = round(quantityMilli × unitPriceMinor ÷ 1000)
2. **Subtotal** = sum of line amounts (each already rounded)
3. **Discount** = round(subtotal × discountBp ÷ 10,000) for a percentage, or the fixed amount. It is then **capped at the subtotal**.
4. **Tax** = round((subtotal − discount) × taxBp ÷ 10,000). It is rounded **once**, on the whole invoice, never per line.
5. **Total** = subtotal − discount + tax

These are stored in the invoice row (`subtotalMinor`, `discountMinor`, `taxMinor`, `totalMinor`) and in each `line_item.amountMinor` on every save (invariant I-7).

### 6.5 Required tests
Expected values were computed independently (2026-10-16) with exact integer maths before being written here.

**Calculation**
| # | Case | Input | Expected (minor units → shown) |
|---|---|---|---|
| C-1 | Usability task T2 | EUR: 1 × 450.00 + 2 × 60.00 | 57000 → **€570.00** |
| C-2 | Fractional hours | 1.5 × $85.00 | 12750 → $127.50 |
| C-3 | Half-up on a line | 1.5 × $0.33 (= 49.5¢) | 50 → $0.50 |
| C-4 | Half-up at exactly .5 | 1.5 × $0.01 (= 1.5¢) | 2 → $0.02 |
| C-5 | Rounds down below .5 | 0.125 × $0.01 (= 0.125¢) | 0 → $0.00 |
| C-6 | Zero-decimal currency | 3 × ¥1,500 | 4500 → ¥4,500 |
| C-7 | Three-decimal currency | 2 × KWD 1.250 | 2500 → KWD 2.500 |
| C-8 | Tax once on the subtotal | $33.33 + $33.33 + $33.34 at 7.5% | sub 10000, tax 750, total 10750 → $107.50 |
| C-9 | Tax half-up | $0.10 at 5% (0.5¢) | tax 1, total 11 → $0.11 |
| C-10 | Fractional tax rate | $1.00 at 12.5% | tax 13, total 113 → $1.13 |
| C-11 | Percent discount, then tax | $99.99, 10% off, 20% tax | discount 1000, tax 1800, total 10799 → $107.99 |
| C-12 | Discount cap | $50.00, $80.00 off | discount 5000, total 0 → $0.00 |
| C-13 | Over the limit | 999,999.999 × 999,999,999.99 | **Refused** (M-6), no overflow or crash |
| C-14 | Mixing currencies | Adding € to $ in code | Throws an error (M-2) |

**Parsing:** every row of the 6.3 table, plus each currency decimal group (0, 2, 3).

**Formatting** (UX_SPEC 7.2): `€1,200.00`, `$1,200.00`, `CA$1,200.00`, `A$1,200.00`, `¥4,500`, `KWD 2.500`, `PKR 1,200.00` (a no-break space follows a letter code). The formatter does its own digit grouping instead of using `java.text.NumberFormat`, whose output differs between Android and the desktop JVM. Output must be identical with the phone set to English, German, French and Japanese, proving the formatter ignores the phone's locale.

**Property tests** (many random inputs): total = subtotal − discount + tax; total ≥ 0; the stored totals equal a fresh recalculation (I-7); parse(format(x)) = x for every currency.

---

## 7. Database migrations and testing (P3-T05)

Goal: **an app update never loses or corrupts a user's invoices** (UP-01).

### 7.1 Rules
| # | Rule |
|---|---|
| G-1 | **Never** use `fallbackToDestructiveMigration` or any setting that wipes data when a migration is missing. A missing migration should crash in testing, never delete data on a user's phone. |
| G-2 | Every schema change bumps the database `version` and ships **in the same commit** as its migration and its test. |
| G-3 | Schemas are exported (`exportSchema = true`), using the Room Gradle plugin's `schemaDirectory("$projectDir/schemas")`. Every version's JSON is committed to Git and **never edited** afterwards. |
| G-4 | **Additive changes** (a new table, or a new nullable or defaulted column) may use Room's `@AutoMigration`. **Anything else** (rename, type change, split, data rewrite) gets a hand-written `Migration` with its SQL reviewed. Either way it's tested (7.3). |
| G-5 | A migration also changes what's **in** the rows when needed (e.g. filling a new column from existing data), inside the same migration. |
| G-6 | **Downgrades aren't supported** (Google Play doesn't downgrade apps). If an older app version meets a newer database, it shows "This data was created by a newer PocketBill. Please update the app." It **never** deletes or rewrites the data. |
| G-7 | Enum names stored in the database (`ISSUED`, `PERCENT`…) are part of the schema. Renaming one needs a migration (section 1). |

### 7.2 Safety copy before upgrading (Should)
Before Room opens a database whose stored version is **lower** than the app's, PocketBill copies the database file to app-private storage (`pre-migration-v1.db`) and keeps the latest copy. Comparing the version needs a plain SQLite read of `user_version`, without Room. If a migration ever fails on a user's phone, the app shows a clear message and keeps the copy for support and recovery, instead of crashing in a loop.

### 7.3 Tests for every schema change
| Test | Where it runs | What it proves |
|---|---|---|
| **Migration test** for each step (v1→v2, v2→v3…) with `MigrationTestHelper` | Instrumented test on the Galaxy A72 | Real rows written in the old schema, including every status, accented text, 0- and 3-decimal currencies and snapshots, read back correctly in the new one |
| **Full-chain test** v1 → latest | Same | A user who skipped several updates is still safe |
| **Schema check** | Same | Room's validation of the migrated schema passes (it fails the test if the migrated tables don't match the new entities) |
| **Update test** | Manual, P5-T05 | Install the previous release build, create data, install the new build over it, and check everything is still there |

### 7.4 Backup file versions
The backup file has its own `formatVersion`, separate from the database version, because not every schema change changes the backup format.
- Restore accepts **every older** `formatVersion`, upgrading it in memory step by step (v1 → v2 → …) before validating.
- Restore **refuses a newer** version: "This backup was made by a newer PocketBill. Please update the app first." Nothing is changed.
- A sample backup file for each `formatVersion` is kept in the test resources and **never edited**. Every release must restore all of them.

### 7.5 Release checklist items (added to RELEASE_CHECKLIST.md)
- [ ] Database version bumped if any entity changed, and the new schema JSON committed
- [ ] Migration and full-chain tests pass on the physical phone
- [ ] Update-over-previous-release test done (7.3)
- [ ] All sample backup files restore
