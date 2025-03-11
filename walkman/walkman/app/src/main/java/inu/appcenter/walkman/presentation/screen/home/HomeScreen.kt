package inu.appcenter.walkman.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartNewRecording: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GAITX",
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 새로고침 버튼
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.loadWeeklyData()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = WalkManColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        containerColor = WalkManColors.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartNewRecording,
                containerColor = WalkManColors.Primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "새 측정 시작"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 오늘의 요약 카드
            TodaySummaryCard(
                steps = uiState.todaySteps,
                distance = uiState.todayDistance,
                calories = uiState.todayCalories,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 주간 걸음 수 차트 카드
            WeeklyStepsCard(
                weeklyData = uiState.weeklyStepData,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // MBTI 분석 요약
            MbtiAnalysisCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 새 측정 시작 버튼
            Button(
                onClick = onStartNewRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkManColors.Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsWalk,
                    contentDescription = "걷기",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "새 측정 시작하기",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TodaySummaryCard(
    steps: Int,
    distance: Float,
    calories: Float,
    isLoading: Boolean
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
                    text = "오늘의 요약",
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Calendar.getInstance().time),
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
                    // 걸음 수
                    StatItem(
                        value = steps.toString(),
                        label = "걸음",
                        color = WalkManColors.Primary
                    )

                    // 거리
                    StatItem(
                        value = String.format("%.2f", distance),
                        label = "km",
                        color = WalkManColors.Success
                    )

                    // 칼로리
                    StatItem(
                        value = String.format("%.1f", calories),
                        label = "kcal",
                        color = WalkManColors.Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 진행률 표시
            val goalSteps = 10000 // 목표 걸음 수
            val progress = (steps.toFloat() / goalSteps).coerceIn(0f, 1f)

            Text(
                text = "일일 목표: $goalSteps 걸음",
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
                text = "${(progress * 100).toInt()}% 달성",
                style = MaterialTheme.typography.bodySmall,
                color = WalkManColors.Primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun WeeklyStepsCard(
    weeklyData: List<Any>, // StepCountData 타입이지만 예시를 위해 Any로 지정
    isLoading: Boolean
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "주간 활동 통계",
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "추세",
                    tint = WalkManColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WalkManColors.Primary)
                }
            } else if (weeklyData.isEmpty()) {
                // 데이터가 없는 경우
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "아직 주간 데이터가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // 여기에 실제 주간 차트를 구현할 수 있습니다.
                // 샘플로 더미 차트 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(WalkManColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "주간 활동 차트가 여기에 표시됩니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 주간 통계 요약
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeeklyStat(
                        value = "45,328",
                        label = "총 걸음 수",
                        color = WalkManColors.Primary
                    )

                    WeeklyStat(
                        value = "32.4",
                        label = "총 거리(km)",
                        color = WalkManColors.Success
                    )

                    WeeklyStat(
                        value = "6,475",
                        label = "평균 걸음/일",
                        color = WalkManColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = WalkManColors.TextSecondary
        )
    }
}

@Composable
fun WeeklyStat(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = WalkManColors.TextSecondary
        )
    }
}

@Composable
fun MbtiAnalysisCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.Primary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "MBTI와 걸음걸이 연관성",
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "수집된 데이터를 바탕으로 MBTI 성격 유형과 걸음걸이 패턴의 연관성을 분석해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { /* MBTI 탭으로 이동 */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkManColors.Primary
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("자세히 보기")
            }
        }
    }
}