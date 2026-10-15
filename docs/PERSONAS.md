# Personas (P1-T03)

_Created 2026-10-15. Built from [USER_PROBLEMS.md](USER_PROBLEMS.md) and decision D-007 (global freelancers, English-only invoices)._

## Read this first

These personas are **working hypotheses, not research findings.** No freelancer has been interviewed yet. Each line is labelled:
- **[E]** comes from evidence (a problem ID in USER_PROBLEMS.md).
- **[D]** comes from a founder decision (DECISIONS.md).
- **[A]** is an assumption to test in P1-T05.

The personas have no names, ages or genders on purpose. Those details would be invented, and they don't change any product decision. If interviews show a pattern, add it then.

---

## Primary persona: the independent remote freelancer

**Who:** A solo professional (for example a designer, developer, writer, translator or virtual assistant) who works **for clients in other countries** and invoices **in English** [D]. Invoicing is a chore that sits between them and getting paid.

**Their situation**
- Sends a handful to a few dozen invoices a month, usually to repeat clients [A].
- Bills in the **client's currency**, sometimes several currencies (USD, EUR, GBP…) [A, A-07].
- Clients' names and addresses often contain **accented characters**, and invoices carry **currency symbols** [D].
- Today likely uses a Word/Excel template, a generic invoice app, or a free cloud tool such as Zoho [A, A-01].
- Works from a laptop *and* a phone, and needs to send an invoice the moment a job is approved, even away from the desk [A, A-02].

**Goals**
1. Send a correct, professional-looking PDF invoice in a couple of minutes.
2. Know at a glance **who still owes money** and how much [A, A-03].
3. Never lose their invoice history, which is their record of income [E, UP-01].

**Frustrations with current tools**
- Invoices vanishing after a glitch, reinstall or failed restore [E, UP-01].
- Waiting for cloud sync, or changes not showing up [E, UP-02].
- Being stopped by a free-tier cap or a paid-only export just when an invoice is due [E-weak, UP-05].
- Paying around $15/month for something they use a few times a month [A, A-05; price is a fact, frustration is assumed].
- Wrong totals or tax, which look unprofessional in front of a client [E-weak, UP-08].

**What PocketBill must do for them (MVP)**
- Create, save, export and share an invoice PDF with no account and no internet.
- Totals and tax that are correct to the cent, in the invoice's currency.
- Mark invoices paid or unpaid, with an overview of what's outstanding.
- Backup and restore that **provably works**, saved to a file the user keeps.
- Render accented names and currency symbols correctly in the PDF [D].

**What would make them leave**
- Losing a single invoice.
- A paywall in front of basic export.
- A PDF a client would see as amateurish.

---

## Secondary persona: the occasional side-gig freelancer

**Who:** Someone with a main job, or just starting out, who invoices **a few clients a month or less**, in English [D]. Their clients may be local or remote.

**Their situation**
- Low invoice volume, so **a subscription is hard to justify** [A, A-05].
- May have tried an invoice app and hit its free limit after only a few documents [E, UP-05: Saldo's 3-document cap].
- Less familiar with invoicing conventions (numbering, due dates, tax lines) [A].

**Goals**
1. Look professional to a client without paying monthly for a tool.
2. Not have to learn an accounting product.

**Frustrations**
- Free tiers that run out almost immediately [E-weak, UP-05].
- Ads interrupting the editor [E-weak, UP-06].
- Not knowing what an invoice should contain [A].

**What PocketBill must do for them**
- A free tier that covers **real use at low volume**, with no ads in the editor or PDF (details in Phase 6).
- Sensible defaults: automatic numbering, due date and a clean template, so they don't have to know the rules.

**Why secondary:** they pay less and less often, but they are the people most likely to try a free app and recommend it. If they can't be served free, the primary persona probably won't try PocketBill either.

---

## Not targeted in the MVP (anti-personas)

| Who | Why not now |
|---|---|
| Agencies and teams needing **multiple users** or shared data | Conflicts with the no-account, local-first design. |
| Businesses needing **accounting integration** (QuickBooks, Xero) or automatic tax filing | Large scope, and Zoho and others already serve them well. |
| Users needing **online card payments** inside the invoice | Needs a server and payment provider. Revisit after the MVP. |
| Field contractors needing photos, estimates on site and job scheduling | JobFLEX's niche. Estimates or quotes may come later (the app name says "Invoice & Quote"), but contractor workflows are out of scope. |
| Freelancers invoicing in non-Latin scripts | D-007. Nice-to-have later. |

---

## Assumptions these personas depend on (test first in P1-T05)

| ID | Assumption | Persona |
|---|---|---|
| A-01 | Their current invoicing method hurts enough to switch | Both |
| A-02 | They want to invoice from their **phone** | Both (critical: PocketBill is Android-only) |
| A-03 | Tracking who has paid is a real pain | Primary |
| A-05 | A ~$15/month subscription feels too expensive for their volume | Both, especially secondary |
| A-07 | They bill in more than one currency | Primary |

**Riskiest:** A-02. A remote freelancer may well invoice from a laptop. If interviews show that, PocketBill's Android-only, offline-first position needs rethinking **before** Phase 4.
