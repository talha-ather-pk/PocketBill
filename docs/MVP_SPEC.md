# PocketBill MVP Specification (P1-T06)

_Draft 1, 2026-10-15. Status: **awaiting founder review.** Once approved, it is the scope contract for Phases 2–5; changing it needs a DECISIONS.md entry._

**Inputs:** [PERSONAS.md](PERSONAS.md) (primary: remote freelancer; secondary: side-gig freelancer) · [USER_PROBLEMS.md](USER_PROBLEMS.md) · [DIFFERENTIATION.md](DIFFERENTIATION.md) · [VALIDATION_RESULTS.md](VALIDATION_RESULTS.md) (founder summary, tally pending) · decisions D-003, D-005, D-007, D-008.

**Detail lives elsewhere:** exact layouts are set in Phase 2 (P2-T01…T08), and the final schema and money rules in Phase 3 (P3-T01…T05). This spec fixes **what** the MVP does and the rules it must obey. Later phases may refine **how**, but not widen or shrink scope without a decision.

---

## 1. Product promise

> Make a correct, professional invoice in about two minutes on your phone. Free, no ads, no account, works offline. Your data stays on your phone and in backups you control.

Each MVP feature traces to a problem or assumption (section 10).

## 2. Scope

Priorities: **Must** = release blocker · **Should** = in the MVP unless it threatens the schedule · **Could** = only if time allows · **Won't (MVP)** = explicitly excluded.

### Must
| ID | Feature |
|---|---|
| F-01 | **Business profile**, set up on first launch: name, contact lines, address, optional tax ID, **payment instructions** (bank / PayPal / Wise text), default currency, default payment terms, default tax rate, invoice number prefix. |
| F-02 | **Clients**: create, edit and archive. Name required; email, address, country and tax ID optional. A client can also be created inside the invoice editor. |
| F-03 | **Invoice editor**: client, currency, issue date, payment terms (on receipt / 7 / 14 / 30 / custom days) and due date, line items (description, quantity, unit price), one invoice-level tax rate, notes. Totals update live. |
| F-04 | **Invoice numbering**: automatic and sequential (`INV-0001`), with the prefix set in F-01. Numbers are unique and never reused. |
| F-05 | **PDF generation** with Android's built-in `PdfDocument`: A4 by default, US Letter as an option, multi-page when needed. |
| F-06 | **Save PDF** to a location the user picks (system file picker) and **share PDF** through the system share sheet. |
| F-07 | **Status tracking**: Draft → Issued (unpaid) → Paid, with the paid date recorded. "Overdue" is calculated (issued, unpaid, past the due date), not stored. Paid can be undone. |
| F-08 | **Home**: amount outstanding **per currency**, overdue count, recent invoices, Create invoice. |
| F-09 | **Invoice list**: filter (All / Draft / Unpaid / Overdue / Paid), search by client name or invoice number. |
| F-10 | **Backup and restore**: export everything to one versioned file the user saves anywhere; restore validates the whole file before replacing anything. |
| F-11 | **Correct money**: integer minor units, per-currency decimals, and the rounding rules in section 5. Unit tested. |
| F-12 | **Accented Latin text and currency symbols** render correctly in the UI and in PDFs (D-007). |

### Should
| ID | Feature | Why |
|---|---|---|
| F-13 | **Duplicate invoice**: start a new invoice from an existing one (same client, items and currency; new number and dates). | Fastest path for repeat clients. Supports A-02's "quick tasks" on the phone. |
| F-14 | **Business logo** on the PDF (picked with the system photo picker, stored privately by the app). | Looks professional, a goal of the primary persona. |
| F-15 | **Invoice-level discount** (a percentage or a fixed amount). | Common competitor feature; already listed in TEST_PLAN. |
| F-16 | **Void / cancel** an issued invoice (kept for the record, excluded from outstanding totals). | Keeps numbering honest without deleting issued invoices. |

