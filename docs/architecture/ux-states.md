# ContractProof UX states

This specification covers every Android MVP screen in [navigation.md](navigation.md). Those screens are the P0 surface. It does not add UI code.

Each screen records loading, empty, success, error, offline, permission denied where it applies, and retry.

## Shared rules

- Loading is a labeled progress state. The screen name or the action in progress stays visible. A blank screen is not a loading state.
- Empty states name what is missing and the single next action.
- Success is the usable screen. The primary content is on screen and the next action is obvious.
- Errors use operational language. They do not invent evidence, requirements, or a report.
- Offline shows cached content when it exists, plus a visible sync banner. If an action needs the network, the screen stays put and the user’s input stays intact.
- Permission denied applies to the camera on `Capture`. Location is optional and never blocks a photo. Notification permission is out of scope.
- Retry repeats the same action. It does not create a second evidence item, extraction, or PDF.

Sync states used on evidence and in the banner: `pending`, `uploading`, `uploaded`, `failed`, `retrying`.

## Splash

- **Loading:** the app is checking the saved session.
- **Empty:** does not apply. There is always a next route.
- **Success:** the session resolves and the app leaves for `Onboarding`, `Login`, `CompanySetup`, `Dashboard`, `Today`, or `ClientHold`.
- **Error:** the session cannot be read. The app continues to `Login`. It does not show another user’s data.
- **Offline:** a saved session still opens the matching shell. A missing session goes to `Login`.
- **Permission denied:** does not apply.
- **Retry:** the check runs once per launch. A failed read does not loop.

## Onboarding

- **Loading:** does not apply. The screen is local.
- **Empty:** does not apply.
- **Success:** three short statements of the product, then a single action to `Login`.
- **Error:** does not apply.
- **Offline:** the screen still works. It does not attempt a network call.
- **Permission denied:** does not apply.
- **Retry:** does not apply.

## Login

- **Loading:** “Signing in” replaces the submit action. The form stays visible.
- **Empty:** the form is waiting for email and password. Submit stays disabled until both are present.
- **Success:** the session is stored and the role route from the navigation spec opens.
- **Error:** wrong credentials or a rejected request. The message says sign-in failed. The password field clears. The email stays.
- **Offline:** “You need a connection to sign in.” The form stays. Submit does not pretend to succeed.
- **Permission denied:** does not apply.
- **Retry:** the user submits again. A failed attempt does not create an account.

## Register

- **Loading:** “Creating account” replaces the submit action. The form stays visible.
- **Empty:** submit stays disabled until email and password are present.
- **Success:** the session is stored. An owner with no organization goes to `CompanySetup`.
- **Error:** the account was not created. If the email is already in use, the message says so and points to `Login`.
- **Offline:** “You need a connection to create an account.” Entered values stay.
- **Permission denied:** does not apply.
- **Retry:** submit again with the same entries. A failed attempt does not create a second organization.

## CompanySetup

- **Loading:** “Saving company” after submit.
- **Empty:** the company name field is blank. Submit stays disabled until it has a name.
- **Success:** the organization exists and the app opens `Dashboard`.
- **Error:** the company was not saved. The name stays in the field.
- **Offline:** the name stays. Submit explains that a connection is required to create the company.
- **Permission denied:** a signed-in cleaner or client who reaches this route is sent to their own home. They do not see the form.
- **Retry:** submit the same name again. Retry does not create a second organization.

## ClientHold

- **Loading:** does not apply.
- **Empty:** does not apply.
- **Success:** one explanation that service review and disputes are not in the field app, plus sign out.
- **Error:** does not apply.
- **Offline:** the explanation still shows.
- **Permission denied:** does not apply. This screen is the client’s only Android destination.
- **Retry:** does not apply.

## Dashboard

- **Loading:** labeled placeholders for coverage alerts, open disputes, and today’s jobs.
- **Empty:** no locations yet. The next action is to add a location. If locations exist but nothing is scheduled today, say that no job is scheduled and offer contracts.
- **Success:** alerts, open disputes, and today’s jobs are listed. Each row opens the matching screen.
- **Error:** the remote refresh failed. Cached cards stay visible when they exist. The banner says the latest refresh failed.
- **Offline:** cached alerts, disputes, and jobs stay visible. The banner says the device is offline. Creating a location or starting extraction is not offered as if it would sync immediately.
- **Permission denied:** a cleaner never lands here.
- **Retry:** pull to refresh loads the remote copy again. It does not clear cached cards first.

## Locations

