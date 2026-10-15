# Decision Log

Each entry: context, options, decision, reason, status. "Proposed" decisions are not final until the founder approves them.

## D-001 — Project folder location
- **Context:** The project currently lives in `C:\Users\DELL\OneDrive\Desktop\invocie app`, which OneDrive syncs. Android/Gradle builds generate thousands of changing files; OneDrive sync often locks them and breaks builds.
- **Options:** (a) keep it in OneDrive; (b) move to `C:\dev\PocketBill` and use Git + GitHub (private repo) for backup.
- **Decision:** (b). Source code is backed up by Git commits pushed to a private remote, not by OneDrive.
- **Status:** Accepted by founder 2026-10-15. Files moved; the old OneDrive folder is now empty. Private remote created and pushed 2026-10-15: https://github.com/talha-ather-pk/PocketBill.

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
