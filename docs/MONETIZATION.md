# PocketBill — Monetization (Phase 6)

_Decided 2026-10-17 by Claude as product owner (D-020). Revisit with real usage numbers, not before 60 days after launch._

## The constraint
Our position (DIFFERENTIATION.md) is **free, no ads, no cap, offline, no account**. Zoho Invoice is free and ad-free, so anything that charges for the core workflow, or adds ads, removes the reason to choose PocketBill. Google Play Billing also needs the INTERNET permission, which we promised users we don't have (Settings privacy statement, D-019).

## Decisions

| Task | Decision | Why |
|---|---|---|
| P6-T01 Free tier | **Everything in v1.0 is free, with no limits**: unlimited invoices and clients, PDFs, sending, backup, logo, saved items. | The free core *is* the product position. A cap (like Saldo's 3 documents) was a top complaint in research. |
| P6-T02 Pro features | **None in v1.0.** Candidates for a later "Pro" (only things the free app never had): recurring invoices, several business profiles, custom email/PDF templates, payment links, a laptop companion with sync (D-010 keeps the data sync-ready). | Charge for new value, never take away what was free. |
| P6-T03 Ads | **No ads, ever, in the editor, PDFs or emails; no ads in v1.0 at all.** | Ads were a top complaint about Wow Invoice; they'd also need INTERNET and an ads SDK (privacy). |
| P6-T04 Billing | **Not built in v1.0.** | No paid features yet; it would add a network permission and complexity. |
| P6-T05 Revenue model | **v1.0 earns nothing, on purpose.** First goal is retained users. When ≥ 1,000 monthly users keep invoicing for 2+ months, test a **one-time "Pro" unlock** (no subscription) for the first Pro feature, priced with a small survey. | Freelancers dislike subscriptions for small tools (C7). One-time pricing fits the offline promise. |

## What would change this
- Retention after 30 days below 20% → fix the product before thinking about money.
- Many users asking for a feature only some need (e.g. recurring invoices) → first Pro candidate.
- Costs appear (none today: no servers) → reconsider sooner.
