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

---

## P1-T07 phone-creation check (gate for Phase 4, D-010)

**Status (2026-10-16): complete, all 6 reported. PASSED: 5 of 6 created a real invoice on the phone (pass mark ≥ 3).**

Founder-reported from the participants' session logs. Only P-a's and P-b's feedback was given word for word; the others are the founder's summaries.

| Participant | Created invoice? | Creation time | Feedback |
|---|---|---|---|
| P-a | **Yes** | 48 s | "Much faster than expected on a mobile screen, though typing items takes a bit of focus." |
| P-b | **Yes** | 50 s | "Fairly straightforward, though finding the currency dropdown took a quick glance." |
| P-c | **Yes** | 55 s | *Summary:* was looking for the tax rate field |
| P-d | **Yes** | 42 s | *Summary:* fast layout; live totals work well |
| P-e | **Gave up** | stopped at 120 s | *Summary:* unsure whether line items were saved automatically |
| P-f | **Yes** | 51 s | *Summary:* adding a client felt very natural |

| Measure | Result | Pass mark | Verdict |
|---|---|---|---|
| Created a real invoice on the phone | **5 of 6** | ≥ 3 of 6 | ✅ **Passed** |
| Creation time (those who finished) | 42–55 s; median 50 s | ≤ 60 s repeat client, ≤ 2 min new client (UX_SPEC 3) | ✅ All 5 under 60 s (existing or new client not reported) |
| Gave up | 1 (P-e) | — | See findings |

**What this means:** the D-010 gate is passed, so Phase 4 can start as specified. Five people built a real invoice on the phone in under a minute, which supports the phone-first decision. What P1-T07 does **not** show is whether they'll keep choosing the phone over the laptop in daily use. Only P-b's comment speaks to that directly.

**Findings for the design** (each is already covered in UX_SPEC 3; the prototype lacks it):
| Finding | Who | Covered by |
|---|---|---|
| Typing items takes focus | P-a | Preset suggestions (F-18) |
| Currency control took a moment to find | P-b | Currency prefilled from the client and always shown in words on the summary line |
| Looked for the tax field | P-c | Tax shown on the summary line ("No tax" / "20% tax") |
| Unsure whether items were saved; gave up | P-e | The "Draft saved" indicator and autosave. **Make it clearly visible**: this was the one failure. |

## T3 sending re-check (UX_SPEC 4)

**Status (2026-10-16): complete, 6 tested. Pass mark NOT met as written: 5 of 6 sent unaided in under 30 s; the rule required all of them.**

| Participant | T3 time | Help needed? | Under 30 s unaided? |
|---|---|---|---|
| P-a | 22 s | No | ✅ |
| P-b | 25 s | No | ✅ |
| P-c | 21 s | No | ✅ |
| P-d | 19 s | No | ✅ |
| P-e | 38 s | **Yes** | ❌ |
| P-f | 26 s | No | ✅ |

| Measure | Result | Pass mark | Verdict |
|---|---|---|---|
| Sent unaided in under 30 s | **5 of 6** (median 22 s) | All tested participants | ❌ **Not met as written** |
| Gave the new client an email in T1 | Not reported | — | — |
| Where they expected the send button | Not reported | — | — |

**What this means:** sending is much better than in the original test, where T3 caused the most friction: 5 of 6 now finish in about 20 seconds without help. The one failure is P-e, the same participant who gave up on creating the invoice. What went wrong for P-e in T3 isn't recorded. Before treating sending as solved, the founder should decide whether P-e's difficulty was specific to sending or general (see PROJECT_STATUS.md). The earlier "passed on the minimum sample" note (2 of 2) is replaced by this result.
