# 7-Day Validation Plan (P1-T05)

_Created 2026-10-15. Tests the riskiest assumptions in [USER_PROBLEMS.md](USER_PROBLEMS.md), [PERSONAS.md](PERSONAS.md) and [DIFFERENTIATION.md](DIFFERENTIATION.md) **before** Phase 4 engineering. The founder runs it._

## What we're trying to learn

| Priority | Assumption | The question in plain words |
|---|---|---|
| 1 | **A-02** | Do these freelancers actually create invoices **on their phone**? |
| 2 | **A-01** | Is their current way of invoicing painful enough to **switch**? |
| 3 | **A-04** | Do they care about **offline / no account / data on their device**, compared with Zoho-style cloud tools? |
| 4 | **A-03** | Is **tracking who has paid** a real problem? |
| 5 | A-05, A-07 | What do they pay for tools today? Do they bill in several currencies? |

## Decision rules (set *before* talking to anyone)

Write the rules down now, so the results can't be read to fit what we hoped. These thresholds are **proposals**: adjust them before Day 3 if you disagree, but not after.

Count behaviour ("I did"), not opinions ("I would"). Target: **8 interviews** (minimum 6).

| Assumption | ✅ Supported if… | ❌ Not supported if… | If not supported |
|---|---|---|---|
| A-02 mobile invoicing | **≥ 4 of 8** created or sent at least one of their last 3 invoices from a phone, **or** describe a specific recent moment they needed to and couldn't | ≤ 2 of 8 have ever invoiced from a phone and none describe that need | **Stop before Phase 4.** Re-examine the platform (web, desktop, or mobile as a companion) with the founder. |
| A-01 pain | **≥ 4 of 8** describe a concrete recent problem with invoicing (lost time, an error, a lost record, a paywall) **and** have tried another tool or workaround | Most say "it's fine" and can't recall a problem | Narrow the target or find a sharper pain before building. |
| A-04 offline/local | **≥ 3 of 8** raise data loss, privacy, connectivity or account fatigue **without being prompted**, or pick the local-first pitch first in the pitch ranking | Nobody raises it and the local-first pitch ranks last | Drop it as the headline. Lead with what did resonate (e.g. no paywall). |
| A-03 payment tracking | **≥ 4 of 8** can't say instantly who owes them, or describe chasing a late payment | Most track it easily already | Make payment tracking a smaller feature. |

**Proceed to MVP spec (P1-T06)** if A-02 **and** A-01 are supported. A-03/A-04 results shape the feature priorities and messaging.

## Who to interview

**Must have all of these (screener):**
1. Invoiced at least **one client in the last 3 months**, in English.
2. Works as a freelancer or self-employed (full-time or side gig).
3. Has at least one client in **another country**. Aim for at least 6 of 8 like this: the primary persona.
4. Uses an **Android** phone. Aim for at least 6 of 8. iPhone users are still useful for A-01, A-03 and A-04.

**Mix to aim for:** at least 2 side-gig freelancers (secondary persona), at least 2 who use Zoho or another invoicing app today, and a range of professions.

**Exclude:** friends who'll be polite, people who've never invoiced, agencies and teams (anti-persona).

