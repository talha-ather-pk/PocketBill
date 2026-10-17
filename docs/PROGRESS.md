# PocketBill — Progress Report

_Snapshot: 2026-10-17 (updated after P4-T08). Last commit before this report: `e9816c9` "Build the Invoices tab and invoice detail (P4-T10, P4-T11)". 41 commits, all pushed to https://github.com/talha-ather-pk/PocketBill (public, D-006)._

This file is a one-place summary. The detailed sources are [PROJECT_STATUS.md](PROJECT_STATUS.md) (next actions, verification log), [ROADMAP.md](ROADMAP.md) (all tasks), [DECISIONS.md](DECISIONS.md) and [TEST_PLAN.md](TEST_PLAN.md).

---

## 1. At a glance

| Area | State |
|---|---|
| Phase 0 – Environment and setup | ✅ Complete |
| Phase 1 – Research and validation | ✅ Complete (P1-T07 gate passed, 5 of 6) |
| Phase 2 – UX design | 📝 All 8 sections written (UX_SPEC 1–8); some still awaiting founder review |
| Phase 3 – Data model and architecture | 📝 All 5 tasks written (DATA_MODEL 1–7, ARCHITECTURE); awaiting founder review |
| Phase 4 – MVP implementation | 🔨 In progress: 7 of 14 tasks done or largely covered; PDF, sending, backup and Home remain |
| Phases 5–9 – QA, monetization, analytics, release, launch | ⏳ Not started |

**Tests:** 171 unit tests and 37 on-phone tests, all passing (verified by Claude on the Galaxy A72, 2026-10-17).

**Database:** version **2** on the founder's phone, which holds a real business profile and clients. Every schema change from now on needs a migration and a test (D-016).

---

## 2. Phase by phase

### Phase 0 — Environment and project setup ✅
| Task | Result |
|---|---|
| P0-T01 Inspect environment | Done ([ENVIRONMENT.md](ENVIRONMENT.md)). Windows 11, JDK 17 at `C:\java`. |
| P0-T02 Technology stack | Verified by build: Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21, compileSdk/targetSdk 37, minSdk 26, Compose BOM 2026.09.00 (D-005). |
| P0-T03 Android project | Builds, installs and runs on the Galaxy A72 (Android 14). |
| P0-T04 Git and recovery | Local repo on `main`, `.gitignore`/`.gitattributes`, pushed to GitHub; README "Saving your work". Repo public at the founder's request (D-006). |

### Phase 1 — Market validation and product definition ✅
| Task | Result | File |
|---|---|---|
| P1-T01 Competitor research | Saldo, JobFLEX, Wow Invoice, Zoho Invoice, with Play data and dated reviews | [MARKET_RESEARCH.md](MARKET_RESEARCH.md) |
| P1-T02 User problems | 8 evidence-rated problems (data loss strongest), 7 assumptions | [USER_PROBLEMS.md](USER_PROBLEMS.md) |
| P1-T03 Personas | Primary: independent remote freelancer. Secondary: side-gig freelancer. Anti-personas listed. | [PERSONAS.md](PERSONAS.md) |
| P1-T04 Differentiation | No single unique claim; the combination (free, no ads, no cap, offline, no account) is the position against Zoho | [DIFFERENTIATION.md](DIFFERENTIATION.md) |
| P1-T05 Validation | 8 interviews; 6/8 used phones for quick tasks; "offline, no sign-up" pitch ranked #1; first invoices created on laptops | [VALIDATION_PLAN.md](VALIDATION_PLAN.md), [VALIDATION_RESULTS.md](VALIDATION_RESULTS.md) |
| P1-T06 MVP specification | Draft 2, all questions answered | [MVP_SPEC.md](MVP_SPEC.md) |
| P1-T07 Phone-creation gate | **Passed**: 5 of 6 made a real invoice on the phone in 42–55 s (median 50 s). T3 sending re-check 5 of 6, accepted (D-013). | VALIDATION_RESULTS.md |

A clickable web prototype was built for testing: https://claude.ai/artifact/Uv6we4zAj9HssR9cLHk2A4 (source: `prototype/pocketbill-prototype.html`, version 3 with the email-first sending design).

### Phase 2 — UX design 📝 ([UX_SPEC.md](UX_SPEC.md))
| Task | Section | Built in |
|---|---|---|
| P2-T01 Navigation | 1 | P4-T01 |
| P2-T02 Home | 2 | **Not built yet** (section 4) |
| P2-T03 Invoice editor | 3 | P4-T06 |
| P2-T04 Sending | 4 | Prototype only; app waits for P4-T08/T09 |
| P2-T05 History & statuses | 5 | P4-T10/T11 |
| P2-T06 Clients | 6 | P4-T05 |
| P2-T07 Accessibility & formatting | 7 | Partly (labels, contrast, formatting); full TalkBack pass in Phase 5 |
| P2-T08 Design tokens | 8 | P4-T01 |

