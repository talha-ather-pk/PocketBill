# PocketBill — Project Status

_Last updated: 2026-10-17 (1.0 complete; release prep done)_

## Current phase

**1.0 is built and tested. Now: release preparation (Phase 8), waiting on founder steps.** Phases 0–5 complete; 6, 7 and 9 decided and documented.

## Task status

| Task | Name | Status | Notes |
|---|---|---|---|
| P0-T01 | Inspect the environment | **Verified** | Results in [ENVIRONMENT.md](ENVIRONMENT.md). Inspection commands were actually run on this machine. |
| P0-T02 | Recommend the technology stack | **Verified** | Toolchain versions confirmed by a successful build (D-005). Room 2.8.5 still proposed; confirmed in P4-T03. |
| P0-T03 | Create the Android project | **Verified** | Builds, unit tests pass, installed and launched on Galaxy A72 (Android 14); all 4 tabs open with no crash (2026-10-15). |
| P0-T04 | Git and recovery practices | **Verified** | Local repo on `main`; `.gitignore`/`.gitattributes`; README "Saving your work". Pushed to https://github.com/talha-ather-pk/PocketBill. Repo made **public** 2026-10-15 at the founder's request (D-006). |

**Phase 1 progress:** P1-T01 to P1-T06 done or drafted; MVP_SPEC.md **draft 2** with all questions answered (D-009, D-010). Validation results in [VALIDATION_RESULTS.md](VALIDATION_RESULTS.md) (A-02 met as written: 6/8, but creation happens on laptops). **P1-T07 phone-creation check is the gate for Phase 4.** Target market: D-007. Later phases: see [ROADMAP.md](ROADMAP.md).

## Failed tasks

None.

## Blocked tasks

None.

## Important decisions

See [DECISIONS.md](DECISIONS.md). Accepted: project moved to `C:\dev\PocketBill` (D-001); physical phone is primary test device (D-004).

## Verification log

