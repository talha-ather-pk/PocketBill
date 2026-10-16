# UX Specification (Phase 2)

_Started 2026-10-16. Implements the screens in [MVP_SPEC.md](MVP_SPEC.md) section 3. Each Phase 2 task adds a section here._

| Task | Section | Status |
|---|---|---|
| P2-T01 Navigation | 1 | Draft, awaiting founder review |
| P2-T02 Home | 2 | Draft, awaiting founder review |
| P2-T03 Invoice editor | 3 | Draft, awaiting founder review |
| P2-T04 Sending (PDF preview / send) | 4 | Draft, awaiting founder review |
| P2-T05 History & statuses | 5 | Draft, awaiting founder review |
| P2-T06 Client management | 6 | Draft, awaiting founder review |
| P2-T07 Accessibility & localization | — | Not started |
| P2-T08 Design tokens | — | Not started |

---

## 1. Navigation (P2-T01)

### Design goals, from validation
1. **Quick tasks first** (A-02: check status, quick edit, re-send). Each must be reachable in **≤ 2 taps from opening the app**.
2. **Sending is never hidden** (T3 had the most friction). It is the primary button wherever an invoice is shown in full.
3. **Creating is fast enough to beat the laptop** (D-010). Create is always one tap from Home and Invoices, and Duplicate is one tap from any invoice.

### Structure

Single activity, Navigation Compose. Four bottom-bar tabs, each **keeping its own back stack**: leaving the Invoices tab while on an invoice and coming back returns to that invoice.

```mermaid
flowchart TD
    Start{Business profile exists?} -- no --> Setup[Business setup]
    Start -- yes --> Home
    Setup -- Done --> Home

    subgraph Tabs[Bottom bar]
      Home
      Invoices[Invoice list]
      Clients[Client list]
      Settings
    end

    Home -- outstanding / overdue card --> InvoicesF[Invoice list, filtered]
    Home -- recent row --> Detail[Invoice detail]
    Home -- Create invoice --> Editor[Invoice editor]
    Invoices -- row --> Detail
    Invoices -- New --> Editor
    Clients -- row --> ClientDetail[Client detail: info + their invoices]
    ClientDetail -- invoice row --> Detail
    ClientDetail -- New invoice for client --> Editor
    ClientDetail -- Edit --> ClientEditor[Client editor]
    Detail -- Send --> Share[[System share sheet]]
    Detail -- Preview --> Preview[PDF preview]
    Detail -- Edit --> Editor
    Detail -- Duplicate --> Editor
    Editor -- Issue --> Preview
    Editor -- + New client --> ClientEditor
    Preview -- Send --> Share
    Settings --> Profile[Business profile]
    Settings --> Presets[Line-item presets]
    Settings --> Backup[Backup & restore]
    Backup -- Restore file picked --> RestoreConfirm[Restore confirmation]
```

### Routes

Type-safe Navigation Compose routes (`@Serializable` objects and classes). This needs the Kotlin serialization plugin, which is added in P4-T01.

| Route | Arguments | Bottom bar | Notes |
|---|---|---|---|
| `Setup` | — | Hidden | Only when no business profile exists. Removed from the back stack when done (`popUpTo(Setup) { inclusive = true }`). |
| `Home` | — | Shown | Start destination of tab 1 |
| `InvoiceList` | `filter: Status? = null` | Shown | Start of tab 2. Home's cards open it pre-filtered (Unpaid, Overdue). |
| `ClientList` | — | Shown | Start of tab 3 |
| `Settings` | — | Shown | Start of tab 4 |
| `InvoiceDetail` | `invoiceId: Long` | Shown | |
| `InvoiceEditor` | `invoiceId: Long? = null`, `duplicateOf: Long? = null`, `clientId: Long? = null` | **Hidden** | Full-screen task with more room for the keyboard. New, edit, duplicate, or new for a given client. |
| `PdfPreview` | `invoiceId: Long` | **Hidden** | |
| `ClientDetail` | `clientId: Long` | Shown | |
| `ClientEditor` | `clientId: Long? = null`, `returnToEditor: Boolean = false` | Hidden | When opened from the invoice editor, the new client is selected on return. |
| `BusinessProfile`, `Presets`, `Backup` | — | Shown | Inside the Settings tab |
| `RestoreConfirm` | `fileUri: String` | Hidden | Shows counts before replacing data (MVP_SPEC section 7) |

