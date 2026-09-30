package com.contractproof.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpLabeledProgress
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing

@Composable
fun SplashScreen() {
    CpScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CpSpacing.md),
            verticalArrangement = Arrangement.Center,
        ) {
            CpLabeledProgress(label = "Checking session")
        }
    }
}
