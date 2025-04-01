package inu.appcenter.walkman.presentation.screen.gaitanalysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.domain.model.DailyGaitData
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.GaitAnalysisViewModel
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaitAnalysisScreen(
    gaitAnalysisViewModel: GaitAnalysisViewModel = hiltViewModel(),
    recordingViewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by gaitAnalysisViewModel.uiState.collectAsState()
    val gaitAnalysisResult by recordingViewModel.gaitAnalysisResult.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 점수 업데이트 (실제 데이터가 있으면 그것을 사용, 없으면 ViewModel의 데이터 사용)
    val stabilityScore = gaitAnalysisResult?.stabilityScore ?: uiState.stabilityScore
    val rhythmScore = gaitAnalysisResult?.rhythmScore ?: uiState.rhythmScore
    val overallScore = gaitAnalysisResult?.overallScore ?: uiState.overallScore

    // 애니메이션 상태
    val animatedStabilityScore by animateFloatAsState(
        targetValue = stabilityScore.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "stabilityAnimation"
    )

    val animatedRhythmScore by animateFloatAsState(
        targetValue = rhythmScore.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "rhythmAnimation"
    )

    val animatedOverallScore by animateFloatAsState(
        targetValue = overallScore.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "overallAnimation"
    )

    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "보행 분석",
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                ),
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "정보",
                            tint = WalkManColors.Primary
                        )
                    }

                    IconButton(onClick = { gaitAnalysisViewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = WalkManColors.Primary
                        )
                    }
                }
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WalkManColors.Primary
                )
            }
        }

        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 종합 점수 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "종합 보행 점수",
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.Primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 원형 진행 표시줄
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 원형 프로그레스 바
                            CircularProgressIndicator(
                                progress = { animatedOverallScore },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 12.dp,
                                color = when {
                                    overallScore >= 80 -> WalkManColors.Success
                                    overallScore >= 60 -> Color(0xFFFFA500) // Orange
                                    else -> WalkManColors.Error
                                },
                                trackColor = WalkManColors.Divider
                            )

                            // 점수 텍스트
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$overallScore",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WalkManColors.TextPrimary
                                )
                                Text(
                                    text = "점",
                                    fontSize = 16.sp,
                                    color = WalkManColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 점수 등급 표시
                        val grade = when {
                            overallScore >= 90 -> "최상"
                            overallScore >= 80 -> "상"
                            overallScore >= 70 -> "중상"
                            overallScore >= 60 -> "중"
                            overallScore >= 50 -> "중하"
                            else -> "하"
                        }

                        Text(
                            text = "등급: $grade",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = WalkManColors.TextPrimary
                        )

                        Text(
                            text = SimpleDateFormat(
                                "yyyy년 MM월 dd일 기준",
                                Locale.getDefault()
                            ).format(uiState.lastUpdated ?: Calendar.getInstance().time),
                            style = MaterialTheme.typography.bodySmall,
                            color = WalkManColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 안정성 점수 카드
                GaitScoreCard(
                    title = "보행 안정성",
                    score = stabilityScore,
                    animatedScore = animatedStabilityScore,
                    description = getStabilityDescription(stabilityScore),
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.DirectionsWalk,
                            contentDescription = "보행 안정성",
                            tint = WalkManColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 리듬성 점수 카드
                GaitScoreCard(
                    title = "보행 리듬성",
                    score = rhythmScore,
                    animatedScore = animatedRhythmScore,
                    description = getRhythmDescription(rhythmScore),
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "보행 리듬성",
                            tint = WalkManColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 그래프 카드 (주별 추이)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "주간 보행 점수 추이",
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState.weeklyData.isEmpty()) {
                            // 데이터가 없는 경우
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WalkManColors.Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "아직 주간 보행 데이터가 충분하지 않습니다.\n더 많은 VIDEO 모드 측정을 완료해주세요.",
                                    textAlign = TextAlign.Center,
                                    color = WalkManColors.TextPrimary
                                )
                            }
                        } else {
                            // 주간 그래프 영역 (실제 데이터 사용)
                            WeeklyGaitChart(
                                weeklyData = uiState.weeklyData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 범례
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            LegendItem(color = WalkManColors.Primary, text = "안정성")
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendItem(color = WalkManColors.Success, text = "리듬성")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 개선 제안 카드
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "개선 제안",
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.Primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 개선 제안 목록 (점수에 따라 다른 제안)
                        val suggestions = getImprovementSuggestions(stabilityScore, rhythmScore)
                        suggestions.forEach { suggestion ->
                            ImprovementItem(
                                title = suggestion.first,
                                description = suggestion.second
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // 에러 메시지 표시
                AnimatedVisibility(visible = uiState.error != null) {
                    uiState.error?.let { errorMessage ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = WalkManColors.Error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.Error,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // 정보 다이얼로그
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("보행 분석 안내")
            },
            text = {
                Column {
                    Text(
                        "이 화면에서는 VIDEO 모드로 측정한 보행 데이터를 분석하여 점수화합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "• 안정성: 걸음의 균형과 흔들림 정도를 분석합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        "• 리듬성: 걸음 간격의 일관성과 규칙성을 분석합니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "더 정확한 분석을 위해 영상을 보면서 걷는 VIDEO 모드를 여러 번 측정해보세요.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

@Composable
fun GaitScoreCard(
    title: String,
    score: Int,
    animatedScore: Float,
    description: String,
    iconContent: @Composable () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconContent()

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${score}점",
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        score >= 80 -> WalkManColors.Success
                        score >= 60 -> Color(0xFFFFA500) // Orange
                        else -> WalkManColors.Error
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 선형 진행 표시줄
            LinearProgressIndicator(
                progress = { animatedScore },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = when {
                    score >= 80 -> WalkManColors.Success
                    score >= 60 -> Color(0xFFFFA500) // Orange
                    else -> WalkManColors.Error
                },
                trackColor = WalkManColors.Divider
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextSecondary
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = WalkManColors.TextSecondary
        )
    }
}

@Composable
fun ImprovementItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = WalkManColors.Primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = WalkManColors.TextPrimary
        )
    }
}

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

/**
 * 안정성 점수에 따른 설명 텍스트
 */
private fun getStabilityDescription(score: Int): String {
    return when {
        score >= 90 -> "보행 중 흔들림이 거의 없고 균형감이 매우 우수합니다."
        score >= 80 -> "보행 중 흔들림이 적고 균형감이 우수합니다."
        score >= 70 -> "전반적으로 안정적인 보행 패턴을 보입니다."
        score >= 60 -> "약간의 불안정함이 있으나 양호한 수준입니다."
        score >= 50 -> "보행 시 불안정함이 감지됩니다."
        else -> "보행 안정성이 부족합니다. 개선이 필요합니다."
    }
}

/**
 * 리듬성 점수에 따른 설명 텍스트
 */
private fun getRhythmDescription(score: Int): String {
    return when {
        score >= 90 -> "매우 일정한 보폭과 속도로 걷는 능력이 탁월합니다."
        score >= 80 -> "일정한 보폭과 속도로 걷는 능력이 우수합니다."
        score >= 70 -> "보행 리듬이 대체로 일정합니다."
        score >= 60 -> "약간의 불규칙함이 있으나 양호한 리듬감입니다."
        score >= 50 -> "보행 리듬에 불규칙함이 감지됩니다."
        else -> "보행 리듬이 불규칙합니다. 개선이 필요합니다."
    }
}

/**
 * 점수에 따른 개선 제안 목록
 */
private fun getImprovementSuggestions(stabilityScore: Int, rhythmScore: Int): List<Pair<String, String>> {
    val suggestions = mutableListOf<Pair<String, String>>()

    // 안정성 관련 제안
    if (stabilityScore < 70) {
        suggestions.add(
            Pair(
                "보행 안정성 향상",
                "발 뒤꿈치부터 지면에 닿도록 하고, 상체를 똑바로 유지하세요. 균형 운동을 통해 안정성을 키울 수 있습니다."
            )
        )
    }

    // 리듬성 관련 제안
    if (rhythmScore < 70) {
        suggestions.add(
            Pair(
                "보행 리듬 개선",
                "일정한 속도로 걷기 위해 메트로놈 앱을 사용해보세요. 규칙적인 걸음걸이를 연습하면 리듬감이 향상됩니다."
            )
        )
    }

    // 데이터 수집 관련 제안
    suggestions.add(
        Pair(
            "더 정확한 분석을 위해",
            "더 정확한 분석을 위해 주 3회 이상 VIDEO 모드에서 걷기 데이터를 수집하세요."
        )
    )

    return suggestions
}