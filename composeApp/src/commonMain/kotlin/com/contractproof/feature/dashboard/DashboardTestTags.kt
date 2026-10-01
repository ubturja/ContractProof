package com.contractproof.feature.dashboard

object DashboardTestTags {
    const val screen = "dashboard_screen"
    const val hero = "dashboard_hero"
    const val refresh = "dashboard_refresh"
    const val retry = "dashboard_retry"
    const val noLocations = "dashboard_no_locations"
    const val jobsEmpty = "dashboard_jobs_empty"
    const val sectionToday = "dashboard_section_today"
    const val sectionGaps = "dashboard_section_gaps"
    const val sectionDisputes = "dashboard_section_disputes"
    const val openContracts = "dashboard_open_contracts"

    fun jobCard(jobId: String): String = "dashboard_job_$jobId"

    fun disputeRow(disputeId: String): String = "dashboard_dispute_$disputeId"
}
