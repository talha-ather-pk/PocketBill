# Validation Results (P1-T05)

_Recorded 2026-10-15._

## Status of this record

**Founder-reported results (summary 2026-10-15, follow-up 2026-10-16). Some per-assumption counts are still not provided.** Raw notes stay in `research-private/` (git-ignored). Only anonymized, aggregated results belong here (VALIDATION_PLAN.md, "Notes: privacy").

## Founder summary (as reported)

> "Our validation interviews and prototype testing are complete, confirming strong demand for an offline-first, ad-free, account-free invoicing app with solid currency and accented character support (Decision D-007), while validating mobile usage patterns for quick tasks (Assumption A-02)."

## Founder follow-up (2026-10-16): answers to MVP_SPEC Q-01 and Q-02

- **8 interviews** completed.
- **6 of 8 used phones for quick tasks:** status checks, quick edits, and re-sending PDFs on the go.
- **Initial invoices are mostly created on laptops** (Q-01).
- **Pitch ranking:** P-C "offline, no sign-up" ranked **#1** overall.
- **Usability:** **T3 (share/send) caused the most friction.** The nature of the friction and the per-task counts were not given.

## Verdict per assumption

| Assumption | Result | Threshold | Verdict |
|---|---|---|---|
| A-02 mobile invoicing | 6/8 used a phone for quick tasks, **including re-sending PDFs**. **Creation happens mostly on laptops.** | ≥ 4/8 created **or sent** one of their last 3 invoices on a phone | ✅ **Met as written** (sending counts). But the finding behind it is narrower than the persona assumed: the phone is used to **check and send**, not to **create**. See MVP_SPEC.md Q-06. |
| A-04 offline / no account valued | P-C (offline, no sign-up) ranked #1 | ≥ 3/8 | ✅ Supported (the exact count of first-choice votes was not given) |
| A-01 pain strong enough to switch | "Strong demand" (founder summary) | ≥ 4/8 | ⚠️ Count not given |
| A-03 payment tracking pain | Not reported | ≥ 4/8 | ❓ Unknown |
| A-07 multiple currencies | "Solid currency support" confirmed as needed | n/a | ✅ Supported (count not given) |
| Usability | T3 share/send had the most friction | ≥ 4/5 tasks unaided by ≥ 6/8 | ⚠️ Counts not given. **T3 needs redesign** (MVP_SPEC F-06). |

## Tally (optional: fill in from research-private/ notes for the remaining counts)

| | Total | Threshold | Verdict |
|---|---|---|---|
| A-02: ≥1 of last 3 invoices created or sent on a phone | **6 / 8** (quick tasks incl. re-sending) | ≥ 4 | Met |
| A-01: concrete recent pain + has tried an alternative | | ≥ 4 | |
| A-04: local/offline/account raised unprompted, or P-A/P-C ranked first | P-C ranked #1 | ≥ 3 | Supported |
| A-03: can't say instantly who owes them | | ≥ 4 | |
| Usability: ≥ 4/5 tasks unaided | | ≥ 6 | |

## Decision

The founder decided to proceed to P1-T06 (MVP specification) on the basis of this summary. See DECISIONS.md D-008. Filling in the tally above lets MVP_SPEC.md's priorities be checked against the actual counts.
