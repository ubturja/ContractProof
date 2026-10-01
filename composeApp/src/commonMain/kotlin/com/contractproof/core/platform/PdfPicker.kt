package com.contractproof.core.platform

import androidx.compose.runtime.Composable

data class PickedPdf(
    val fileName: String,
    val bytes: ByteArray,
)

@Composable
expect fun rememberPdfPickerLauncher(onPicked: (PickedPdf?) -> Unit): () -> Unit

@Composable
expect fun rememberOpenUrl(): (String) -> Unit
