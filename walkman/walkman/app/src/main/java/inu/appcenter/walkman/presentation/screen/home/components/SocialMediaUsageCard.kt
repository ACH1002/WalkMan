package inu.appcenter.walkman.presentation.screen.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Facebook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.domain.model.AppUsageData
import inu.appcenter.walkman.presentation.theme.WalkManColors
import java.util.concurrent.TimeUnit

@Composable
fun SocialMediaUsageCard(
    appUsageData: List<AppUsageData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "앱별 사용 시간",
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (appUsageData.isNotEmpty()) {
                // 앱 사용 시간 원형 그래프 추가
                AppUsageGraph(
                    appUsageData = appUsageData,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (appUsageData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "걷는 중 소셜 미디어 사용 내역이 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 사용 시간 순으로 정렬
                val sortedData = appUsageData.sortedByDescending { it.usageDurationMs }

                sortedData.forEach { appData ->
                    SocialAppUsageRow(
                        appName = appData.appName,
                        usageDuration = appData.usageDurationMs
                    )

                    if (sortedData.last() != appData) {
                        Divider(
                            color = WalkManColors.Divider,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SocialAppUsageRow(
    appName: String,
    usageDuration: Long
) {
    val icon = getAppIcon(appName)
    val color = getAppColor(appName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 앱 아이콘
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = appName,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 앱 이름
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyLarge,
            color = WalkManColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        // 사용 시간
        Text(
            text = formatDuration(usageDuration),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// 앱 아이콘 가져오기
private fun getAppIcon(appName: String): ImageVector {
    return when {
        appName.contains("YouTube", ignoreCase = true) -> Icons.Default.PlayArrow
        appName.contains("Facebook", ignoreCase = true) -> Icons.Default.Facebook
        appName.contains("Instagram", ignoreCase = true) -> Icons.Default.PlayArrow
        appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> Icons.Default.Star
        else -> Icons.Default.PlayArrow
    }
}
// 앱별 색상 가져오기
private fun getAppColor(appName: String): Color {
    return when {
        appName.contains("YouTube", ignoreCase = true) -> Color(0xFFFF0000) // 빨간색
        appName.contains("Facebook", ignoreCase = true) -> Color(0xFF1877F2) // 파란색
        appName.contains("Instagram", ignoreCase = true) -> Color(0xFFE1306C) // 핑크색
        appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> Color(0xFF1DA1F2) // 하늘색
        appName.contains("TikTok", ignoreCase = true) -> Color(0xFF000000) // 검은색
        appName.contains("Threads", ignoreCase = true) -> Color(0xFF000000) // 검은색
        else -> WalkManColors.Primary
    }
}
// 시간 형식 변환 함수
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return when {
        hours > 0 -> "${hours}시간 ${minutes}분"
        minutes > 0 -> "${minutes}분 ${seconds}초"
        else -> "${seconds}초"
    }
}