package inu.appcenter.walkman.presentation.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onStartNewRecording: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()


    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 현재 날짜 표시 카드
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors = CardDefaults.cardColors(
//                containerColor = WalkManColors.CardBackground
//            ),
//            shape = RoundedCornerShape(12.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp).fillMaxWidth(),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = stringResource(R.string.app_name),
//                    style = MaterialTheme.typography.headlineSmall,
//                    color = WalkManColors.Primary,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text = SimpleDateFormat(
//                        "yyyy년 MM월 dd일 (EEE)",
//                        Locale.getDefault()
//                    ).format(Calendar.getInstance().time),
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = WalkManColors.TextSecondary
//                )
//            }
//        }

//        Spacer(modifier = Modifier.height(24.dp))
        Spacer(Modifier.height(16.dp))
        // 걷기 중 소셜 미디어 사용 경고 기능 설명 카드
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
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = WalkManColors.Primary,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.walking_detection_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.Primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.walking_detection_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 기능 상태 표시 및 설정 페이지 이동 버튼
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (uiState.isTrackingEnabled)
                                        WalkManColors.Success
                                    else
                                        WalkManColors.Error,
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(
                                    if (uiState.isTrackingEnabled)
                                        R.string.tracking_enabled
                                    else
                                        R.string.tracking_disabled
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.detailed_settings_guide),
                                style = MaterialTheme.typography.bodySmall,
                                color = WalkManColors.TextSecondary
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onNavigateToNotificationSettings,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WalkManColors.Primary
                    ),
                    border = BorderStroke(1.dp, WalkManColors.Primary)
                ) {
                    Text(stringResource(R.string.settings_button))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 제한된 소셜 미디어 앱 설명 카드
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
                    text = stringResource(R.string.monitored_apps_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.Primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.monitored_apps_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 소셜 미디어 앱 리스트
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    listOf(
                        "YouTube",
                        "Instagram",
                        "Facebook",
                        "Twitter/X",
                        "TikTok",
                        "Snapchat"
                    ).forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = WalkManColors.Success,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = app,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 데이터 측정 시작 버튼 - 복원
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
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.start_new_recording),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}