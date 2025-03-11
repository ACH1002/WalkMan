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
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.presentation.theme.WalkManColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartNewRecording: () -> Unit = {}
) {
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
            TodaySummaryCard()

            Spacer(modifier = Modifier.height(16.dp))

            // 최근 측정 데이터 카드
            RecentMeasurementsCard()

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
fun TodaySummaryCard() {
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
                text = "오늘의 요약",
                style = MaterialTheme.typography.titleMedium,
                color = WalkManColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 걸음 수
                StatItem(
                    value = "0",
                    label = "걸음",
                    color = WalkManColors.Primary
                )

                // 거리
                StatItem(
                    value = "0.0",
                    label = "km",
                    color = WalkManColors.Success
                )

                // 칼로리
                StatItem(
                    value = "0",
                    label = "kcal",
                    color = WalkManColors.Error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "아직 오늘의 걸음 데이터가 없습니다.\n새로운 측정을 시작해보세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
fun RecentMeasurementsCard() {
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
                    text = "최근 측정 데이터",
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

            // 최근 측정 데이터가 없는 상태
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "아직 측정 데이터가 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "3가지 측정 모드로 걸음걸이 데이터를 수집해보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
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