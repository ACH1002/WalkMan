package inu.appcenter.walkman.presentation.screen.result


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingResultsScreen(
    viewModel: RecordingViewModel,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "측정 완료",
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 큰 체크 아이콘
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(WalkManColors.PrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "모든 측정 완료",
                    tint = WalkManColors.Primary,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 축하 메시지
            Text(
                text = "모든 측정이 완료되었습니다!",
                style = MaterialTheme.typography.headlineSmall,
                color = WalkManColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "수집된 데이터는 보행 패턴 분석에 활용됩니다.\n참여해 주셔서 감사합니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = WalkManColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 완료된 측정 목록
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
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
                        text = "수집 완료된 데이터",
                        style = MaterialTheme.typography.titleMedium,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 완료된 모드 목록
                    CompletedModeItem(title = "영상 보면서 측정")
                    Divider(color = WalkManColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
                    CompletedModeItem(title = "주머니에 넣고 측정")
                    Divider(color = WalkManColors.Divider, modifier = Modifier.padding(vertical = 8.dp))
                    CompletedModeItem(title = "걸으면서 텍스트 입력")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 서버 업로드 정보
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = WalkManColors.Success.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = "업로드 완료",
                    tint = WalkManColors.Success,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "모든 데이터가 서버에 업로드되었습니다",
                    color = WalkManColors.Success,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 홈으로 돌아가는 버튼
            Button(
                onClick = {
                    // 상태 초기화 후 홈으로 이동
                    viewModel.resetCompletionStatus()
                    onNavigateHome()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkManColors.Primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "홈으로 돌아가기",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CompletedModeItem(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "완료",
            tint = WalkManColors.Success,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = WalkManColors.TextPrimary
        )
    }
}