| Date | What was verified | How |
|---|---|---|
| 2026-10-15 | Tool inventory, JDK version, Git version, folder contents, disk/RAM/virtualization | PowerShell commands run by Claude Code on the laptop |
| 2026-10-15 | JAVA_HOME fixed to `C:\java`; docs moved to `C:\dev\PocketBill` | Read back env var; listed copied files |
| 2026-10-15 | `winget install Google.AndroidStudio` (v2026.2.1.8 found) **failed, exit 65**: package licence agreement not accepted | Nothing was installed. |
| 2026-10-15 | Founder accepted the licence; Android Studio 2026.2.1.8 installed (exit 0); `studio64.exe` present | winget output plus file check. SDK not installed yet. |
| 2026-10-18 | On-phone walkthrough in the QA copy: T1, T2, T4, autosave pass; 2 bugs found and fixed (email opened the full share sheet; UTC dates after midnight). testDebugUnitTest 231/231; `am instrument` OK (53 tests). **PocketBill-1.0.0.apk** built, signed (v2) and verified, saved to `C:\dev\PocketBill-APK` | adb, apksigner |
| 2026-10-17 | Completing 1.0 (D-023): CSV export for accountants (F-19), Home "payment details changed" card. testDebugUnitTest 230/230; `am instrument` OK (53 tests) | Test reports, adb |
| 2026-10-17 | Logo import bug found by re-reading (bounds decode always returned null → every pick failed); fixed; LogoStoreTest added. `am instrument` OK (52 tests) | adb |
| 2026-10-17 | Phases 6–9: decisions D-020–D-022, monetization, analytics, store listing, privacy policy, release checklist, launch plan; `bundleRelease` builds (9 MB AAB, unsigned until the key exists); Send feedback row (hidden until support email set) | Build output, docs |
| 2026-10-17 | Phase 5: testDebugUnitTest 223/223; on SM-A725F `am instrument` OK (49 tests) incl. PDF matrix; security review; logs no longer include error messages (could contain client names); very large amounts shrink to fit PDF columns | Test reports, adb, SECURITY_REVIEW.md |
| 2026-10-17 | F-14 logo: testDebugUnitTest 216/216; on SM-A725F `am instrument` OK (44 tests) incl. logo drawing and logo in backup | Test reports, adb |
| 2026-10-17 | On SM-A725F over **Wi-Fi debugging** (USB not detected): `am instrument` → **OK (42 tests)**, incl. the 5 backup tests. Gradle's connectedDebugAndroidTest reports 42/42 passed in its own report but marks the task failed over Wi-Fi (tooling quirk; same command passed over USB). App updated with `install -r`; founder's database untouched (file time unchanged). PDF visual check of made-up samples (A4, Letter, draft, paid 50 items/3 pages, void): layout correct; found and fixed two bugs: digits 6 and 8 vanished from copied/searched PDF text (tnum feature), and the stamp overlapped the dates (moved beside the totals). Samples deleted from phone and laptop | adb + PDF text and page images |
| 2026-10-17 | P4-T13/T14: testDebugUnitTest 215/215 (4 new ManifestTest); merged release manifest inspected; lintDebug 0 errors, 6 deliberate warnings (3 version updates deferred to Phase 5, 3 plural candidates that are always correct) | Test XML reports, merged manifest, lint report |
| 2026-10-17 | Saved items screen: testDebugUnitTest 211/211 (4 new). No placeholder screens remain | Test XML reports |
| 2026-10-17 | P4-T12 backup & restore: testDebugUnitTest 207/207 (12 new incl. a frozen v1 sample file). Tests caught a real bug (backup creation time overwrote each record's createdAt), fixed. 5 on-phone tests written (round trip, safety copy, bad files, half-way failure rollback, reminder) but **not run**: phone disconnected | Test XML reports |
| 2026-10-17 | P4-T15 Home: testDebugUnitTest 195/195 (9 new). Not checked on the phone (disconnected) | Test XML reports |
| 2026-10-17 | P4-T09 sending: testDebugUnitTest 186/186 (15 new: templates, send flow, Issue and send). Build passes. **Not checked on the phone** (disconnected): Gmail opening directly with the PDF attached, WhatsApp via the share sheet, Save PDF | Test XML reports |
| 2026-10-17 | P4-T08 PDF: testDebugUnitTest 171/171 (25 new); connectedDebugAndroidTest on SM-A725F 37/37 (6 new PDF tests: D-007 glyphs, A4/Letter sizes, 50 items in <2 s, marks); also ran the 3 pending P4-T10/T11 tests: pass. App kept installed. Visual check of the PDF layout **not done** (phone disconnected mid-run) | Test XML reports |
| 2026-10-17 | P4-T10/T11: testDebugUnitTest 146/146 (18 new); on-phone tests not run (phone disconnected) | Test XML reports |
| 2026-10-17 | P4-T06 on-phone tests and hands-on check: pass | **Founder-reported** (Claude could not re-run: phone disconnected) |
| 2026-10-17 | P4-T06: testDebugUnitTest 128/128 (34 new); on-phone tests not run (phone disconnected) | Test XML reports |
| 2026-10-17 | P4-T05: 94/94 unit tests; connectedDebugAndroidTest on SM-A725F 25/25 (incl. v1→v2 migration); app kept installed, founder data untouched, safety copy made | Test XML reports + `run-as ls` before/after |
| 2026-10-17 | P4-T04 on SM-A725F: Setup on fresh data, profile saved and remembered after restart, Settings and Business profile screens; two layout fixes | adb + screenshots (deleted afterwards: they contained the founder's details) |
| 2026-10-17 | P4-T03 database: 64/64 unit tests; `connectedDebugAndroidTest` on SM-A725F → 20/20 RepositoryTest pass | Test XML reports read back by Claude Code |
| 2026-10-17 | P4-T01 on the Galaxy A72: editor hides the bottom bar, Back and tab navigation correct, dark theme, no crashes | adb install + taps, checked by screenshots (the UI text dump proved unreliable during transitions) |
| 2026-10-16 | P4-T01 app shell: `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL, 57/57 tests, no compiler warnings | Test XML reports + Gradle problems report read back by Claude Code |
| 2026-10-16 | P1-T07 phone-creation check: 5 of 6 participants created a real invoice on the phone (42–55 s); T3 re-check 5 of 6 under 30 s unaided | Founder-reported from participants' session logs |
| 2026-10-16 | P4-T02 money code: `gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 47/47 tests (45 new) | Test XML reports read back by Claude Code |
| 2026-10-15 | Clickable prototype: tasks T1–T5 and the session log work; reset afterwards | Founder dry run on Galaxy A72 (phone browser) |
| 2026-10-15 | `gradlew installDebug` on SM-A725F (Android 14); app launched, MainActivity resumed, Home/Invoices/Clients/Settings tabs tapped, same process, empty crash log | adb install + `am start`, `input tap`, screenshots, `logcat -b crash` |
| 2026-10-15 | SDK installed (platform android-37.0, build-tools 36.0.0). `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21); `app-debug.apk` produced | Command run by Claude Code with `JAVA_HOME=C:\java` |

## Exact next action

**State (2026-10-17): the app is complete for 1.0.** All MVP features are built (Phases 4–5), plus CSV export for accountants and the payment-details Home card (D-023), 231 unit tests and 53 on-phone tests pass, and the Play release is prepared (Phases 6–9 documents, release bundle builds). What's left needs the founder:

0. **APK ready:** `C:\dev\PocketBill-APK\PocketBill-1.0.0.apk` (D-024). To move your own data to it: Back up now → uninstall the old PocketBill → install the APK → restore the backup.
1. **Hand checks** in [QA_REPORT.md](QA_REPORT.md) (~20 min on the phone): sending, Home, backup/restore, saved items, logo, large font, TalkBack, dark mode.
2. **Identity** ([RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) section 1): choose the final application ID and a support email. Claude then renames the package, fills in the support email (shows Send feedback) and the privacy policy.
3. **Upload key:** run `tools\make-upload-key.ps1` once and back up `C:\dev\keys`.
4. **Google Play:** developer account, enable GitHub Pages for the privacy policy, create the app, closed test with ≥ 12 testers for 14 days, store graphics.
5. Then production release and [LAUNCH_PLAN.md](LAUNCH_PLAN.md).

Phone connection: USB is currently not detected; use Wireless debugging (`adb mdns services`, `adb connect`). Run on-phone tests with `adb shell am instrument -w com.example.pocketbill.test/androidx.test.runner.AndroidJUnitRunner` (Gradle mislabels Wi-Fi runs as failed).

## Resume prompt for a new Claude session

```
I'm building PocketBill, an Android invoice app (Kotlin/Compose). Read README.md and
docs/PROJECT_STATUS.md, docs/RELEASE_CHECKLIST.md, docs/DECISIONS.md, docs/ENVIRONMENT.md first.
The app is complete for 1.0 (Phases 4-5 done; Phases 6-9 planned and documented). Decisions up to D-024.
230 unit tests, 53 on-phone tests pass. Remaining: founder hand checks, app ID + support email,
upload key, Google Play account and closed test (12 testers x 14 days), then launch.
Project folder: C:\dev\PocketBill. Continue from "Exact next action" in PROJECT_STATUS.md.
Rules: small numbered tasks, never fake verification, explain things simply (I'm a beginner on Windows).
```
