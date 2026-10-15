# Decision Log

Each entry: context, options, decision, reason, status. "Proposed" decisions are not final until the founder approves them.

## D-001 — Project folder location
- **Context:** The project currently lives in `C:\Users\DELL\OneDrive\Desktop\invocie app`, which OneDrive syncs. Android/Gradle builds generate thousands of changing files; OneDrive sync often locks them and breaks builds.
- **Options:** (a) keep it in OneDrive; (b) move to `C:\dev\PocketBill` and use Git + GitHub (private repo) for backup.
- **Decision:** (b). Source code is backed up by Git commits pushed to a private remote, not by OneDrive.
- **Status:** Accepted by founder 2026-10-15. Files moved; the old OneDrive folder is now empty. Private remote created and pushed 2026-10-15: https://github.com/talha-ather-pk/PocketBill.

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
- **Status:** Proposed — finalize after the template project is generated and builds.
