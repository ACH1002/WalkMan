package inu.appcenter.walkman.presentation.screen.gaitanalysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.data.model.GaitAnalysis
import inu.appcenter.walkman.presentation.screen.gaitanalysis.components.GaitScoreCard
import inu.appcenter.walkman.presentation.screen.gaitanalysis.components.ImprovementItem
import inu.appcenter.walkman.presentation.screen.gaitanalysis.utils.GaitAnalysisFunctions.getImprovementSuggestions
import inu.appcenter.walkman.presentation.screen.gaitanalysis.utils.GaitAnalysisFunctions.getRhythmDescription
import inu.appcenter.walkman.presentation.screen.gaitanalysis.utils.GaitAnalysisFunctions.getStabilityDescription
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedGaitAnalysisScreen(
    onNavigateToProfiles: () -> Unit,
    viewModel: ProfileGaitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showInfoDialog by remember { mutableStateOf(false) }
    var showProfileSelector by remember { mutableStateOf(false) }

    // 보행 분석 데이터
    val gaitAnalysis = uiState.latestGaitAnalysis

    // 애니메이션 상태
    val stabilityScore = gaitAnalysis?.stabilityScore ?: 0
    val rhythmScore = gaitAnalysis?.rhythmScore ?: 0
    val overallScore = gaitAnalysis?.overallScore ?: 0

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

    // 에러 스낵바 표시
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    // 프로필 및 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadUserProfiles()
    }

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

                    IconButton(onClick = { viewModel.loadUserProfiles() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = WalkManColors.Primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        // 프로필이 없는 경우
        if (uiState.userProfiles.isEmpty() && !uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = WalkManColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "등록된 프로필이 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WalkManColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "보행 분석을 위해 프로필을 추가하세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToProfiles,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalkManColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("프로필 관리")
                }
            }
            return@Scaffold
        }

        // 데이터 로딩 중 상태
        AnimatedVisibility(
            visible = uiState.isLoading || uiState.isLoadingGaitData,
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

        // 메인 콘텐츠
        AnimatedVisibility(
            visible = !uiState.isLoading && !uiState.isLoadingGaitData,
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
                // 프로필 선택 카드
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileSelector = true },
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 프로필 아이콘
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WalkManColors.Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "프로필",
                                tint = WalkManColors.Primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 선택된 프로필 정보
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.selectedProfile?.name ?: "프로필을 선택하세요",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (uiState.selectedProfile != null) {
                                val profile = uiState.selectedProfile!!
                                Text(
                                    text = "${profile.gender} | 키 ${profile.height} | 체중 ${profile.weight}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WalkManColors.TextSecondary
                                )
                            }
                        }

                        // 선택 아이콘
                        Text(
                            text = "변경",
                            color = WalkManColors.Primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 보행 분석 데이터가 없는 경우
                if (gaitAnalysis == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = WalkManColors.Primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = WalkManColors.Primary,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "보행 분석 데이터가 없습니다",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "VIDEO 모드로 걷기를 측정하면 보행 분석 점수를 확인할 수 있습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { /* 측정 화면으로 이동 */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WalkManColors.Primary
                                )
                            ) {
                                Text("측정 시작하기")
                            }
                        }
                    }
                } else {
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

                            val formattedDate = gaitAnalysis.analysisDate?.let {
                                SimpleDateFormat("yyyy년 MM월 dd일 기준", Locale.getDefault()).format(it)
                            } ?: SimpleDateFormat("yyyy년 MM월 dd일 기준", Locale.getDefault()).format(Date())

                            Text(
                                text = formattedDate,
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
                }
            }
        }
    }

    // 프로필 선택 다이얼로그
    if (showProfileSelector && uiState.userProfiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showProfileSelector = false },
            title = { Text("프로필 선택") },
            text = {
                Column {
                    Text("보행 분석을 확인할 프로필을 선택하세요.")

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        uiState.userProfiles.forEach { profile ->
                            val isSelected = profile.id == uiState.selectedProfile?.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) WalkManColors.Primary.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.selectUserProfile(profile)
                                        showProfileSelector = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) WalkManColors.Primary
                                            else WalkManColors.TextSecondary.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else WalkManColors.TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = profile.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )

                                    Text(
                                        text = "${profile.gender} | 키 ${profile.height} | 체중 ${profile.weight}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WalkManColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileSelector = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onNavigateToProfiles()
                        showProfileSelector = false
                    }
                ) {
                    Text("프로필 관리")
                }
            }
        )
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

// 간단한 변환 함수 - GaitAnalysis를 GaitScoreData로 변환
fun GaitAnalysis.toGaitScoreData(): inu.appcenter.walkman.domain.model.GaitScoreData {
    return inu.appcenter.walkman.domain.model.GaitScoreData(
        stabilityScore = stabilityScore,
        rhythmScore = rhythmScore,
        overallScore = overallScore,
        analysisDate = analysisDate ?: Date(),
        stabilityDetails = stabilityDetails,
        rhythmDetails = rhythmDetails,
        recordingMode = recordingMode?.let { inu.appcenter.walkman.domain.model.RecordingMode.valueOf(it) },
        sessionId = sessionId ?: ""
    )
}