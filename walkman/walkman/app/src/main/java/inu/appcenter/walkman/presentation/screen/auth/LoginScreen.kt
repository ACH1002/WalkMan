// presentation/screen/auth/LoginScreen.kt
package inu.appcenter.walkman.presentation.screen.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    fromLogout: Boolean = false,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var emailValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }

    var hasAttemptedLogin by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var isFromLogout by remember { mutableStateOf(fromLogout || viewModel.wasJustLoggedOut()) }

    LaunchedEffect(key1 = true) {
        Log.d("LoginScreen", "초기화 - fromLogout: $fromLogout, wasJustLoggedOut: ${viewModel.wasJustLoggedOut()}")

        // 로그인 상태 확인 (항상 호출)
        viewModel.isUserLoggedIn()

        // 세션 매니저의 로그아웃 플래그 초기화
        if (isFromLogout) {
            viewModel.clearLogoutFlag()
        }
    }

    LaunchedEffect(authState.isLoggedIn) {
        Log.d("LoginScreen", "상태 변경 - isLoggedIn: ${authState.isLoggedIn}, isFromLogout: $isFromLogout")

        // 로그아웃 직후가 아닐 때만 자동 로그인 처리
        if (authState.isLoggedIn && !isFromLogout) {
            onLoginSuccess()
        }

        // 로그인 상태가 확실히 false로 확인되면 로그아웃 플래그 초기화
        if (!authState.isLoggedIn) {
            isFromLogout = false
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
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                WalkManColors.Primary,
                                WalkManColors.Primary.copy(alpha = 0.1f),
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
                        onClick = {
                            hasAttemptedLogin = true
                            viewModel.signInWithGoogle()
                        },
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
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력 필드
                    OutlinedTextField(
                        value = passwordValue,
                        onValueChange = { passwordValue = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (emailValue.isNotBlank() && passwordValue.isNotBlank()) {
                                hasAttemptedLogin = true
                                viewModel.signInWithEmail(emailValue, passwordValue)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("이메일과 비밀번호를 입력해주세요.")
                                }
                            }
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