package com.contractproof.core.design

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class CpButtonStyle {
    Primary,
    Secondary,
}

@Composable
fun CpButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CpButtonStyle = CpButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(CpRadius.card)
    val buttonModifier = modifier
        .fillMaxWidth()
        .height(CpTouch.buttonHeight)
    when (style) {
        CpButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
        CpButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
