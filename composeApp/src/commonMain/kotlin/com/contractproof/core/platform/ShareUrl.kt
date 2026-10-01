package com.contractproof.core.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberShareUrl(): (url: String, title: String) -> Unit