### Phase 3 — Data model and architecture 📝
| Task | Where |
|---|---|
| P3-T01 Room schema | [DATA_MODEL.md](DATA_MODEL.md) 1–4 |
| P3-T02 Snapshots & numbering | DATA_MODEL 5 |
| P3-T03 Money rules | DATA_MODEL 6 |
| P3-T04 Layer responsibilities | [ARCHITECTURE.md](ARCHITECTURE.md) |
| P3-T05 Migrations & testing | DATA_MODEL 7 (with version history) |

### Phase 4 — MVP implementation 🔨
| Task | What it covers | State | Evidence |
|---|---|---|---|
| P4-T01 App shell | Theme tokens (contrast-tested), type-safe navigation for every screen, per-tab back stacks, AppContainer, Clock, Saving/Draft-saved indicator | ✅ Verified | 57 unit tests; checked on the phone by screenshots |
| P4-T02 Calculations | `domain/money`: 48 currencies, parser, half-up calculator, locale-independent formatter | ✅ Unit tested | 45 tests incl. 232,000 random cases |
| P4-T03 Room persistence | Schema, DAOs, Invoice/Client/Profile repositories, atomic issuing, numbering, frozen details | ✅ Verified | 20 on-phone tests |
| P4-T04 Business profile & settings | First-launch Setup, Settings tab, Business profile editor, "update unpaid invoices" dialog | ✅ Verified | Checked on the phone; founder confirmed the empty-name error |
| P4-T05 Clients | Clients tab (search, sort, archived), client page, add/edit, duplicate check, preferred currency/terms (D-015), database v2 migration (D-016) | ✅ Verified | 25 on-phone tests incl. migration; founder checked by hand |
| P4-T06 Invoice editor | Client defaults, items, live totals, tax, discount, saved items, autosave, issue with checks, number preview, edit issued, duplicate | ✅ Verified | 128 unit tests; on-phone tests and hands-on check **reported passing by the founder** |
| P4-T07 Numbering & dates | Numbering rules and preview, date formatting | ✅ Largely covered by P4-T03/T06 | Remaining date edge cases go into Phase 5 |
| P4-T08 PDF generation | Real invoice PDFs (A4/Letter, pages, stamps, character check) and the PDF preview screen | 🟡 Implemented | 25 unit + 6 on-phone tests pass (D-007 glyphs, 50 items < 2 s); visual check by eye and logo still to do |
| P4-T09 PDF save/share | Save as file; Email to client / Send another way (UX_SPEC 4) | 🟡 Implemented | 15 unit tests pass; Gmail/WhatsApp/Save check on the phone pending (D-017) |
| P4-T10 Status tracking | Invoice page: mark paid + Undo, change paid date, mark unpaid, void + 6 s Undo, delete draft | 🟡 Implemented | Unit and on-phone tests pass; hands-on check pending |
| P4-T11 History & search | Invoices tab: filters (incl. Void) with counts, search, totals, month groups; history block | 🟡 Implemented | Unit and on-phone tests pass; hands-on check pending |
| P4-T12 Backup & restore | Backup file, validated restore, backup reminder | ⏳ Not started | — |
| P4-T13 Empty/loading/error states | Review across all screens | ⏳ Not started | Many screens already have them |
| P4-T14 Permissions & manifest | Confirm no permissions, no INTERNET, allowBackup off | ⏳ Not started | Manifest currently has none of them |

---

## 3. What the app does today

**Works now:**
- First launch asks for your business details, then opens Home.
- **Clients:** add, edit, search, archive or delete. Duplicate-name warning. Optional preferred currency and terms per client.
- **Invoice editor:**
  - pick a client (defaults filled in), add items, set tax and discount, see live totals
  - autosaves drafts, shows "Will be INV-000X"
  - issues with field-by-field checks, and edits issued invoices
  - copies (duplicates) invoices, suggests saved items
- **Invoices tab:** filters with counts, search, totals per currency.
- **Invoice page:**
  - shows all details and a short history
  - mark paid or unpaid (with Undo, and Change paid date), void (with Undo), delete draft, edit, duplicate
- **Settings:** business profile (all fields), and the privacy statement.

**Placeholders for now:**
- **Home** is still the early version. It doesn't yet show amounts owed, overdue items, "Needs attention" or recent invoices.
- **Saved items** and **Backup** in Settings say "coming in a later build step".

---