### Back behaviour
| Situation | Back goes to | Why |
|---|---|---|
| Editor, any time | Previous screen. The **draft is already autosaved**, so there is **no "discard changes?" dialog**; a short message says "Draft saved". | Interruptions on a phone are normal; nothing is lost (UP-01). |
| Editor → **Issue** → Preview | Back from Preview goes to **Invoice detail**, not the editor. The editor is replaced (`popUpTo(InvoiceEditor) { inclusive = true }`, then Detail, then Preview). | After issuing, the invoice is "done". Returning to the form would feel like it wasn't saved. |
| Editing an issued invoice → **Save** | Back to where the edit started (Detail or Preview), which shows the updated data | Same as the prototype |
| Draft deleted from the editor | Back to the list it came from, with "Draft deleted · Undo" | |
| Tab re-selected while already on that tab | Pops to the tab's start screen | Standard Android behaviour |
| Back on a tab's start screen (not Home) | Home | Standard Material behaviour |
| Back on Home | Leaves the app | |

### Quick-task tap counts (target vs this design)
| Task | Path | Taps |
|---|---|---|
| "Who owes me?" | Open app → Home shows outstanding per currency and overdue count | **0** |
| See overdue invoices | Home → Overdue card | **1** |
| **Re-send an invoice** | Home → recent row → **Send** → pick app | **2** + pick app |
| Re-send an older one | Invoices tab → row → **Send** | **2** + pick app |
| Quick edit (e.g. due date) | Home/Invoices → row → Edit → change → Save | 4 |
| Mark paid | row → **Mark paid** | **2** |
| New invoice for a repeat client | Home → Create → pick from **recent clients** (top 3 shown as chips) → pick a preset item → Issue → Send | ~6–8 taps plus typing the amount |
| **Duplicate last month's invoice** | Row → **Duplicate** → (adjust) → Issue → Send | **4** + pick app |

### Invoice detail: action order
Following the T3 finding, the detail screen shows actions in this order:
1. **Email to client** (or **Send…** when the client has no email): filled primary button, showing "Last sent 16 Oct via Gmail" underneath when sent before. Details in section 4.
2. **Mark as paid** (or "Mark as unpaid"): tonal button.
3. Row of icon buttons with labels: **Preview**, **Edit**, **Duplicate**, **More** (Void, Delete draft).

Drafts show **Issue** instead of Send, and Mark paid is hidden.

### What this deliberately leaves out
- **No navigation drawer**: four tabs cover everything.
- **No floating "+" button**: Create is a full-width button on Home and a button on the Invoices list. A floating button on a phone screen covers the last list row, and the T3 testing showed people miss small controls.
- **No swipe actions on list rows** in the MVP: they're invisible to new users and to TalkBack. They could be added later as a shortcut.
- **No incoming deep links**: nothing outside the app opens PocketBill screens. The F-17 due-date notification (Could) would open Invoice detail.

### To validate
- Section 4 (P2-T04) defines the sending redesign and its check with 2–3 participants.
- Whether **recent clients as chips** plus **presets** bring a repeat invoice under 2 minutes is measured by P1-T07.

---

## 2. Home (P2-T02)

### The job
Home answers the validated phone tasks **without a tap** (A-02, A-03): *Who owes me? Is anything overdue? Is there something I started and didn't finish?* It also starts a new invoice in one tap.

### Layout (normal use)

