package com.contractproof.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTitleBar

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "ContractProof")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.lg),
            ) {
                Text(
                    text = "Proof of service, built for the field",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "· Capture photos and exceptions against contract requirements",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "· Work offline; uploads sync when you are back online",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "· Owners see coverage and disputes in one place",
                    style = MaterialTheme.typography.bodyLarge,
                )
                CpButton(label = "Continue to sign in", onClick = onContinue)
            }
        }
    }
}
