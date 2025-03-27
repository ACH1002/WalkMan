package inu.appcenter.walkman.presentation.screen.setting.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun LanguageOption(
    languageCode: String,
    languageName: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelect(languageCode) }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(languageCode) },
            colors = RadioButtonDefaults.colors(
                selectedColor = WalkManColors.Primary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = languageName,
            style = MaterialTheme.typography.bodyLarge,
            color = WalkManColors.TextPrimary
        )
    }
}