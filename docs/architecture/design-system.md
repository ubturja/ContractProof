# ContractProof design system

Shared UI rules for the Android MVP. Components live in `composeApp/src/commonMain/kotlin/com/contractproof/core/design/`. Screens call those components. They do not restyle Material widgets one by one.

The product is professional, trustworthy, operational, and mobile-first. Navy is the action color. Surfaces are white on a cool gray background. Amber and red are only for warning and error. Green is only for a satisfied requirement or an uploaded record.

Cleaner actions use a 56 dp target, a verb label, and as little typing as possible.

## Typography

Material 3 type scale. No custom font.

- Screen titles use `headlineMedium` or `titleLarge`.
- Cleaner actions and task names use `titleMedium` or larger.
- Body copy uses `bodyLarge`.
- Status labels and supporting notes use `labelLarge` or `bodyMedium`.

## Spacing

`CpSpacing` in `Tokens.kt`: 4, 8, 12, 16, 24, and 32 dp. Screen padding is 16 dp. Related items inside a card use 8 or 12 dp.

## Corner radii

Cards, buttons, inputs, dialogs, and bottom sheets use 12 dp. Chips and status pills are fully rounded.

## Elevation

Cards stay flat and use a 1 dp outline. Dialogs and bottom sheets may lift one step above the page. Nothing else uses a shadow.

## Icons

Material Symbols, filled, one weight, from `material-icons-core` 1.7.3. An icon is used only to identify an action or a sync state. It does not decorate a heading.

## Buttons

`CpButton` is 56 dp tall and full width. `Primary` is the one next action. `Secondary` is the alternate, such as reporting an exception. Labels are verbs: “Take photo”, “Save exception”, “Finish service”.

## Inputs

`CpTextField` is single-line. The complaint and the exception note pass `singleLine = false`. The error string sits under the field. The typed value stays on screen when the save fails.

## Cards

`CpCard` is the plain container: white, 12 dp radius, outline, 16 dp padding. Domain cards below use it.

## Chips and status indicators

`CpStatusIndicator` is the only status chip. The statuses are `pending`, `uploading`, `uploaded`, `failed`, `retrying`, `missing`, and `exception`.

- Pending, retrying, and exception use amber.
- Uploading uses navy.
- Uploaded uses green.
- Failed and missing use red.

A pending photo is evidence on the device. A failed upload is not a server record. The chip does not say the work was skipped unless the status is `missing`.

## Progress

`CpCoverageProgress` shows a percent and a filled bar for evidence coverage. `CpLabeledProgress` is the indeterminate bar for “Reading requirements from the contract” and “Building the evidence report”. The label stays visible. A blank screen is not progress.

## Dialogs

`CpConfirmDialog` is for finishing a service or another action that should not happen from a stray tap. It has a verb confirm label and a dismiss label. It is not used to display a report or a photo.

## Bottom sheets

`CpSheet` holds a short choice, such as an exception reason. It shows a title and the choices. It is not a second screen.

## Navigation

`CpTitleBar` is the top chrome: a title, and an optional back action. The navigation library is not part of this foundation. Product screens are not built here.

## Error states

`CpErrorState` names what failed and offers one retry. The wording stays operational. Retry repeats the same action. It does not create a second photo, extraction, or PDF.

## Evidence cards

`EvidenceCard` shows the requirement name and one `CpWorkStatus`. It does not load a file and it does not call a repository.

## Task cards

`TaskCard` shows the requirement and one next-action button. The action label is passed in, so the card does not decide whether the next step is a photo or an exception.

## Dispute cards

`DisputeCard` shows the client, location, date, and complaint. It does not say who is right. It does not show a generated report.
