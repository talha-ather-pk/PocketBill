# PocketBill — Project Status

_Last updated: 2026-10-17_

## Current phase

**Phase 0 — Environment discovery and project setup: complete (2026-10-15)**

## Task status

| Task | Name | Status | Notes |
|---|---|---|---|
| P0-T01 | Inspect the environment | **Verified** | Results in [ENVIRONMENT.md](ENVIRONMENT.md). Inspection commands were actually run on this machine. |
| P0-T02 | Recommend the technology stack | **Verified** | Toolchain versions confirmed by a successful build (D-005). Room 2.8.5 still proposed; confirmed in P4-T03. |
| P0-T03 | Create the Android project | **Verified** | Builds, unit tests pass, installed and launched on Galaxy A72 (Android 14); all 4 tabs open with no crash (2026-10-15). |
| P0-T04 | Git and recovery practices | **Verified** | Local repo on `main`; `.gitignore`/`.gitattributes`; README "Saving your work". Pushed to https://github.com/talha-ather-pk/PocketBill. Repo made **public** 2026-10-15 at the founder's request (D-006). |

**Phase 1 progress:** P1-T01 to P1-T06 done or drafted; MVP_SPEC.md **draft 2** with all questions answered (D-009, D-010). Validation results in [VALIDATION_RESULTS.md](VALIDATION_RESULTS.md) (A-02 met as written: 6/8, but creation happens on laptops). **P1-T07 phone-creation check is the gate for Phase 4.** Target market: D-007. Later phases: see [ROADMAP.md](ROADMAP.md).

## Failed tasks

None.

## Blocked tasks

None.

## Important decisions

See [DECISIONS.md](DECISIONS.md). Accepted: project moved to `C:\dev\PocketBill` (D-001); physical phone is primary test device (D-004).

## Verification log

