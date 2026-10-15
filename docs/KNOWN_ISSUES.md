# Known Issues

| ID | Area | Issue | Impact | Status |
|---|---|---|---|---|
| KI-001 | Environment | `JAVA_HOME` (user variable) is `C:\java\bin`; it must be `C:\java`. | Command-line Gradle builds (`gradlew.bat`) fail with a "JAVA_HOME is set to an invalid directory" error. Android Studio itself is not affected because it uses its bundled JDK. | Resolved 2026-10-15 — set to `C:\java`; `C:\java\bin\java.exe -version` confirmed 17.0.12 |
| KI-002 | Environment | Project folder is inside OneDrive and its name contains a space and a typo. | Risk of locked-file build errors and sync conflicts. | Resolved 2026-10-15 — moved to `C:\dev\PocketBill` (D-001) |
| KI-003 | Environment | No hypervisor currently running. | The Android emulator cannot start until the emulator hypervisor driver or Windows Hypervisor Platform is enabled. | Open |
