# Row level security

This is who can read and write the nineteen tables in [schema.md](schema.md). The rules live in [supabase/migrations/20260930170000_row_level_security.sql](../../supabase/migrations/20260930170000_row_level_security.sql). The app does not enforce them. A query the database rejects is rejected.

## Identity

`auth.uid()` is the caller. The migration does not create or replace Supabase's function.

`private.current_membership()` is `stable` and `security definer`. It reads `organization_members` for `auth.uid()` and returns `organization_id`, `role`, `location_ids`, and `client_id`. Policies use that row. A client cannot pass a different organization id or role.

`private.can_claim_organization(target)` is the first-owner check. It is `security definer` because a user with no membership cannot see an existing owner. It is true only when that organization was created by `auth.uid()` and does not already have an owner. `private.can_create_organization(target)` is the same kind of check for the organization insert: the caller has no membership yet and has not already created an organization. A policy cannot query its own table, so these checks stay in `security definer` functions.

`authenticated` can execute those two functions. PostgreSQL also requires `authenticated` to execute the trigger functions, because it checks that privilege before a trigger runs. Those functions return `trigger` and cannot be called as queries. `private.member_organization(uuid)` and `private.current_actor()` stay with the table owner. `anon` has no grants and no policies.

## Grants

`authenticated` can select, insert, and update the business tables. `authenticated` cannot insert, update, or delete `subscriptions` or `audit_logs`. `authenticated` cannot delete `evidence_records`, `evidence_files`, `exceptions`, `dispute_items`, or `reports`. There is no delete grant on the other business tables either. Leaving the product is a status change.

The free-subscription seed, the audit insert, and the other integrity triggers are `security definer` with a fixed `search_path` and `row_security` off. They still see the rows they compare after policies are on. The service role bypasses row-level security and writes subscription changes from the webhook.

## Who can access a row

An empty `location_ids` array matches no location. A manager still reads organization-level rows that have no location, such as clients. Versions, requirements, job requirements, evidence, exceptions, dispute items, and reports follow the parent row, so a manager does not read another location's contract or dispute by that path.

- **Owner.** Read and write the organization's clients, locations, contracts, versions, requirements, schedules, jobs, and disputes, including dispute items and reports. Read subscriptions and audit logs. Update other members in the organization. The caller's own `role` cannot change, because an update policy does not include `user_id = auth.uid()`.
- **Manager.** Read rows whose location is in `location_ids`, plus clients and other members in the organization. No writes to contracts, disputes, or subscriptions. No job, evidence, or exception writes.
- **Cleaner.** Read and update jobs assigned to them, plus those jobs' requirements, evidence, and exceptions. Insert evidence and exceptions only on those jobs, and only when `captured_by` or `recorded_by` is `auth.uid()`. An update cannot move the job to another assignee or organization. No contracts, disputes, subscriptions, or audit logs.
- **Client.** Read only their own `users` and `organization_members` rows. No writes.
- **First organization.** A user with no membership may insert one organization whose `created_by` is `auth.uid()`, then one `owner` membership for themselves. They cannot insert that membership when the organization already has an owner, and they cannot insert a second organization.

Row-level security is enabled and forced on all nineteen tables. The table owner is subject to it unless the role is a superuser or has `bypassrls`.
