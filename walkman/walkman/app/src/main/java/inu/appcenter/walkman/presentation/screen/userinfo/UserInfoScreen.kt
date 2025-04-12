package inu.appcenter.walkman.presentation.screen.userinfo

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.presentation.components.GenderSelector
import inu.appcenter.walkman.presentation.components.HeightSelector
import inu.appcenter.walkman.presentation.components.MBTISelector
import inu.appcenter.walkman.presentation.components.WeightSelector
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    profileViewModel: ProfileGaitViewModel,
    isEdit: Boolean,
    onNavigateNext: () -> Unit
) {
    // UI 상태 구독
    val uiState by profileViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // 로컬 상태 관리
    var name by rememberSaveable { mutableStateOf(uiState.name) }
    var gender by rememberSaveable { mutableStateOf(uiState.gender) }
    var height by rememberSaveable { mutableStateOf(uiState.height) }
    var weight by rememberSaveable { mutableStateOf(uiState.weight) }
    var mbti by rememberSaveable { mutableStateOf(uiState.mbti) }
    var isLoading by remember { mutableStateOf(false) }

    // 수정 모드일 경우 기존 정보 불러오기
    LaunchedEffect(Unit) {
        if (isEdit && uiState.selectedProfile != null) {
            name = uiState.selectedProfile?.name ?: ""
            gender = uiState.selectedProfile?.gender ?: ""
            height = uiState.selectedProfile?.height ?: ""
            weight = uiState.selectedProfile?.weight ?: ""
            mbti = uiState.selectedProfile?.mbti ?: ""
        }
    }

    // 필요한 정보가 모두 채워졌는지 확인
    val isComplete = name.isNotBlank() && gender.isNotBlank() &&
            height.isNotBlank() && weight.isNotBlank()

    // 에러 메시지 표시
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error!!)
            profileViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEdit) {
                            stringResource(id = R.string.edit_profile)
                        } else {
                            stringResource(id = R.string.create_profile)
                        },
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isEdit) {
                        IconButton(onClick = { onNavigateNext() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_back),
                                tint = WalkManColors.Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(WalkManColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isEdit) {
                    Text(
                        text = stringResource(id = R.string.user_info_intro),
                        fontSize = 16.sp,
                        color = WalkManColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                // 이름
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 성별 선택기
                Text(
                    text = stringResource(id = R.string.gender_label),
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                GenderSelector(
                    selectedGender = gender,
                    onGenderSelected = { gender = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 신체 정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 키 선택기
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.height_label),
                            color = WalkManColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        HeightSelector(
                            selectedHeight = height,
                            onHeightSelected = { height = it }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 몸무게 선택기
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.weight_label),
                            color = WalkManColors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        WeightSelector(
                            selectedWeight = weight,
                            onWeightSelected = { weight = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // MBTI 선택기 (선택 사항)
                Text(
                    text = stringResource(id = R.string.mbti_label_optional),
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                MBTISelector(
                    selectedMBTI = mbti,
                    onMBTISelected = { mbti = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 완료 버튼
                Button(
                    onClick = {
                        isLoading = true
                        if (isEdit && uiState.selectedProfile != null) {
                            // 프로필 업데이트
                            val updatedProfile = UserProfile(
                                id = uiState.selectedProfile?.id,
                                accountId = uiState.selectedProfile?.accountId,
                                name = name,
                                gender = gender,
                                height = height,
                                weight = weight,
                                mbti = mbti,
                                createdAt = uiState.selectedProfile?.createdAt,
                                updatedAt = null // 자동 업데이트
                            )
                            profileViewModel.updateUserProfile(updatedProfile)
                        } else {
                            // 새 프로필 생성
                            profileViewModel.createUserProfile(name, gender, height, weight, mbti)
                        }
                        // 저장 완료 후 이동
                        onNavigateNext()
                    },
                    enabled = isComplete && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalkManColors.Primary,
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = if (isEdit) {
                                stringResource(id = R.string.save_changes)
                            } else {
                                stringResource(id = R.string.create_profile)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 로딩 오버레이
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WalkManColors.Primary)
                }
            }
        }
    }
}