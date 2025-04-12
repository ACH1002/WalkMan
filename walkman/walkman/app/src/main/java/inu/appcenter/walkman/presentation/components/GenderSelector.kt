package inu.appcenter.walkman.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun GenderSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    val options = listOf(
        stringResource(id = R.string.gender_male),
        stringResource(id = R.string.gender_female)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { gender ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = (gender == selectedGender),
                        onClick = { onGenderSelected(gender) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (gender == selectedGender),
                    onClick = { onGenderSelected(gender) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = WalkManColors.Primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = gender,
                    style = MaterialTheme.typography.bodyLarge,
                    color = WalkManColors.TextPrimary
                )
            }
        }
    }
}