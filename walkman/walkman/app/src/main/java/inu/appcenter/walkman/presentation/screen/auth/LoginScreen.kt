// presentation/screen/auth/LoginScreen.kt
package inu.appcenter.walkman.presentation.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var emailValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }

    // 이미 로그인되어 있으면 바로 다음 화면으로 이동
    LaunchedEffect(Unit) {
        if (authState.isLoggedIn) {
            // 온보딩 완료 상태에 따라 적절한 화면으로 이동
            if (viewModel.isOnboardingCompleted()) {
                // 여기서는 MainActivity의 determineStartDestination 로직과
                // 일관성을 유지해야 합니다
                onLoginSuccess()
            } else {
                onLoginSuccess() // 혹은 다른 처리
            }
        }
    }

    // 로그인 상태 변경 감지
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    // Error message display
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
            // 배경 그라디언트
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WalkManColors.Primary,
                                WalkManColors.Primary.copy(alpha = 0.7f),
                                WalkManColors.Background
                            )
                        )
                    )
            )

            if (authState.isLoading) {
                CircularProgressIndicator(color = WalkManColors.Primary)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 앱 로고
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_gaitx),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(120.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 앱 제목
                    Text(
                        text = "GAITX",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = WalkManColors.Primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 앱 설명
                    Text(
                        text = "Analyze your walking patterns and improve your gait",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = WalkManColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // 구글 로그인 버튼
                    OutlinedButton(
                        onClick = { viewModel.signInWithGoogle() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 이메일 입력 필드
                    OutlinedTextField(
                        value = emailValue,
                        onValueChange = { emailValue = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력 필드
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { passwordValue = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.signInWithEmail(emailValue, passwordValue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WalkManColors.Primary
                        )
                    ) {
                        Text(
                            text = "로그인",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 회원가입 버튼 추가
                    OutlinedButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp,
                            brush = SolidColor(WalkManColors.Primary)
                        )
                    ) {
                        Text(
                            text = "회원가입",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WalkManColors.Primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 게스트로 계속 버튼
                    TextButton(
                        onClick = { onContinueAsGuest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Continue as Guest",
                            style = MaterialTheme.typography.bodyLarge,
                            color = WalkManColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}