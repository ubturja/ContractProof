package com.contractproof.feature.service

sealed interface CoveragePrimaryAction {
    data class OpenTask(val requirementId: String, val label: String) : CoveragePrimaryAction

    data class RetryUpload(val requirementId: String, val label: String) : CoveragePrimaryAction

    data object FinishService : CoveragePrimaryAction

    data object BackToJob : CoveragePrimaryAction
}

data class CoverageUiState(
    val title: String = "Evidence Coverage",
    val coveragePercent: Int = 0,
    val summaryLine: String = "",
    val missingLines: List<String> = emptyList(),
    val exceptionLines: List<String> = emptyList(),
    val uploadPendingLines: List<String> = emptyList(),
    val uploadFailedLines: List<String> = emptyList(),
    val primaryAction: CoveragePrimaryAction = CoveragePrimaryAction.BackToJob,
    val canFinish: Boolean = false,
)
