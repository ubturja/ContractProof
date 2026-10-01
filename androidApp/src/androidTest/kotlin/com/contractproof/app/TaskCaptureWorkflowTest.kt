package com.contractproof.app

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.contractproof.core.design.ContractProofTheme
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpWorkStatus
import com.contractproof.core.platform.CapturedPhoto
import com.contractproof.domain.TaskNextAction
import com.contractproof.feature.service.CaptureScreen
import com.contractproof.feature.service.CaptureTestTags
import com.contractproof.feature.service.JobScreen
import com.contractproof.feature.service.JobTestTags
import com.contractproof.feature.service.JobUiState
import com.contractproof.feature.service.TaskRowUi
import com.contractproof.feature.service.TaskScreen
import com.contractproof.feature.service.TaskTestTags
import com.contractproof.feature.service.TaskUiState
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskCaptureWorkflowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captureScreenFakeFlowConfirmsPhoto() {
        var confirmed: CapturedPhoto? = null
        composeRule.setContent {
            ContractProofTheme {
                CaptureScreen(
                    requirementText = "Vacuum lobby",
                    mandatoryLabel = "Photo required · Required",
                    saving = false,
                    banner = null,
                    onPhotoConfirmed = { confirmed = it },
                    onBack = {},
                    captureContent = { onPhotoConfirmed ->
                        CpButton(
                            label = "Fake shutter",
                            onClick = {
                                onPhotoConfirmed(
                                    CapturedPhoto(
                                        localPath = "/fake/path.jpg",
                                        bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
                                    ),
                                )
                            },
                            modifier = Modifier.testTag(CaptureTestTags.shutter),
                        )
                    },
                )
            }
        }
        composeRule.onNodeWithTag(CaptureTestTags.requirement).assertIsDisplayed()
        composeRule.onNodeWithText("Vacuum lobby").assertIsDisplayed()
        composeRule.onNodeWithTag(CaptureTestTags.shutter).performClick()
        composeRule.runOnIdle {
            assert(confirmed != null)
            assert(confirmed!!.localPath == "/fake/path.jpg")
        }
    }

    @Test
    fun captureRetakeDoesNotConfirmUntilUsePhoto() {
        var confirms = 0
        composeRule.setContent {
            ContractProofTheme {
                var inReview by remember { mutableStateOf(false) }
                CaptureScreen(
                    requirementText = "Vacuum lobby",
                    mandatoryLabel = null,
                    saving = false,
                    banner = null,
                    onPhotoConfirmed = { confirms += 1 },
                    onBack = {},
                    captureContent = { onPhotoConfirmed ->
                        if (!inReview) {
                            CpButton(
                                label = "Take photo",
                                onClick = { inReview = true },
                                modifier = Modifier.testTag(CaptureTestTags.shutter),
                            )
                        } else {
                            CpButton(
                                label = "Retake",
                                onClick = { inReview = false },
                                modifier = Modifier.testTag(CaptureTestTags.retake),
                            )
                            CpButton(
                                label = "Use photo",
                                onClick = {
                                    onPhotoConfirmed(
                                        CapturedPhoto(
                                            localPath = "/fake/review.jpg",
                                            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
                                        ),
                                    )
                                },
                                modifier = Modifier.testTag(CaptureTestTags.usePhoto),
                            )
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithTag(CaptureTestTags.shutter).performClick()
        composeRule.onNodeWithTag(CaptureTestTags.retake).performClick()
        composeRule.runOnIdle { assert(confirms == 0) }
        composeRule.onNodeWithTag(CaptureTestTags.shutter).performClick()
        composeRule.onNodeWithTag(CaptureTestTags.usePhoto).performClick()
        composeRule.runOnIdle { assert(confirms == 1) }
    }

    @Test
    fun taskScreenShowsCapturedPreviewAndRetake() {
        val path = writeTinyJpeg()
        var retakes by mutableStateOf(0)
        composeRule.setContent {
            ContractProofTheme {
                TaskScreen(
                    state = TaskUiState(
                        requirementText = "Vacuum lobby",
                        evidenceHint = "Photo required",
                        mandatoryLabel = "Photo required · Required",
                        capturedLocalPath = path,
                        canRetakePhoto = true,
                        workStatus = CpWorkStatus.Pending,
                        primaryAction = TaskNextAction.MarkDone,
                    ),
                    onPrimaryAction = {},
                    onReportException = {},
                    onRetryUpload = {},
                    onRetakePhoto = { retakes += 1 },
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(TaskTestTags.capturedPreview).assertIsDisplayed()
        composeRule.onNodeWithText("Saved on this device").assertIsDisplayed()
        composeRule.onNodeWithTag(TaskTestTags.retakePhoto).performClick()
        composeRule.runOnIdle {
            assert(retakes == 1)
        }
    }

    @Test
    fun taskScreenRetryUploadInvokesCallback() {
        var retries by mutableStateOf(0)
        composeRule.setContent {
            ContractProofTheme {
                TaskScreen(
                    state = TaskUiState(
                        requirementText = "Vacuum lobby",
                        evidenceHint = "Photo required",
                        workStatus = CpWorkStatus.Failed,
                        canRetryUpload = true,
                        primaryAction = TaskNextAction.CapturePhoto,
                    ),
                    onPrimaryAction = {},
                    onReportException = {},
                    onRetryUpload = { retries += 1 },
                    onRetakePhoto = {},
                    onBack = {},
                )
            }
        }
        composeRule.onNodeWithTag(TaskTestTags.retryUpload).assertIsDisplayed()
        composeRule.onNodeWithTag(TaskTestTags.retryUpload).performClick()
        composeRule.runOnIdle {
            assert(retries == 1)
        }
    }

    @Test
    fun jobScreenShowsOfflineBannerWhenCached() {
        composeRule.setContent {
            ContractProofTheme {
                JobScreen(
                    state = JobUiState(
                        locationName = "Lobby",
                        clientName = "Acme",
                        statusLabel = "In progress",
                        coveragePercent = 50,
                        banner = "You are offline. Showing cached job details.",
                        tasks = emptyList(),
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
        composeRule.onNodeWithText("You are offline. Showing cached job details.").assertIsDisplayed()
        composeRule.onNodeWithTag(JobTestTags.screen).assertIsDisplayed()
    }

    @Test
    fun jobScreenShowsFinishBlockedForMissingMandatory() {
        composeRule.setContent {
            ContractProofTheme {
                JobScreen(
                    state = JobUiState(
                        locationName = "Lobby",
                        clientName = "Acme",
                        statusLabel = "In progress",
                        coveragePercent = 0,
                        finishMessage = "Complete mandatory tasks before finishing.",
                        tasks = listOf(
                            TaskRowUi(
                                id = "req-1",
                                text = "Vacuum lobby",
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
        composeRule.onNodeWithTag(JobTestTags.finishMessage).assertIsDisplayed()
        composeRule.onNodeWithText("Complete mandatory tasks before finishing.").assertIsDisplayed()
        composeRule.onNodeWithTag("${JobTestTags.taskCard}_req-1").assertIsDisplayed()
        composeRule.onNodeWithText("Vacuum lobby").assertIsDisplayed()
    }

    private fun writeTinyJpeg(): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "task-capture-test.jpg")
        Bitmap.createBitmap(4, 4, Bitmap.Config.RGB_565).compress(
            Bitmap.CompressFormat.JPEG,
            90,
            file.outputStream(),
        )
        return file.absolutePath
    }
}
