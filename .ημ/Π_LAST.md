# Π handoff

- time: 2026-03-21T19:38:06Z
- branch: main
- pre-Π HEAD: 86b8d71
- Π HEAD: pending at capture time; resolved by the final commit after artifact assembly

## Summary
- Persist the Fork Tales canonical-path relocation across backend config, story-engine docs/specs, receipts, and the deployed web node shim file.
- Keep the currently deployed dedicated gates.ussy.promethean.rest storyteller path documented against the new orgs/octave-commons/fork_tales narrative home.

## Notes
- push branch: pi/fork-tax/2026-03-21-193439
- origin remains git@github.com:octave-commons/gates-of-aker.git; current local main diverges from origin/main, so the snapshot is published on a dedicated Π branch plus tag.

## Verification
- pass: cd backend && clojure -X:test (149 tests / 545 assertions / 0 failures / 0 errors)
- pass: npm run build --prefix web
