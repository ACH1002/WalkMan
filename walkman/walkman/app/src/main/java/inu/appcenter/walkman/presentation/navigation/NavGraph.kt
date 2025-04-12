package inu.appcenter.walkman.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
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
import inu.appcenter.walkman.presentation.screen.auth.LoginScreen
import inu.appcenter.walkman.presentation.screen.auth.SignUpScreen
import inu.appcenter.walkman.presentation.screen.gaitanalysis.UpdatedGaitAnalysisScreen
import inu.appcenter.walkman.presentation.screen.onboarding.OnboardingScreen
import inu.appcenter.walkman.presentation.screen.profile.ProfileCreateEditScreen
import inu.appcenter.walkman.presentation.screen.profile.UserProfileManagementScreen
import inu.appcenter.walkman.presentation.screen.recording.RecordingScreen
import inu.appcenter.walkman.presentation.screen.recordingmodes.RecordingModesScreen
import inu.appcenter.walkman.presentation.screen.result.RecordingResultsScreen
import inu.appcenter.walkman.presentation.screen.userinfo.UserInfoScreen
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import inu.appcenter.walkman.presentation.viewmodel.UserInfoViewModel

@Composable
fun GaitxNavGraph(
    startDestination: String = "login",
    navController: NavHostController = rememberNavController(),
    onOnboardingComplete: () -> Unit = {}
) {
    // ViewModels
    val authViewModel: AuthViewModel = hiltViewModel()
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val userInfoViewModel: UserInfoViewModel = hiltViewModel()
    val profileGaitViewModel: ProfileGaitViewModel = hiltViewModel()

    // 현재 사용자 정보
    val userInfoState by userInfoViewModel.uiState.collectAsState()
    val profileGaitState by profileGaitViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 로그인 화면
        composable(
            route = "login?fromLogout={fromLogout}",
            arguments = listOf(
                navArgument("fromLogout") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val fromLogout = backStackEntry.arguments?.getBoolean("fromLogout") ?: false
            LoginScreen(
                fromLogout = fromLogout,
                onLoginSuccess = {
                    // 로그인 성공 시 유저 정보 입력 화면 또는 메인 화면으로 이동
                    Log.d("LoginScreen", profileGaitState.userProfiles.toString())
                    if (profileGaitState.userProfiles.isEmpty()) {
                        navController.navigate("onboarding") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("main_navigation") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                viewModel = authViewModel,
                onContinueAsGuest = {
                },
                onNavigateToSignUp = {
                    // 회원가입 화면으로 이동
                    navController.navigate("signup")
                }
            )
        }

        // 회원가입 화면
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

        // 사용자 정보 입력 화면
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
                profileViewModel = profileGaitViewModel,
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

        // 메인 네비게이션
        composable("main_navigation") {
            // 외부 네비게이션 컨트롤러 전달
            MainNavigationScreen(
                externalNavController = navController,
                authViewModel = authViewModel
            )
        }

        // 측정 모드 선택 화면
        composable("recording_modes") {
            RecordingModesScreen(
                viewModel = recordingViewModel,
                onNavigateToRecording = { mode ->
                    navController.navigate("recording/${mode.name}")
                },
                onNavigateToResults = {
                    navController.navigate("recording_results")
                },
                onNavigateToUserInfo = {
                    navController.navigate("profile_management")
                }
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

        // 프로필 관리 화면
        composable("profile_management") {
            UserProfileManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateProfile = {
                    navController.navigate("profile_create")
                },
                onNavigateToEditProfile = { profile ->
                    // 편집할 프로필 ID 전달
                    navController.navigate("profile_edit/${profile.id}")
                }
            )
        }

        // 프로필 생성 화면
        composable("profile_create") {
            ProfileCreateEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 프로필 편집 화면
        composable(
            route = "profile_edit/{profileId}",
            arguments = listOf(
                navArgument("profileId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            val profileToEdit = profileGaitState.userProfiles.find { it.id == profileId }

            ProfileCreateEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                profileToEdit = profileToEdit
            )
        }

        // 보행 분석 화면
        composable("gait_analysis") {
            UpdatedGaitAnalysisScreen(
                onNavigateToProfiles = {
                    navController.navigate("profile_management")
                }
            )
        }
    }
}