// SignUpScreen.kt
package inu.appcenter.walkman.presentation.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSignUpSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var emailValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var confirmPasswordValue by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // 비밀번호 확인 유효성 검사
    fun validatePasswords(): Boolean {
        return if (passwordValue != confirmPasswordValue) {
            passwordError = "비밀번호가 일치하지 않습니다"
            false
        } else {
            passwordError = null
            true
        }
    }

    // 회원가입 상태 감지
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onSignUpSuccess()
        }
    }

    // 오류 메시지 표시
    LaunchedEffect(authState.error) {
        authState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(WalkManColors.Background),
            contentAlignment = Alignment.Center
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(color = WalkManColors.Primary)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 앱 로고
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_gaitx),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 회원가입 제목
                    Text(
                        text = "회원가입",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = WalkManColors.Primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 회원가입 설명
                    Text(
                        text = "계정을 생성하여 GAITX의 모든 기능을 이용해보세요",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = WalkManColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 이메일 입력 필드
                    OutlinedTextField(
                        value = emailValue,
                        onValueChange = { emailValue = it },
                        label = { Text("이메일") },
                        placeholder = { Text("example@email.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력 필드
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { passwordValue = it },
                        label = { Text("비밀번호") },
                        placeholder = { Text("8자 이상 입력") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 확인 입력 필드
                    OutlinedTextField(
                        value = confirmPasswordValue,
                        onValueChange = {
                            confirmPasswordValue = it
                            if (passwordValue.isNotEmpty() && confirmPasswordValue.isNotEmpty()) {
                                validatePasswords()
                            }
                        },
                        label = { Text("비밀번호 확인") },
                        placeholder = { Text("비밀번호 재입력") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (validatePasswords()) {
                                    viewModel.signUpWithEmail(emailValue, passwordValue)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 회원가입 버튼
                    Button(
                        onClick = {
                            if (validatePasswords()) {
                                viewModel.signUpWithEmail(emailValue, passwordValue)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalkManColors.Primary
                        ),
                        enabled = emailValue.isNotEmpty() &&
                                passwordValue.isNotEmpty() &&
                                confirmPasswordValue.isNotEmpty() &&
                                passwordError == null
                    ) {
                        Text(
                            text = "회원가입",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 로그인으로 돌아가기
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "이미 계정이 있으신가요? 로그인하기",
                            color = WalkManColors.Primary
                        )
                    }
                }
            }
        }
    }
}