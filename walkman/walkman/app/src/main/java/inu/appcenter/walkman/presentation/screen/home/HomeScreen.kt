package inu.appcenter.walkman.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.HomeViewModel
import inu.appcenter.walkman.util.LanguageManager
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
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }

    // 언어 코드에 따라 리컴포지션 트리거
    val languageCode by remember { mutableStateOf(languageManager.getLanguageCode()) }

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
                            contentDescription = stringResource(R.string.start_new_recording),
                            tint = WalkManColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                ),
                // 커스텀 수정자를 통해 높이 줄이기
                modifier = Modifier.height(56.dp), // 기본값은 일반적으로 56dp
                // 패딩 조정
            )
        },
        containerColor = WalkManColors.Background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TodaySummaryCard(
                steps = uiState.todaySteps,
                distance = uiState.todayDistance,
                calories = uiState.todayCalories,
                isLoading = uiState.isLoading,
                languageManager = languageManager
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeeklyStepsCard(
                weeklyData = uiState.weeklyStepData,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            MbtiAnalysisCard()

            Spacer(modifier = Modifier.height(16.dp))

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
                    contentDescription = stringResource(R.string.start_new_recording),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start_new_recording),
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

@Composable
fun WeeklyStepsCard(
    weeklyData: List<Any>,
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
                    text = stringResource(R.string.weekly_stats),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = stringResource(R.string.weekly_stats),
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_weekly_data),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(WalkManColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.weekly_chart_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeeklyStat(
                        value = "0",
                        label = stringResource(R.string.total_steps),
                        color = WalkManColors.Primary
                    )

                    WeeklyStat(
                        value = "0",
                        label = stringResource(R.string.total_distance),
                        color = WalkManColors.Success
                    )

                    WeeklyStat(
                        value = "0",
                        label = stringResource(R.string.avg_steps_per_day),
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
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.mbti_gait_correlation),
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.Primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.mbti_gait_analysis_description),
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
                Text(stringResource(R.string.view_details))
            }
        }
    }
}