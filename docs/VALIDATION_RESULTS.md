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

**Status (2026-10-16): in progress. 2 of 6 participants reported. Sessions are still running.**

Known so far (founder-reported):
- Follow-up messages sent to the 6 phone-using participants (2026-10-16).
- The browser prototype works for the test. The founder dry-ran it on a Galaxy A72 (T1–T5 and the session log). Version 3 carries over participants' earlier data.

| Measure | Result | Pass mark |
|---|---|---|
| Completed a real invoice on the phone | **2 so far** (P-a, P-b); 4 not yet reported | **≥ 3 of 6** |
| Gave up / didn't try | 0 so far; 4 not yet reported | — |
| Time from "Create invoice" to PDF preview (from session logs) | P-a: 48 s · P-b: 50 s. Others not yet reported. | Target ≤ 60 s for a repeat client, ≤ 2 min for a new client (UX_SPEC 3) |
| Phone vs laptop: one-line answers | P-a, P-b (below) | — |
| **Verdict** | **Pending** | — |

Per participant (codes only; details stay in `research-private/`):

| Participant | Completed? | Time | Phone or laptop, and why |
|---|---|---|---|
| P-a | **Yes** | **48 s** | "Much faster than expected on a mobile screen, though typing items takes a bit of focus." |
| P-b | **Yes** | **50 s** | "Fairly straightforward, though finding the currency dropdown took a quick glance." |
| P-c | Not yet reported | Not yet reported | Not yet reported |
| P-d | Not yet reported | Not yet reported | Not yet reported |
| P-e | Not yet reported | Not yet reported | Not yet reported |
| P-f | Not yet reported | Not yet reported | Not yet reported |

**Window:** two weeks from 2026-10-16 (until **2026-10-30**), or until 6 responses, whichever comes first (VALIDATION_PLAN.md).

**Notes on P-a (2026-10-16):** creation took 48 s. That is under both targets (60 s for a repeat client, 2 min for a new one), but whether P-a used an existing or a new client wasn't reported. Their comment that **typing items takes focus** supports the preset suggestions planned in UX_SPEC 3 (F-18), which the prototype doesn't have yet.

**Notes on P-b (2026-10-16):** creation took 50 s, under both targets (existing or new client not reported). Their comment that **finding the currency dropdown took a glance** fits the editor design in UX_SPEC 3: the real editor prefills the client's usual currency and always shows it in words in the summary line, so it is visible without searching. The prototype has neither.

## T3 sending re-check (UX_SPEC 4)

**Status (2026-10-16): 2 participants reported (P-a, P-b). The 2–3 participant minimum is reached; both passed.** The prototype has had the new sending design since 2026-10-16 (version 2, same link).

| Measure | Result | Pass mark |
|---|---|---|
| Participants tested | 2 so far (P-a, P-b) | 2–3 |
| Completed T3 unaided in under 30 s | P-a: **yes, 22 s** · P-b: **yes, 25 s**; both without help | All of them |
| Gave the new client an email in T1 | P-a, P-b: not reported | — |
| Where they expected the send button | P-a, P-b: not reported | — |
| **Verdict** | **Passed on the minimum sample (2 of 2 under 30 s, no help).** A third participant, ideally one who struggled with T3 originally, would make it firmer. Whether P-a or P-b struggled before isn't recorded. | — |
