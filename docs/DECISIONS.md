# Decision Log

Each entry: context, options, decision, reason, status. "Proposed" decisions are not final until the founder approves them.

## D-001 — Project folder location
- **Context:** The project currently lives in `C:\Users\DELL\OneDrive\Desktop\invocie app`, which OneDrive syncs. Android/Gradle builds generate thousands of changing files; OneDrive sync often locks them and breaks builds.
- **Options:** (a) keep it in OneDrive; (b) move to `C:\dev\PocketBill` and use Git + GitHub (private repo) for backup.
- **Decision:** (b). Source code is backed up by Git commits pushed to a private remote, not by OneDrive.
- **Status:** Accepted by founder 2026-10-15. Files moved; the old OneDrive folder is now empty. Private remote created and pushed 2026-10-15: https://github.com/talha-ather-pk/PocketBill.

## D-022 — Release build: signing outside the repo, R8 off for 1.0 (P8-T06)
- **Decision:** Release signing reads `keystore.properties` (git-ignored), created once by `tools/make-upload-key.ps1`, which puts the upload key in `C:\dev\keys`. Use **Play App Signing**. R8/minification stays **off** for 1.0: the app is 9 MB as a bundle, and a shrunk build that was never tested on a phone (installing it over the founder's debug app would need an uninstall and lose data) is the bigger risk.
- **Revisit:** turn R8 on once a spare phone or emulator can run the release build end to end.
- **Status:** Accepted 2026-10-17 (Claude as product owner).

## D-021 — No in-app analytics or crash reporting in 1.0 (Phase 7)
- **Decision:** No analytics, crash-reporting or ads SDKs. Measure with Play Console (installs, retention, Android vitals for crashes/ANRs, ratings) plus a "Send feedback" email that attaches nothing from invoices. Details: [ANALYTICS_PLAN.md](ANALYTICS_PLAN.md).
- **Why:** The app has no INTERNET permission and promises that data never leaves the phone unless the user shares it.
- **Status:** Accepted 2026-10-17 (Claude as product owner).

## D-020 — 1.0 is completely free: no ads, no paywall, no billing (Phase 6)
- **Decision:** Every 1.0 feature is free and unlimited; no ads ever in the editor, PDFs or emails; no billing code. Later Pro features must be new value (e.g. recurring invoices, several business profiles), sold as a one-time unlock after retention is proven. Details: [MONETIZATION.md](MONETIZATION.md).
- **Why:** "Free, no ads, no cap" is the position against Zoho and Saldo; billing would need network access.
- **Status:** Accepted 2026-10-17 (Claude as product owner). Founder can override before launch.

## D-019 — Moving to a new phone is allowed; cloud backup stays off (P4-T14)
- **Context:** On Android 12+, `allowBackup="false"` alone doesn't settle device-to-device transfer (setting up a new phone from the old one, by cable or directly). D-009 forbids copying invoice data to Google's servers, but losing invoices when changing phones is the top user problem (UP-01).
- **Decision:** `data_extraction_rules.xml` **excludes everything from cloud backup** and **includes the database, preferences and files in device-to-device transfer**. On Android 8–11, `allowBackup="false"` and `fullBackupContent="false"` keep everything off. The PocketBill backup file (P4-T12) stays the main safety net.
- **Also checked (P4-T14):** the merged release manifest requests no permissions except androidx.core's app-private, signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (no user prompt, nothing leaves the phone). No `INTERNET`. Exported: only `MainActivity` and androidx's profile-install receiver, which only the system shell can call (`DUMP` permission). The debug-only Compose preview activity is not in release builds. `<queries>` (mailto and PDF sharing) are package visibility, not permissions. `ManifestTest` now fails the build if any of this changes.
- **Status:** Accepted 2026-10-17 (Claude as product owner).

## D-018 — Backup and restore details (P4-T12)
- **Context:** MVP_SPEC 7 sets the rules (JSON file, validate first, one transaction, safety copy, reminder) but left some details open.
- **Decision:**
  1. **Restore confirmation is a dialog** on the Backup screen (file date, counts of invoices, clients and saved items, "Replace data"), not a separate screen. The unused `RestoreConfirm` route was removed.
  2. A backup **must contain a business profile**; files without one are refused (the app can't run without it).
  3. **Logo:** format v1 reserves a `logo` field but leaves it empty until the logo picker exists (F-14). A restored profile has no logo file.
  4. **Safety copy:** `files/backups/pre-restore-backup.json` in the app's private storage, overwritten by each restore (the last one is kept).
  5. **Reminder:** the Home card shows when 10+ invoices were created since the last backup, or 30 days have passed since the last backup (or, if never backed up, since the first invoice). ✕ hides it for 7 days; that date is a phone preference, not invoice data, so it isn't in backups.
  6. **Checks before restore:** the app name, the profile, unique IDs, uuids and invoice numbers, links between tables, statuses (issued, paid and void invoices need a number and client; paid ones a paid date), known currencies, money and quantity limits, and issue/due dates for non-drafts. Half-finished drafts are accepted as they are.
- **Status:** Accepted 2026-10-17 (Claude as product owner).

## D-017 — Sending: who gets the email, and drafts
- **Context:** P4-T09 (UX_SPEC 4). Issued invoices freeze the client's details (DATA_MODEL 5.2), including their email. If the client later changes their billing address, "Email again" would go to the old one. Also, the PDF preview could offer Send for drafts.
- **Decision:**
  1. "Email to client" goes to the client's **current** email; if they have none, to the email frozen on the invoice. The address is a delivery detail, not part of what the invoice says. Names, amounts and payment details in the email still come from the frozen details, so the email matches the PDF.
  2. Drafts are sent only through **Issue and send** on invoice detail (with its confirmation). The preview of a draft shows the DRAFT watermark and no Send or Save buttons.
  3. Save PDF is offered for issued, paid and void invoices (in the preview and under More on detail). It never counts as sent.
  4. When Android lets the user pick between several email apps, the app can't tell which one was used, so it records "via email".
- **Status:** Accepted 2026-10-17 (Claude as product owner, at the founder's request to decide).

## D-016 — Migrations from now on (replaces D-014)
- **Context:** D-014 kept database version 1 open because no one had data yet. Since P4-T04 the founder has a real business profile on the phone. Changing version 1 again would make Room refuse to open that database (identity hash mismatch), and the app would crash at launch until its data was wiped.
- **Decision:** From P4-T05 on, **every** schema change bumps the version and ships with a migration and a migration test, exactly as DATA_MODEL 7 (G-1…G-7) describes. Version 1's schema JSON is frozen as committed in P4-T03.
- **First use:** version 2 adds `client.preferredCurrency` and `client.preferredTermsDays` (D-015) with Room's `@AutoMigration(from = 1, to = 2)`, an additive change (G-4). `MigrationTest` checks real version-1 rows survive.
- **Status:** Accepted 2026-10-17. Supersedes D-014.

## D-015 — Optional preferred currency and payment terms per client
- **Context:** The founder asked for custom currency and terms on the client form (P4-T05). The design (UX_SPEC 3) instead fills these from the client's last invoice.
- **Decision:** Add two **optional** client fields, `preferredCurrency` and `preferredTermsDays`. The editor fills a new invoice from, in order: the client's preference → the client's last invoice → the business defaults. Empty means "no preference".
- **Schema:** two nullable columns on `client`, in database **version 2** with an automatic migration (D-016).
- **Status:** Accepted 2026-10-17 (founder request).

## D-014 — Database version 1 stays open until the first release
- **Context:** DATA_MODEL 7 (G-1…G-7) requires a version bump, a migration and a test for every schema change. That protects users' data, but before the first release no user has any data, and the schema is still being built across Phase 4.
- **Decision:** Until the **first release build** (closed testing or production), the schema stays at **version 1** and its exported JSON (`app/schemas/`) is regenerated as entities change. From the first release build onward, G-1…G-7 apply in full and version 1's JSON is frozen.
- **Still applies now:** no `fallbackToDestructiveMigration` (G-1), and schema JSON is committed so every change is visible in Git.
- **Versions chosen for P4-T03** (checked against official release pages 2026-10-17): Room **2.8.5** (D-005; still the latest stable), KSP **2.3.12** (KSP versions are independent of Kotlin since 2.3.0; 2.3.10 added the Kotlin 2.4 fix), kotlinx-serialization-json **1.11.0**, AndroidX test core/runner **1.7.0**, ext-junit **1.3.0**.
- **Status:** Accepted 2026-10-17 (founder approved starting P4-T03 with this approach).

## D-013 — Accept the T3 sending re-check at 5 of 6
- **Context:** The T3 re-check rule needed every tested participant to send unaided in under 30 s. 5 of 6 did. P-e needed help (38 s) and had also given up on creating the invoice (120 s) (VALIDATION_RESULTS.md).
- **Decision:** Accept the result. P-e's difficulty looks general rather than specific to sending, since they struggled with both tasks. Respond to P-e's stated confusion (unsure items were saved) by making the editor's "Saving…" / "Draft saved ✓" indicator a prominent requirement (UX_SPEC 3).
- **Note:** P-e did attempt T3 (38 s, with help), so this rests on P-e struggling overall, not on P-e skipping the send step.
- **Status:** Accepted by founder 2026-10-16.

## D-012 — Start the money code (P4-T02) before P1-T07 finishes
- **Context:** D-010 holds Phase 4 feature code until the P1-T07 phone-creation check passes.
- **Decision:** Exception for **P4-T02 (money and calculations)** only. It's pure Kotlin with no screens, it's needed whatever P1-T07 decides (including a laptop companion), and it's fully testable on the computer.
- **Still held:** all other Phase 4 tasks wait for P1-T07.
- **Status:** Accepted by founder 2026-10-16.

## D-011 — No invoice activity log in the MVP
- **Context:** UX_SPEC 5 asked whether to record every send and status change, which needs a new `invoice_event` table that is cheapest to add before P4-T03.
- **Decision:** No. Invoice history shows only what existing columns hold: issued date, last sent (date and app), and paid date.
- **Consequence:** Earlier sends and earlier status changes are not kept. Adding a log later needs a database migration (DATA_MODEL 4, migration policy).
- **Status:** Accepted by founder 2026-10-16.

## D-010 — Phone-first creation, with a check before building (MVP_SPEC Q-06)
- **Context:** Validation found that freelancers use phones for status checks, quick edits and re-sending, but **create first invoices on laptops** (Q-01). PocketBill keeps data only on the phone, so invoices made elsewhere never reach it.
- **Options:** (a) phone-first, making creation fast enough to move to the phone; (b) a laptop companion with account-free sync (about 2× scope); (c) log and track invoices made elsewhere; (d) re-interview first.
- **Decision:** (a). Keep the MVP phone-only and invest in **fast creation**: duplicate invoice, recent clients, line-item presets, prefilled defaults.
- **Safeguards:**
  1. **Gate before Phase 4 (P1-T07):** ask the 6 phone-using participants to create their next real invoice in the prototype, and record how many do and how long it takes. Pass mark: **≥ 3 of 6 complete a real invoice on the phone.** If it fails, reopen options (b) to (d) before writing feature code.
  2. **Sync-ready data model:** Phase 3 gives every record a stable UUID and an update timestamp, so a laptop companion (option b) can be added later without migrating away from the MVP schema.
- **Status:** Accepted by founder 2026-10-16.

## D-009 — MVP scope answers (MVP_SPEC Q-03, Q-04, Q-05)
- **Quotes and estimates:** out of the MVP (Q-03).
- **Android cloud backup** (`android:allowBackup`): stays **off**, to keep the "data stays on your phone" promise strictly (Q-04). Data safety relies on PocketBill's own backup file (MVP_SPEC section 7).
- **Tax:** one tax rate per invoice is enough for the MVP (Q-05). Multiple tax lines would need a schema migration later.
- **Status:** Accepted by founder 2026-10-16.

## D-008 — Proceed to MVP specification
- **Context:** VALIDATION_PLAN.md says to proceed to P1-T06 only if A-02 (mobile invoicing) and A-01 (pain) are supported.
- **Decision:** The founder reports validation complete, with strong demand for offline-first, ad-free, account-free invoicing and A-02 supported "for quick tasks". Proceed to P1-T06.
- **Caveat:** The per-participant tally and counts are not yet recorded ([VALIDATION_RESULTS.md](VALIDATION_RESULTS.md)). "Quick tasks" needs clarifying, because whether people *create* invoices on the phone, or only check and send them, changes MVP priorities.
- **Status:** Accepted by founder 2026-10-15.

## D-007 — Target market and invoice language
- **Context:** P1-T02 asked which freelancers PocketBill targets and what language they invoice in (assumption A-06), because that sets the priority of non-Latin PDF support (UP-03).
- **Decision:** Target **global freelancers who invoice exclusively in English**. Full non-Latin script support in PDFs (Urdu, Arabic, Bengali, Hindi, right-to-left) is a **nice-to-have**, not an MVP requirement.
- **Still required in the MVP:** English invoices for global clients routinely contain **accented Latin names** (José, Müller, Øresund) and **currency symbols** (€ £ ¥ ₹ ₦ ₱ ₨). PDFs must render these correctly. That needs only the Latin-1/Latin Extended range and common currency symbols, so it costs little.
- **Status:** Accepted by founder 2026-10-15.

## D-006 — Repository is public
- **Context:** The founder wants anyone with the link to see the code and propose changes.
- **Decision:** The GitHub repo is **public**. Anyone can view or fork it and open pull requests. Only the founder can change `main`, by merging. The founder chose to keep the commit email (`talhaather.freelance@gmail.com`) visible in the history.
- **Consequences:** Never commit secrets (signing keys, API keys, `local.properties`), because anything pushed is world-readable and may be copied even if deleted later. There is no licence file yet, so by default others may view the code but not legally reuse it.
- **Status:** Accepted 2026-10-15. Visibility confirmed PUBLIC via `gh repo view`.

## D-004 — Primary test device
- **Decision:** Use the founder's physical Android phone (USB debugging) as the main test device. An emulator is secondary because this laptop's CPU is slow for emulation.
- **Status:** Accepted 2026-10-15.

## D-002 — Application ID is a placeholder
- **Context:** The application ID (e.g. `com.example.pocketbill`) permanently identifies the app on Google Play. After the first upload it cannot be changed without publishing a brand-new app listing (losing installs, reviews, and update continuity for existing users).
- **Decision:** Use a placeholder during development. Choose the final ID (typically a reversed domain you control, e.g. `com.yourname.pocketbill`) before the first Play Console upload. Google Play rejects `com.example.*`.
- **Status:** Accepted (placeholder). Final ID tracked in P8-T01.

## D-003 — Default technology stack (to be verified in P0-T02)
- **Proposed:** Kotlin, Jetpack Compose, Material 3, single activity, MVVM (ViewModel + repository), Room, coroutines/Flow, Android's built-in `PdfDocument` API for PDFs, system share sheet via `FileProvider`, Gradle Kotlin DSL with a version catalog, JUnit.
- **Status:** Proposed. Exact versions must be checked against current official documentation before the project is created. Versions are not guessed.

## D-005 — Version research for the stack (P0-T02, in progress)
Checked against official pages on 2026-10-15:

| Component | Current stable | Source |
|---|---|---|
| Android Studio | 2026.2.1.8 (version winget offered) | winget manifest for `Google.AndroidStudio` |
| Android Gradle Plugin | 9.4.0 (Sept 2026). Needs Gradle ≥ 9.6.0, JDK 17, Build Tools 36.0.0. Supports up to API 37. | https://developer.android.com/build/releases/gradle-plugin |
| Compose BOM | 2026.09.00 → ui/foundation 1.12.1, material3 1.4.0 (*mapping read by position from the table; to be confirmed against the generated project*) | https://developer.android.com/develop/ui/compose/bom/bom-mapping |
| Room 2 | 2.8.5 (2026-09-09), requires KSP and Kotlin 2.0+, minSdk 23 | https://developer.android.com/jetpack/androidx/releases/room |
| Room 3 | 3.0.3 stable (2026-09-09). New `androidx.room3` package, built for Kotlin Multiplatform, requires a `SQLiteDriver` | https://developer.android.com/jetpack/androidx/releases/room3 |

- **Proposed Room choice: Room 2.8.5.** It is still actively maintained, nearly all tutorials and Android docs use its API (which helps a beginner), and PocketBill does not need Kotlin Multiplatform. We can revisit Room 3 before the first release, because changing persistence libraries after users have data is riskier.
- **Proposed project-creation method:** create the project with Android Studio's built-in "Empty Activity" (Compose) template instead of hand-writing Gradle files. The template ships Kotlin, Compose compiler, and AGP versions that Google has already tested together, which avoids guessing Kotlin and KSP versions. Claude then inspects the generated version catalog (`gradle/libs.versions.toml`) and records the exact versions here.
- **Outcome (2026-10-15):** The project was not generated from the Android Studio template. Its Gradle files were written directly, using the versions above. The build verified them: `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL. Versions in use (`gradle/libs.versions.toml`):

  | Component | Version |
  |---|---|
  | Gradle | 9.6.0 |
  | Android Gradle Plugin | 9.4.0 |
  | Kotlin + Compose compiler plugin | 2.4.21 |
  | JDK | 17 (`C:\java`, 17.0.12) |
  | compileSdk / targetSdk / minSdk | 37 / 37 / 26 |
  | Compose BOM | 2026.09.00 → resolved ui 1.12.1, material3 1.4.0 (confirmed from the Gradle cache; matches the mapping above) |
  | core-ktx / lifecycle / activity-compose / navigation-compose | 1.19.1 / 2.11.0 / 1.13.0 / 2.10.2 |
  | JUnit | 4.13.2 |
- **Status:** Toolchain versions **verified** by a successful build. The Room 2.8.5 choice remains *proposed*. It will be confirmed (with KSP version) when Room is added in P4-T03.
