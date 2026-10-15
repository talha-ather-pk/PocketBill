# Market Research

**Status: P1-T01 complete (2026-10-15)**, with known gaps listed under "Known gaps". Sources: a first pass by Claude Code (web search and page fetches) plus three rounds of the founder's Play Store checks (Saldo, JobFLEX, Wow Invoice, Zoho Invoice). No apps were installed or tested.

## Research limitations (read first)

- **Google Play pages could not be read directly.** Fetching `play.google.com/store/apps/details?...` returned no usable content, so ratings and install counts below come from **third-party trackers** (AppPricingLab). These may be outdated.
- Several official pricing pages blocked automated access (Invoice Simple returned 403; one Zoho pricing URL returned 404). A review-analysis report (Kimola) returned 410 (removed).
- Review quotes come from **JustUseApp**, which mirrors **Apple App Store** reviews. They show iOS users' experiences with an app that may also exist on Android; they are **not Google Play reviews**.
- **Next step to close the gaps:** the founder opens Google Play on their phone, looks at the apps listed below, and records rating, installs, price, "Contains ads", and the 1–2★ reviews sorted by "Most recent" in the spreadsheet template at the end of this file. This is a 30–45 minute manual task.

Confidence levels: **High** = official source or several agreeing sources · **Medium** = one reputable third-party source · **Low** = single unverified source, or conflicting sources.

## Competitors

