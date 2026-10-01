package com.contractproof.feature.service

object TodayTestTags {
    const val screen = "today_screen"
    const val empty = "today_empty"
    const val retry = "today_retry"
    const val settings = "today_settings"

    fun jobCard(jobId: String): String = "today_job_card_$jobId"

    fun openJob(jobId: String): String = "today_open_job_$jobId"
}
