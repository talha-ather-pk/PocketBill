# PocketBill — Project Status

_Last updated: 2026-10-15_

## Current phase

**Phase 0 — Environment discovery and project setup: complete (2026-10-15)**

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
| 2026-10-15 | Clickable prototype: tasks T1–T5 and the session log work; reset afterwards | Founder dry run on Galaxy A72 (phone browser) |
| 2026-10-15 | `gradlew installDebug` on SM-A725F (Android 14); app launched, MainActivity resumed, Home/Invoices/Clients/Settings tabs tapped, same process, empty crash log | adb install + `am start`, `input tap`, screenshots, `logcat -b crash` |
| 2026-10-15 | SDK installed (platform android-37.0, build-tools 36.0.0). `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21); `app-debug.apk` produced | Command run by Claude Code with `JAVA_HOME=C:\java` |

## Exact next action

Phases 2 and 3 are fully drafted. Phase 4 (building features) waits for **P1-T07** (D-010).

1. Founder: keep P1-T07 running (VALIDATION_PLAN.md, "Follow-up: phone-creation check"). Pass mark: >= 3 of 6 create a real invoice on the phone.
2. Founder: run the T3 sending re-check with 2–3 participants (VALIDATION_PLAN.md, "Follow-up: sending re-check").
3. Founder: review the drafts: UX_SPEC.md sections 1–8, DATA_MODEL.md sections 1–7, ARCHITECTURE.md.
4. Claude: when P1-T07 passes, start Phase 4 with P4-T01 (app shell: tokens, navigation, AppContainer) and P4-T02 (money and calculations, DATA_MODEL 6). If the founder agrees, P4-T02 can start before P1-T07 finishes, because the money code is needed whatever P1-T07 decides.

## Resume prompt for a new Claude session

```
I'm building PocketBill, an Android invoice app (Kotlin/Compose). Read README.md and
docs/PROJECT_STATUS.md, docs/ROADMAP.md, docs/DECISIONS.md, docs/ENVIRONMENT.md first.
Phase 0 complete (all tasks verified). Current phase: Phases 2 and 3 drafted (UX_SPEC, DATA_MODEL, ARCHITECTURE), awaiting founder review. Phase 4 waits for P1-T07 (D-010). Decisions up to D-011.
Project folder: C:\dev\PocketBill. Continue from "Exact next action" in PROJECT_STATUS.md. Follow the master project prompt
rules: small numbered tasks, never fake verification, explain things simply (I'm a beginner on Windows).
```
