# PocketBill — Project Status

_Last updated: 2026-10-15_

## Current phase

**Phase 0 — Environment discovery and project setup**

## Task status

| Task | Name | Status | Notes |
|---|---|---|---|
| P0-T01 | Inspect the environment | **Verified** | Results in [ENVIRONMENT.md](ENVIRONMENT.md). Inspection commands were actually run on this machine. |
| P0-T02 | Recommend the technology stack | In progress | Official versions researched (DECISIONS D-005); finalize after the template project builds. |
| P0-T03 | Create the Android project | In progress | Project files exist; `assembleDebug` + unit tests **pass** (2026-10-15). Launch on a device not yet verified. |
| P0-T04 | Git and recovery practices | **Verified** | Local repo on `main`; `.gitignore`/`.gitattributes`; README "Saving your work". Pushed to https://github.com/talha-ather-pk/PocketBill. Repo made **public** 2026-10-15 at the founder's request (D-006). |

Later phases: see [ROADMAP.md](ROADMAP.md). **P1-T01 (market research) is in progress**: first pass in MARKET_RESEARCH.md; Google Play pages could not be read automatically, so the founder needs to do a manual Play Store pass.

## Failed tasks

None.

## Blocked tasks

- **P0-T03 (launch step only)**: the phone was plugged in, but Windows does not detect it (no USB device; `adb devices` empty). Likely a charge-only cable or the phone's USB mode. See KI-004.

## Important decisions

See [DECISIONS.md](DECISIONS.md). Accepted: project moved to `C:\dev\PocketBill` (D-001); physical phone is primary test device (D-004).

## Verification log

| Date | What was verified | How |
|---|---|---|
| 2026-10-15 | Tool inventory, JDK version, Git version, folder contents, disk/RAM/virtualization | PowerShell commands run by Claude Code on the laptop |
| 2026-10-15 | JAVA_HOME fixed to `C:\java`; docs moved to `C:\dev\PocketBill` | Read back env var; listed copied files |
| 2026-10-15 | `winget install Google.AndroidStudio` (v2026.2.1.8 found) **failed, exit 65**: package licence agreement not accepted | Nothing was installed. |
| 2026-10-15 | Founder accepted the licence; Android Studio 2026.2.1.8 installed (exit 0); `studio64.exe` present | winget output plus file check. SDK not installed yet. |
| 2026-10-15 | SDK installed (platform android-37.0, build-tools 36.0.0). `gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.21); `app-debug.apk` produced | Command run by Claude Code with `JAVA_HOME=C:\java` |

## Exact next action

1. Founder: get the phone detected (KI-004).
2. Then Claude: install and launch the app (`gradlew installDebug`), finish P0-T03, and finalize P0-T02 (D-005).

## Resume prompt for a new Claude session

```
I'm building PocketBill, an Android invoice app (Kotlin/Compose). Read README.md and
docs/PROJECT_STATUS.md, docs/ROADMAP.md, docs/DECISIONS.md, docs/ENVIRONMENT.md first.
Current phase: Phase 0. P0-T01 verified. P0-T02 in progress (see DECISIONS D-005). P0-T03: project builds and unit tests pass; launch on the phone still to verify.
Project folder: C:\dev\PocketBill. Continue from "Exact next action" in PROJECT_STATUS.md. Follow the master project prompt
rules: small numbered tasks, never fake verification, explain things simply (I'm a beginner on Windows).
```
