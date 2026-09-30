package com.contractproof.app

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
fun PlaceholderPage(
    title: String,
    message: String,
    onBack: (() -> Unit)? = null,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
) {
    CpScreenSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            CpTitleBar(title = title, onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CpSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CpSpacing.md),
            ) {
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
                actions.forEach { (label, action) ->
                    CpButton(label = label, onClick = action)
                }
            }
        }
    }
}
