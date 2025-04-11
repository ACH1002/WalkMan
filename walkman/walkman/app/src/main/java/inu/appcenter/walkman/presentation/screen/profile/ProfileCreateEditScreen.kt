package inu.appcenter.walkman.presentation.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.data.model.UserProfile
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCreateEditScreen(
    onNavigateBack: () -> Unit,
    profileToEdit: UserProfile? = null,
    viewModel: ProfileGaitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Form state
    var name by remember { mutableStateOf(profileToEdit?.name ?: "") }
    var gender by remember { mutableStateOf(profileToEdit?.gender ?: "") }
    var height by remember { mutableStateOf(profileToEdit?.height ?: "") }
    var weight by remember { mutableStateOf(profileToEdit?.weight ?: "") }
    var mbti by remember { mutableStateOf(profileToEdit?.mbti ?: "") }

    // Dropdown state for MBTI
    var mbtiDropdownExpanded by remember { mutableStateOf(false) }

    // MBTI options
    val mbtiTypes = listOf(
        "ISTJ", "ISFJ", "INFJ", "INTJ",
        "ISTP", "ISFP", "INFP", "INTP",
        "ESTP", "ESFP", "ENFP", "ENTP",
        "ESTJ", "ESFJ", "ENFJ", "ENTJ"
    )

    // Gender options
    var genderDropdownExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("남성", "여성")

    // Form validation
    val isFormValid = name.isNotBlank() && gender.isNotBlank() &&
            height.isNotBlank() && weight.isNotBlank()

    // Show error in snackbar if present
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    // Handle form submission
    val handleSubmit = {
        if (isFormValid) {
            keyboardController?.hide()

            if (profileToEdit != null) {
                // Update existing profile
                val updatedProfile = profileToEdit.copy(
                    name = name,
                    gender = gender,
                    height = height,
                    weight = weight,
                    mbti = mbti
                )
                viewModel.updateUserProfile(updatedProfile)
            } else {
                // Create new profile
                viewModel.createUserProfile(
                    name = name,
                    gender = gender,
                    height = height,
                    weight = weight,
                    mbti = mbti
                )
            }

            // Navigate back after success
            onNavigateBack()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("모든 필수 정보를 입력해주세요")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (profileToEdit != null) "프로필 편집" else "새 프로필 추가",
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = WalkManColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 이름 입력
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름", color = WalkManColors.TextSecondary) },
                placeholder = { Text("이름을 입력하세요", color = WalkManColors.TextSecondary) },
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

            // 성별 선택 (드롭다운)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "성별",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = gender,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("성별", color = WalkManColors.TextSecondary) },
                    placeholder = { Text("성별을 선택하세요", color = WalkManColors.TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { genderDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "성별 선택",
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
                    expanded = genderDropdownExpanded,
                    onDismissRequest = { genderDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 키 입력
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("키", color = WalkManColors.TextSecondary) },
                placeholder = { Text("키를 입력하세요 (cm)", color = WalkManColors.TextSecondary) },
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
                value = weight,
                onValueChange = { weight = it },
                label = { Text("체중", color = WalkManColors.TextSecondary) },
                placeholder = { Text("체중을 입력하세요 (kg)", color = WalkManColors.TextSecondary) },
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

            // MBTI 선택
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "MBTI (선택사항)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WalkManColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = mbti,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("MBTI", color = WalkManColors.TextSecondary) },
                    placeholder = { Text("MBTI를 선택하세요", color = WalkManColors.TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { mbtiDropdownExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "MBTI 선택",
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
                                mbti = mbtiType
                                mbtiDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 저장 버튼
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("취소")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {handleSubmit()},
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WalkManColors.Primary,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = isFormValid
                ) {
                    Text(
                        text = if (profileToEdit != null) "저장" else "추가",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}