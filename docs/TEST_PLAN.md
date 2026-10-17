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

Not yet checked on the phone: formatting runs the same pure-Kotlin code on Android, but an instrumented check on the Galaxy A72 is still to be added (P5-T01).
