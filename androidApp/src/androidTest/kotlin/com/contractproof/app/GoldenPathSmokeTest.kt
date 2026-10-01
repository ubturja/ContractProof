package com.contractproof.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.contractproof.core.design.ContractProofTheme
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.domain.TaskNextAction
import com.contractproof.feature.service.JobScreen
import com.contractproof.feature.service.JobTestTags
import com.contractproof.feature.service.JobUiState
import com.contractproof.feature.service.TaskRowUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoldenPathSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cleanerJobScreenGoldenPathTagsVisible() {
        composeRule.setContent {
            ContractProofTheme {
                JobScreen(
                    state = JobUiState(
                        locationName = "Meridian Lobby",
                        clientName = "Meridian Office Tower",
                        statusLabel = "In progress",
                        coveragePercent = 0,
                        tasks = listOf(
                            TaskRowUi(
                                id = "req-1",
                                text = "Vacuum main walkway",
                                isMandatory = true,
                                workStatus = CpWorkStatus.Missing,
                                evidenceHint = "Photo required",
                                nextAction = TaskNextAction.CapturePhoto,
                            ),
                        ),
                        canFinish = false,
                    ),
                    onStartService = {},
                    onOpenTask = {},
                    onOpenCoverage = {},
                    onFinishService = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(JobTestTags.screen).assertIsDisplayed()
        composeRule.onNodeWithText("Meridian Lobby").assertIsDisplayed()
        composeRule.onNodeWithTag("${JobTestTags.taskCard}_req-1").assertIsDisplayed()
    }
}
