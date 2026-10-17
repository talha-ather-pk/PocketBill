# Release Checklist

**Status:** filled in P8-T07 on 2026-10-17. Play requirements were checked that day against Google's pages ([target API](https://developer.android.com/google/play/requirements/target-sdk), [testing for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)). **Re-check both links on release day**; policies change.

**Who:** 👤 = founder (needs your Google account, money or a personal choice) · 🤖 = Claude can do it.

## 1. Identity (P8-T01) — blockers
- [ ] 👤 Final **application ID** (D-002). It can never change after the first upload. Suggested: `com.pocketbill.invoices` or `io.pocketbill.app` (no personal name needed). 🤖 then renames the package everywhere.
- [ ] 👤 **Support email** for the store listing, privacy policy and Send feedback (a new address like pocketbill.app@gmail.com keeps your personal one private). 🤖 then fills `support_email` and the documents.
- [ ] 👤 Keep the name "PocketBill"? Quick search of Play for apps with the same name first.

## 2. Technical
- [x] Target API ≥ 36 for new apps from 31 Aug 2026 → app targets **37**
- [x] Android App Bundle builds: `gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [ ] 👤 Create the upload key once: `powershell -ExecutionPolicy Bypass -File tools\make-upload-key.ps1`, then **back up `C:\dev\keys` and `keystore.properties`** (USB drive / password manager, never GitHub)
- [ ] Opt in to **Play App Signing** when creating the app (Google keeps the app signing key; a lost upload key can be reset)
- [ ] 🤖 `versionCode` 1 → bump every upload; `versionName` "1.0.0"
- [x] R8/minification off for 1.0 (D-022); debuggable is off in release by default
- [x] No permissions, no INTERNET, cloud backup off (`ManifestTest`, D-019)
- [x] Large screens: no locked orientation, edge-to-edge (API 36 rules)

## 3. Policy
- [ ] 👤 Host the privacy policy: GitHub → repository Settings → Pages → Source "Deploy from a branch", branch `main`, folder `/docs` → URL `https://talha-ather-pk.github.io/PocketBill/privacy-policy.html`. 🤖 fills in the date and support email first.
- [ ] Data Safety form: answers in [STORE_LISTING.md](STORE_LISTING.md) (no data collected or shared)
- [ ] Content rating questionnaire: answers in STORE_LISTING.md
- [ ] Ads declaration: **No ads**. App access: **all features available without login**.
- [ ] Target audience: 18+ (business tool), not designed for children

## 4. Testing (P8-T05)
- [ ] 👤 Google Play developer account (one-time fee; identity verification)
- [ ] 👤 New **personal** accounts (created after 13 Nov 2023): run a **closed test with at least 12 testers opted in for 14 days in a row** before applying for production. Invite the validation participants (P1-T05) plus friends; ask them to keep it installed for the whole 14 days.
- [ ] 🤖+👤 Hand checks in [QA_REPORT.md](QA_REPORT.md) pass on the release build from the testing track
- [ ] Play Console pre-launch report: no crashes

## 5. Store listing (P8-T02)
- [ ] Text: [STORE_LISTING.md](STORE_LISTING.md)
- [ ] 👤 512 px icon, 1024 × 500 feature graphic, 2–8 screenshots with **made-up data only**

## 6. Privacy
- [x] No invoice data in logs (`SafeLog`, `PrivacyGuardTest`)
- [x] No analytics, ads or crash SDKs (D-021)
- [x] [SECURITY_REVIEW.md](SECURITY_REVIEW.md) re-read

## 7. Rollback / hotfix plan
- A bad release: in Play Console, **halt the rollout** (use staged rollouts: 20% → 50% → 100% over a week).
- Fix, bump `versionCode`, upload; Play can't roll back to an older version code, so the fix is always a new build.
- Database: never ship a schema change without its migration and test (below); a broken migration is the one thing a hotfix can't undo.

## Data safety (known now, from DATA_MODEL.md section 7)
These come from PocketBill's own design, not from Play policy, so they can be listed ahead of time.
- [ ] Database version bumped if any Room entity changed, and the new schema JSON committed
- [ ] Migration tests and the full-chain test (v1 to latest) pass on the physical phone
- [ ] Update test: previous release installed, data created, new build installed over it, all data still present
- [ ] Every sample backup file in the test resources restores
- [ ] No `fallbackToDestructiveMigration` anywhere in the code