### Could
| ID | Feature |
|---|---|
| F-17 | A reminder notification on the due date (local, no server). |
| F-18 | Line-item presets (saved services with a default price). |
| F-19 | Export a CSV of invoices for an accountant. |

### Won't (MVP)
| Excluded | Reason |
|---|---|
| Accounts, sign-in, cloud sync, any server | Core promise; DIFFERENTIATION C1/C2. |
| **Internet permission** of any kind | The MVP needs none. The absence can be **verified** in the manifest and is a concrete privacy claim. |
| Ads, in-app purchases, billing | Phase 6, after the core is reliable. Creating and exporting basic invoices will never be paywalled (UP-05). |
| Quotes and estimates | Not validated yet. Could follow as "convert quote → invoice". See open question Q-03. |
| Partial payments, payment links, online card payments | Need a server or a payment provider (anti-persona). |
| Multiple tax lines, compound taxes, tax reports | Complexity. One rate per invoice covers the personas. |
| Non-Latin scripts, right-to-left invoices, translated UI | D-007: nice-to-have later. |
| Multiple businesses or users | Anti-persona (agencies and teams). |
| Recurring invoices | Not yet requested; F-13 covers repeat clients for now. |

## 3. User flows (Jetpack Compose)

**Architecture:** single activity, Navigation Compose, a ViewModel exposing `StateFlow` UI state per screen, repositories as the only data access, Room, and pure-Kotlin domain logic ([ARCHITECTURE.md](ARCHITECTURE.md)). Material 3 with the navy theme already in the app. Phone portrait first; must not break in landscape or with large font sizes.

### Screens
| Screen | Reached from | Key elements |
|---|---|---|
| **Business setup** (first launch only) | App start when no profile exists | Name (required), currency, payment instructions. Everything else can be skipped and edited later in Settings. |
| **Home** (tab) | Bottom nav | Outstanding per currency, overdue count, Create invoice, recent 5 invoices. Empty state on first use. |
| **Invoices** (tab) | Bottom nav | Filter chips, search, list rows (client, number, due or paid date, amount, status chip). |
| **Clients** (tab) | Bottom nav | List with amount owed per client; add, edit, archive. |
| **Settings** (tab) | Bottom nav | Business profile, defaults, numbering prefix, paper size, Backup, Restore, About / privacy statement. |
| **Invoice detail** | List row, Home row | Summary, status, actions: Preview PDF, Share, Mark paid / unpaid, Edit (while Draft or unpaid), Duplicate, Void. |
| **Invoice editor** | Create invoice, Edit, Duplicate | Section 2 F-03. Saves a **draft automatically** so a closed app or an incoming call loses nothing. "Issue & preview" finalizes it. |
| **PDF preview** | Editor (after Issue), detail | Rendered pages, Save, Share. |
| **Client editor** | Clients tab, inside the invoice editor | F-02 fields. |
| **Restore confirmation** | Settings → Restore | Shows the counts in the file ("12 clients, 48 invoices, backup from 3 Oct 2026") before replacing data. |

### Core flow: target ≤ 2 minutes for a repeat client
Home → **Create invoice** → choose a recent client → currency, terms and tax prefilled from client or defaults → add item → **Issue & preview** → **Share** → pick an app.

The prototype's usability tasks T1–T5 are the acceptance walk-through for this flow (section 9).

### Editing rules
- **Drafts** can be edited freely and deleted.
- **Issued** invoices can be edited while unpaid. The number stays the same, and the PDF regenerates from the saved data.
- **Paid** and **void** invoices are read-only. Mark unpaid first to edit.
- An issued invoice can't be deleted, only voided (F-16). If F-16 slips to post-MVP, the delete button is hidden for issued invoices.

## 4. Data model (Room)

Room 2.8.5 with KSP (D-005; the KSP version is confirmed against Kotlin 2.4.21 in P4-T03). `exportSchema = true`, with schema JSON committed to `app/schemas/` so every migration can be tested (P3-T05). Version 1 ships with the MVP. **No destructive migrations, ever.**