- **Loading:** placeholders for the location list.
- **Empty:** “No locations yet.” The next action is add a location. Free entitlement with the one-location limit reached replaces that action with `Paywall`.
- **Success:** the organization’s locations are listed.
- **Error:** the list failed to refresh. Cached locations stay.
- **Offline:** cached locations stay. Add location keeps the draft name and submits when a connection returns, or explains that the first location must be saved online if nothing is cached yet.
- **Permission denied:** a manager sees only assigned locations. A cleaner does not open this list.
- **Retry:** refresh the list. Retry does not duplicate a location that already saved.

## LocationDetail

- **Loading:** placeholders for the address, contracts, and recent jobs.
- **Empty:** the location exists and has no contract. The next action is upload a contract.
- **Success:** location facts, its contracts, and recent service records.
- **Error:** the refresh failed. The cached location stays.
- **Offline:** the cached location and its cached contracts stay. Upload stays available as a local file pick, and extraction waits for a connection.
- **Permission denied:** a manager who is not assigned to this location sees that it is unavailable. They are returned to `Locations`.
- **Retry:** refresh. Opening the screen again uses the cache first.

## Contracts

- **Loading:** placeholders for contract rows.
- **Empty:** “No contracts yet.” The next action is upload a contract.
- **Success:** each contract shows whether its requirements are unapproved or approved.
- **Error:** the refresh failed. Cached contracts stay, including unapproved extractions.
- **Offline:** cached contracts stay. Their approved or unapproved label stays. A contract that has not uploaded shows `pending`.
- **Permission denied:** cleaners do not open this list.
- **Retry:** refresh the list. Retry does not approve anything.

## ContractUpload

- **Loading:** “Saving contract” while the file is copied into app storage.
- **Empty:** no file is selected. The next action is choose a PDF, or enter requirements manually.
- **Success:** the file is stored on the device and the app opens `ExtractionReview`. The row shows `pending` until the server has the file.
- **Error:** the file could not be read. The picker can be used again. No contract row is created.
- **Offline:** the file still saves on the device. Extraction does not start. The screen says the file is saved and will be read when the connection returns.
- **Permission denied:** if the system file picker is denied, the message says a contract file is required and offers the picker again or manual entry. The camera is not involved.
- **Retry:** pick the file again, or retry the upload of the same saved file. Retry does not create a second contract for the same selection.

## ExtractionReview

This screen is the human gate. Extraction and approval are different successes.

- **Loading:** “Reading requirements from the contract.” The file name stays visible. Partial JSON is not shown as if it were approved.
- **Empty:** the model returned no requirements. The next action is add requirements manually. The contract stays unapproved.
- **Success:** extracted requirements are listed and can be edited. A separate approve action writes them as evidence rules. After approval, the contract is marked approved and the app returns to `Contracts`.
- **Error:** extraction failed. The contract stays unapproved. The message says the requirements could not be read. Manual entry remains available.
- **Offline:** the saved file stays. The extract action is disabled. The message says a connection is required to read the contract. Any requirements already reviewed stay on screen.
- **Permission denied:** Free entitlement disables extraction and sends the owner to `Paywall`. Manual entry is still allowed. A manager who cannot approve sees the list without the approve action.
- **Retry:** run extraction again on the same file. Retry replaces the unapproved draft. It does not approve the contract and it does not create a second contract.

## ServiceHistory

- **Loading:** placeholders for completed jobs.
- **Empty:** “No completed services yet.”
- **Success:** completed jobs with their coverage percent.
- **Error:** the refresh failed. Cached completed jobs stay.
- **Offline:** cached completed jobs stay. Jobs that finished on this device and have not synced show their local coverage and a `pending` sync mark.
- **Permission denied:** cleaners do not open history. A manager sees assigned locations only.
- **Retry:** refresh the list.

## ServiceRecord

- **Loading:** placeholders for the timeline, evidence, and exceptions.
- **Empty:** does not apply to a finished job. If the id is unknown, the screen says the service record is unavailable and returns to history.
- **Success:** read-only requirement, worker, timestamps, evidence, exceptions, and coverage.
- **Error:** the remote record failed to load. The cached record stays when this device has it.
- **Offline:** the cached record stays. Evidence files that are only on the server are marked unavailable on this device. Local pending photos still appear.
- **Permission denied:** a user outside the organization, or a manager outside the location, sees that the record is unavailable.
- **Retry:** reload the record. Retry does not change coverage.

## Disputes

- **Loading:** placeholders for open disputes.
- **Empty:** “No disputes.” The next action is record a dispute.
- **Success:** open disputes with client, location, and date.
- **Error:** the refresh failed. Cached disputes stay.
- **Offline:** cached disputes stay. Record a dispute keeps the draft locally and marks it `pending` if it cannot be sent.
- **Permission denied:** cleaners do not open this list.
- **Retry:** refresh. Retry does not file a second copy of a dispute that already saved.

## DisputeCreate

