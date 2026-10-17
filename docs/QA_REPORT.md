# PocketBill — QA Report (Phase 5)

_As of 2026-10-17. Device: Samsung Galaxy A72 (SM-A725F), Android 14._

## Automated tests

| Suite | Count | Result | Covers |
|---|---|---|---|
| Unit tests (computer) | 230 | ✅ all pass | Money and rounding (232,000 random cases), numbering, dates incl. leap days, year ends and time zones, editor, clients, invoices list and detail, sending flow and email templates, Home, backup format and validation, CSV export (quoting, formula safety), saved items, theme contrast, manifest and privacy guards |
| On-phone tests | 53 | ✅ all pass | Database and repositories, logo import (shrinking, refusing non-images), v1→v2 migration, backup round trip / safety copy / bad files / half-way failure / logo, PDF: D-007 characters, page sizes, 50 and 200 items, marks, logo, extreme amounts and long text |

On-phone tests use an in-memory database and test folders: the founder's real data is never touched.

## Phase 5 tasks

| Task | State | Notes |
|---|---|---|
| P5-T01 Unit tests | ✅ | 230, incl. date edge cases left over from P4-T07 |
| P5-T02 Database tests | ✅ | 30 repository + 1 migration + 6 backup tests on the phone |
| P5-T03 UI tests | 🟡 Manual | Automated UI tests would drive the real app on the founder's phone and touch real data, so the UI is checked by hand with the list below. Automate once a spare test phone or emulator is available. |
| P5-T04 PDF matrix | ✅ | Many items, long text, a description taller than a page, huge amounts, 0- and 3-decimal currencies, empty optional fields, A4/Letter, all marks, logo. Visual check of samples done (fixed: digits lost in copied text; stamp position; very large amounts now shrink to fit their column). |
| P5-T05 Device & lifecycle | 🟡 Manual | List below. Autosave, transactions and the backup safety copy are tested automatically. |
| P5-T06 Security & privacy | ✅ | [SECURITY_REVIEW.md](SECURITY_REVIEW.md) |

## Hand checks for the founder (about 20 minutes)

Tick each one; note anything confusing.

**Main flow (MVP_SPEC 9 walk-through, in airplane mode)**
- [ ] T1: new client **Müller Design GmbH**, invoice for **€450**, due in 14 days → Issue → PDF preview opens
- [ ] T2: add 2 × €60 → total shows **€570.00**
- [ ] T3: **Email to client** opens the email app with the PDF attached (turn airplane mode off just to see Gmail; don't send) and the invoice then says "Sent via …"
- [ ] T4: **Mark as paid** → the PDF shows the PAID stamp
- [ ] T5: Home shows the outstanding total correctly

**Lifecycle**
- [ ] In the editor, type an item, press Home, swipe PocketBill away from recent apps, reopen → the draft is still there
- [ ] Rotate the phone on the editor and on an invoice page → nothing is lost
- [ ] Turn on dark mode → every screen is readable; PDFs stay white

**Accessibility**
- [ ] Settings → Display → Font size at the largest → Home, editor and invoice page still usable (nothing cut off that you need)
- [ ] TalkBack on: create an invoice (T1) using only swipes and double-taps

**Backup**
- [ ] Back up now → save to Drive or Downloads → Restore from that file → counts look right → Replace → everything is still there

**Accountant export and payment-details card**
- [ ] Backup and restore → Export invoices (CSV) → open the file in Google Sheets: one row per issued/paid/void invoice
- [ ] Business profile → change Payment instructions → choose "update unpaid invoices" → Home shows "Your payment details changed" with Email again on invoices you had sent

**Saved items and logo**
- [ ] Add a saved item; in a new invoice type its first 2 letters → it's suggested
- [ ] Business profile → Choose logo → the next PDF shows it