### Conventions
- Money: `Long` in **minor units** of the invoice currency (cents for USD). Never `Double` or `Float`.
- Quantity: `Long` in **thousandths** (`1500` = 1.5 hours), allowing up to 3 decimal places.
- Tax and discount percentages: `Int` in **basis points** (`750` = 7.5%).
- Dates the user sees (issue, due, paid): `Long` **epoch day** (`LocalDate.toEpochDay()`), so time zones never shift a due date.
- Audit timestamps: `Long` epoch milliseconds (UTC).
- Currency: ISO 4217 code `String` (`"EUR"`). Decimal places come from `java.util.Currency.getDefaultFractionDigits()`: JPY 0, USD 2, KWD 3.
- IDs: `Long` auto-generated primary keys.

### Tables

**`business_profile`** (exactly one row, `id = 1`)
| Column | Type | Notes |
|---|---|---|
| id | Long PK | Always 1 |
| name | String | Required |
| contactLines | String | Email, phone, website; free text |
| address | String | Multi-line |
| taxId | String? | |
| paymentInstructions | String | Printed on every invoice |
| logoPath | String? | Private app file (F-14) |
| defaultCurrency | String | ISO 4217 |
| defaultTermsDays | Int | e.g. 14 |
| defaultTaxRateBp | Int | 0 = no tax |
| invoicePrefix | String | `"INV-"` |
| nextInvoiceNumber | Long | Incremented in the same transaction that issues an invoice |
| paperSize | String | `"A4"` / `"LETTER"` |
| updatedAt | Long | |

**`client`**
| Column | Type | Notes |
|---|---|---|
| id | Long PK | |
| name | String | Required |
| email, address, country, taxId, notes | String? | |
| archived | Boolean | Archived clients are hidden from pickers; their invoices remain |
| createdAt, updatedAt | Long | |

**`invoice`**
| Column | Type | Notes |
|---|---|---|
| id | Long PK | |
| number | String? | **Unique index.** Null while Draft; assigned when issued |
| status | String | `DRAFT` / `ISSUED` / `PAID` / `VOID` (overdue is calculated) |
| clientId | Long FK → client | `ON DELETE RESTRICT`; clients with invoices are archived, never deleted |
| currency | String | ISO 4217 |
| issueDate, dueDate | Long | Epoch days |
| paidDate | Long? | Epoch day; set when PAID |
| taxRateBp | Int | |
| discountType / discountValue | String? / Long? | F-15: `PERCENT` (bp) or `AMOUNT` (minor units) |
| notes | String | |
| subtotalMinor, discountMinor, taxMinor, totalMinor | Long | **Stored** results of section 5, recalculated on every save. They make lists fast and record exactly what the PDF showed |
| fromSnapshot, toSnapshot | String | Business and client details **frozen when issued**, so editing a client later doesn't change old invoices (P3-T02) |
| createdAt, updatedAt | Long | |

Indexes: `number` (unique), `status`, `clientId`, `dueDate`.

**`line_item`**
| Column | Type | Notes |
|---|---|---|
| id | Long PK | |
| invoiceId | Long FK → invoice | `ON DELETE CASCADE` (only drafts can be deleted) |
| position | Int | Display order |
| description | String | Required |
| quantityMilli | Long | > 0 |
| unitPriceMinor | Long | ≥ 0 |
| amountMinor | Long | Stored result of section 5 |

Index: `invoiceId`.

## 5. Calculation rules (pure Kotlin, unit tested)

1. **Line amount** = quantity × unit price, rounded **half-up** to the currency's minor unit.
   `amountMinor = roundHalfUp(quantityMilli × unitPriceMinor / 1000)`
2. **Subtotal** = sum of line amounts.
3. **Discount** (F-15): a percentage of the subtotal rounded half-up, or a fixed amount. Capped at the subtotal.
4. **Tax** = (subtotal − discount) × rate, rounded half-up **once** at invoice level.
5. **Total** = subtotal − discount + tax. It is never negative.
6. Calculations use `Long`, with `BigDecimal` only where division happens. Overflow is checked (`Math.multiplyExact`), and an amount too large to store is rejected with a clear message.
7. **Overdue** = status ISSUED and today's local date > dueDate.