```
┌──────────────────────────────────────┐
│ Your Studio                          │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ Outstanding                      │ │  ← tap: Invoices, Unpaid filter
│ │ $1,200.00                        │ │
│ │ €570.00                          │ │
│ │ £800.00                          │ │
│ │ 4 unpaid invoices                │ │
│ ├──────────────────────────────────┤ │
│ │ ! 1 overdue · $1,200.00        › │ │  ← tap: Overdue filter (red row)
│ └──────────────────────────────────┘ │
│ ┌──────────────────────────────────┐ │
│ │  +  Create invoice               │ │  ← filled primary button
│ └──────────────────────────────────┘ │
│                                      │
│ [card: Back up your invoices  ✕]     │  ← at most one card (below)
│                                      │
│ Needs attention                      │
│  Acme Studio · INV-0012              │
│  Overdue 8 days · $1,200   [Remind]  │
│  Müller Design · INV-0014            │
│  Issued, not sent yet      [Email]   │
│  Lumen Labs · Draft                  │
│  Started yesterday       [Continue]  │
│                                      │
│ Recent                               │
│  (last 5 invoices, same rows as the  │
│   Invoices list)                     │
└──────────────────────────────────────┘
```

### Outstanding card
- **One line per currency**, never added together. There are no exchange rates offline, and a converted total would be wrong.
- Order: the currency with the most unpaid invoices first, then by currency code. Up to 3 lines are shown; more becomes "+2 more currencies", which expands in place.
- The sub-line counts unpaid invoices. Issued invoices count; drafts and void ones don't.
- **The overdue row** appears only when something is overdue. It uses the late colour **and** a "!" icon, so colour isn't the only signal.
- TalkBack reads it as one sentence: "Outstanding: 1,200 US dollars, 570 euros, 800 British pounds. 4 unpaid invoices. 1 overdue."

### Needs attention (up to 3 rows, most urgent first)
| Order | Shows | Row action (one tap) |
|---|---|---|
| 1 | **Overdue** invoices, oldest due date first: "Overdue 8 days" | **Remind**: emails the client using the reminder template (section 4). Without a client email it opens Send…. |
| 2 | **Issued but never sent** (`lastSentAt` is empty): "Issued, not sent yet" | **Email** (or **Send…**) |
| 3 | **Drafts**, most recently edited first: "Started yesterday" | **Continue**: opens the editor |

- Tapping the row itself (outside the button) opens invoice detail.
- "See all (5)" appears when more than 3 items qualify, and opens the matching filter.
- With nothing to show, the section is hidden. No "all caught up" filler.

### Cards (at most one at a time, under Create invoice)
| Priority | Card | When | Action |
|---|---|---|---|
| 1 | "**Your payment details changed.** 3 unpaid invoices were updated. Send them again?" | After the DATA_MODEL 5.5 update | **Review**: list with Email again on each |
| 2 | "**Back up your invoices.** Your invoices exist only on this phone." | 10+ invoices since the last backup, or 30 days since it (MVP_SPEC 7) | **Back up now**. ✕ hides it for 7 days. |

### Recent
The last 5 invoices by last change, using the same row design as the Invoices list (section 5). "See all" opens the Invoices tab.

### Empty and edge states
| State | What Home shows |
|---|---|
| **First use** (no invoices) | No Outstanding card. A welcome block: "Create your first invoice. It takes about a minute." plus **Create invoice**, and one tip: "Add your clients' email addresses to send invoices in one tap." No sample data. |
| Everything paid | Outstanding card shows "**Nothing outstanding**" and "All invoices are paid." Needs attention hidden. |
| Only drafts | Outstanding shows "Nothing outstanding". Needs attention shows the drafts. |
| Loading | Data comes from the phone in well under a second, so no spinner or placeholder. |
| Pull to refresh | None: the data is always current. |

### Data needed
Existing queries (DATA_MODEL 4) plus two: issued-but-unsent (`status = 'ISSUED' AND lastSentAt IS NULL`) and recent drafts (`status = 'DRAFT' ORDER BY updatedAt DESC`).

---

## 3. Invoice editor (P2-T03)

### The goal
Validation found that people **create invoices on laptops** today (D-010). The editor has to make creating one on the phone fast enough that they'd rather not wait for the laptop. P1-T07 measures whether it works.

