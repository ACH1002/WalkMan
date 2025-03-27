package inu.appcenter.walkman.presentation.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.presentation.screen.home.components.MbtiAnalysisCard
import inu.appcenter.walkman.presentation.screen.home.components.SocialMediaUsageCard
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.HomeViewModel
import inu.appcenter.walkman.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartNewRecording: () -> Unit = {}
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }

    val uiState by viewModel.uiState.collectAsState()
    val appUsageData by viewModel.appUsageData.collectAsState()
    val scrollState = rememberScrollState()

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
                    // 데이터 초기화 버튼
                    IconButton(onClick = {
                        viewModel.resetAppUsage()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "데이터 초기화",
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 오늘 날짜 및 총 사용 시간 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "오늘의 소셜 미디어 사용 현황",
                        style = MaterialTheme.typography.titleMedium,
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = SimpleDateFormat(
                            "yyyy년 MM월 dd일 (EEE)",
                            Locale.KOREA
                        ).format(Calendar.getInstance().time),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = WalkManColors.Primary)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 총 사용 시간 표시
                            Text(
                                text = formatDuration(uiState.totalSocialMediaUsage),
                                style = MaterialTheme.typography.headlineMedium,
                                color = WalkManColors.Primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "걷는 중 소셜 미디어 총 사용 시간",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 앱별 사용 시간 카드
            SocialMediaUsageCard(
                appUsageData = appUsageData,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            MbtiAnalysisCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 측정 시작 버튼
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
                    contentDescription = "걷기 측정 시작",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "걷기 측정 시작",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 시간 형식 변환 함수
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return when {
        hours > 0 -> "${hours}시간 ${minutes}분 ${seconds}초"
        minutes > 0 -> "${minutes}분 ${seconds}초"
        else -> "${seconds}초"
    }
}