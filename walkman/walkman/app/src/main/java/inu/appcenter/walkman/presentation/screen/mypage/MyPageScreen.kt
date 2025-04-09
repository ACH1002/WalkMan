package inu.appcenter.walkman.presentation.screen.mypage

import android.util.Log
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.screen.mypage.components.SettingItem
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import inu.appcenter.walkman.presentation.viewmodel.UserInfoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    navController: NavController,
    userInfoViewModel: UserInfoViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onLogOut : () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()

    val uiState by userInfoViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(
        key1 = true
    ) {
        Log.d("isUserLoggedInInMyPageScreen", authState.toString())
        authViewModel.isUserLoggedIn()
    }

    // 로그아웃 완료 후 이동
    LaunchedEffect(authState.logoutCompleted) {
        if (authState.logoutCompleted && !authState.isLoggedIn) {
            Log.d("MyPageScreen", "isLoggedIn : ${authState.isLoggedIn}, logoutCompleted : ${authState.logoutCompleted}")

            // 로그아웃 상태를 네비게이션 인자로 전달
            onLogOut()
            authViewModel.resetLogoutCompleted()
        }
    }

    val onLogoutClick = {
        authViewModel.signOut()
    }

    val genderText = when(uiState.gender) {
        stringResource(id = R.string.gender_male) -> stringResource(id = R.string.gender_male)
        stringResource(id = R.string.gender_female) -> stringResource(id = R.string.gender_female)
        "남성" -> stringResource(id = R.string.gender_male)
        "여성" -> stringResource(id = R.string.gender_female)
        "Male" -> stringResource(id = R.string.gender_male)
        "Female" -> stringResource(id = R.string.gender_female)
        else -> uiState.gender
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_mypage),
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 사용자 프로필 섹션
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 프로필 아이콘
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(WalkManColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(id = R.string.profile),
                            tint = WalkManColors.Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 사용자 이름
                    Text(
                        text = uiState.name.ifEmpty { stringResource(id = R.string.default_user) },
                        style = MaterialTheme.typography.titleLarge,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 사용자 정보 요약
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.gender.isNotEmpty()) {
                            Text(
                                text = genderText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextSecondary
                            )

                            Text(
                                text = " | ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextSecondary
                            )
                        }

                        if (uiState.height.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.profile_height, uiState.height),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextSecondary
                            )

                            if (uiState.weight.isNotEmpty()) {
                                Text(
                                    text = " | ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WalkManColors.TextSecondary
                                )
                            }
                        }

                        if (uiState.weight.isNotEmpty()) {
                            Text(
                                text = stringResource(id = R.string.profile_weight, uiState.weight),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WalkManColors.TextSecondary
                            )
                        }
                    }

                    if (uiState.mbti.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.profile_mbti, uiState.mbti),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkManColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 프로필 편집 버튼
                    OutlinedButton(
                        onClick = { navController.navigate("user_info?isEdit=true") },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WalkManColors.Primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(WalkManColors.Primary)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.edit_profile),
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(text = stringResource(id = R.string.edit_profile))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 설정 섹션
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
                        text = stringResource(id = R.string.settings),
                        style = MaterialTheme.typography.titleMedium,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 언어 설정
                    SettingItem(
                        icon = Icons.Default.Language,
                        title = stringResource(id = R.string.settings_language),
                        subtitle = stringResource(id = if (isKorean()) R.string.language_ko else R.string.language_en),
                        onClick = { navController.navigate("language_settings") }
                    )

                    Divider(
                        color = WalkManColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 알림 설정
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(id = R.string.settings_notifications),
                        subtitle = stringResource(id = R.string.settings_notifications_desc),
                        onClick = { navController.navigate("notification_settings") }
                    )

                    Divider(
                        color = WalkManColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 앱 정보
                    SettingItem(
                        icon = Icons.Default.Info,
                        title = stringResource(id = R.string.settings_about),
                        subtitle = stringResource(id = R.string.settings_about_desc),
                        onClick = { /* 앱 정보 화면으로 이동 */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 데이터 관리 섹션
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
                        text = stringResource(id = R.string.data_management),
                        style = MaterialTheme.typography.titleMedium,
                        color = WalkManColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 새 측정 시작
                    SettingItem(
                        icon = Icons.Default.PlayArrow,
                        title = stringResource(id = R.string.start_new_recording),
                        subtitle = stringResource(id = R.string.start_new_recording_desc),
                        onClick = { navController.navigate("recording_modes") }
                    )

                    Divider(
                        color = WalkManColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 데이터 내보내기
                    SettingItem(
                        icon = Icons.Default.Share,
                        title = stringResource(id = R.string.export_data),
                        subtitle = stringResource(id = R.string.export_data_desc),
                        onClick = { /* 데이터 내보내기 기능 */ }
                    )
                }
                if (authState.isLoggedIn) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onLogoutClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalkManColors.Error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("로그아웃")
                    }
                }
            }
        }
    }
}



// 현재 언어가 한국어인지 확인하는 함수 (LanguageManager 대신 사용)
@Composable
private fun isKorean(): Boolean {
    return stringResource(id = R.string.language_code) == "ko"
}