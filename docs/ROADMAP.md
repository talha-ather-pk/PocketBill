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
| P1-T01 | Research competitors (sourced, dated) → `MARKET_RESEARCH.md` | In progress — first pass done; Play Store manual pass needed |
| P1-T02 | Organize user problems (evidence vs hypothesis) | Not started |
| P1-T03 | Primary + secondary persona | Not started |
| P1-T04 | Test differentiation claims against competitors | Not started |
| P1-T05 | 7-day validation plan (interviews, prototype, usability script) | Not started |
| P1-T06 | MVP specification → `MVP_SPEC.md` | Not started |

Phase 1 research can run in parallel with Phase 0 setup, because it does not need Android Studio.

## Phase 2 — Product design and UX
P2-T01 Navigation · P2-T02 Home · P2-T03 Invoice editor · P2-T04 PDF preview/sharing · P2-T05 History & statuses · P2-T06 Client management · P2-T07 Accessibility & localization · P2-T08 Design tokens — all Not started.

## Phase 3 — Data model and architecture
P3-T01 Room schema · P3-T02 Historical snapshots & numbering · P3-T03 Money representation & rounding · P3-T04 Layer responsibilities · P3-T05 Migrations — all Not started.

## Phase 4 — MVP implementation
P4-T01 App shell · P4-T02 Calculations (tested) · P4-T03 Room persistence · P4-T04 Business profile/settings · P4-T05 Clients · P4-T06 Invoice editor · P4-T07 Numbering & dates · P4-T08 PDF generation · P4-T09 PDF save/share · P4-T10 Status tracking · P4-T11 History & search · P4-T12 Backup & restore · P4-T13 Empty/loading/error states · P4-T14 Permissions & manifest review — all Not started.

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
