package com.contractproof.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.contractproof.core.design.ContractProofTheme
import com.contractproof.feature.service.CoveragePrimaryAction
import com.contractproof.feature.service.CoverageScreen
import com.contractproof.feature.service.CoverageTestTags
import com.contractproof.feature.service.CoverageUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class CoverageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCoverageSectionsAndPrimaryAction() {
        composeRule.setContent {
            ContractProofTheme {
                CoverageScreen(
                    state = CoverageUiState(
                        coveragePercent = 94,
                        summaryLine = "3 of 4 required · 1 missing",
                        missingLines = listOf("Bathroom 3 photo", "Friday deep-clean verification"),
                        primaryAction = CoveragePrimaryAction.OpenTask(
                            requirementId = "req-1",
                            label = "Take photo: Bathroom 3",
                        ),
                    ),
                    onOpenTask = {},
                    onRetryUpload = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(CoverageTestTags.percent).assertIsDisplayed()
        composeRule.onNodeWithText("3 of 4 required · 1 missing").assertIsDisplayed()
        composeRule.onNodeWithText("Missing:").assertIsDisplayed()
        composeRule.onNodeWithText("· Bathroom 3 photo").assertIsDisplayed()
        composeRule.onNodeWithTag(CoverageTestTags.primaryAction).assertIsDisplayed()
    }
}
