package inu.appcenter.walkman.presentation.screen.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.domain.model.AppUsageData
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun AppUsageGraph(
    appUsageData: List<AppUsageData>,
    modifier: Modifier = Modifier
) {
    if (appUsageData.isEmpty()) return

    // 총 사용 시간
    val totalUsageTime = appUsageData.sumOf { it.usageDurationMs }.toFloat()

    // 사용 시간 순으로 정렬
    val sortedData = appUsageData.sortedByDescending { it.usageDurationMs }

    // 앱 색상 맵
    val appColors = sortedData.mapIndexed { index, appData ->
        appData.appName to getAppColorForPie(appData.appName, index)
    }.toMap()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 원 그래프 그리기
            var startAngle = 0f

            sortedData.forEach { appData ->
                val sweepAngle = (appData.usageDurationMs / totalUsageTime) * 360f
                val color = appColors[appData.appName] ?: WalkManColors.Primary

                // 원 조각 그리기
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                // 테두리 그리기
                drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2f)
                )

                startAngle += sweepAngle
            }

            // 내부 원 (도넛 모양을 만들기 위해)
            drawCircle(
                color = WalkManColors.CardBackground,
                radius = radius * 0.5f,
                center = center
            )
        }
    }
}

// 앱별 색상 가져오기
private fun getAppColorForPie(appName: String, index: Int): Color {
    // 기본 색상 배열
    val colors = listOf(
        Color(0xFFFF0000), // 빨간색 (YouTube)
        Color(0xFF1877F2), // 파란색 (Facebook)
        Color(0xFFE1306C), // 핑크색 (Instagram)
        Color(0xFF1DA1F2), // 하늘색 (Twitter/X)
        Color(0xFF000000), // 검은색 (TikTok)
        Color(0xFF8A2BE2), // 보라색 (Threads)
        Color(0xFF20B038), // 녹색
        Color(0xFFFFA500)  // 주황색
    )

    // 앱 이름에 따라 색상 지정
    return when {
        appName.contains("YouTube", ignoreCase = true) -> colors[0]
        appName.contains("Facebook", ignoreCase = true) -> colors[1]
        appName.contains("Instagram", ignoreCase = true) -> colors[2]
        appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> colors[3]
        appName.contains("TikTok", ignoreCase = true) -> colors[4]
        appName.contains("Threads", ignoreCase = true) -> colors[5]
        else -> colors[index % colors.size]
    }
}