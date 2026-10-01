package com.contractproof.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.contractproof.core.design.ContractProofTheme
import com.contractproof.feature.service.TodayJobCardUi
import com.contractproof.feature.service.TodayScreen
import com.contractproof.feature.service.TodayTestTags
import com.contractproof.feature.service.TodayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsJobDetails() {
        composeRule.setContent {
            ContractProofTheme {
                TodayScreen(
                    state = TodayUiState(
                        organizationName = "Northside",
                        jobs = listOf(
                            TodayJobCardUi(
                                jobId = "job-1",
                                locationName = "Lobby",
                                clientName = "Acme",
                                serviceTimeLabel = "9:00 AM – 10:00 AM",
                                statusLabel = "Scheduled",
                                coveragePercent = 75,
                            ),
                        ),
                    ),
                    onOpenJob = {},
                    onOpenSettings = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("Lobby").assertIsDisplayed()
        composeRule.onNodeWithText("9:00 AM – 10:00 AM").assertIsDisplayed()
        composeRule.onNodeWithText("Scheduled").assertIsDisplayed()
        composeRule.onNodeWithText("Evidence coverage 75%").assertIsDisplayed()
    }

    @Test
    fun showsEmptyState() {
        composeRule.setContent {
            ContractProofTheme {
                TodayScreen(
                    state = TodayUiState(loading = false),
                    onOpenJob = {},
                    onOpenSettings = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithTag(TodayTestTags.empty).assertIsDisplayed()
    }

    @Test
    fun openJobInvokesCallback() {
        var opens by mutableIntStateOf(0)
        composeRule.setContent {
            ContractProofTheme {
                TodayScreen(
                    state = TodayUiState(
                        jobs = listOf(
                            TodayJobCardUi(
                                jobId = "job-42",
                                locationName = "Lobby",
                                clientName = "Acme",
                                serviceTimeLabel = "9:00 AM – 10:00 AM",
                                statusLabel = "In progress",
                                coveragePercent = 0,
                            ),
                        ),
                    ),
                    onOpenJob = { opens += 1 },
                    onOpenSettings = {},
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithTag(TodayTestTags.openJob("job-42")).performClick()
        composeRule.runOnIdle {
            assert(opens == 1)
        }
    }
}