| Date | What was verified | How |
|---|---|---|
| 2026-10-15 | Tool inventory, JDK version, Git version, folder contents, disk/RAM/virtualization | PowerShell commands run by Claude Code on the laptop |
| 2026-10-15 | JAVA_HOME fixed to `C:\java`; docs moved to `C:\dev\PocketBill` | Read back env var; listed copied files |
| 2026-10-15 | `winget install Google.AndroidStudio` (v2026.2.1.8 found) **failed, exit 65**: package licence agreement not accepted | Nothing was installed. |
| 2026-10-15 | Founder accepted the licence; Android Studio 2026.2.1.8 installed (exit 0); `studio64.exe` present | winget output plus file check. SDK not installed yet. |
| 2026-10-17 | P4-T12 backup & restore: testDebugUnitTest 207/207 (12 new incl. a frozen v1 sample file). Tests caught a real bug (backup creation time overwrote each record's createdAt), fixed. 5 on-phone tests written (round trip, safety copy, bad files, half-way failure rollback, reminder) but **not run**: phone disconnected | Test XML reports |
| 2026-10-17 | P4-T15 Home: testDebugUnitTest 195/195 (9 new). Not checked on the phone (disconnected) | Test XML reports |
| 2026-10-17 | P4-T09 sending: testDebugUnitTest 186/186 (15 new: templates, send flow, Issue and send). Build passes. **Not checked on the phone** (disconnected): Gmail opening directly with the PDF attached, WhatsApp via the share sheet, Save PDF | Test XML reports |
| 2026-10-17 | P4-T08 PDF: testDebugUnitTest 171/171 (25 new); connectedDebugAndroidTest on SM-A725F 37/37 (6 new PDF tests: D-007 glyphs, A4/Letter sizes, 50 items in <2 s, marks); also ran the 3 pending P4-T10/T11 tests: pass. App kept installed. Visual check of the PDF layout **not done** (phone disconnected mid-run) | Test XML reports |
| 2026-10-17 | P4-T10/T11: testDebugUnitTest 146/146 (18 new); on-phone tests not run (phone disconnected) | Test XML reports |
| 2026-10-17 | P4-T06 on-phone tests and hands-on check: pass | **Founder-reported** (Claude could not re-run: phone disconnected) |
| 2026-10-17 | P4-T06: testDebugUnitTest 128/128 (34 new); on-phone tests not run (phone disconnected) | Test XML reports |
| 2026-10-17 | P4-T05: 94/94 unit tests; connectedDebugAndroidTest on SM-A725F 25/25 (incl. v1→v2 migration); app kept installed, founder data untouched, safety copy made | Test XML reports + `run-as ls` before/after |
| 2026-10-17 | P4-T04 on SM-A725F: Setup on fresh data, profile saved and remembered after restart, Settings and Business profile screens; two layout fixes | adb + screenshots (deleted afterwards: they contained the founder's details) |
| 2026-10-17 | P4-T03 database: 64/64 unit tests; `connectedDebugAndroidTest` on SM-A725F → 20/20 RepositoryTest pass | Test XML reports read back by Claude Code |
| 2026-10-17 | P4-T01 on the Galaxy A72: editor hides the bottom bar, Back and tab navigation correct, dark theme, no crashes | adb install + taps, checked by screenshots (the UI text dump proved unreliable during transitions) |
| 2026-10-16 | P4-T01 app shell: `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 57/57 tests, no compiler warnings | Test XML reports + Gradle problems report read back by Claude Code |
| 2026-10-16 | P1-T07 phone-creation check: 5 of 6 participants created a real invoice on the phone (42–55 s); T3 re-check 5 of 6 under 30 s unaided | Founder-reported from participants' session logs |
| 2026-10-16 | P4-T02 money code: `gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 47/47 tests (45 new) | Test XML reports read back by Claude Code |
| 2026-10-15 | Clickable prototype: tasks T1–T5 and the session log work; reset afterwards | Founder dry run on Galaxy A72 (phone browser) |
| 2026-10-15 | `gradlew installDebug` on SM-A725F (Android 14); app launched, MainActivity resumed, Home/Invoices/Clients/Settings tabs tapped, same process, empty crash log | adb install + `am start`, `input tap`, screenshots, `logcat -b crash` |
| 2026-10-15 | SDK installed (platform android-37.0, build-tools 36.0.0). `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21); `app-debug.apk` produced | Command run by Claude Code with `JAVA_HOME=C:\java` |

## Exact next action

**P1-T07 passed (2026-10-16): 5 of 6 created a real invoice on the phone in 42–55 s.** The D-010 gate is open, so Phase 4 can start. T3 re-check accepted at 5 of 6 (D-013).

1. Founder: review the design drafts (UX_SPEC 1–8, DATA_MODEL 1–7, ARCHITECTURE).
2. P4-T03 Room persistence is **verified**: 64/64 unit tests and 20/20 database tests on the phone.
3. P4-T05 clients is **implemented**: Clients tab (search, A–Z / most owed, archived), client detail (owed per currency, invoices, archive/delete), add/edit with duplicate check and "update unpaid invoices" (D-015 preferences). Database is now **version 2** (D-016). 94/94 unit tests, 25/25 on-phone tests.
4. P4-T06 invoice editor is **verified** (founder ran the on-phone tests and tried it by hand).
5. P4-T10 status tracking and P4-T11 history & search are **implemented**: Invoices tab (filters with counts incl. Void, search, per-currency totals, month groups) and invoice detail (mark paid with Undo and Change date, mark unpaid, void with 6 s Undo, delete draft, duplicate, history). 146/146 unit tests. (The founder called these "P4-T07/T08"; the roadmap IDs are kept.)
6. P4-T08 PDF generation is **implemented and tested**: `pdf/InvoiceDocument` (what the PDF shows; frozen vs live details), `PdfPagination` (header repeated, totals never split), `InvoicePdfRenderer` (PdfDocument; A4/Letter; DRAFT watermark, PAID/VOID stamps; "Page 1 of N"), ASCII file names (`PdfFileName`), and a real **PDF preview** screen. 171 unit tests, 37/37 on-phone tests. Not yet: business logo on the PDF (F-14, no logo picker yet).
7. Founder: reconnect the phone (USB debugging switches itself off), open an invoice → **Preview**, and say whether the PDF looks right (A4 and Letter, a paid one, a draft). Claude's visual check is still owed. Sample files from the interrupted check may be left in `/sdcard/Android/data/com.example.pocketbill/files/samples`; delete them when the phone is back.
8. P4-T09 sending is **implemented**: Email to client (opens the email app directly with address, subject and message filled in; reminder and receipt wording by status), Send another way (share sheet, records the chosen app), Save PDF, "Sent via Gmail" with Undo, Issue and send for drafts, FileProvider with read access for the chosen app only. Decision D-017. 186 unit tests. **Needs a phone check.**
9. P4-T15 Home is **implemented** (UX_SPEC 2): Outstanding per currency (+N more), overdue row, Create invoice, Needs attention (Remind / Email / Send… / Continue in one tap, See all), Recent 5, welcome on first use, "Nothing outstanding". Deferred: the backup card (with P4-T12) and the "payment details changed" card (needs the update to be recorded first).
10. P4-T12 backup & restore is **implemented** (D-018): Settings → Backup and restore (Back up now via the save picker, last backup date; Choose backup file → checked → dialog with counts → Replace data), safety copy before restore, one transaction, Home "Back up your invoices" card. 207 unit tests; **5 on-phone tests waiting for the phone**.
11. Claude: Saved items screen, then P4-T13 (empty/loading/error review) and P4-T14 (permissions/manifest review).

## Resume prompt for a new Claude session

```
I'm building PocketBill, an Android invoice app (Kotlin/Compose). Read README.md and
docs/PROJECT_STATUS.md, docs/ROADMAP.md, docs/DECISIONS.md, docs/ENVIRONMENT.md first.
Phase 0 complete (all tasks verified). Current phase: Phase 4 (MVP implementation). P1-T07 passed (5/6), so the D-010 gate is open. P4-T02 money code done (47 tests). P4-T01 verified on the phone; P4-T03 Room verified (20/20 on phone). P4-T05 clients implemented; DB v2 (D-016). P4-T06 verified; P4-T10/T11 implemented and on-phone tests pass; P4-T08 PDF generation + preview implemented (37 on-phone tests); P4-T09 sending implemented (186 unit tests), phone check pending. P4-T15 Home implemented; P4-T12 backup implemented (207 unit tests, 5 on-phone tests not yet run). Next: Saved items screen, P4-T13, P4-T14. Decisions up to D-018.
Project folder: C:\dev\PocketBill. Continue from "Exact next action" in PROJECT_STATUS.md. Follow the master project prompt
rules: small numbered tasks, never fake verification, explain things simply (I'm a beginner on Windows).
```