**Targets:**
- **Repeat client: ≤ 60 seconds** from tapping Create to the issued PDF.
- **New client: ≤ 2 minutes.**
- **Duplicate last month's invoice: ≤ 20 seconds.**

### Principles
1. **Defaults do the work.** Picking a client fills in their usual currency, payment terms and tax, taken from their last invoice. New clients get the business defaults (Settings).
2. **One screen, few fields.** Client and items are always visible. Currency, dates, tax, discount and notes sit in a one-line summary that expands when tapped.
3. **Nothing is lost.** The draft saves automatically while typing. There is no Save button for drafts and no "discard changes?" dialog.
4. **Errors appear when issuing, not while typing.** A draft can be incomplete.

### Layout (new invoice)

```
┌──────────────────────────────────────┐
│ ←  New invoice          Draft saved ⋮│  ← ⋮ = Delete draft
├──────────────────────────────────────┤
│ Client                               │
│ [Acme Studio] [Lumen Labs] [Harbor…] │  ← recent-client chips (top 3)
│ [All clients…]  [+ New client]       │
│                                      │
│ EUR · Due in 14 days (30 Oct) · No tax ›│ ← summary line; tap to expand
│                                      │
│ Items                                │
│ ┌──────────────────────────────────┐ │
│ │ Description                      │ │
│ │ Logo design▏                     │ │
│ │  ↳ Logo design (preset) €450     │ │  ← preset suggestion while typing
│ │ Qty [1    ]   Price [450.00] EUR │ │
│ │                   Line €450.00 ⋮ │ │  ← ⋮ = Remove, Move up/down, Save as preset
│ └──────────────────────────────────┘ │
│ + Add item                           │
│                                      │
│ Notes (optional)                   › │
├──────────────────────────────────────┤
│ Total €450.00          [  Issue  ]   │  ← sticky bar, stays above the keyboard
└──────────────────────────────────────┘
```

**Expanded summary line ("Invoice details"):**
| Field | Default | Control |
|---|---|---|
| Currency | Client's last currency → suggestion from the client's country (below) → business default | Searchable list, recent currencies first |
| Issue date | Today | Date picker |
| Payment due | Client's last terms → business default | Chips: **On receipt · 7 · 14 · 30 days · Custom date**. The due date is shown next to them ("30 Oct 2026"). |
| Tax | Client's last rate → business default | Percent field, with "No tax" as a one-tap option |
| Discount (F-15) | None | Percent or amount |

The summary line always shows the **currency and due date** in words, so a wrong default is visible without expanding it. The currency code also appears beside every price field and in the total.

### Choosing a client
- **Recent-client chips:** the 3 clients invoiced most recently (`client.lastInvoicedAt`). One tap selects.
- **All clients…** opens a searchable bottom sheet, with archived clients hidden.
- **+ New client** opens a short sheet: **Name** (required), **Email** (labelled "for one-tap sending", UX_SPEC 4), **Country** (optional, picked from a list). Save returns to the editor with the client selected.
- **Currency suggestion from the country:** picking Germany for a new client suggests EUR, and the United Kingdom suggests GBP. It's a suggestion chip ("Use EUR?"), never a silent change, because many freelancers bill foreign clients in USD.
- A selected client shows as a single row (name and email) with **Change**.

### Line items
- A new invoice starts with **one empty item**, cursor in the description.
- **Preset suggestions (F-18)** appear under the description after 2 typed characters: up to 3 matches, most used first. Choosing one fills the description, plus the price if its currency matches the invoice's.
- **Quantity** defaults to 1. **Price** uses the number keyboard and accepts `.` or `,` as the decimal separator. On leaving the field it's shown with the currency's decimals (`450` → `450.00`; yen shows no decimals).
- **Line total** updates live.
- **Item menu (⋮):** Remove, Move up, Move down, Save as preset. No drag-to-reorder in the MVP.
- **Add item** adds an empty item and moves the cursor to it.
- Keyboard: Description → Next → Qty → Next → Price → Done. "Done" hides the keyboard so the total and Issue button are visible.

