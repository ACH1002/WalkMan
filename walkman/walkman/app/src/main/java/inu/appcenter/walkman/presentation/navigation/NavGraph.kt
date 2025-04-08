package inu.appcenter.walkman.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.presentation.screen.auth.LoginScreen
import inu.appcenter.walkman.presentation.screen.auth.SignUpScreen
import inu.appcenter.walkman.presentation.screen.onboarding.OnboardingScreen
import inu.appcenter.walkman.presentation.screen.recording.RecordingScreen
import inu.appcenter.walkman.presentation.screen.recordingmodes.RecordingModesScreen
import inu.appcenter.walkman.presentation.screen.result.RecordingResultsScreen
import inu.appcenter.walkman.presentation.screen.userinfo.UserInfoScreen
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import inu.appcenter.walkman.presentation.viewmodel.UserInfoViewModel

@Composable
fun GaitxNavGraph(
    startDestination: String = "login",
    navController: NavHostController = rememberNavController(),
    onOnboardingComplete: () -> Unit = {}
) {
    // RecordingViewModel은 여러 화면에서 공유
    val authViewModel: AuthViewModel = hiltViewModel()
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val userInfoViewModel: UserInfoViewModel = hiltViewModel()

    // 현재 사용자 정보
    val userInfoState by userInfoViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면 추가
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // 로그인 성공 시 유저 정보 입력 화면 또는 메인 화면으로 이동
                    if (!userInfoState.isComplete) {
                        navController.navigate("onboarding") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main_navigation") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onContinueAsGuest = {
                },
                onNavigateToSignUp = {
                    // 회원가입 화면으로 이동
                    navController.navigate("signup")
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = authViewModel,
                onSignUpSuccess = {
                    // 회원가입 성공 후 로그인
                    navController.popBackStack()
                },
                onNavigateBack = {
                    // 로그인 화면으로 돌아가기
                    navController.popBackStack()
                }
            )
        }
        // 온보딩 화면
        composable("onboarding") {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate("user_info") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                    onOnboardingComplete()
                }
            )
        }

        // NavGraph.kt의 UserInfoScreen 부분 수정
        composable(
            route = "user_info?isEdit={isEdit}",
            arguments = listOf(
                navArgument("isEdit") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isEdit = backStackEntry.arguments?.getBoolean("isEdit") ?: false

            UserInfoScreen(
                viewModel = userInfoViewModel,
                isEdit = isEdit,
                onNavigateNext = {
                    if (isEdit) {
                        // 편집 모드에서는 이전 화면으로 돌아감
                        navController.popBackStack()
                    } else {
                        // 최초 설정에서는 홈 화면으로 바로 이동
                        navController.navigate("main_navigation") {
                            popUpTo("user_info") { inclusive = true }
                        }
                    }
                }
            )
        }

        // 측정 모드 선택 화면
        composable("recording_modes") {
            RecordingModesScreen(
                viewModel = recordingViewModel,
                userInfo = UserInfo(
                    name = userInfoState.name,
                    gender = userInfoState.gender,
                    height = userInfoState.height,
                    weight = userInfoState.weight,
                    mbti = userInfoState.mbti
                ),
                onNavigateToRecording = { mode ->
                    navController.navigate("recording/${mode.name}")
                },
                onNavigateToResults = {
                    navController.navigate("recording_results")
                },
                onNavigateToUserInfo = {
                    // 사용자 정보 편집 모드로 이동
                    navController.navigate("user_info?isEdit=true")
                }
            )
        }

        // 메인 네비게이션
        composable("main_navigation") {
            // 외부 네비게이션 컨트롤러 전달
            MainNavigationScreen(
                externalNavController = navController
            )
        }

        // 측정 화면
        composable(
            route = "recording/{mode}",
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode")?.let {
                RecordingMode.valueOf(it)
            } ?: RecordingMode.POCKET

            RecordingScreen(
                viewModel = recordingViewModel,
                mode = mode,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 측정 결과 화면
        composable("recording_results") {
            RecordingResultsScreen(
                viewModel = recordingViewModel,
                onNavigateHome = {
                    navController.navigate("main_navigation") {
                        popUpTo("recording_modes") { inclusive = true }
                    }
                }
            )
        }
    }
}