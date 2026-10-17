# PocketBill — Launch and Growth Plan (Phase 9)

_Draft 2026-10-17 by Claude as product owner. Starts after production access is granted (RELEASE_CHECKLIST section 4)._

## Who we launch to
The primary persona (PERSONAS.md): independent remote freelancers who invoice foreign clients in English, often in several currencies, and want something simpler and more private than Zoho. Our one-line promise (DIFFERENTIATION.md): *"Professional invoices in two minutes, free and offline, with no account, no ads, and data that stays on your phone."*

## P9-T01 Channels (no budget)

| Channel | Why | First action |
|---|---|---|
| The 8 validation participants and their circles | Already know the problem; became the closed testers | Personal message with the Play link; ask for an honest review after their first real invoice |
| Freelancer communities (Reddit r/freelance, r/Upwork, r/fiverr; Facebook and WhatsApp freelancer groups in the founder's region) | Where the persona asks "what do you use for invoices?" | Answer existing questions helpfully; mention PocketBill only where it fits and rules allow |
| Play Store search | Free, compounding | Listing keywords: invoice, invoice maker, offline, freelancer, PDF |
| Short demo video (30–60 s, screen recording) | Shows "two minutes" instead of claiming it | Record T1–T3 with demo data; post to YouTube Shorts / LinkedIn |

## P9-T02 Launch content
1. 45-second screen recording: new client → invoice → Email to client (demo data).
2. A post: "I built a free, offline invoice app for freelancers. No account, no ads, your data stays on your phone. Feedback welcome." with the video and link.
3. A one-page FAQ (could live in the repository's docs): offline? backups? currencies? cost?

## P9-T03 Feedback process
- Every review answered within 3 days (founder).
- Feedback emails and reviews go into one list (a GitHub issue per request, labelled bug / idea); Claude groups them weekly.
- Bugs that lose data or block sending are fixed first, then crashes from Android vitals, then the most-requested idea.

## P9-T04 30-day experiment plan

| Week | Experiment | Measure (Play Console) | Success |
|---|---|---|---|
| 1 | Launch to testers' circles + 2 communities | Installs, crashes | ≥ 50 installs, 0 data-loss reports |
| 2 | Listing A/B test (Play store listing experiments): short description "offline, no account" vs "invoices in two minutes" | Listing conversion | Pick the winner (≥ 90% confidence) |
| 3 | Demo video posted; ask satisfied users for a review | Ratings count and average | ≥ 10 ratings, ≥ 4.3★ |
| 4 | Five 15-minute user calls | What they tried, what stopped them | Top 3 problems → next roadmap |

**Decision after 30 days:** if 30-day retention ≥ 40% and rating ≥ 4.3★, keep growing and pick the first Pro candidate (MONETIZATION.md). If not, stop promotion and fix the top problems first.