### Issuing
**Issue** checks (DATA_MODEL invariants I-4 to I-8):
| Problem | Message (shown at the field, and the field is scrolled to and focused) |
|---|---|
| No client | "Choose a client." |
| An item with no description | "Add a description." |
| Quantity ≤ 0 or not a number | "Enter a quantity above 0." |
| Price missing or invalid | "Enter a price, like 450 or 450.00." |
| Due date before issue date | "The due date can't be before the issue date." |
| Total is 0 | Not blocked. A confirmation asks: "This invoice totals €0.00. Issue it anyway?" |

Completely empty extra items are dropped silently. On success: number assigned (DATA_MODEL 5.1), then go to **PDF preview**, where **Email to client** is the main action (UX_SPEC 4).

### Other ways into the editor
| Mode | What's prefilled | Title | Bottom button |
|---|---|---|---|
| New | Defaults only | New invoice | Issue |
| New for client (from client detail) | That client + their defaults | New invoice | Issue |
| **Duplicate** | Client, currency, items, tax, discount, notes. Dates reset: issue today, due by the same terms. **No number** (N-9). | New invoice · copy of INV-0014 | Issue |
| Edit draft | Everything as saved | Draft | Issue |
| **Edit issued (unpaid)** | Everything as saved | Edit INV-0014 | **Save changes** |

**Editing an issued invoice (a "quick edit" from validation):**
- A banner explains: "Sent on 16 Oct. Saving updates the PDF. The client won't see changes until you send it again." (If never sent: "Saving updates the PDF.")
- Changes are **not** autosaved, because this is a real document. Back with unsaved changes asks: **Save changes / Discard / Keep editing**. This is the one exception to "no discard dialog".
- After saving: back to detail with "INV-0014 updated · **Email again**" as a one-tap prompt.

### Autosave (drafts)
- Saves 500 ms after the last change, and immediately when the app goes to the background.
- The top bar shows "Draft saved" quietly. If saving fails (very rare: a full disk), it shows "Couldn't save. Your changes are kept on screen" and retries.
- A draft is created only once something has been entered. Opening New and pressing Back leaves no empty draft.
- **Delete draft** (⋮ menu) removes it, with Undo.

### Tap budget: repeat client, one preset item
| Step | Taps |
|---|---|
| Home → **Create invoice** | 1 |
| Client chip | 1 |
| Description → type 2 letters → preset suggestion | 1 + typing |
| (price prefilled; terms, currency and tax prefilled from the client) | 0 |
| **Issue** | 1 |
| **Email to client** → Send in email app | 2 |
| **Total** | **6 taps + 2 letters** |

Duplicate: row → ⋮ **Duplicate** → **Issue** → **Email** → Send = **5 taps**, with no typing.

### Accessibility
- Every field has a visible label (not just a placeholder) and a TalkBack label.
- Errors are announced when shown (Compose `liveRegion`). The total is announced when it changes after leaving a price field, not on every keystroke.
- Chips and menu buttons are at least 48 dp.

### Data model
No new columns. The client's last currency, terms and tax come from a query on their most recent invoice. The country → currency suggestions are a fixed table in code.

### To validate
- **P1-T07** measures real creation time on the phone against the targets above.
- In the prototype, the summary line and presets aren't built yet. If P1-T07 shows creation is slow, add them to the prototype next and retest.

---

## 4. Sending an invoice (P2-T04)

### The problem
In usability testing, **T3 ("Send the PDF to the client") caused the most friction** (VALIDATION_RESULTS.md). The cause wasn't recorded, so the design targets the four likely causes visible in the prototype:

| # | Likely cause | Fix in this design |
|---|---|---|
| C1 | **Finding** Send: it sat in a bottom bar on the preview only, beside Edit and Save | Send is the **primary button** on Invoice detail *and* PDF preview (UX_SPEC 1) |
| C2 | **Too many choices**: a share sheet with 8+ apps for what is usually "email it to the client" | **"Email to client"** opens the email app directly, skipping the share sheet. Other apps sit behind "Send another way". |
| C3 | **Retyping**: the client's address, subject and message had to be entered in the email app | Everything is **prefilled** from the invoice |
| C4 | **No confirmation** afterwards that it was sent | The invoice shows **"Sent 16 Oct · Gmail"** and the button becomes **"Send again"** |

