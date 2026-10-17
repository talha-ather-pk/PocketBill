# PocketBill — Security and Privacy Review (P5-T06)

_Reviewed 2026-10-17 by Claude Code against the code at the commit that adds this file. Re-run before every release (RELEASE_CHECKLIST)._

**Summary:** PocketBill keeps all data on the phone, asks for no permissions, has no network access and exposes nothing to other apps except the PDF the user chooses to share. No high or medium issues are open. Two low items are accepted below.

## 1. What data exists and where it lives

| Data | Where | Who can read it |
|---|---|---|
| Business profile, clients, invoices, items, saved items | Room database in the app's private storage | Only PocketBill (Android app sandbox) |
| Logo images | `files/logos/` (private) | Only PocketBill |
| Generated PDFs | `cache/pdf/`, only the latest one is kept | PocketBill; the app the user shares with gets read access to that one file |
| Pre-restore safety copy | `files/backups/pre-restore-backup.json` (private) | Only PocketBill |
| Backup files the user saves | Wherever the user picks in the system "save as" picker | The user decides |
| Backup-card preference | SharedPreferences (private) | Only PocketBill |

## 2. Checks

| # | Check | Result | Evidence |
|---|---|---|---|
| S-1 | No permissions requested (release manifest merged from all libraries) | ✅ Only androidx.core's app-private, signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (no prompt) | `ManifestTest`; merged manifest inspected (D-019) |
| S-2 | No INTERNET permission and no networking code | ✅ | `ManifestTest`, `PrivacyGuardTest` |
| S-3 | Android cloud backup off; phone-to-phone transfer allowed | ✅ `allowBackup=false`, `fullBackupContent=false`, extraction rules exclude cloud | `ManifestTest` (D-009, D-019) |
| S-4 | Exported components | ✅ Only `MainActivity` (launcher) and androidx's profile receiver (system shell only, `DUMP` permission) | `ManifestTest`, release manifest |
| S-5 | FileProvider scope | ✅ Only `cache/pdf/`; `exported=false`; read access granted per share to the chosen app | `ManifestTest`, `ShareIntents` |
| S-6 | PendingIntent for the share-sheet callback | ✅ Explicit intent to our own non-exported receiver; `FLAG_MUTABLE` only so Android can add the chosen app | `ShareIntents.chooser` |
| S-7 | Personal data in logs | ✅ Only `SafeLog`, which writes the error type, never the message (file names contain client names) | `PrivacyGuardTest` |
| S-8 | SQL injection | ✅ Room with bound parameters only; no raw SQL | code search |
| S-9 | Untrusted input: backup files | ✅ Size capped at 50 MB, version checked first, every table validated before anything changes, one transaction, safety copy | `BackupFormatTest`, `BackupRepositoryTest` |
| S-10 | Untrusted input: images for the logo | ✅ Decoded with size limits (sampled, max 600 px), stored as PNG we create | `LogoStore` |
| S-11 | Untrusted input: text fields | ✅ Amounts parsed with limits (DATA_MODEL 6); text is drawn as text in PDFs, never interpreted | `AmountParserTest`, PDF matrix tests |
| S-12 | Analytics, ads, crash-reporting SDKs | ✅ None (D-021) | dependency list |
| S-13 | Debug-only code in release | ✅ Compose preview activity is `debugImplementation` only | release manifest |
| S-14 | Secrets in the public repository (D-006) | ✅ None; signing keys and `keystore.properties` are git-ignored (P8) | `.gitignore` |

## 3. Accepted low risks

| # | Risk | Why accepted |
|---|---|---|
| L-1 | Data isn't encrypted beyond Android's own file-based encryption | Android encrypts app storage on every supported device (Android 10+ requires it); a second layer adds key-loss risk for no real gain on a single-user phone. Revisit if a PIN/lock feature is added. |
| L-2 | A shared PDF or saved backup is outside our control | That is the user's explicit action; the privacy statement in Settings says so. |

## 4. Re-check before each release
Run the unit tests (`ManifestTest`, `PrivacyGuardTest` fail on regressions), inspect the merged **release** manifest, and re-read this table if a dependency was added.
