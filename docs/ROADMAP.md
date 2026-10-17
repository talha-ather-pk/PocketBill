# PocketBill — Roadmap

Priority order: **validate the problem → build the core invoice workflow → verify correctness and data safety → test with real users → prepare release → monetize responsibly → grow.**

Status values: Not started · In progress · Blocked · Implemented · Verified

## Phase 0 — Environment and project setup
| ID | Task | Status |
|---|---|---|
| P0-T01 | Inspect the environment | Verified |
| P0-T02 | Recommend the technology stack (verify current versions) | Verified |
| P0-T03 | Create the Android project (compile + launch) | Verified |
| P0-T04 | Git and recovery practices | Verified |

## Phase 1 — Market validation and product definition
| ID | Task | Status |
|---|---|---|
| P1-T01 | Research competitors (sourced, dated) → `MARKET_RESEARCH.md` | Verified (founder-collected Play data, 4 apps; known gaps listed) |
| P1-T02 | Organize user problems (evidence vs hypothesis) → `USER_PROBLEMS.md` | Implemented: updated for D-007; awaiting founder review |
| P1-T03 | Primary + secondary persona → `PERSONAS.md` | Implemented: awaiting founder review |
| P1-T04 | Test differentiation claims against competitors → `DIFFERENTIATION.md` | Implemented: awaiting founder review |
| P1-T05 | 7-day validation plan (interviews, prototype, usability script) → `VALIDATION_PLAN.md` | Done (founder-reported; tally pending in `VALIDATION_RESULTS.md`) |
| P1-T06 | MVP specification → `MVP_SPEC.md` | Implemented: draft 2, all questions answered (D-009, D-010) |
| P1-T07 | Phone-creation check: 6 phone users create a real invoice in the prototype (gate for Phase 4, D-010) | **Passed** (founder-reported): 5 of 6 created an invoice on the phone, 42–55 s. T3 re-check: 5 of 6, not met as written (P-e) |

Phase 1 research can run in parallel with Phase 0 setup, because it does not need Android Studio.

## Phase 2 — Product design and UX
P2-T01 Navigation (`UX_SPEC.md`: draft, awaiting review) · P2-T02 Home (`UX_SPEC.md` 2: draft) · P2-T03 Invoice editor (`UX_SPEC.md` 3: draft, awaiting review) · P2-T04 Sending (`UX_SPEC.md` 4: draft, awaiting review) · P2-T05 History & statuses (`UX_SPEC.md` 5: draft) · P2-T06 Clients (`UX_SPEC.md` 6: draft) · P2-T07 Accessibility & formatting (`UX_SPEC.md` 7: draft) · P2-T08 Design tokens (`UX_SPEC.md` 8: draft). **All Phase 2 tasks drafted; awaiting founder review.**

## Phase 3 — Data model and architecture
P3-T01 Room schema (`DATA_MODEL.md` 1–4: draft) · P3-T02 Snapshots & numbering (`DATA_MODEL.md` 5: draft) · P3-T03 Money (`DATA_MODEL.md` 6: draft) · P3-T04 Layer responsibilities (`ARCHITECTURE.md`: draft) · P3-T05 Migrations (`DATA_MODEL.md` 7: draft). **All Phase 3 tasks drafted; awaiting founder review.**

## Phase 4 — MVP implementation
P4-T01 App shell: **Verified** (57 unit tests; checked on the Galaxy A72) · P4-T02 Calculations (tested): **unit tested, 45 tests pass** (`domain/money`; started early under D-012) · P4-T03 Room persistence: **implemented** (schema v1, DAOs, repositories; 64 unit tests pass); 20 on-phone tests written, not yet run · P4-T04 Business profile/settings · P4-T05 Clients · P4-T06 Invoice editor · P4-T07 Numbering & dates · P4-T08 PDF generation · P4-T09 PDF save/share · P4-T10 Status tracking · P4-T11 History & search · P4-T12 Backup & restore · P4-T13 Empty/loading/error states · P4-T14 Permissions & manifest review — all Not started.

## Phase 5 — QA and stability
P5-T01 Unit tests · P5-T02 Database tests · P5-T03 UI tests · P5-T04 PDF matrix · P5-T05 Device & lifecycle · P5-T06 Security & privacy review — all Not started.

## Phase 6 — Monetization (only after core workflow is reliable)
P6-T01 Free tier · P6-T02 Pro features · P6-T03 Ads evaluation · P6-T04 Billing (if justified) · P6-T05 Revenue model — all Not started.

## Phase 7 — Analytics and feedback
`ANALYTICS_PLAN.md`, minimal privacy-preserving metrics — Not started.

## Phase 8 — Google Play release
P8-T01 Identity & branding · P8-T02 Store listing · P8-T03 Privacy policy & Data Safety · P8-T04 Compliance · P8-T05 Testing-track requirements · P8-T06 Signed bundle · P8-T07 Release checklist — all Not started.

## Phase 9 — Launch and growth
P9-T01 Channels · P9-T02 Launch content · P9-T03 Feedback process · P9-T04 30-day experiment plan — all Not started.