**Check it:** before Phase 4, ask 2–3 of the original participants to repeat T3 in an updated prototype. Success: each completes T3 unaided in under 30 seconds.

### Invoice detail with a client email on file (the common case)

```
┌──────────────────────────────────────┐
│ ←  INV-0014                          │
├──────────────────────────────────────┤
│ Müller Design GmbH          [Unpaid] │
│ €570.00                              │
│ Due 30 Oct 2026                      │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │  ✉  Email to client              │ │  ← filled primary button
│ └──────────────────────────────────┘ │
│   to billing@mueller.example         │  ← shows where it's going
│   Send another way (WhatsApp, …)     │  ← text button: opens the share sheet
│                                      │
│ ┌──────────────────────────────────┐ │
│ │  ✓  Mark as paid                 │ │  ← tonal button
│ └──────────────────────────────────┘ │
│  Preview   Edit   Duplicate   More   │
└──────────────────────────────────────┘
```
After sending, the primary button reads **"Email again"**, with **"Last sent 16 Oct via Gmail"** under it.

### Variations
| Situation | Primary button | Secondary |
|---|---|---|
| Client has an email | **Email to client** | Send another way |
| Client has **no** email | **Send…** (opens the share sheet) | "Add client email for one-tap sending" (opens the client editor; returns here) |
| Draft | **Issue and send** → confirmation sheet: "This gives the invoice its number, INV-0015, and opens your email app." [Issue and send] [Cancel] | Preview shows a DRAFT watermark |
| Paid | **Email receipt** (the PDF carries the PAID stamp) | Send another way |
| Void | No send button. "Voided invoices can't be sent." | — |

### What "Email to client" does (Android)
1. Generate the PDF from saved data, off the main thread. A progress indicator appears only if it takes over 300 ms.
2. Write it to app cache as `INV-0014_Muller-Design-GmbH.pdf`, exposed through `FileProvider`.
3. Launch `ACTION_SEND` with:
   - `type = application/pdf`, `EXTRA_STREAM` = the content URI, plus `ClipData` and `FLAG_GRANT_READ_URI_PERMISSION` (read access only, only for the chosen app).
   - `EXTRA_EMAIL = [client email]`, `EXTRA_SUBJECT`, `EXTRA_TEXT` (template below).
   - **`selector = ACTION_SENDTO mailto:`**, which limits the choice to email apps. With one email app (usually Gmail on Android), it opens **directly** and the share sheet never appears.
4. If no email app is installed, fall back to the full share sheet and say so: "No email app found. Choose another way to send."

**"Send another way"** opens the standard chooser (`Intent.createChooser`) with the same file and text, titled "Send INV-0014".

### Recording that it was sent
Android doesn't report whether the user actually pressed Send in Gmail or WhatsApp. It only reports **which app was chosen** (the `createChooser` callback with `EXTRA_CHOSEN_COMPONENT`, or the email intent launching).
- When an app is chosen, record `lastSentAt = now` and `lastSentVia = "Gmail"`, and show "Marked as sent via Gmail · **Undo**".
- The wording says **"Sent"** in the UI, with Undo for the case where the user backed out of the email app.
- Sending never changes the invoice **status** (it stays Unpaid until marked paid).

### Email template (fixed in the MVP; editable in a later version)