## 4. Decisions on record ([DECISIONS.md](DECISIONS.md))
| ID | Decision |
|---|---|
| D-001 | Project moved out of OneDrive to `C:\dev\PocketBill`; GitHub is the backup |
| D-002 | Application ID `com.example.pocketbill` is a placeholder; final ID before the first Play upload |
| D-003 | Stack: Kotlin, Compose, Material 3, MVVM, Room, PdfDocument, FileProvider |
| D-004 | The founder's phone is the main test device |
| D-005 | Toolchain versions (verified by build) |
| D-006 | GitHub repository is public; never commit secrets |
| D-007 | Target: global freelancers, English-only invoices; accented names and currency symbols required, other scripts nice-to-have |
| D-008 | Proceed to MVP spec on founder-reported validation |
| D-009 | No quotes in the MVP; Android cloud backup off; one tax rate per invoice |
| D-010 | Phone-first creation, gated by P1-T07; sync-ready data model |
| D-011 | No invoice activity log in the MVP |
| D-012 | Money code started before P1-T07 finished |
| D-013 | T3 sending re-check accepted at 5 of 6 |
| D-014 | (Replaced by D-016) Database version 1 open until first release |
| D-015 | Optional preferred currency and terms per client |
| D-016 | Every schema change now gets a migration and a test (founder has real data) |

---

## 5. Testing summary ([TEST_PLAN.md](TEST_PLAN.md))

**Unit tests on the computer: 146, all pass.**
| Area | Test classes |
|---|---|
| Money | AmountParserTest, CurrencyCodeTest, InvoiceCalculatorTest, MoneyFormatterTest, MoneyPropertyTest |
| Domain | InvoiceRulesTest, DateFormatterTest, ClockTest, DomainPurityTest |
| Theme and navigation | ThemeContrastTest, TopLevelDestinationTest |
| Settings | ProfileFormTest |
| Clients | ClientFormTest, ClientViewModelsTest |
| Editor | EditorFormTest, InvoiceEditorViewModelTest |
| Invoices tab and page | InvoicesTest |

**On-phone tests (Galaxy A72), 31 in total:**
- **Classes:** RepositoryTest (30) and MigrationTest (1).
- **25 verified by Claude** (P4-T03 and P4-T05, including the v1 → v2 migration).
- **3 reported passing by the founder** (P4-T06: number preview, client's last invoice, saved items).
- **3 not run yet** (P4-T10/T11: change paid date, undo void, line descriptions for search).

**Rule for phone runs:** always use `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`. Without it, Gradle uninstalls the app, which would delete the founder's data.

---

## 6. Remaining work

### Phase 4 (in suggested order)
1. **Run the 3 pending on-phone tests.** Needs the phone connected; right now Windows doesn't see it at all.
2. **P4-T08 PDF generation.**
   - Android's built-in `PdfDocument`; A4 and US Letter; multiple pages.
   - Paid, void and draft stamps.
   - Test that accented names and currency symbols print correctly (D-007).
3. **P4-T09 Sending.**
   - "Email to client", with the address, subject and message prefilled.
   - "Send another way"; recording last sent; the reminder template.
   - Save PDF to a file.
4. **Home screen** (UX_SPEC 2; needs a roadmap task ID):
   - amount owed per currency, and the overdue row
   - "Needs attention" (Remind / Email / Continue)
   - cards for "payment details changed" and "back up your invoices"
   - recent invoices
5. **P4-T12 Backup & restore** (MVP_SPEC 7, DATA_MODEL 7.4):
   - a versioned JSON file
   - restore that checks everything before changing anything
   - an automatic safety copy, and a backup reminder
6. **Saved items screen** in Settings (manage presets).
7. **P4-T13** empty, loading and error states review; **P4-T14** permissions and manifest review.

### Later phases
- **Phase 5 – QA:**
  - full TalkBack walk-through, and 200% font size
  - device and lifecycle tests
  - a PDF test across invoice types
  - update-over-previous-version test
  - the security and privacy review
- **Phase 6 – Monetization:** free tier, Pro features, ads decision (no ads in the editor or PDFs), billing if justified.
- **Phase 7 – Analytics:** minimal and privacy-preserving, or none.
- **Phase 8 – Google Play release:**
  - final application ID and branding
  - store listing, privacy policy, Data Safety form
  - signed app bundle, and backing up the signing key
- **Phase 9 – Launch and growth.**

### Open items for the founder
- Reconnect the phone (USB data cable, File transfer mode, USB debugging allowed).
- Review the design docs: UX_SPEC 1–8, DATA_MODEL 1–7, ARCHITECTURE.
- A safety copy of the pre-v2 database is in the app's private storage (`files/pre-migration-v1`). It can be removed once v2 has been in use for a while.

---

## 7. How to resume
1. Read [PROJECT_STATUS.md](PROJECT_STATUS.md), "Exact next action" and the resume prompt.
2. Build and test: `.\gradlew.bat assembleDebug testDebugUnitTest`, with `JAVA_HOME=C:\java`.
3. Phone tests: `.\gradlew.bat connectedDebugAndroidTest "-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true"`.
4. Working rules:
   - small numbered tasks
   - never fake a test result
   - record every decision in DECISIONS.md
   - commit and push after each task
   - explain things simply (the founder is new to Windows and Android development)
