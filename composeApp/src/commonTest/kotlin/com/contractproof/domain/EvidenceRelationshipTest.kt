package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EvidenceRelationshipTest {
    @Test
    fun alignedEvidencePassesRelationshipChecks() {
        val job = sampleJob()
        val evidence = sampleEvidence(job)
        EvidenceRules.requireReadyToPersist(evidence, job)
        assertTrue(EvidenceRules.isSameRequirement(evidence, job.requirements.first().id))
    }

    @Test
    fun wrongOrganizationFails() {
        val job = sampleJob()
        val evidence = sampleEvidence(job).copy(organizationId = "other-org")
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireConsistentKeys(evidence, job)
        }
    }

    @Test
    fun unknownRequirementFails() {
        val job = sampleJob()
        val evidence = sampleEvidence(job).copy(serviceJobRequirementId = "missing-req")
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireConsistentKeys(evidence, job)
        }
    }

    @Test
    fun fileMustBelongToRecord() {
        val job = sampleJob()
        val evidence = sampleEvidence(job)
        val mismatched = evidence.copy(
            file = evidence.file?.copy(evidenceRecordId = "other-record"),
        )
        assertFailsWith<EvidenceRuleViolation> {
            EvidenceRules.requireReadyToPersist(mismatched, job)
        }
    }

    @Test
    fun coordinatesOnlyLocationIsValid() {
        val job = sampleJob()
        val evidence = sampleEvidence(job).copy(
            location = EvidenceLocation(
                locationId = null,
                coordinates = EvidenceCoordinates(37.0, -122.0, 5.0),
            ),
        )
        EvidenceRules.requireReadyToPersist(evidence, job)
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

    private fun sampleEvidence(job: ServiceJob): Evidence {
        val recordId = "ev-1"
        return Evidence(
            id = recordId,
            organizationId = job.organizationId,
            serviceJobId = job.id,
            serviceJobRequirementId = job.requirements.first().id,
            capturedByUserId = "user-1",
            capturedAt = "2026-10-01T09:30:00Z",
            type = EvidenceType.Photo,
            location = EvidenceLocation(locationId = job.location.id, coordinates = null),
            file = EvidenceFile(
                id = "file-1",
                evidenceRecordId = recordId,
                bucket = "evidence",
                objectPath = "org/job/req/ev",
                mimeType = "image/jpeg",
                byteSize = 100,
                sha256 = "a".repeat(64),
                syncStatus = EvidenceSyncStatus.Pending,
                uploadedAt = null,
            ),
            syncStatus = EvidenceSyncStatus.Pending,
        )
    }
}
