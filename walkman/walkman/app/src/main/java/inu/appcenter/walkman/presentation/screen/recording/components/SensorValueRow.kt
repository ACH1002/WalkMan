package inu.appcenter.walkman.presentation.screen.recording.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.presentation.theme.WalkManColors

// 센서 데이터 고정 너비 표시를 위한 컴포저블
@Composable
fun SensorValueRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = WalkManColors.TextSecondary, modifier = Modifier.width(24.dp))

        // 고정된 너비로 값 표시
        Text(
            text = value,
            color = WalkManColors.TextSecondary,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )

        Text(" $unit", color = WalkManColors.TextSecondary)
    }
}