### 1. Zoho Invoice (Zoho Corporation)
- **Price:** Free. Zoho's own pages say it is free ("forever free") with no ads. **High:** [zoho.com/us/invoice/pricing](https://www.zoho.com/us/invoice/pricing/) (from search snippet), [free-version PDF](https://www.zoho.com/invoice/migration/zoho-invoice-free-version-details.pdf).
- **Android app:** Yes, listed on Zoho's mobile apps page. **High:** [zoho.com/us/invoice/mobile-apps](https://www.zoho.com/us/invoice/mobile-apps/).
- **Offline:** A Zoho help article says Zoho Invoice cannot be used offline and needs an internet connection. The article is undated, so this may have changed. **Medium:** [zoho.com/us/invoice/kb/general/zoho-invoice-offline.html](https://www.zoho.com/us/invoice/kb/general/zoho-invoice-offline.html).
- **Requires account:** Yes (cloud product). *Inferred from the product design, not separately verified.* **Medium.**
- **Free-plan limits:** Third-party sites disagree; one claims a 5-client cap. **Low:** [temperstack.com](https://www.temperstack.com/plans/zoho-invoice/), [costbench.com](https://costbench.com/software/invoicing/zoho-invoice/free-plan).
- **Implication:** PocketBill cannot compete with Zoho on price. If there is room to compete, it is offline use, no account, and simplicity. That is a **hypothesis**.

### 2. Invoice Maker & Estimate Maker (Wow Invoice LLC), package `com.wow.invoice`
- 4.5★, 2.5K reviews, 100K installs; free with in-app purchases ("Pro plans", prices not shown); last updated 2026-02-28. **Medium:** [AppPricingLab](https://www.apppricinglab.com/app/google_play/com.wow.invoice).
- Features listed: PDF export, customer management, logo, e-signature, tax and discount, multiple currencies and date formats, recovery of deleted invoices. Offline use is not mentioned.

### 3. Invoice Maker & Estimating App (JobFLEX), package `com.jobflex.android`
- 3.4★, 1.0K reviews, 100K installs; free with in-app purchases; **last updated 2020-06-03** (appears unmaintained). **Medium:** [AppPricingLab](https://www.apppricinglab.com/app/google_play/com.jobflex.android).
- Says "No Internet Required". Includes estimates, invoices, clients, e-signature, and PDF. Aimed at contractors (field work) rather than freelancers.

### 4. Invoice Maker・Billing App (Saldo Apps Inc.), iOS review evidence
- Overall 4.8/5 on the App Store per JustUseApp. **Medium:** [JustUseApp reviews](https://justuseapp.com/en/app/1447239633/invoice-maker-billing-app/reviews).
- Negative themes in reviews (verbatim quotes, dates not shown on the source page):
  - Data loss: *"I have lost my invoices now 2 times due to InvoiceMakerBillingApp not syncing and saving my data."*
  - Lost financial records: *"I lost my data & money even I don't remember how much money my Customers owed me"*
  - Billing errors: *"We were charged for the yearly AND the lifetime."*
  - Support: *"I have tirelessly emailed their support ( the only way to contact the by the way)"*
  - Errors: *"constantly telling me "Something went wrong""*

### 5. Other apps seen, not yet researched
- Apple App Store listings that market **offline use, no login, or a one-time purchase**: "Offline Invoice: Paper Invoice" (one-time purchase, no subscription, no login) and "Invoices, Quotes & Estimates" (offline, no account) ([link](https://apps.apple.com/us/app/id6756224960), [link](https://apps.apple.com/us/app/-/id6756826568)). **Android availability not verified.**
- Not yet researched: Invoice Simple, Invoice2go, InvoiceFlow, Pocket Invoice, QuickQuote (from the master prompt list). Each needs its exact developer and Play listing confirmed first.

## Early observations

| # | Observation | Type | Confidence |
|---|---|---|---|
| O1 | "Offline", "no account", and "one-time purchase" are **already claimed by several existing apps**. PocketBill cannot claim them as unique. | Evidence | Medium |
| O2 | A strong free option (Zoho Invoice) exists, but it is cloud-based and needs an account. | Evidence | High (free) / Medium (online-only) |
| O3 | Reviews repeatedly mention **lost invoices** after logout, reinstall, sync failure, or a lapsed subscription. Reliable local storage plus user-controlled backup may matter to users. | Evidence (iOS reviews, one app) + hypothesis that it matters to PocketBill's users | Medium |
| O4 | **Billing confusion** (double charges, plans changing from one-time to subscription) damages trust. Transparent, stable pricing may be a differentiator. | Evidence (one app) + hypothesis | Low–Medium |
| O5 | Invoice-maker apps in the top-grossing Business charts are mostly free downloads with in-app purchases. | Evidence (tracker ranks) | Medium |

None of these observations proves demand for PocketBill. They feed P1-T02 (problems) and P1-T04 (differentiation).

## Founder Play Store pass, round 1 (supplied 2026-10-15)

The founder reported these as verified on Google Play. **They replace the third-party tracker figures above where they differ**, because a live listing is a better source than a tracker. Limitations:
- No Play links or exact check dates were given. The date below is the day the data was supplied.
- The complaint columns are the **founder's summaries of reviews**, not verbatim quotes with dates. They show recurring themes but cannot be quoted.
- **JobFLEX:** a rating *range* and two developer names cannot both come from one listing, so this row is **Low** confidence until one exact listing is recorded.

| App (as listed) | Developer | Rating | Installs | Monetization | Reported complaints (founder summary) | Confidence |
|---|---|---|---|---|---|---|
| Saldo: Bill & Invoice Maker (`com.simple.invoice.maker`) | "S/A Saldo Apps" (round 2; round 1 said "Saldo Apps Inc.") | 4.7★ | 1,000,000+ | Free tier limited to **3 documents**; subscription ~**$14.99/month** | Occasional sync glitches; **data loss when restoring a backup**. Verbatim reviews below. | Medium |
| Invoice Maker & Estimating App (`com.jobflex.android`) | JobFLEX (round 2; round 1 gave two other names) | ~3.4★ | 100,000+ | Subscription tiers up to ~$15/month; works offline; aimed at contractors | **Support unreachable / app appears abandoned**. Verbatim reviews below. | Medium (round 2 replaces the round-1 range and developer names) |
| Invoice Maker & Estimate Maker | Wow Invoice LLC | 4.7★ | 100,000+ | Free with in-app purchases | **Intrusive full-screen ads**; export limits on free tier | Medium |

### Verbatim Play Store reviews (founder pass, round 2, supplied 2026-10-15)

Copied by the founder from the Play listings. Quotes are as supplied and end where the founder's excerpt ended ("...").

**Caveat: every review here is from 2020–2022, so 3.5–6 years old.** They show what went wrong *then*. They are not "most recent" reviews and don't prove these problems still exist today.

**Saldo (`com.simple.invoice.maker`)**
- 1★, 2022-03-15: *"Horrible. It is a great app when it works. Quite often it just deletes information. Contact them and they ask if you backed up. I did but some glitch deletes it. There only response is sorry but try this other app we are working on..."*
- 1★, 2021-04-07: *"UPDATE! 4/2/21-ALL OF MY INFO HAS DISAPPEARED. 1/16/21-I've been using this app for a while now and it worked fine until yesterday. I'm able to create the invoice but when I go to send it just spins. I uninstalled and reinstalled hoping that would work, it didnt and it erased all of my invoices..."*

**JobFLEX (`com.jobflex.android`)**
- 1★, 2021-01-30: *"Is anyone else having issues with Job flex software? I can't access the app, I can't access their website, I've left voice-mails, I've sent emails, all to no response or luck. I've never had any issues before. It's like they just disappeared."*
- 1★, 2020-07-14: *"No support for over a year. Did not respond to multiple emails, or phone calls (get the answering service every time). Was a good app to the actual customer support for the first six or seven months and then nothing..."*

**What these show**
- **Data loss is now evidence on Android, not just iOS.** Both Saldo reviews describe invoices disappearing, even with a backup made, and after a reinstall. This is the strongest pattern so far (O3 → **Medium-High**). For PocketBill it argues for: data that survives app updates, a restore that is tested, and never deleting data during reinstall or sign-out flows.
- **Abandoned apps strand users.** The JobFLEX reviews ("It's like they just disappeared") match the tracker's last-update date of 2020-06-03. Users of an app that depends on its developer's servers lose access when the developer stops running them. This supports offline-first with **user-owned export/backup files**, so users keep their data even if PocketBill stops. That is a hypothesis about what users value, not yet evidence.
- **Support responsiveness** appears in both apps' complaints. That is relevant later (P9-T03, feedback and support process).

### Founder pass, round 3 (supplied 2026-10-15): Wow Invoice and Zoho Invoice

| App | Play link | Developer | Rating | Installs | Confidence |
|---|---|---|---|---|---|
| Invoice Maker & Estimate Maker | [com.wow.invoice](https://play.google.com/store/apps/details?id=com.wow.invoice) | Wow Invoice LLC | 4.7★ | 100,000+ | Medium |
| Zoho Invoice | [com.zoho.invoice](https://play.google.com/store/apps/details?id=com.zoho.invoice) | Zoho Corporation | 4.7★ | 1,000,000+ | Medium |

Recent reviews (star rating not stated; supplied as negative reviews):
- **Wow Invoice, 2026-07-20:** *"When I create an invoice in Bengali, the PDF shows square boxes (□) instead of Bengali text. Please add proper Unicode font support in PDF generation."*
- **Zoho Invoice, 2026 (exact date not given):** *"Sometimes after updating it will take some time to reflect. Disappointed with tax record updates taking too long and sync delays."*

**What these show**
- **Non-Latin text breaking in PDFs is a current problem (2026).** A user writing in Bengali got empty boxes instead of letters. For PocketBill's likely markets (for example Urdu, Hindi, Bengali and Arabic speakers), PDFs must render **any script the user can type**, including right-to-left text. This is a concrete, testable requirement for P4-T08 (PDF generation): add PDF test cases in Urdu (right-to-left), Bengali, Hindi and Arabic, and include a missing-font fallback check.
- **Cloud sync delay is a current complaint about the strongest free competitor (Zoho, 2026).** A local-first app has no sync step for the user's own data. Whether users would *switch* for that is still a hypothesis.
- Zoho's offline behaviour on Android is still only sourced from an undated help article (section 1).

### What this resolves
| Earlier open question | Resolution |
|---|---|
| Saldo developer: "CA Publishing / BillGo" vs Saldo Apps Inc. | Play listing developer is **Saldo Apps Inc.**, matching the iOS evidence (section 4). The "CA Publishing / BillGo" name in the older table is probably a different app. |
| Wow Invoice rating: 4.5★ (tracker) vs ~4.8–4.9★ (older table) | **4.7★** on Play per this pass. Ratings drift over time; use this figure. |
| JobFLEX installs: 50,000+ vs 100K | **100,000+** per this pass, matching the tracker. |
| Is JobFLEX still maintained? | **Probably not.** Reviews from 2020–2021 report no support and an unreachable website, consistent with the tracker's last update of 2020-06-03. The listing's current "Updated on" date was not recorded. |
| JobFLEX rating range and developer names | Round 2: developer **JobFLEX**, ~3.4★. |

### What it adds to the evidence
- **Data loss on backup/restore** now appears for Saldo on **both iOS (verbatim reviews, section 4) and Android (founder summary)**. That strengthens O3: reliable local storage and a trustworthy backup/restore are worth treating as core.
- **Hard free-tier caps** (Saldo: 3 documents) and **paywalled exports** (Wow) support H1, now Medium rather than Low.
- **Intrusive ads** (Wow) support H2, now Medium.
- Pricing anchor: the subscription apps here sit around **$15/month**. That is a reference point for Phase 6 (monetization), not a recommendation.

### Known gaps (accepted when P1-T01 closed, 2026-10-15)
- Only **one or two reviews per app**. That is enough to show themes exist, not how common they are.
- The data-loss reviews (Saldo) are from 2021–2022. No 2025–2026 review yet confirms data loss is still happening.
- Zoho's offline behaviour on Android has not been tested.
- Not researched: Invoice Simple, Invoice2go, and the others in section 5. Revisit in P1-T04 if differentiation depends on them.

## Founder-supplied competitor table (2026-10-15), mixed sources, unverified

*Partly superseded by "Founder Play Store pass, round 1" above.*

The founder supplied this table and said its sources are **mixed**: some rows were checked on Google Play and some came from elsewhere. It is not yet known which rows are which. **Treat every cell as Low confidence** until it is re-checked against a live Play Store listing. Ranges such as "~3.3★ – 4.7★" and rows combining two apps cannot come from a single listing.

| App | Rating | Installs | Monetization | Ads? | Reported 1–2★ complaints |
|---|---|---|---|---|---|
| Zoho Invoice | ~4.7–4.8★ | 1,000,000+ | Free | No | Web dashboard sync delays; multi-user/access caps on free plan |
| Invoice Maker・Billing App ("CA Publishing / BillGo") | ~3.3–4.7★ | 10K+ to 5M+ | Free + IAP $4.99–29.99 | Yes | Hard paywall after a few free invoices; PDF logo permission and layout export bugs |
| JobFLEX | ~3.4–4.2★ | 50,000+ | Trial, $8–15/mo subscription | No | Photo attachments/line items paywalled; tax calculation inconsistencies in exports |
| Wow Invoice | ~4.8–4.9★ | 100,000+ | Free + IAP $3.49–19.99 | Yes | Intrusive pop-up ads during quote creation; CSV/data export limited without premium |
| Offline Invoice: Paper Invoice / Invoice+ | ~4.5–5.0★ | 10,000+ | Free / one-time IAP | No | Limited cloud backup; no payment-gateway integration |
| Invoices, Quotes & Estimates ("FlowBill / CodeEra") | ~4.2–4.6★ | 10K+ to 100K+ | Free + IAP / subscription | Yes | Inaccurate AI parsing for custom tax rules; mandatory login/cloud account |

### Conflicts with earlier sources (must resolve before relying on either)

| App | This table says | Earlier source says |
|---|---|---|
| JobFLEX | 50,000+ installs; $8–15/mo | 100K installs, last updated **2020-06-03** (AppPricingLab). If that update date is right, the app may be unmaintained. |
| Wow Invoice | ~4.8–4.9★ | 4.5★ from 2.5K reviews (AppPricingLab, last updated 2026-02-28) |
| Invoice Maker・Billing App | Developer "CA Publishing / BillGo" | Saldo Apps Inc. (JustUseApp, iOS). **These may be different apps with similar names.** |
| Invoices, Quotes & Estimates | Mandatory login/cloud account | Its App Store listing says it works offline with no account required. **These may be different apps or platforms.** |

### What the table adds, if confirmed (hypotheses for P1-T02)
- H1: **Paywalls after a few free invoices** and **paywalled exports** frustrate users. This supports PocketBill's rule of never paywalling basic invoice export.
- H2: **Intrusive ads during document creation** are disliked. This supports keeping ads out of the editor and out of PDFs (master prompt P6-T03).
- H3: **Tax calculation inconsistencies** hurt trust. This supports tested, explicit money and tax rules (P3-T03).
- H4: Offline one-time-purchase apps are criticized for **limited backup**. This supports making backup and restore a core MVP feature (P4-T12).

### To verify (founder, on phone)
For each row: open the exact Play listing and record the developer name exactly as shown, the single rating, the install band, the "Contains ads" label, and 3 recent 1–2★ reviews. Note which rows you already checked directly so they can be marked Medium/High.

## Founder spreadsheet template (for the manual Play Store pass)

| App | Developer | Play link | Date checked | Rating | Installs | Contains ads? | Price / IAP | Offline? | Account required? | Quotes/estimates? | Backup? | Top 3 complaints (1–2★, most recent) | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