**Required test examples (P4-T02):**
| Case | Input | Expected |
|---|---|---|
| Usability task T2 | EUR: 1 × 450.00 + 2 × 60.00, no tax | **€570.00** |
| Fractional hours | 1.5 × $85.00 | $127.50 |
| Half-up rounding | 1.5 × $0.33 (= 49.5¢) | **$0.50** |
| Zero-decimal currency | 3 × ¥1,500 | ¥4,500 (no decimals shown) |
| Tax once on total | $33.33 + $33.33 + $33.34, 7.5% | subtotal $100.00, tax **$7.50** (not a per-line sum) |
| Discount cap | subtotal $50.00, discount $80.00 | discount $50.00, total $0.00 |

## 6. PDF generation

- **Engine:** `android.graphics.pdf.PdfDocument`, drawing with `Canvas` and `StaticLayout`. No third-party PDF library and no screenshots of the UI.
- **Source:** always the **saved** invoice data and snapshots, never the screen's state.
- **Page:** A4 (595 × 842 pt) or US Letter (612 × 792 pt), with 40 pt margins.
- **Layout:** business block (with logo if set) · "INVOICE" + number · Bill to · issue, due and currency · items table (description wraps; numbers right-aligned) · subtotal, discount, tax, **total due** · payment instructions · notes · footer with page "1 of N".
- **Pagination:** the items table continues across pages, repeating its header row. The totals block never splits across pages.
- **Fonts:** system fonts through `Typeface` with Android's fallback chain. **Acceptance test:** accented Latin text (José, Müller, Øresund, François) and the symbols € £ ¥ ₹ ₦ ₱ ₨ must render with no empty boxes (□) (TEST_PLAN; D-007). If any symbol fails on the test phone, bundle a font that covers it.
- **Status marks:** a Paid invoice gets a "PAID" stamp and its paid date; a Void invoice gets "VOID". Drafts get a "DRAFT" watermark and can be previewed but not shared without confirmation.
- **Files:** generated into the app's cache, shared through a `FileProvider` content URI with read permission granted only to the receiving app, and saved with `ACTION_CREATE_DOCUMENT`. **No storage permissions.**
- **File name:** `INV-0014_Muller-Design-GmbH.pdf` (ASCII-safe; the PDF content keeps the accents).
- **Performance:** a 50-item invoice generates in under 2 seconds on the Galaxy A72, off the main thread.

## 7. Backup and restore (data safety, UP-01)

- **Format:** one JSON file, `PocketBill-backup-2026-10-15.json`, containing `formatVersion`, `appVersion`, `createdAt` and every table (the logo is base64-encoded if present). It is human-readable, so users own their data even without the app (DIFFERENTIATION C6).
- **Backup:** Settings → Back up now → system "save as" picker. The date of the last backup appears in Settings.
- **Reminder:** after creating 10 invoices, or 30 days since the last backup, Home shows a dismissible "Back up your invoices" card.
- **Restore:** pick file → **validate everything first** (format version, required fields, foreign keys, unique numbers, money within range) → show counts → user confirms → **one database transaction** replaces all data. Any failure leaves existing data untouched and says what was wrong.
- **Safety copy:** before a restore replaces anything, the app writes an automatic pre-restore backup to its private storage. The last one is kept.
- **Android's own cloud backup** (`allowBackup`): it stays **off** in the MVP until decided (Q-04), because it would copy invoice data to Google servers.
- **Tests (P5):** a full round trip (backup → wipe → restore → identical data, checked field by field), a corrupted file, a newer `formatVersion`, a file with a duplicate invoice number, and an interrupted restore.

## 8. Non-functional requirements

