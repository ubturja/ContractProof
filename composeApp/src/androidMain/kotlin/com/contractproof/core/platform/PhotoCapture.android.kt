package com.contractproof.core.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.contractproof.feature.service.CaptureTestTags
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpSpacing
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
actual fun EvidencePhotoCapture(
    modifier: Modifier,
    shutterEnabled: Boolean,
    onStateChanged: (EvidencePhotoCaptureState) -> Unit,
    onPhotoConfirmed: (CapturedPhoto) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraBindGeneration by remember { mutableIntStateOf(0) }

    var uiState by remember { mutableStateOf<EvidencePhotoCaptureState>(EvidencePhotoCaptureState.Initializing) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var captureInFlight by remember { mutableStateOf(false) }
    var pendingPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }

    LaunchedEffect(uiState) {
        onStateChanged(uiState)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            uiState = EvidencePhotoCaptureState.Initializing
        } else {
            uiState = EvidencePhotoCaptureState.PermissionDenied
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    suspend fun bindCamera(preview: PreviewView): Boolean {
        uiState = EvidencePhotoCaptureState.Initializing
        return try {
            val provider = suspendCoroutine { continuation ->
                ProcessCameraProvider.getInstance(context).also { future ->
                    future.addListener(
                        { continuation.resume(future.get()) },
                        mainExecutor,
                    )
                }
            }
            provider.unbindAll()
            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = preview.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                capture,
            )
            cameraProvider = provider
            imageCapture = capture
            uiState = EvidencePhotoCaptureState.LivePreview
            true
        } catch (_: Exception) {
            uiState = EvidencePhotoCaptureState.Error(
                message = "Could not open the camera.",
                canRetry = true,
            )
            false
        }
    }

    fun retake() {
        pendingPhoto?.localPath?.let { path -> File(path).delete() }
        pendingPhoto = null
        uiState = EvidencePhotoCaptureState.LivePreview
    }

    fun takePhoto() {
        val capture = imageCapture ?: return
        if (captureInFlight || !shutterEnabled) return
        if (uiState !is EvidencePhotoCaptureState.LivePreview) return
        captureInFlight = true
        val file = File(context.cacheDir, "capture-${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    mainExecutor.execute {
                        captureInFlight = false
                        val bytes = runCatching { file.readBytes() }.getOrNull()
                        if (bytes == null || bytes.isEmpty()) {
                            file.delete()
                            uiState = EvidencePhotoCaptureState.Error(
                                message = "The photo was not saved.",
                                canRetry = true,
                            )
                            return@execute
                        }
                        val photo = CapturedPhoto(localPath = file.absolutePath, bytes = bytes)
                        pendingPhoto = photo
                        uiState = EvidencePhotoCaptureState.FrozenPreview(photo)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainExecutor.execute {
                        captureInFlight = false
                        file.delete()
                        uiState = EvidencePhotoCaptureState.Error(
                            message = "The photo was not saved.",
                            canRetry = true,
                        )
                    }
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is EvidencePhotoCaptureState.PermissionDenied -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(CpSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
                ) {
                    Text(
                        text = "Camera access is required to capture evidence for this requirement.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    CpButton(
                        label = "Open settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                    )
                    CpButton(label = "Cancel", onClick = onCancel)
                }
            }

            is EvidencePhotoCaptureState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(CpSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
                ) {
                    Text(text = state.message, style = MaterialTheme.typography.bodyLarge)
                    if (state.canRetry) {
                        CpButton(
                            label = "Retry",
                            onClick = {
                                uiState = EvidencePhotoCaptureState.Initializing
                                cameraBindGeneration++
                            },
                        )
                    }
                    CpButton(label = "Cancel", onClick = onCancel)
                }
            }

            is EvidencePhotoCaptureState.FrozenPreview -> {
                val bitmap = remember(state.photo.bytes) {
                    BitmapFactory.decodeByteArray(state.photo.bytes, 0, state.photo.bytes.size)
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured photo preview",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CpSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
                    ) {
                        CpButton(
                            label = "Use photo",
                            onClick = { onPhotoConfirmed(state.photo) },
                            enabled = shutterEnabled,
                            modifier = Modifier.testTag(CaptureTestTags.usePhoto),
                        )
                        CpButton(
                            label = "Retake",
                            onClick = { retake() },
                            enabled = !captureInFlight,
                            modifier = Modifier.testTag(CaptureTestTags.retake),
                        )
                    }
                }
            }

            EvidencePhotoCaptureState.Initializing,
            EvidencePhotoCaptureState.LivePreview,
            -> {
                if (!hasCameraPermission) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).also { view ->
                                    previewView = view
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            update = { view ->
                                previewView = view
                            },
                        )
                        LaunchedEffect(previewView, hasCameraPermission, cameraBindGeneration) {
                            val preview = previewView
                            if (preview != null && hasCameraPermission) {
                                bindCamera(preview)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(CpSpacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState is EvidencePhotoCaptureState.Initializing || captureInFlight) {
                                CircularProgressIndicator()
                            } else {
                                CpButton(
                                    label = "Take photo",
                                    onClick = { takePhoto() },
                                    enabled = shutterEnabled && !captureInFlight,
                                    modifier = Modifier.testTag(CaptureTestTags.shutter),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
