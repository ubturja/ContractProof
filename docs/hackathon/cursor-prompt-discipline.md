# Cursor prompt discipline

ContractProof MVP is **frozen**. Each Cursor session should do **one job**.

## Rules

1. **Implement only the current prompt.** Do not pull in work from future prompts or “while we’re here” improvements.
2. **Follow [global.mdc](../../global.mdc):** minimal scope, no unrelated refactors, no new libraries unless necessary, preserve architecture.
3. **Respect [mvp-freeze.md](mvp-freeze.md):** no out-of-scope features (NFC, CRM, payroll, etc.) unless judging explicitly requires them.
4. **Stop when the prompt’s deliverable is done** — run the relevant test or build step, report results, do not continue into the next prompt unless asked.
5. **No commit/push** unless the user explicitly requests it.

## When unsure

If a prompt conflicts with the freeze charter, prefer the **narrower** interpretation and document the gap in docs—not new product code.
