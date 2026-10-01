package com.contractproof.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPdfPickerLauncher(onPicked: (PickedPdf?) -> Unit): () -> Unit {
    return remember(onPicked) {
        { presentPdfPicker(onPicked) }
    }
}

@Composable
actual fun rememberOpenUrl(): (String) -> Unit {
    return remember {
        { url -> openUrl(url) }
    }
}