- **Loading:** “Saving dispute” after submit.
- **Empty:** submit stays disabled until client, location, date, and complaint text are present.
- **Success:** the dispute exists and the app opens `DisputeDetail`.
- **Error:** the dispute was not saved. The form stays filled. The message says it was not saved.
- **Offline:** the complaint text and the other fields stay. The dispute is stored locally as `pending` and opens `DisputeDetail` from that local record. It is not described as delivered to the server.
- **Permission denied:** cleaners cannot open this form.
- **Retry:** submit the same draft again. If the local draft already exists, retry updates that draft. It does not create a second dispute.

## DisputeDetail

- **Loading:** “Gathering the service record.”
- **Empty:** no matching job is on the device or the server. The screen says no service record was found for that client, location, and date. It does not infer that the work was skipped.
- **Success:** a neutral timeline with the requirement, scheduled job, worker, timestamps, evidence, exceptions, and missing items. Wording stays factual. The client’s complaint is labeled as the complaint.
- **Error:** the timeline could not be assembled. Any cached portion stays, and the rest is marked unavailable. Nothing is filled in.
- **Offline:** cached job, evidence, and exceptions are shown. Anything not on the device is labeled unavailable on this device. The complaint text still shows.
- **Permission denied:** a user who cannot see that location gets an unavailable state, not an empty timeline.
- **Retry:** reload the timeline from local data, then from the server if online. Retry does not generate a PDF.

## ReportPreview

- **Loading:** “Building the evidence report from the service record.”
- **Empty:** no report file exists yet. The next action is generate, unless the Free entitlement blocks it.
- **Success:** the report is shown only after a real file exists in storage. The screen offers share or download of that file.
- **Error:** generation failed. No document is shown. The message says the report was not created.
- **Offline:** generation is disabled. A report file already on the device can still be opened. The screen says a connection is required to create a new report.
- **Permission denied:** Free entitlement does not generate. The action opens `Paywall`. A cleaner cannot open this screen.
- **Retry:** generate again for the same dispute. Retry replaces a failed attempt. It does not create a second dispute, and it does not show a report that was not stored.

## Today

- **Loading:** placeholders for the assigned job.
- **Empty:** “No job assigned today.” No fake tasks.
- **Success:** the assigned job, its location, and its coverage so far. The next action opens `Job`.
- **Error:** the assignment refresh failed. The cached job stays.
- **Offline:** the cached job stays and can be started. The banner says changes will sync later.
- **Permission denied:** an owner opening the cleaner shell is sent to `Dashboard`. This screen is the cleaner home.
- **Retry:** refresh the assignment. The cached job stays visible during the refresh.

## Job

- **Loading:** placeholders for the requirement list.
- **Empty:** the job has no requirements. The message says no requirements are on this job. Complete stays unavailable.
- **Success:** each requirement shows satisfied, missing, or exception.
- **Error:** the requirement refresh failed. Cached requirements stay.
- **Offline:** cached requirements stay. The cleaner can still open `Task`.
- **Permission denied:** a cleaner who is not assigned to this job sees that it is unavailable and returns to `Today`.
- **Retry:** refresh requirements. Retry does not mark any requirement complete.

## Task

- **Loading:** the requirement title stays visible while its evidence loads.
- **Empty:** no photo and no exception yet. The two actions are take a photo, if the requirement needs one, and report an exception.
- **Success:** the requirement text, what evidence it needs, and the evidence or exception already saved.
- **Error:** the requirement could not be read and nothing is cached. The screen says this task is unavailable and returns to `Job`.
- **Offline:** cached requirement text and local evidence stay. Both actions still work.
- **Permission denied:** does not apply on this screen. The camera asks on `Capture`.
- **Retry:** reload the requirement from cache. Retry does not add evidence.

## Capture

The photo is saved on the device before any upload is attempted.

- **Loading:** the camera preview is starting. The shutter stays disabled until the preview is ready.
- **Empty:** the preview is ready and no photo has been taken for this visit to the screen.
- **Success:** the photo is written to app files immediately. The evidence row shows `pending`. The app returns to `Task`, where the photo counts as on-device evidence.
- **Error:** the photo could not be saved. Nothing is added to the requirement. The message says the photo was not saved, and the shutter can be used again.
- **Offline:** the shutter still works. The row stays `pending`. The message says the photo is saved on the device and will upload later.
- **Permission denied:** the preview is replaced with why the camera is required and an action that opens system settings. Location denial, if asked, is a separate note. The photo can still be taken. There is no gallery workaround that drops the link to the requirement.
- **Retry:** a failed upload retries the same file and moves the row to `retrying`, then `uploading`. Retry does not take a second photo and does not create a second evidence row. A failed save lets the cleaner take the photo again, because no row was created.

## Exception

