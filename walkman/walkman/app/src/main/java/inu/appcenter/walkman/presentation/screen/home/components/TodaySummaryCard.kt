package inu.appcenter.walkman.presentation.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TodaySummaryCard(
    steps: Int,
    distance: Float,
    calories: Float,
    isLoading: Boolean,
    languageManager: LanguageManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_summary),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = SimpleDateFormat(
                        stringResource(R.string.date_format),
                        Locale(languageManager.getLanguageCode())
                    ).format(Calendar.getInstance().time),
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkManColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WalkManColors.Primary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = steps.toString(),
                        label = stringResource(R.string.steps),
                        color = WalkManColors.Primary
                    )

                    StatItem(
                        value = String.format("%.2f", distance),
                        label = stringResource(R.string.distance),
                        color = WalkManColors.Success
                    )

                    StatItem(
                        value = String.format("%.1f", calories),
                        label = stringResource(R.string.calories),
                        color = WalkManColors.Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val goalSteps = 10000
            val progress = (steps.toFloat() / goalSteps).coerceIn(0f, 1f)

            Text(
                text = stringResource(R.string.daily_goal, goalSteps),
                style = MaterialTheme.typography.bodySmall,
                color = WalkManColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = WalkManColors.Primary,
                trackColor = WalkManColors.Primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.achievement, (progress * 100).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = WalkManColors.Primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}