| Area | Requirement |
|---|---|
| Offline | Every feature works in airplane mode. **No `INTERNET` permission in the manifest.** |
| Permissions | None in the MVP. Photo picker, file picker and share sheet need none. Any addition needs a documented reason (P4-T14). |
| Data durability | Writes are transactional. The editor autosaves drafts. No data is deleted on app update. The Room migration tests pass. |
| Platform | minSdk 26 (Android 8.0), target and compile SDK 37 (D-005). |
| Performance | Cold start under 2 s, and the list scrolls smoothly with 1,000 invoices, on the Galaxy A72. |
| Accessibility | TalkBack labels on all controls, touch targets ≥ 48 dp, usable at 200% font size, contrast at WCAG AA. |
| Formatting | UI is in English. Default date format `15 Oct 2026` (unambiguous for global clients). Money uses the currency's own symbol and decimal places. |
| Privacy | No analytics or crash reporting SDKs in the MVP (Phase 7 decides). The privacy statement in Settings says data never leaves the phone unless the user shares or backs it up. |
| Ads | None, anywhere. |

## 9. Acceptance criteria (the MVP is done when…)

1. **Walk-through:** T1–T5 from VALIDATION_PLAN.md pass on the Galaxy A72 in the real app, with no help:
   - T1: create **Müller Design GmbH**, **€450**, due in 14 days, and reach the PDF.
   - T2: add 2 × €60, and the total shows **€570.00**.
   - T3: share it.
   - T4: mark it paid.
   - T5: read the outstanding total.
2. **Speed:** a repeat-client invoice can be created and shared in **≤ 2 minutes** by someone who has done it once.
3. **Money:** every section 5 example passes as a unit test, plus the TEST_PLAN unit list.
4. **PDF:** the D-007 character test renders with no □. A 50-item invoice paginates correctly. A4 and Letter both look right.
5. **Data safety:** every section 7 backup and restore test passes. Data survives an app update and a database migration test. Force-stopping the app while in the editor loses nothing.
6. **Offline:** the full walk-through passes in airplane mode. The manifest declares no permissions.
7. **Quality:** no crashes in a 30-minute exploratory session. TalkBack can complete T1.

## 10. Traceability

| Problem or assumption | Covered by |
|---|---|
| UP-01 data loss | F-10, section 7, durability requirements, autosaved drafts |
| UP-02 sync delays | No server: everything is local |
| UP-03 PDF characters (reduced by D-007) | F-12, section 6 font test |
| UP-04 abandoned apps | Human-readable backup file (section 7), no server dependency |
| UP-05 free-tier caps | Won't (MVP): no paywall on create or export |
| UP-06 ads | Section 8: no ads |
| UP-08 wrong tax | F-11, section 5 |
| A-02 phone, "quick tasks" | ≤ 2-minute flow, F-13 duplicate, recent clients, prefilled defaults, autosaved drafts |
| A-03 who owes me | F-07, F-08 outstanding per currency, F-09 overdue filter |
| A-07 multiple currencies | Currency per invoice; outstanding grouped per currency |

## 11. Open questions for the founder

| ID | Question | Why it matters |
|---|---|---|
| Q-01 | **What were the "quick tasks" in A-02?** Did participants *create* invoices on the phone, or mainly check status and resend? | If creation happens on a laptop, F-13 and the editor's speed matter less than F-08/F-09 and sharing, and the platform question comes back. |
| Q-02 | What were the **tally counts**, the **pitch ranking** and the **usability problems**? | They confirm the Must/Should split and the store-listing message (Phase 8). |
| Q-03 | Should **quotes/estimates** be in the MVP? The app's working name is "Invoice & Quote Maker". | Adds a document type and a "convert to invoice" flow, roughly +15% scope. |
| Q-04 | Enable **Android cloud backup** of the database? | Protects against a lost phone (UP-01), but sends data to Google, which conflicts with "data stays on your phone". |
| Q-05 | Is **one tax rate per invoice** enough for your participants (for example EU VAT, or GST + PST in Canada)? | If not, the schema needs tax lines before version 1, since adding them later means a migration. |
