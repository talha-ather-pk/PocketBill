# User Problems (P1-T02)

_Created 2026-10-15. Sources: [MARKET_RESEARCH.md](MARKET_RESEARCH.md). Feeds P1-T03 (personas), P1-T04 (differentiation) and P1-T05 (validation plan)._

## How to read this

Each problem gets two separate ratings, because "this happens" and "this matters to PocketBill's users" are different claims:

- **Evidence that it happens:** how sure we are that real users of existing apps hit this problem.
  - **Strong:** several verbatim, dated reviews, ideally recent and on more than one app or platform.
  - **Moderate:** at least one verbatim dated review, or several consistent summaries.
  - **Weak:** summaries only, a single undated source, or conflicting sources.
  - **None:** an assumption; nobody has been heard saying it.
- **Relevance to PocketBill:** always a **hypothesis** until P1-T05 interviews test it.

**The biggest caveat:** every piece of evidence so far comes from **complaints about competitor apps**. None of it shows that freelancers *want a new invoice app*, how they invoice today, or what they would pay. Those questions are the job of P1-T05.

## Problems backed by evidence

| ID | Problem | Evidence that it happens | Sources | PocketBill response (hypothesis) |
|---|---|---|---|---|
| UP-01 | **Invoices disappear**: data lost after a glitch, a reinstall, a failed sync, or even a restore from backup | **Moderate–Strong.** 2 verbatim dated Android reviews (Saldo, 2021-04-07 and 2022-03-15), several verbatim iOS reviews (Saldo, undated), and a founder summary of restore failures. The same pattern appears on two platforms. *But nothing newer than 2022.* | Saldo Play and iOS reviews | Local-first storage, data that survives app updates, backup/restore with a **tested** restore, and a check that nothing is ever deleted on reinstall. Make this a core MVP feature (P4-T12) and test it (P5). |
| UP-02 | **Cloud sync delays**: changes take time to appear, especially tax records | **Moderate.** 1 verbatim 2026 review (Zoho), plus founder summaries for Saldo ("sync glitches") and JobFLEX ("sync delays"). | Zoho, Saldo, JobFLEX | Local-first means the user's own data needs no sync. Whether users would **switch** apps for that is unknown. |
| UP-03 | **PDFs break for non-Latin scripts**: Bengali text shows as empty boxes (□) | **Moderate.** 1 verbatim, recent review (Wow Invoice, 2026-07-20). It is very specific and technically plausible. | Wow Invoice | **Deprioritized by D-007** (English-only invoices). MVP: render accented Latin names and currency symbols (€ £ ₹ ₦…) correctly. Full non-Latin and right-to-left scripts are a nice-to-have. |
| UP-04 | **The app or its company disappears**: support unreachable, website down, app no longer maintained | **Moderate, but old.** 2 verbatim reviews (JobFLEX, 2020-07-14 and 2021-01-30), consistent with a tracker showing no update since 2020. Saldo's support reportedly redirected a user to "this other app". | JobFLEX, Saldo | Users' data must not depend on PocketBill's servers: plain export and backup files the user keeps. A real support channel is a later task (P9-T03). |
| UP-05 | **Free tier is too limited**: a hard cap (Saldo: 3 documents) or exports locked behind payment | **Weak–Moderate.** The **limits are facts** from listings (Saldo 3 documents; Wow export limits). **User frustration is reported only in founder summaries**, with no verbatim review yet. | Saldo, Wow Invoice, older founder table | Never paywall creating or exporting a basic invoice (already a principle in H1). The free-tier design is decided in P6-T01. |
| UP-06 | **Intrusive ads** while creating documents | **Weak.** Founder summary for Wow Invoice only; no verbatim review. | Wow Invoice | No ads in the editor or in PDFs (P6-T03). |
| UP-07 | **Billing confusion**: charged twice, plan changed from one-time purchase to subscription | **Weak.** 1 verbatim iOS review (Saldo, undated). | Saldo iOS | Simple, stable pricing (Phase 6). |
| UP-08 | **Wrong tax calculations** in exports | **Weak.** Appears only in the older mixed-source table, unverified. | Founder table, round 0 | Tested, explicit money and tax rules (P3-T03, P4-T02). Worth doing regardless, because a wrong total destroys trust. |

## Assumptions with no evidence yet

These are central to PocketBill's idea, but **no user has been heard saying them**. P1-T05 must test them first.

| ID | Assumption | Why it matters | How to test (P1-T05) |
|---|---|---|---|
| A-01 | Freelancers find their **current way of invoicing** (Word, Excel, WhatsApp messages, paper, an existing app) painful enough to change. | If it isn't, nobody switches, however good the app is. | Interviews: "Walk me through the last invoice you sent." Look for the workaround and how much it hurts. |
| A-02 | Freelancers want to **create invoices on their phone**, not on a laptop. | PocketBill is mobile-only. | Interviews: where and when did they make their last 3 invoices? |
| A-03 | **Tracking who has paid** is a real pain point. | It is one of PocketBill's 3 core promises. | Interviews: "How do you know who still owes you money right now?" |
| A-04 | **No account / works offline** is valued, not just tolerated. | It is a candidate differentiator, but several competitors already claim it (O1). | Interviews plus prototype: does anyone mention it unprompted? |
| A-05 | Users find subscriptions of about **$15/month** too expensive. | It shapes pricing. We only know competitors charge this; no complaint about the price was found. | Interviews: what do they pay for tools now; would they pay once vs monthly? |
| A-06 | ~~The target users write invoices in Urdu, Hindi, Bengali or Arabic, or mix scripts.~~ | **Resolved by founder decision D-007:** global freelancers, English-only invoices. | Not needed. Interviews should still note any client names or currencies that need special characters. |
| A-07 | Global freelancers bill clients in **more than one currency**. | It decides whether multi-currency belongs in the MVP. | Interviews: which currencies have they invoiced in during the last 6 months? |

## Ranking for the MVP (current view)

Based on evidence strength × fit with an offline-first app × cost to get right:

1. **UP-01, data never lost.** Strongest evidence, and it is the core promise of a local-first app. A failure here would be fatal to trust.
2. **UP-08 + UP-05, correct totals and a usable free tier.** Basic trust and fairness. The evidence is weak, but the cost of getting them wrong is high.
3. **UP-02 and UP-04, no sync and user-owned data.** These come almost free with the local-first design. Treat them as marketing claims only after P1-T04 checks competitors.
4. **UP-03, reduced to accented names and currency symbols** (D-007). Full non-Latin scripts are a nice-to-have.
5. **UP-06 and UP-07, ads and billing.** Decide in Phase 6.

**Key takeaway:** the evidence says competitors fail users in ways a careful local-first app could avoid. It does **not** yet say that freelancers will switch. A-01 to A-03 are the riskiest unknowns, and P1-T05 should target them first.

## Open evidence gaps
- No data-loss reviews newer than 2022 (UP-01). One or two 2025–2026 examples would raise confidence.
- Frustration with free-tier limits and ads (UP-05, UP-06) has no verbatim quotes.
- One or two reviews per app only, so this shows **which** problems exist, not **how common** they are.
