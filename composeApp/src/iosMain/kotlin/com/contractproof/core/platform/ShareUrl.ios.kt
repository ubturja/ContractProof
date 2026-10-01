package com.contractproof.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberShareUrl(): (url: String, title: String) -> Unit {
    return remember {
        { url, title -> shareUrl(url, title) }
    }
}