**Subject:** `Invoice INV-0014 from Your Studio`
**Body:**
```
Hi Müller Design GmbH,

Please find attached invoice INV-0014 for €570.00, due on 30 Oct 2026.

Payment details:
<payment instructions from the business profile>

Thank you,
Your Studio
```
- For a paid invoice: subject `Receipt for invoice INV-0014 from Your Studio`, body "Thank you for your payment of €570.00, received on 23 Oct 2026."
- Names and amounts use the invoice's **frozen details** (DATA_MODEL section 5), so the email matches the PDF.
- **For an overdue invoice** (used by Home's **Remind** and by Email again on an overdue invoice): subject `Reminder: invoice INV-0014 was due on 30 Oct 2026`, body "Hi Müller Design GmbH, this is a friendly reminder that invoice INV-0014 for €570.00 was due on 30 Oct 2026. I've attached it again for convenience." followed by the payment details. The button reads **Send reminder**.
- Messaging apps such as WhatsApp ignore the subject and use the body as the caption.

### Save to device
Available on PDF preview and under **More** on detail: **Save PDF** opens the system "save as" picker (`ACTION_CREATE_DOCUMENT`), prefilled with the same file name. It does not count as "sent".

### Errors
| Error | Message | Action |
|---|---|---|
| PDF generation fails | "Couldn't create the PDF. Your invoice is saved." | Try again. The error details go to the local log only. |
| No app can receive a PDF (very rare) | "No app on this phone can send PDF files. Save it to the device instead." | Opens Save PDF |
| Client email is invalid | Caught when the client is saved, not at send time | — |

### Data model change
Adds `invoice.lastSentVia: String?` (the app's label, e.g. "Gmail") next to `lastSentAt` (DATA_MODEL section 1).

---

## 5. Invoice history and statuses (P2-T05)

### Statuses
| Status | Chip | Meaning | Counts as outstanding? |
|---|---|---|---|
| Draft | Outlined, grey: "Draft" | Not issued, no number | No |
| Unpaid | Neutral: "Unpaid" | Issued, not yet due | Yes |
| Overdue | Red, with "!": "Overdue" | Issued, unpaid, past the due date (calculated, never stored) | Yes |
| Paid | Green, with ✓: "Paid" | Payment recorded | No |
| Void | Grey, number struck through: "Void" | Cancelled; the number stays reserved | No |

Every chip has text, so colour is never the only signal. "Sent" isn't a status. It shows in the row's second line ("Sent 16 Oct") or as "Not sent yet".

```mermaid
stateDiagram-v2
    [*] --> Draft: Create / Duplicate
    Draft --> [*]: Delete draft
    Draft --> Unpaid: Issue (number assigned)
    Unpaid --> Paid: Mark as paid
    Paid --> Unpaid: Mark as unpaid
    Unpaid --> Void: Void (confirm)
    note right of Unpaid: Shown as Overdue when today is after the due date
```

### Status actions
- **Mark as paid** records today immediately (2 taps from any list, as in the prototype). The message reads "INV-0014 marked paid today · **Change date** · **Undo**". Change date opens a date picker for payments recorded late.
- **Mark as unpaid** (on a paid invoice) clears the paid date, with Undo.
- **Void** (detail → More): a confirmation explains it: "Void INV-0014? It stays in your records with its number, but no longer counts as owed. This can't be undone after a few seconds." Undo is available for 6 seconds. Paid invoices must be marked unpaid before voiding.
- **Delete** exists only for drafts (UX_SPEC 3).

### Invoices list
**Filters** (chips with counts): **All · Unpaid 4 · Overdue 1 · Paid · Drafts 2**. Void invoices appear only under All. The selected filter is remembered while the app is open.

| Filter | Sort | Header line |
|---|---|---|
| All | Issue date, newest first, grouped by month ("October 2026") | — |
| Unpaid | Due date, soonest first | "Unpaid: $1,200.00 · €570.00 · £800.00" |
| Overdue | Due date, **oldest first** (most urgent on top) | "Overdue: $1,200.00" |
| Paid | Paid date, newest first, grouped by month | — |
| Drafts | Last edited, newest first | — |

**Row design** (the same everywhere: Home, Invoices, client detail):
```
Müller Design GmbH                 €570.00
INV-0014 · Due 30 Oct · Sent 16 Oct [Unpaid]
```
- The first line is the client name (the frozen one for issued invoices, DATA_MODEL 5.6).
- The second line changes with status: "Overdue 8 days", "Paid 23 Oct", "Not sent yet", "Draft · edited 2 h ago".
- Tapping a row opens invoice detail. There are no swipe actions (section 1).

**Search** (app-bar search icon): matches invoice number, client name (current or frozen) and item descriptions. Results ignore the active filter and show a note: "Searching all invoices". Matching is case- and accent-insensitive, so "muller" finds "Müller".

**Empty states:**
| Filter | Message |
|---|---|
| All, nothing yet | "No invoices yet. Invoices you create appear here." + **Create invoice** |
| Unpaid / Overdue | "Nothing unpaid." / "Nothing overdue." |
| Paid | "No paid invoices yet. Mark an invoice as paid when the money arrives." |
| Drafts | "No drafts." |
| Search | "No invoices match “logo”." |

**Performance:** lists use Paging once there are more than 200 invoices, so 1,000 invoices scroll smoothly on the Galaxy A72 (MVP_SPEC 8).

### Invoice detail: history block
Below the actions, a short history built from existing columns (no new table):
```
Issued      16 Oct 2026
Last sent   16 Oct 2026 via Gmail
Paid        23 Oct 2026
```
A full activity log (every send and status change) would need a new `invoice_event` table. It's a **Could**, and it has to be decided before the schema is finalised in P4-T03, because adding it later means a database migration.

---

## 6. Clients (P2-T06)

### Clients list (tab)
```
┌──────────────────────────────────────┐
│ Clients                      🔍   +  │
├──────────────────────────────────────┤
│ Acme Studio                $1,200.00 │
│ United States · 1 overdue      owed  │
│ Harbor & Pine                    —   │
│ Canada · nothing owed                │
│ Lumen Labs Ltd               £800.00 │
│ United Kingdom · 1 unpaid      owed  │
│ …                                    │
│ Archived clients (2)               › │
└──────────────────────────────────────┘
```
- Sorted **A–Z**, with a toggle for **Most owed**. Owed amounts are per currency and never added together ("$1,200.00 + €570.00").
- Search matches name, email and country, ignoring case and accents.
- **+** opens the client editor. Archived clients are listed separately at the bottom.

### Client detail
- The name is the title. **Email** (selectable, with a copy button), country, address and tax ID follow.
- **Owed** per currency, plus counts: "2 unpaid · 1 overdue".
- **New invoice for this client** is the primary button. It opens the editor with this client and their defaults (UX_SPEC 3).
- **Edit** and **More** (Archive, or Delete when allowed).
- Below that, **all of this client's invoices** using the standard rows, newest first.

### Client editor
| Field | Required | Notes |
|---|---|---|
| Name | Yes | |
| Email | No, but encouraged | Helper text: "Lets you email invoices in one tap." Format checked on save. |
| Country | No | Chosen from a list. Used for the currency suggestion (UX_SPEC 3). |
| Address | No | Several lines |
| Tax ID | No | e.g. a VAT number |
| Notes | No | Private: never printed on invoices |

Saving a client with unpaid issued invoices shows the "Update unpaid invoices too?" choice (DATA_MODEL 5.4).

**Possible duplicate:** creating a client whose name matches an existing one (ignoring case and accents) asks: "You already have a client called Müller Design GmbH. **Use existing** / **Create new anyway**".

### Archive and delete
| Client has… | Allowed |
|---|---|
| No invoices at all | **Delete** (with confirmation) or Archive |
| Any invoice, including drafts | **Archive only**. Invoices keep working, and the client disappears from pickers and the list. **Unarchive** from "Archived clients". |

Deleting is blocked at the database level too (`ON DELETE RESTRICT`, DATA_MODEL 1).

### Empty state
"No clients yet. Clients you add while creating an invoice appear here." + **Add client**.

### Could (not in the MVP plan)
**Pick from phone contacts**: Android's contact picker returns one chosen contact **without** the contacts permission, so it would fit the no-permissions rule (MVP_SPEC 8). Worth adding if P1-T07 shows that typing new clients slows people down.
