package inu.appcenter.walkman.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.walkman.domain.model.StepCountData
import inu.appcenter.walkman.presentation.theme.WalkManColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * 주간 걸음 수 차트 컴포넌트
 */
@Composable
fun WeeklyStepsChart(
    weeklyData: List<StepCountData>,
    modifier: Modifier = Modifier
) {
    if (weeklyData.isEmpty()) {
        Box(
            modifier = modifier.height(200.dp),
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

    // 일별 표시 포맷
    val dayFormat = SimpleDateFormat("E", Locale.KOREA)

    // 보여줄 최대 걸음 수 (차트 최대치)
    val maxSteps = weeklyData.maxByOrNull { it.steps }?.steps?.toFloat() ?: 10000f
    val normalizedMax = ((maxSteps / 1000).toInt() + 1) * 1000f // 1000 단위로 반올림 상향

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val barWidth = canvasWidth / (weeklyData.size * 2f)
            val spaceBetween = barWidth

            // 그리드 선 그리기
            val gridCount = 4
            val gridSpacing = canvasHeight / gridCount

            for (i in 0..gridCount) {
                val y = canvasHeight - (i * gridSpacing)

                // 수평 그리드 선
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                // 그리드 값 표시 (생략)
            }

            // 데이터 막대 그리기
            weeklyData.forEachIndexed { index, data ->
                val normalizedHeight = (data.steps / normalizedMax) * canvasHeight
                val startX = index * (barWidth + spaceBetween) + spaceBetween / 2

                // 막대 그리기
                drawRect(
                    color = WalkManColors.Primary,
                    topLeft = Offset(startX, canvasHeight - normalizedHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, normalizedHeight)
                )
            }

            // 선 그래프 그리기
            if (weeklyData.size > 1) {
                val path = Path()

                weeklyData.forEachIndexed { index, data ->
                    val normalizedHeight = (data.steps / normalizedMax) * canvasHeight
                    val x = index * (barWidth + spaceBetween) + spaceBetween / 2 + barWidth / 2
                    val y = canvasHeight - normalizedHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF8867D0), // 진한 보라색
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 요일 레이블
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weeklyData.forEach { data ->
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = data.date
                }

                Text(
                    text = dayFormat.format(calendar.time),
                    color = WalkManColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}