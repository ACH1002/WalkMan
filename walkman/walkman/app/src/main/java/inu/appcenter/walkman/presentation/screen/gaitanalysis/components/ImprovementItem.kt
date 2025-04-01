package inu.appcenter.walkman.presentation.screen.gaitanalysis.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun ImprovementItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = WalkManColors.Primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = WalkManColors.TextPrimary
        )
    }
}