# PocketBill — Analytics and Feedback Plan (Phase 7)

_Decided 2026-10-17 by Claude as product owner (D-021)._

## Decision: no in-app analytics in v1.0
PocketBill has **no INTERNET permission**, so it cannot send analytics or crash reports, and adding them would break the promise in Settings ("data never leaves the phone unless you share or back it up"). We learn from sources that need nothing inside the app.

## What we measure, and from where

| Question | Source | Target (first 60 days) |
|---|---|---|
| Are people installing? | Play Console: store listing visitors → installs (conversion) | ≥ 25% listing conversion |
| Do they keep it? | Play Console: installed audience over time, uninstalls | ≥ 40% still installed after 30 days |
| Is it stable? | Play Console **Android vitals** (crashes and ANRs are reported by Android itself, not by our app) | Crash rate < 0.5% of users; ANR < 0.2% |
| Do they like it? | Ratings and reviews | ≥ 4.3★; every review answered within 3 days |
| What's missing or confusing? | Feedback email (below) and short user interviews (like P1-T05) | 5 interviews in month 1 |

## Feedback channel
- **Settings → Send feedback** (Phase 9 build): opens the user's email app to the support address with the app version and Android version prefilled, nothing else. No invoice data is ever attached automatically.
- Play Store reviews are answered by the founder.

## Privacy rules (apply to every future change)
1. Nothing about clients, invoices or amounts is ever collected.
2. Any future opt-in analytics must be off by default, explained in plain words, and listed in the Data Safety form before release.
3. Removing a promise from the privacy statement is a founder decision, recorded in DECISIONS.md.
