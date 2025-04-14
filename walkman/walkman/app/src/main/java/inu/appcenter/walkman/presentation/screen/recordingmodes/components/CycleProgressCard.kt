package inu.appcenter.walkman.presentation.screen.recordingmodes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

@Composable
fun CycleProgressCard(
    cycleState: RecordingViewModel.CycleState,
    completedModes: Set<RecordingMode>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.Primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    id = R.string.cycle_progress,
                    cycleState.currentCycle,
                    cycleState.totalCycles
                ),
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.cycle_description),
                style = MaterialTheme.typography.bodySmall,
                color = WalkManColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 진행 상태 바
            LinearProgressIndicator(
                progress = { cycleState.completedInCurrentCycle / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = WalkManColors.Primary,
                trackColor = WalkManColors.Primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 각 모드별 완료 상태를 표시하는 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModeCompletionIndicator(
                    title = stringResource(id = R.string.recording_video_mode_short),
                    completedCount = completedModes.count { it == RecordingMode.VIDEO },
                    targetCount = cycleState.currentCycle
                )

                ModeCompletionIndicator(
                    title = stringResource(id = R.string.recording_pocket_mode_short),
                    completedCount = completedModes.count { it == RecordingMode.POCKET },
                    targetCount = cycleState.currentCycle
                )

                ModeCompletionIndicator(
                    title = stringResource(id = R.string.recording_text_mode_short),
                    completedCount = completedModes.count { it == RecordingMode.TEXT },
                    targetCount = cycleState.currentCycle
                )
            }
        }
    }
}

@Composable
fun ModeCompletionIndicator(
    title: String,
    completedCount: Int,
    targetCount: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = WalkManColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$completedCount/$targetCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (completedCount >= targetCount)
                    WalkManColors.Success
                else
                    WalkManColors.TextPrimary
            )

            if (completedCount >= targetCount) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "완료",
                    tint = WalkManColors.Success,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}