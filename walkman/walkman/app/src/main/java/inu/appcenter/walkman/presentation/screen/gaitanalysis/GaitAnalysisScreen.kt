package inu.appcenter.walkman.presentation.screen.gaitanalysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.GaitAnalysisViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaitAnalysisScreen(
    gaitAnalysisViewModel: GaitAnalysisViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 모의 데이터
    val stabilityScore = 78
    val rhythmScore = 85
    val overallScore = (stabilityScore + rhythmScore) / 2

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
                    IconButton(onClick = { /* 안내 다이얼로그 표시 */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "정보",
                            tint = WalkManColors.Primary
                        )
                    }
                    IconButton(onClick = { /* 더 많은 옵션 표시 */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더 보기",
                            tint = WalkManColors.Primary
                        )
                    }
                }
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
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
                        ).format(Calendar.getInstance().time),
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
                description = "보행 중 흔들림이 적고 균형감이 우수합니다.",
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
                description = "일정한 보폭과 속도로 걷는 능력이 우수합니다.",
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

                    // 주간 그래프 영역
                    // 여기에 실제 LineChart 구현 예정
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WalkManColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "여기에 주간 보행 점수 추이 그래프가 표시됩니다.\n" +
                                    "Line Chart를 사용하여 안정성과 리듬성 점수의\n" +
                                    "일주일 동안의 변화를 시각화합니다.",
                            textAlign = TextAlign.Center,
                            color = WalkManColors.TextPrimary
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

            // 분석 데이터 카드 (히스토그램)
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
                        text = "보행 패턴 분석",
                        style = MaterialTheme.typography.titleMedium,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 히스토그램 영역
                    // 여기에 실제 BarChart 구현 예정
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(WalkManColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "여기에 보행 패턴 분석 히스토그램이 표시됩니다.\n" +
                                    "Bar Chart를 사용하여 다양한 보행 특성\n" +
                                    "(보폭, 속도, 흔들림 등)을 시각화합니다.",
                            textAlign = TextAlign.Center,
                            color = WalkManColors.TextPrimary
                        )
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

                    // 개선 제안 목록
                    ImprovementItem(
                        title = "보행 안정성 향상",
                        description = "발 뒤꿈치부터 지면에 닿도록 하고, 상체를 똑바로 유지하세요."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ImprovementItem(
                        title = "보행 리듬 개선",
                        description = "일정한 속도로 걷기 위해 메트로놈 앱을 사용해보세요."
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ImprovementItem(
                        title = "데이터 수집 빈도 증가",
                        description = "더 정확한 분석을 위해 주 3회 이상 걷기 데이터를 수집하세요."
                    )
                }
            }
        }
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