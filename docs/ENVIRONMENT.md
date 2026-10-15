# Development Environment (P0-T01)

Inspected by Claude Code on **2026-10-15** using PowerShell on this machine.
Everything below was observed directly unless marked *estimate*.

## Machine

| Item | Observed value |
|---|---|
| OS | Windows 11 Pro, version 10.0.26300, 64-bit |
| CPU | Intel Core i5-8365U @ 1.60 GHz (4 cores, 2019 laptop class) |
| RAM | 15.8 GB |
| Free disk on C: | ~34.9 GB |
| CPU virtualization (BIOS/firmware) | Enabled |
| Hypervisor currently running | No (`HypervisorPresent = False`) |
| winget (Windows package manager) | v1.29.380 — available |

## Developer tools

| Tool | Status | Notes |
|---|---|---|
| Git | ✅ Installed — 2.56.0.windows.1 | Global identity set: "Talha Ather" / talhaather.freelance@gmail.com |
| JDK | ✅ Installed (JAVA_HOME fixed 2026-10-15) | Oracle JDK 17.0.12 at `C:\java`. `JAVA_HOME` (user variable) was `C:\java\bin` (wrong); changed to `C:\java` on 2026-10-15. `java` is not on PATH (not required). |
| Android Studio | ✅ Installed 2026-10-15 | 2026.2.1.8 via winget, at `C:\Program Files\Android\Android Studio` (build 262.9437.185.2621.16467767). Setup wizard completed. |
| Android SDK | ✅ Installed | `%LOCALAPPDATA%\Android\Sdk`: platform android-37.0, build-tools 36.0.0, platform-tools, emulator, system image android-37.0 google_apis_playstore x86_64. `ANDROID_HOME` unset (not needed; `local.properties` points to the SDK). |
| adb (Android Debug Bridge) | ✅ Available | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` (not on PATH). |
| Gradle | Not installed globally | Not needed. The project ships its own Gradle Wrapper (`gradlew.bat`, Gradle 9.6.0). |
| Android emulator / AVDs | ⚠️ Cannot run | No AVD created; `emulator -accel-check` reports the hypervisor driver is not installed (KI-003). |
| Connected Android device | ✅ Verified | Test phone: Samsung Galaxy A72 (SM-A725F), Android 14 (API 34), One UI 6.1. Detected by adb over USB (needs a data cable; Samsung USB driver present). |

Note: a `Microsoft.NET.Sdk.Android.Manifest` entry exists in the registry. That belongs to .NET/MAUI tooling, not to the Android SDK we need; it can be ignored.

## Project folder

- **Current location: `C:\dev\PocketBill`** (moved 2026-10-15, decision D-001).
- Original location `C:\Users\DELL\OneDrive\Desktop\invocie app` was **empty** before this session (no files, not a Git repository).
- Risk: the folder is inside **OneDrive**. OneDrive syncs every file as it changes, and Android builds write thousands of temporary files into `build/` and `.gradle/` folders. This commonly causes "file is locked / access denied" build failures and wastes cloud storage. The folder name also contains a space and a typo ("invocie").
- `C:\dev` exists and contains one unrelated project (`aether-erp`). `C:\dev\PocketBill` is free.

## Missing prerequisites (blockers for P0-T03)

1. ~~**Android Studio**~~ — installed 2026-10-15.
2. **Android SDK platform + build tools** (installed through Android Studio's setup wizard).
3. **A test target:** an emulator image *or* a physical Android phone with USB debugging.
4. ~~Fix `JAVA_HOME`~~ — done 2026-10-15.

Install attempt 2026-10-15: `winget install --id Google.AndroidStudio -e` found version 2026.2.1.8, but cancelled with exit code 65 because the Android SDK License Agreement had not been accepted. Nothing was installed. Second attempt, after the founder accepted the licence: **succeeded** (exit 0).

## Capacity notes

- *Estimate:* Android Studio + SDK + one emulator system image need roughly 10–15 GB. 34.9 GB free is enough but not generous; avoid installing many emulator images.
- *Estimate:* an i5-8365U with 16 GB RAM can run Android Studio and one emulator, but slowly. A **physical Android phone** over USB will give a much faster and more realistic test loop and is recommended as the primary test device.
- The emulator needs a hypervisor. Android Studio's Standard setup offers the "Android Emulator hypervisor driver"; alternatively enable "Windows Hypervisor Platform" in Windows Features. Firmware virtualization is already enabled, so either should work (not yet verified).
