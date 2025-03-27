package inu.appcenter.walkman.presentation.screen.mypage.settings.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.NotificationViewModel
import inu.appcenter.walkman.service.StepCounterService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_notifications),
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back),
                            tint = WalkManColors.TextPrimary
                        )
                    }
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 알림 설정 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 알림 아이콘 및 제목
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isNotificationEnabled)
                                Icons.Default.NotificationsActive
                            else
                                Icons.Default.NotificationsOff,
                            contentDescription = stringResource(id = R.string.settings_notifications),
                            tint = WalkManColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(id = R.string.step_tracking_notifications),
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 알림 설명
                    Text(
                        text = stringResource(id = R.string.step_tracking_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 토글 스위치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                id = if (uiState.isNotificationEnabled)
                                    R.string.step_tracking_enabled
                                else
                                    R.string.step_tracking_disabled
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WalkManColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Switch(
                            checked = uiState.isNotificationEnabled,
                            onCheckedChange = { isChecked ->
                                // 알림 설정 변경 시 처리
                                if (isChecked) {
                                    // 알림 활성화 요청
                                    viewModel.enableNotifications(context)
                                } else {
                                    // 알림 비활성화 요청
                                    viewModel.disableNotifications(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WalkManColors.Primary,
                                checkedTrackColor = WalkManColors.Primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = WalkManColors.TextSecondary,
                                uncheckedTrackColor = WalkManColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 배터리 최적화 비활성화 안내 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.PrimaryLight.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Information",
                        tint = WalkManColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.battery_optimization_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.battery_optimization_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            // 배터리 최적화 설정으로 이동
                            viewModel.openBatteryOptimizationSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalkManColors.Primary
                        )
                    ) {
                        Text(stringResource(id = R.string.battery_optimization_button))
                    }
                }
            }
        }
    }

    // 권한 요청 다이얼로그
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = {
                Text(stringResource(id = R.string.notification_permission_title))
            },
            text = {
                Text(stringResource(id = R.string.notification_permission_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 앱 설정으로 이동
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        showPermissionDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}