- **Loading:** “Saving exception” after the reason is chosen.
- **Empty:** no reason is selected. Save stays disabled. Optional note and optional photo are not required.
- **Success:** the exception is stored against the requirement with the worker and the time. The app returns to `Task`. The requirement is no longer missing.
- **Error:** the exception was not saved. The selected reason and the note stay.
- **Offline:** the exception saves locally as `pending`. It still satisfies the requirement on this device.
- **Permission denied:** the optional photo uses the same camera rules as `Capture`. Declining the camera leaves the exception valid without a photo.
- **Retry:** save the same exception again. If it already saved locally, retry updates that record. It does not add a second exception for the same requirement.

## Coverage

- **Loading:** “Checking evidence” while local rows are counted.
- **Empty:** does not apply when the job has requirements. Zero satisfied requirements shows 0% and the missing names. That is a success state, not an empty screen.
- **Success:** the percent, the missing requirements, and which rows are `pending` or `failed`. A pending photo counts as evidence on the device. A failed upload is listed as not on the server.
- **Error:** the count could not be read. The screen says coverage is unavailable. It does not show 100%.
- **Offline:** the count uses local records. The banner states that server sync has not finished. Missing items stay listed.
- **Permission denied:** does not apply.
- **Retry:** recount from local records. A failed row offers upload retry, which uses the sync rules below. Recounting does not mark a missing item complete.

## Complete

- **Loading:** “Finishing service” after the cleaner confirms.
- **Empty:** does not apply. If coverage is incomplete, this screen explains what is missing and does not offer finish. The next action returns to `Coverage`.
- **Success:** every mandatory requirement has evidence or an exception. Finish records the end time locally and returns to `Today`.
- **Error:** the finish was not saved. The job stays open. The message says the service is still open.
- **Offline:** finish still saves locally as `pending`. The job is complete on the device. It is not described as synced.
- **Permission denied:** the action stays disabled when mandatory evidence is missing and no exception exists. That is a rule, not a system permission.
- **Retry:** finish once. If the local save failed, retry writes the same completion. It does not create a second job.

## Settings

- **Loading:** account and sync summary are loading. Sign out stays available.
- **Empty:** does not apply. The account is always shown for a signed-in user.
- **Success:** account, organization for an owner, sync counts, subscription entry, and sign out.
- **Error:** the remote profile failed to refresh. The cached account stays. Sync counts still reflect the local outbox.
- **Offline:** the cached account stays. The sync section lists how many items are `pending`, `failed`, and `retrying`. Sign out stays available and does not delete unsynced evidence.
- **Permission denied:** organization editing is hidden for anyone who is not the owner. Subscription management is hidden for managers and cleaners. They can still open `Paywall` only if a later prompt allows it. In this MVP, cleaners do not see purchase management.
- **Retry:** refresh the profile. Failed outbox rows can be retried from here. That retry uploads the existing item.

## Paywall

- **Loading:** “Loading plans” and, after a tap, “Processing purchase” or “Restoring purchases.”
- **Empty:** plans failed to load and none are cached. The next action is retry. The current entitlement, if known locally, stays visible.
- **Success:** Free, Pro, and Business are shown with the prices from the dependency decisions. The current plan is marked. Purchase and restore are both available.
- **Error:** the store failed. The message says the purchase did not complete. The current entitlement does not change.
- **Offline:** purchase and restore are disabled. The message says a connection is required. The last known entitlement stays.
- **Permission denied:** does not apply. A locked feature arrives here instead of failing inside the feature.
- **Retry:** load products again, or retry purchase or restore. Retry does not grant Pro or Business unless RevenueCat returns that entitlement. The demo flag may skip the gate while a demo is recorded. This screen still exists on the judge build.

## Offline synchronization

These rules apply on `Settings`, on evidence rows in `Task`, `Coverage`, and `ServiceRecord`, and on any contract or dispute marked `pending`.

- **Loading:** a row in `uploading` or `retrying` shows that label. The rest of the screen stays usable.
- **Empty:** the outbox has nothing waiting. The banner is hidden.
- **Success:** rows that reached the server show `uploaded`. Coverage and dispute timelines keep using the local copy.
- **Error:** a row shows `failed` and the action that failed, such as photo upload or dispute save. Other rows are unchanged.
- **Offline:** cached jobs, requirements, photos, and exceptions stay readable and editable. The banner says the device is offline. New photos and exceptions join the outbox as `pending`.
- **Permission denied:** does not apply to sync itself.
- **Retry:** when the network returns, `pending` and `failed` items move to `retrying` and then `uploading` without user action. The user can also retry one failed row. Retry sends the same local file or the same draft. It does not take a new photo or file a second dispute.

A pending photo counts as evidence on the device. A failed upload does not count as stored on the server. Neither state is shown as missing work, and neither is shown as a finished server record.
