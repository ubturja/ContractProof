package com.contractproof.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.contractproof.app.CompanySetupState
import com.contractproof.core.design.CpButton
import com.contractproof.core.design.CpScreenSurface
import com.contractproof.core.design.CpSpacing
import com.contractproof.core.design.CpTextField
import com.contractproof.core.design.CpTitleBar

@Composable
fun CompanySetupScreen(
    state: CompanySetupState,
    onCompanyNameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = "Company")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                if (state.banner != null) {
                    Text(text = state.banner, style = MaterialTheme.typography.bodyLarge)
                }
                CpTextField(
                    value = state.companyName,
                    onValueChange = onCompanyNameChange,
                    label = "Company name",
                )
                CpTextField(
                    value = state.displayName,
                    onValueChange = onDisplayNameChange,
                    label = "Your name",
                )
                CpButton(
                    label = if (state.saving) "Saving company" else "Save company",
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                )
            }
        }
    }
}
