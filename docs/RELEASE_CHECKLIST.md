# Release Checklist

**Status: placeholder.** Completed in P8-T07, after re-checking current Google Play requirements from official documentation at release time. Requirements are not listed here yet on purpose; Play policies change and must not be copied from memory.

Sections to fill in:
- Technical (target API level, App Bundle, signing, version code, R8/minification, debug flags off)
- Policy (Data Safety, privacy policy URL, content rating, ads declaration, app access, permissions)
- Testing (closed-testing requirements for this developer account, device coverage)
- Store listing (icon, feature graphic, screenshots, descriptions free of unsupported claims)
- Privacy (no invoice data in logs or analytics)
- Signing key backup (stored outside the repository, with a recovery copy)
- Rollback / hotfix plan

## Data safety (known now, from DATA_MODEL.md section 7)
These come from PocketBill's own design, not from Play policy, so they can be listed ahead of time.
- [ ] Database version bumped if any Room entity changed, and the new schema JSON committed
- [ ] Migration tests and the full-chain test (v1 to latest) pass on the physical phone
- [ ] Update test: previous release installed, data created, new build installed over it, all data still present
- [ ] Every sample backup file in the test resources restores
- [ ] No `fallbackToDestructiveMigration` anywhere in the code
