package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

class EvidenceRulesTest {
    @Test
    fun photoRequiresImageFile() {
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireFileForType(EvidenceType.Photo, null)
        }
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireFileForType(
                EvidenceType.Photo,
                sampleFile(mimeType = "application/pdf"),
            )
        }
    }

    @Test
    fun checklistAndTimestampAllowMissingFile() {
        EvidenceRules.requireFileForType(EvidenceType.ChecklistCompletion, null)
        EvidenceRules.requireFileForType(EvidenceType.Timestamp, null)
    }

    @Test
    fun accuracyRequiresCoordinates() {
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireLocation(
                EvidenceLocation(
                    locationId = null,
                    coordinates = EvidenceCoordinates(1.0, 2.0, -1.0),
                ),
                sampleJob(),
            )
        }
    }

    @Test
    fun locationIdMustMatchJob() {
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireLocation(
                EvidenceLocation(locationId = "other-location", coordinates = null),
                sampleJob(),
            )
        }
    }

    private fun sampleFile(mimeType: String): EvidenceFile {
        return EvidenceFile(
            id = "file-1",
            evidenceRecordId = "ev-1",
            bucket = "evidence",
            objectPath = "org/job/req/ev",
            mimeType = mimeType,
            byteSize = 10,
            sha256 = "a".repeat(64),
            syncStatus = EvidenceSyncStatus.Pending,
            uploadedAt = null,
        )
    }

    private fun sampleJob(): ServiceJob {
        return ServiceJob(
            id = "job-1",
            organizationId = "org-1",
            client = ServiceJobClientRef("c1", "Client"),
            location = ServiceJobLocationRef("l1", "Lobby", "UTC"),
            serviceDate = kotlinx.datetime.LocalDate(2026, 10, 1),
            scheduledStart = "2026-10-01T09:00:00Z",
            scheduledEnd = "2026-10-01T10:00:00Z",
            assignee = ServiceJobAssignee("user-1", "Cleaner"),
            status = ServiceJobRules.InProgress,
            requirements = listOf(
                ServiceJobRequirement(
                    id = "req-1",
                    contractRequirementId = "cr-1",
                    requirementText = "Sweep",
                    requiresPhoto = true,
                    isMandatory = true,
                    sortOrder = 0,
                    status = JobRequirementStatus.Missing,
                ),
            ),
            evidenceStatus = ServiceJobEvidenceStatus(1, 0, 0, ServiceJobEvidenceState.NotStarted),
            contractId = "contract-1",
            contractVersionId = "version-1",
            scheduleId = null,
        )
    }
}
