# Test Plan

**Status: early. One unit test class exists (`TopLevelDestinationTest`); full coverage is planned for Phase 5.**

Verification levels used in this project (stated explicitly for every result):
Implemented · Compiled · Unit tested · Instrumentation tested · Tested on emulator · Tested on physical device · Manually reviewed · Not tested

Planned coverage (detailed in Phase 5):
- Unit: money arithmetic, rounding, currency precision, discounts, tax, totals, due dates/overdue, status transitions, numbering, validation, backup serialization, restore validation.
- Database: CRUD, invoice↔line-item relations, foreign keys, migrations, persistence across restart.
- UI: first launch → business setup → client → invoice → draft → reopen → PDF export → share → mark paid → dashboard → backup → restore.
- PDF matrix: many items, long text, multi-page, large totals, multiple currencies, empty optional fields, and non-Latin text: **Urdu and Arabic (right-to-left), Bengali, Hindi**, with a check that no character renders as an empty box (□). Source: Wow Invoice Play review 2026-07-20 (MARKET_RESEARCH.md).
- Devices: at least one emulator and one physical phone.

## Results

None yet.
