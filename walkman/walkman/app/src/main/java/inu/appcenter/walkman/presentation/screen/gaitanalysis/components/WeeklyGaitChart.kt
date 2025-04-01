package inu.appcenter.walkman.presentation.screen.gaitanalysis.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.domain.model.DailyGaitData
import inu.appcenter.walkman.presentation.theme.WalkManColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 주간 보행 데이터 차트
 */
@Composable
fun WeeklyGaitChart(
    weeklyData: List<DailyGaitData>,
    modifier: Modifier = Modifier
) {
    if (weeklyData.isEmpty()) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(WalkManColors.Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "데이터가 없습니다",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextSecondary
            )
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WalkManColors.Background)
    ) {
        // 그래프 그리기
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 24f

            // 차트 영역
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)

            // x축 간격 (최대 7개 데이터)
            val xStep = chartWidth / (weeklyData.size - 1).coerceAtLeast(1)

            // y축 범위 (0-100)
            val maxScore = 100f
            val yScale = chartHeight / maxScore

            // 그리드 라인 그리기
            val gridLines = 5
            val gridStep = chartHeight / gridLines
            val gridColor = Color.LightGray.copy(alpha = 0.5f)

            for (i in 0..gridLines) {
                val y = padding + (gridStep * i)
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = 1f
                )
            }

            // 안정성 점수 선 그리기
            if (weeklyData.size > 1) {
                for (i in 0 until weeklyData.size - 1) {
                    val startX = padding + (i * xStep)
                    val startY = padding + chartHeight - (weeklyData[i].stabilityScore * yScale)
                    val endX = padding + ((i + 1) * xStep)
                    val endY = padding + chartHeight - (weeklyData[i + 1].stabilityScore * yScale)

                    drawLine(
                        color = WalkManColors.Primary,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 리듬성 점수 선 그리기
            if (weeklyData.size > 1) {
                for (i in 0 until weeklyData.size - 1) {
                    val startX = padding + (i * xStep)
                    val startY = padding + chartHeight - (weeklyData[i].rhythmScore * yScale)
                    val endX = padding + ((i + 1) * xStep)
                    val endY = padding + chartHeight - (weeklyData[i + 1].rhythmScore * yScale)

                    drawLine(
                        color = WalkManColors.Success,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 데이터 포인트 표시
            weeklyData.forEachIndexed { index, data ->
                val x = padding + (index * xStep)

                // 안정성 점수 포인트
                val stabilityY = padding + chartHeight - (data.stabilityScore * yScale)
                drawCircle(
                    color = WalkManColors.Primary,
                    radius = 6f,
                    center = Offset(x, stabilityY)
                )

                // 리듬성 점수 포인트
                val rhythmY = padding + chartHeight - (data.rhythmScore * yScale)
                drawCircle(
                    color = WalkManColors.Success,
                    radius = 6f,
                    center = Offset(x, rhythmY)
                )
            }
        }

        // x축 레이블 (요일)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklyData.forEach { data ->
                val dateFormat = SimpleDateFormat("E", Locale.getDefault())
                Text(
                    text = dateFormat.format(data.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = WalkManColors.TextSecondary
                )
            }
        }
    }
}