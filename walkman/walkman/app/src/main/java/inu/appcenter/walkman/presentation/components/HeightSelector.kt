package inu.appcenter.walkman.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeightSelector(
    selectedHeight: String,
    onHeightSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 키 옵션 (140cm ~ 200cm)
    val heightOptions = (140..200).map { "$it cm" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedHeight,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            placeholder = { Text(stringResource(id = R.string.select_height)) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedBorderColor = WalkManColors.Primary,
                unfocusedBorderColor = WalkManColors.TextSecondary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            heightOptions.forEach { height ->
                DropdownMenuItem(
                    text = { Text(height) },
                    onClick = {
                        onHeightSelected(height)
                        expanded = false
                    }
                )
            }
        }
    }
}