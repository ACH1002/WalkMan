package inu.appcenter.walkman.presentation.navigation


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.presentation.screen.onboarding.OnboardingScreen
import inu.appcenter.walkman.presentation.screen.recording.RecordingScreen
import inu.appcenter.walkman.presentation.screen.recordingmodes.RecordingModesScreen
import inu.appcenter.walkman.presentation.screen.result.RecordingResultsScreen
import inu.appcenter.walkman.presentation.screen.userinfo.UserInfoScreen
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import inu.appcenter.walkman.presentation.viewmodel.UserInfoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun GaitxNavGraph(
    startDestination: String = "onboarding",
    navController: NavHostController = rememberNavController(),
    onOnboardingComplete: () -> Unit = {}
) {
    // RecordingViewModel은 여러 화면에서 공유
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val userInfoViewModel: UserInfoViewModel = hiltViewModel()

    // 현재 사용자 정보
    val userInfoState by userInfoViewModel.uiState.collectAsState()

    // 코루틴 스코프
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
                viewModel = userInfoViewModel,
                onNavigateNext = {
                    if (isEdit) {
                        // 편집 모드에서는 이전 화면으로 돌아감
                        navController.popBackStack()
                    } else {
                        // 최초 설정에서는 측정 모드 화면으로 이동
                        navController.navigate("recording_modes") {
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
                    weight = userInfoState.weight
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
                    navController.navigate("recording_modes") {
                        popUpTo("recording_modes") { inclusive = true }
                    }
                }
            )
        }
    }
}