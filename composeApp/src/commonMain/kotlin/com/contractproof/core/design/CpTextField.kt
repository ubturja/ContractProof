package com.contractproof.core.design

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun CpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    singleLine: Boolean = true,
    concealed: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        visualTransformation = if (concealed) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error)
            }
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(CpRadius.card),
    )
}
