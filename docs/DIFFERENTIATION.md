# Differentiation Check (P1-T04)

_Created 2026-10-15. Tests PocketBill's candidate claims against the competitors in [MARKET_RESEARCH.md](MARKET_RESEARCH.md), for the personas in [PERSONAS.md](PERSONAS.md)._

## Method

For each claim PocketBill might make, the question is: **does any researched competitor already offer this?** "Unknown" means our research did not check it. It does not mean the competitor lacks it.

Competitors checked: **Zoho Invoice**, **Saldo**, **Wow Invoice**, **JobFLEX**, and two iOS-only offline apps (Android availability unverified).

## Claim-by-claim results

| # | Candidate claim | Competitors that already offer it | Unique? | Verdict |
|---|---|---|---|---|
| C1 | **Works fully offline** | JobFLEX ("No Internet Required"); iOS offline apps. Not Zoho (needs internet; Medium confidence). | ❌ No | Real advantage **over Zoho only**. Not a headline claim. |
| C2 | **No account or sign-up** | iOS offline apps ("no login"). Zoho requires an account. Saldo/Wow unknown. | ❌ Probably not | Advantage over Zoho. Check Saldo/Wow before claiming. |
| C3 | **Free, with no ads** | **Zoho Invoice: free, no ads** (High confidence, official pages). | ❌ No | **Zoho beats or matches us here.** Can't be a differentiator. |
| C4 | **No cap on invoices in the free tier** | Zoho is free (whether it has caps is disputed, Low confidence). Saldo caps at 3 documents; Wow limits exports. | ⚠️ Partly | Beats Saldo/Wow; vs Zoho, unknown. |
| C5 | **Your invoices are never lost** | No competitor can be shown to *fail* to offer this, but Saldo has repeated data-loss reviews (UP-01). | ⚠️ Can't claim yet | The strongest *need*, but a promise has to be **earned through testing** (P4-T12, P5). Making it before then would be dishonest. |
| C6 | **Your data stays in files you own** (export and backup files outside the app) | Unknown for all competitors. Wow lists "recovery of deleted invoices" (inside the app). | ❓ Unknown | Possibly distinctive. Verify Zoho/Saldo/Wow export options first. |
| C7 | **One-time purchase, no subscription** | iOS offline app ("one-time purchase, no subscription"). | ❌ No | Pricing decision for Phase 6, not a differentiator. |
| C8 | **Correct totals and tax, tested** | Not verifiable from outside for any app. | ❓ Untestable | Necessary for trust; not a marketing claim. |

## Conclusion

**No single claim is unique.** Each strength PocketBill could offer is already offered by at least one competitor.

What *no single researched competitor* offers is the **combination** for the target persona:

| | Free core, no ads | No document cap | Offline | No account | Maintained |
|---|---|---|---|---|---|
| Zoho Invoice | ✅ | ❓ | ❌ | ❌ | ✅ |
| Saldo | ❌ (3 docs, then ~$15/mo) | ❌ | ❓ | ❓ | ✅ (4.7★, 1M+) |
| Wow Invoice | ❌ (ads reported) | ❌ (export limits) | ❓ | ❓ | ✅ |
| JobFLEX | ❌ (paid tiers) | ❌ | ✅ | ❓ | ❌ (likely abandoned) |
| **PocketBill (planned)** | ✅ | ✅ | ✅ | ✅ | — |

**Candidate positioning (a hypothesis):**
> *"Professional invoices in two minutes, free and offline, with no account, no ads, and data that stays on your phone and in your backups."*

In plain terms: **Zoho's price without Zoho's account and internet, and Saldo's simplicity without Saldo's paywall.**

## Risks this exposes

1. **Zoho is the real competitor, and it's strong.** It's free, ad-free, well-rated (4.7★, 1M+) and maintained. PocketBill only wins if the target freelancers **actually care** about offline and no-account use (assumption A-04). If they don't, there's little reason to switch.
2. **Combinations are hard to market.** A list of five features is weaker than one sharp promise. Interviews should find which single benefit users react to most.
3. **"Never lose data" is the strongest promise and the most dangerous one.** It must be backed by tested restore before it's said publicly.
4. **The research has gaps.** Saldo/Wow offline and account behaviour (C1, C2) and every competitor's export options (C6) are unknown. A 15-minute check of each app's listing or a quick install would close them.

## Feeds into
- **P1-T05 (validation plan):** test A-04 (do users value offline and no account?) and which of C4/C5/C6 users react to most strongly.
- **Phase 6:** C3/C4/C7 pricing choices.
