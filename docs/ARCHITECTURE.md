# Architecture

**Status: draft. No code exists yet.** Finalized in Phase 3. The direction below is proposed (see DECISIONS D-003).

## Intended shape

- Single-activity Android app; screens are Jetpack Compose functions connected by Navigation Compose.
- Layers:
  - **UI** (composables): displays state and sends user actions. No database access.
  - **ViewModels:** hold screen state and survive rotation; call repositories.
  - **Repositories:** the only code that talks to the database, file system, PDF generator, and backup.
  - **Room database:** entities + DAOs, with schema export so migrations can be tested.
  - **Domain logic (pure Kotlin, no Android):** money arithmetic, totals, tax/discount, overdue rules, invoice numbering. Fully unit-tested.
  - **PDF generation:** builds documents from saved invoice data, never from screenshots.
  - **Backup/restore:** versioned file written through the Android document picker (no broad storage permission).
- No custom server, no accounts, no network access in the MVP.
- Money stored as integer minor units (e.g. cents) with per-currency precision; no `Double` for money. Rules to be finalized in P3-T03.
