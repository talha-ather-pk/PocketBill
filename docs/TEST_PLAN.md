# Test Plan

**Status: early.** Money code (P4-T02) is unit tested; full coverage is planned for Phase 5.

Verification levels used in this project (stated explicitly for every result):
Implemented · Compiled · Unit tested · Instrumentation tested · Tested on emulator · Tested on physical device · Manually reviewed · Not tested

Planned coverage (detailed in Phase 5):
- Unit: money arithmetic, rounding, currency precision, discounts, tax, totals, due dates/overdue, status transitions, numbering, validation, backup serialization, restore validation.
- Database: CRUD, invoice↔line-item relations, foreign keys, migrations, persistence across restart.
- UI: first launch → business setup → client → invoice → draft → reopen → PDF export → share → mark paid → dashboard → backup → restore.
- PDF matrix: many items, long text, multi-page, large totals, multiple currencies, empty optional fields.
  - **MVP (D-007):** accented Latin names (José, Müller, Øresund) and currency symbols (€ £ ¥ ₹ ₦ ₱ ₨) render correctly, with no empty boxes (□).
  - **Nice-to-have:** Urdu and Arabic (right-to-left), Bengali, Hindi. Source: Wow Invoice Play review 2026-07-20 (MARKET_RESEARCH.md).
- Devices: at least one emulator and one physical phone.

## Results

| Date | What | Level | Result |
|---|---|---|---|
| 2026-10-16 | `domain/money` (P4-T02): calculator C-1 to C-14, parser (DATA_MODEL 6.3), formatter incl. locale independence, currency table vs JDK ISO 4217 data, property tests (232,000 random cases), domain purity check | Unit tested (computer, JDK 17) | **47 of 47 tests pass** (45 new + 2 existing), `gradlew testDebugUnitTest` |
| 2026-10-16 | P4-T01 app shell: theme contrast (both themes + status colours, WCAG AA), routes serializable and tab order, bottom bar hidden on full-screen tasks, Clock | Unit tested (computer) | **57 of 57 tests pass** (10 new), `gradlew assembleDebug testDebugUnitTest` |
| 2026-10-17 | P4-T01 on the Galaxy A72 (Android 14, dark mode): Create invoice opens the editor with the bottom bar hidden; Back returns to Home; Invoices and Settings tabs open with the right tab highlighted; Back from Settings returns to Home; no crashes | Tested on physical device (adb taps + screenshots) | **Pass.** Not covered: light mode, re-tapping a tab from a deeper screen (no deeper screens reachable yet) |
| 2026-10-17 | P4-T03 rules: numbering format and first-free number (N-2, N-7), snapshot JSON (round trip, unknown fields, unreadable), overdue rule, name matching | Unit tested (computer) | **64 of 64 unit tests pass** (7 new) |
| 2026-10-17 | P4-T03 repositories on SQLite (`RepositoryTest`, 20 tests: invariants, issuing, numbering, frozen details, status changes, Home queries) | Instrumented, on the Galaxy A72 (Android 14) | **20 of 20 pass** (`gradlew connectedDebugAndroidTest`, in-memory database; app data untouched) |
| 2026-10-17 | P4-T04 profile form: required name, typed tax rates, next number not below current, prefix length, round trip, frozen/payment change detection, suggested currency | Unit tested (computer) | **72 of 72 unit tests pass** (8 new) |
| 2026-10-17 | P4-T04 on the Galaxy A72: fresh install opens on Setup; a profile filled in by hand saved and opened Home; after restart the app opens on Home (profile remembered); Settings shows the business name; Business profile shows saved fields; profile edits saved; no crashes. Fixed after first look: doubled top gap on Business profile, lighter band behind Settings rows | Tested on physical device (adb + screenshots, plus founder using the app) | **Pass.** Not yet seen on the phone: the name-required error message, and the "update unpaid invoices" dialog (needs issued invoices, which no screen can create yet) |
| 2026-10-17 | P4-T05 clients: client form rules, list search/sort/archived, detail owed and delete rules, editor duplicate and update-unpaid prompts (ViewModels with a fake store), date formatting | Unit tested (computer) | **94 of 94 unit tests pass** (22 new) |
| 2026-10-17 | P4-T05 database: preferred currency/terms, field validation, owed and overdue per client and currency, same-name check excluding self; **migration v1 → v2** with real v1 rows | Instrumented, on the Galaxy A72 | **25 of 25 pass** (RepositoryTest 24, MigrationTest 1); app left installed (`leaveApksInstalledAfterRun`) so the founder's data was kept |
| 2026-10-17 | P4-T06 invoice editor: live recalculation (T2 €570, unreadable lines, comma decimals, JPY), tax once on subtotal, % and amount discounts with cap, issue checks per field, draft input, D-015 default order, saved-invoice and duplicate forms; ViewModel: 500 ms debounced autosave and Saving/Draft saved states, save on background, client defaults vs hand edits, issue flow and zero-total prompt, number advancing, back/delete, issued-invoice editing, duplicates, saved-item suggestions | Unit tested (computer) | **128 of 128 unit tests pass** (34 new) |
| 2026-10-17 | P4-T06 database: next-number preview equals the issued number (incl. prefix and starting-number changes), client's last invoice, saved items (suggestions, most used first, no duplicates) | Instrumented, on the Galaxy A72 | **Pass, founder-reported** (run by the founder; Claude could not re-run it: phone disconnected). Also hands-on: creating items, autosave indicator, issuing |
| 2026-10-17 | P4-T10/T11 Invoices tab and detail: every filter and chip count, sort per filter, month groups, totals per currency (never mixed), frozen vs live client names, search (number, names, items; accents; across statuses), list updates on status change; detail: mark paid + Undo, change paid date, mark unpaid + Undo restores date, void confirm + 6 s Undo and expiry, invalid transitions refused, delete draft, edit availability, history, overdue | Unit tested (computer) | **146 of 146 unit tests pass** (18 new) |
| 2026-10-17 | P4-T10/T11 database: change paid date, undo void, line descriptions for search | Instrumented, written and compiled | **Not yet run**: phone not connected |

Not yet checked on the phone: formatting runs the same pure-Kotlin code on Android, but an instrumented check on the Galaxy A72 is still to be added (P5-T01).
