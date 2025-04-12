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
fun MBTISelector(
    selectedMBTI: String,
    onMBTISelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // MBTI 옵션
    val mbtiOptions = listOf(
        "INTJ", "INTP", "ENTJ", "ENTP",
        "INFJ", "INFP", "ENFJ", "ENFP",
        "ISTJ", "ISFJ", "ESTJ", "ESFJ",
        "ISTP", "ISFP", "ESTP", "ESFP",
        stringResource(id = R.string.prefer_not_to_say) // 선택 안함 옵션
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedMBTI,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            placeholder = { Text(stringResource(id = R.string.select_mbti)) },
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
            mbtiOptions.forEach { mbti ->
                DropdownMenuItem(
                    text = { Text(mbti) },
                    onClick = {
                        onMBTISelected(mbti)
                        expanded = false
                    }
                )
            }
        }
    }
}