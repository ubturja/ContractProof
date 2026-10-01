# ContractProof MVP freeze (post–Prompt 89)

**Status:** Frozen for hackathon submission. New user-facing capabilities require an explicit prompt and judging justification.

## Product message

> **Contracts say what must happen. ContractProof proves what happened.**

ContractProof is a **contract evidence and dispute-defense** platform—not a generic cleaning-management or field-service CRM.

## Hackathon artifact

```
                CONTRACTPROOF
                     |
          +----------+----------+
          |                     |
      Android APK          Public GitHub Repo
          |                     |
          |                Source Code
          |                Documentation
          |                Architecture
          |                License
          |                README
          |
     Full MVP App
          |
          +-----------------------------+
          |                             |
     Contract → Evidence          RevenueCat
          |                             |
       Dispute                     Subscription
          |
       PDF Report
```

| Deliverable | Location |
|-------------|----------|
| Universal APK | `dist/ContractProof-1.0.0-1-universal.apk` (not in git); [README](../../README.md) |
| Judge flow | [final-golden-flow.md](../qa/final-golden-flow.md) |
| P0 scope | [p0-mvp-audit.md](../audits/p0-mvp-audit.md) |
| KMP showcase | [ship-kotlin-everywhere-showcase.md](../kmp/ship-kotlin-everywhere-showcase.md) |

## In scope (frozen)

Authentication, organization, roles, clients, locations, contracts, upload, AI extraction with **human approval**, schedules/jobs, cleaner workflow, evidence capture, exceptions, coverage, offline sync, service history, disputes, reconstruction timeline, PDF report, RevenueCat (Android), ClearLine demo seed, Android release build, documentation.

## Explicitly out of scope

Do **not** add unless a **specific judging requirement** demands it:

- NFC
- Advanced AI beyond current extraction/summary edge functions
- Computer vision / ML on device
- Route optimization
- Payroll
- Accounting
- CRM
- Social features
- Enterprise integrations (SAP, Salesforce, etc.)

## Change policy

| Allowed without scope review | Requires new prompt / judging rule |
|------------------------------|-------------------------------------|
| Bug fixes, crash fixes | New screens or modules |
| Docs, seed alignment, copy | New integrations |
| Security/RLS fixes | Feature parity expansions |
| Test fixes | “Nice to have” product ideas |

## Development rules

1. **One task per Cursor prompt** — implement only that task ([cursor-prompt-discipline.md](cursor-prompt-discipline.md), [global.mdc](../../global.mdc)).
2. No drive-by refactors or unrelated files.
3. Run relevant tests after meaningful code changes.
4. Demo data stays fictional (`is_demo`); no real PII in seed or screenshots.
5. Do not commit APKs, keystores, or `local.properties`.

## Related

- [hackathon-submission-readiness.md](../audits/hackathon-submission-readiness.md)
- [golden-path-hackathon.md](../qa/golden-path-hackathon.md)
