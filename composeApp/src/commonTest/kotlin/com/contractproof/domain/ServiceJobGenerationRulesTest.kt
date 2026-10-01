package com.contractproof.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class ServiceJobGenerationRulesTest {
    private val contextV1 = ServiceJobGenerationContext(
        contractId = "c1",
        contractVersionId = "v1",
        clientId = "cl1",
        locationId = "l1",
        organizationId = "org1",
    )
    private val contextV2 = contextV1.copy(contractVersionId = "v2")

    @Test
    fun inactiveSchedulesProduceNoDrafts() {
        val drafts = ServiceJobGenerationRules.buildDrafts(
            context = contextV1,
            schedules = listOf(
                sampleSchedule(status = RequirementRules.SchedulePaused),
                sampleSchedule(status = RequirementRules.ScheduleEnded),
            ),
            requirements = listOf(sampleRequirement("v1", "r1")),
            today = LocalDate.parse("2026-10-01"),
            horizonDays = 7,
        )
        assertEquals(0, drafts.size)
    }

    @Test
    fun activeScheduleProducesDraftsWithRequirementSnapshots() {
        val drafts = ServiceJobGenerationRules.buildDrafts(
            context = contextV1,
            schedules = listOf(sampleSchedule()),
            requirements = listOf(sampleRequirement("v1", "r1")),
            today = LocalDate.parse("2026-10-01"),
            horizonDays = 3,
        )
        assertEquals(3, drafts.size)
        assertTrue(drafts.all { it.contractVersionId == "v1" })
        assertEquals("r1", drafts.first().requirements.single().contractRequirementId)
    }

    @Test
    fun contractVersionChangeUsesNewVersionInDrafts() {
        val requirementsV1 = listOf(sampleRequirement("v1", "r1"))
        val requirementsV2 = listOf(sampleRequirement("v2", "r2"))
        val firstRun = ServiceJobGenerationRules.buildDrafts(
            context = contextV1,
            schedules = listOf(sampleSchedule()),
            requirements = requirementsV1,
            today = LocalDate.parse("2026-10-01"),
            horizonDays = 2,
        )
        val secondRun = ServiceJobGenerationRules.buildDrafts(
            context = contextV2,
            schedules = listOf(sampleSchedule()),
            requirements = requirementsV2,
            today = LocalDate.parse("2026-10-01"),
            horizonDays = 2,
        )
        assertEquals("v1", firstRun.first().contractVersionId)
        assertEquals("r1", firstRun.first().requirements.single().contractRequirementId)
        assertEquals("v2", secondRun.first().contractVersionId)
        assertEquals("r2", secondRun.first().requirements.single().contractRequirementId)
    }

    @Test
    fun emptyRequirementsProduceNoDrafts() {
        val drafts = ServiceJobGenerationRules.buildDrafts(
            context = contextV1,
            schedules = listOf(sampleSchedule()),
            requirements = emptyList(),
            today = LocalDate.parse("2026-10-01"),
        )
        assertEquals(0, drafts.size)
    }

    private fun sampleSchedule(
        status: String = RequirementRules.ScheduleActive,
    ): ScheduleRecord {
        return ScheduleRecord(
            id = "s1",
            contractId = "c1",
            locationId = "l1",
            frequency = ScheduleFrequency.STORAGE_DAILY,
            visit = RequirementVisit(
                weekday = 1,
                startTime = "08:00",
                endTime = "10:00",
                timezone = "America/New_York",
                startsOn = "2026-10-01",
                endsOn = null,
                status = status,
            ),
        )
    }

    private fun sampleRequirement(versionId: String, id: String): RequirementRecord {
        return RequirementRecord(
            id = id,
            organizationId = "org1",
            contractId = "c1",
            contractVersionId = versionId,
            locationId = "l1",
            locationName = "Lobby",
            zoneCode = null,
            task = "Sweep",
            sortOrder = 0,
            requiresPhoto = true,
            isMandatory = true,
            source = RequirementRules.SourceManual,
            extractionKey = null,
            isActive = true,
            visits = emptyList(),
        )
    }
}