**Where to find them** (ask, don't spam):
- Your own freelance clients and colleagues; ask each one for an introduction to someone else.
- Freelancer communities: r/freelance, r/freelanceWriters, r/web_design, Upwork/Fiverr community forums, freelancer Facebook and LinkedIn groups, Discord servers for designers and developers. Read each group's rules about research requests first.
- LinkedIn: people with "freelance" in their headline, via a direct message.

**Outreach message (template):**
> Hi [name], I'm building a small invoicing app for freelancers and I'm trying to learn how people handle invoicing today. **I'm not selling anything.** Would you have 25 minutes for a video call this week? I'll ask about the last few invoices you sent. Happy to share what I learn from everyone afterwards.

Expect roughly 1 reply for every 4–6 messages, so send about 40.

## The 7 days

| Day | Do | Output |
|---|---|---|
| **1** | Send about 40 outreach messages. Confirm the decision thresholds above. Prepare the note template. | Messages sent; thresholds fixed |
| **2** | Book interviews. Prepare the prototype (see "Prototype"). Do a practice interview with a friend to time the script. | 6–10 calls booked; prototype ready |
| **3** | Interviews 1–3. Fill in the note template **within an hour** of each call. | 3 sets of notes |
| **4** | Interviews 4–6. After 3 interviews, fix anything confusing in the prototype, but **don't change the questions**. | 6 sets of notes |
| **5** | Interviews 7–8, plus any reschedules. | 8 sets of notes |
| **6** | Fill in the results tally. Compare it against the decision rules. | Tally plus verdict per assumption |
| **7** | Write `VALIDATION_RESULTS.md` (anonymized). Decide: proceed to P1-T06, adjust, or stop. | Results doc plus decision in DECISIONS.md |

## Interview script (about 25 minutes)

**Rules for you, the interviewer:**
- **Ask about the past, not the future.** "When did you last…" beats "Would you…". People are bad at predicting what they'll do.
- **Don't pitch PocketBill until Part 4.** Mentioning it earlier makes people polite.
- **Ask "why?" and "tell me more"** at least twice per topic.
- **Let silences run.** People fill them with the honest bit.
- Get **permission before recording.** If they say no, take notes instead.

### Part 0: Opening (2 min)
> "Thanks for your time. I'm researching how freelancers handle invoicing. There are no right answers, and nothing you say will hurt my feelings: honest is most useful. May I take notes [and record, for my notes only]?"

### Part 1: Context (3 min)
1. What kind of freelance work do you do? Full-time or alongside something else?
2. Roughly how many invoices do you send in a typical month?
3. Where are your clients based? Which currencies do you bill in? → **A-07**

### Part 2: Their last invoice (8 min) → A-01, A-02
4. "Think about the **last invoice you sent**. Walk me through it from the moment you knew you needed to send it."
   - What did you use to make it? (template, app, accounting tool, email…)
   - **Which device were you on?** Where were you? → **A-02**
   - How long did it take?
   - Anything annoying, slow, or that went wrong?
5. "What about the two before that? Same device, same process?" → **A-02** (record the device for each of the 3)
6. "Have you ever needed to send or check an invoice when you **weren't at your computer**? What happened?" → **A-02**
7. "Have you **switched** invoicing tools or methods before? What made you switch?" → **A-01**
8. "What's the **most frustrating thing** about invoicing for you?" → **A-01**

### Part 3: Getting paid and keeping records (6 min) → A-03, A-04
9. "Right now, without checking, could you tell me **who owes you money** and how much?" → **A-03**
   - How do you keep track? When did a payment last slip through the cracks?
10. "Where do your old invoices live? If that disappeared tomorrow, what would happen?" → **A-04**
    - Has anything like that ever happened to you?
11. "Does your invoicing tool need an account or internet? Has that ever been a problem?" → **A-04** (ask neutrally; don't suggest it *should* be a problem)
12. "What do you pay for invoicing or business tools today? Monthly or one-off?" → **A-05**

### Part 4: Pitch ranking (3 min) → A-04, C4/C5/C6
Show or read these **three one-line pitches**, in a different order for each person, and ask: *"Which of these would make you most likely to try an app? Why? Which least?"*
- **P-A, data safety:** "Your invoices never disappear: stored on your phone with backups you control."
- **P-B, no paywall:** "Unlimited professional invoices for free: no caps, no ads."
- **P-C, offline / no account:** "Make and send invoices anywhere, offline, with no sign-up."

Record the ranking and the *reason*, word for word if possible.

### Part 5: Close (1 min)
13. "Is there anything about invoicing I should have asked but didn't?"
14. "Can I show you a rough prototype in a few days, or would you try an early version?" (A **yes** is a weak but useful signal of interest; record it.)
15. "Is there anyone else you think I should talk to?"

## Prototype for the usability test

**Purpose:** see whether someone can complete the core flow on a phone **without help**, and where they hesitate. This is not a test of the visual design.

**Format, cheapest first:**
1. **Clickable phone mockup.** Claude can build this as a web page that opens on any phone browser, with no app install. *Recommended.*
2. Figma (free) clickable prototype.
3. Paper sketches, photographed and shown over video.

**The screens to include:**
1. Home: outstanding total, recent invoices, a **Create invoice** button.
2. New invoice: client, line items (description, quantity, price), currency, due date, with the total calculated.
3. PDF preview, with **Share** and **Save**.
4. Invoice list with Paid / Unpaid status, and an action to mark an invoice paid.

## Usability test (about 10 minutes, at the end of the interview or as a follow-up)

Run it on **their phone** if possible (share a link). Ask them to **think out loud**. Don't help unless they're stuck for more than 60 seconds, and record if you did.

| # | Task (read exactly) | Success = | Record |
|---|---|---|---|
| T1 | "You just finished a logo for a client called **Müller Design GmbH** in Germany. Send them an invoice for **€450**, due in 14 days." | PDF preview reached with the correct client, amount and currency | Time, wrong taps, where they paused, what they said |
| T2 | "The client says they also want **2 extra revisions at €60 each**. Update the invoice." | Total shows **€570** | Did they find editing? Did they check the total? |
| T3 | "Send the PDF to the client." | Share sheet reached | Did they look for email, WhatsApp, or something else? |
| T4 | "A week later, they paid. Record that." | Invoice marked Paid | Did they find it? Where did they look first? |
| T5 | "How much are you still owed in total?" | Reads the outstanding total | Instant, or did they hunt? |

After the tasks, ask:
- "On a scale of 1–5, how easy was that?" and "Why not a 5?"
- "Would you use this for your next real invoice? What would stop you?"

**Success bar:** at least **4 of 5 tasks** completed unaided by at least 6 of 8 participants. Any task that 3 or more people fail goes into the P2 design backlog.

## Notes: privacy and where to keep them

**This repository is public (D-006).** Raw interview notes contain people's names, clients and income, so they must **never** be committed.
- Keep raw notes in `research-private/` in the project folder. **Git ignores this folder**, so it is never uploaded. Back it up separately (for example to a private cloud folder).
- In `research-private/`, refer to each person by code only (P1, P2…). Keep the code-to-name list in a separate file there.
- Delete recordings once the notes are written, and tell participants that you will.
- `VALIDATION_RESULTS.md` (public) contains **only anonymized, aggregated** findings: counts, themes, and quotes with no names, companies or amounts that could identify someone.

### Per-interview note template (copy into `research-private/P#.md`)

```
Participant: P#    Date:    Persona: primary / secondary    Android: yes/no
Work / clients' countries / currencies:
Invoices per month:
Last 3 invoices: tool + DEVICE for each:  1)        2)        3)
Ever needed to invoice away from computer? (story):
Biggest invoicing frustration (quote):
Switched tools before? why:
Knows who owes them instantly? (y/n) + how tracked:
Old invoices stored where / ever lost?:
Account/internet ever a problem? (unprompted / prompted / no):
Pays for tools: what, how much:
Pitch ranking (A/B/C) + reason (quote):
Usability: T1 T2 T3 T4 T5 (✓ / ✓ with help / ✗) + notes:
Ease 1–5:      Would use for next invoice? why/why not:
Wants to try early version? y/n
Surprises:
```

### Results tally (Day 6)

| | P1 | P2 | P3 | P4 | P5 | P6 | P7 | P8 | Total | Threshold | Verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| A-02: ≥1 of last 3 invoices on a phone, or a specific need | | | | | | | | | | ≥ 4 | |
| A-01: concrete recent pain + has tried an alternative | | | | | | | | | | ≥ 4 | |
| A-04: raised local/offline/account unprompted, or ranked P-A/P-C first | | | | | | | | | | ≥ 3 | |
| A-03: can't say instantly who owes them | | | | | | | | | | ≥ 4 | |
| Usability: ≥ 4/5 tasks unaided | | | | | | | | | | ≥ 6 | |

## Common traps
- **Counting compliments as evidence.** "That's a great idea!" is not data. "I used my phone for the last two" is.
- **Interviewing only people who already like you.** Get at least half the participants through strangers or introductions.
- **Changing the questions halfway.** Answers then can't be compared. Fix only the prototype between days.
- **Stopping at 3 interviews because the answers feel clear.** Patterns at 3 often reverse by 8.
