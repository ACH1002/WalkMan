package inu.appcenter.walkman.presentation.screen.userinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.UserInfoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    viewModel: UserInfoViewModel,
    onNavigateNext: () -> Unit,
    isEdit: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var mbtiDropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    // stringResource 값을 먼저 변수에 저장
    val maleLabelText = stringResource(id = R.string.gender_male)
    val femaleLabelText = stringResource(id = R.string.gender_female)

    val mbtiTypes = listOf(
        "ISTJ", "ISFJ", "INFJ", "INTJ",
        "ISTP", "ISFP", "INFP", "INTP",
        "ESTP", "ESFP", "ENFP", "ENTP",
        "ESTJ", "ESFJ", "ENFJ", "ENTJ"
    )

    // 화면이 처음 표시될 때 기존 데이터 로드를 확인
    LaunchedEffect(Unit) {
        // viewModel에서 데이터가 이미 로드되어 있는지 확인
        // 필요한 경우 여기에 추가 로직을 넣을 수 있습니다
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = if (isEdit) R.string.edit_profile else R.string.title_user_info),
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isEdit) {
                        IconButton(onClick = onNavigateNext) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_back),
                                tint = WalkManColors.TextPrimary
                            )
                        }
                    } else null
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = WalkManColors.Background,
        // 키보드 영역을 고려한 insets 설정
        contentWindowInsets = WindowInsets.ime.union(WindowInsets(0, 0, 0, 0))
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .imePadding(), // 키보드 패딩 추가
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 온보딩 진행 상태 표시 (초기 설정시에만 표시)
            if (!isEdit) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(WalkManColors.Primary, shape = RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color.LightGray, shape = RoundedCornerShape(6.dp))
                    )
                }
            }

            if (!isEdit) {
                Text(
                    text = stringResource(id = R.string.title_user_info),
                    style = MaterialTheme.typography.headlineSmall,
                    color = WalkManColors.Primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }

            // 이름 입력
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(stringResource(id = R.string.label_name), color = WalkManColors.TextSecondary) },
                placeholder = { Text(stringResource(id = R.string.placeholder_name), color = WalkManColors.TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedTextColor = WalkManColors.TextPrimary,
                    focusedTextColor = WalkManColors.TextPrimary,
                    cursorColor = WalkManColors.Primary,
                    focusedBorderColor = WalkManColors.Primary,
                    unfocusedBorderColor = WalkManColors.TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // 성별 선택
            Text(
                text = stringResource(id = R.string.label_gender),
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextPrimary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.updateGender(maleLabelText) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.gender == maleLabelText) WalkManColors.Primary else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        maleLabelText,
                        color = if (uiState.gender == maleLabelText) Color.White else WalkManColors.TextPrimary
                    )
                }

                Button(
                    onClick = { viewModel.updateGender(femaleLabelText) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.gender == femaleLabelText) WalkManColors.Primary else Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        femaleLabelText,
                        color = if (uiState.gender == femaleLabelText) Color.White else WalkManColors.TextPrimary
                    )
                }
            }

            // MBTI 선택
            Text(
                text = stringResource(id = R.string.label_mbti),
                style = MaterialTheme.typography.bodyMedium,
                color = WalkManColors.TextPrimary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.mbti,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.label_mbti), color = WalkManColors.TextSecondary) },
                    placeholder = { Text(stringResource(id = R.string.placeholder_mbti), color = WalkManColors.TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { mbtiDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(id = R.string.select_mbti),
                                tint = WalkManColors.TextSecondary
                            )
                        }
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        unfocusedTextColor = WalkManColors.TextPrimary,
                        focusedTextColor = WalkManColors.TextPrimary,
                        cursorColor = WalkManColors.Primary,
                        focusedBorderColor = WalkManColors.Primary,
                        unfocusedBorderColor = WalkManColors.TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = mbtiDropdownExpanded,
                    onDismissRequest = { mbtiDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    mbtiTypes.forEach { mbtiType ->
                        DropdownMenuItem(
                            text = { Text(mbtiType) },
                            onClick = {
                                viewModel.updateMbti(mbtiType)
                                mbtiDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 키 입력
            OutlinedTextField(
                value = uiState.height,
                onValueChange = { viewModel.updateHeight(it) },
                label = { Text(stringResource(id = R.string.label_height), color = WalkManColors.TextSecondary) },
                placeholder = { Text(stringResource(id = R.string.placeholder_height), color = WalkManColors.TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedTextColor = WalkManColors.TextPrimary,
                    focusedTextColor = WalkManColors.TextPrimary,
                    cursorColor = WalkManColors.Primary,
                    focusedBorderColor = WalkManColors.Primary,
                    unfocusedBorderColor = WalkManColors.TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // 체중 입력
            OutlinedTextField(
                value = uiState.weight,
                onValueChange = { viewModel.updateWeight(it) },
                label = { Text(stringResource(id = R.string.label_weight), color = WalkManColors.TextSecondary) },
                placeholder = { Text(stringResource(id = R.string.placeholder_weight), color = WalkManColors.TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedTextColor = WalkManColors.TextPrimary,
                    focusedTextColor = WalkManColors.TextPrimary,
                    cursorColor = WalkManColors.Primary,
                    focusedBorderColor = WalkManColors.Primary,
                    unfocusedBorderColor = WalkManColors.TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
            )

            // 저장 버튼 텍스트를 변수에 저장
            val buttonText = stringResource(id = if (isEdit) R.string.btn_save else R.string.btn_next)

            Button(
                onClick = {
                    viewModel.saveUserInfo()
                    onNavigateNext()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkManColors.Primary,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState.isComplete
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}