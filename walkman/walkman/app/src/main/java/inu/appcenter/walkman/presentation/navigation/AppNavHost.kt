package inu.appcenter.walkman.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import inu.appcenter.walkman.domain.model.AuthState
import inu.appcenter.walkman.presentation.screen.auth.LoginScreen
import inu.appcenter.walkman.presentation.screen.auth.SignUpScreen
import inu.appcenter.walkman.presentation.screen.onboarding.OnboardingScreen
import inu.appcenter.walkman.presentation.screen.splash.SplashScreen
import inu.appcenter.walkman.presentation.screen.userinfo.UserInfoScreen
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel

/**
 * 앱의 최상위 네비게이션 호스트
 * 인증 상태에 따라 적절한 화면 표시
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavDestinations.SPLASH
) {
    // ViewModels
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileGaitViewModel: ProfileGaitViewModel = hiltViewModel()

    // 인증 상태 구독
    val authState by authViewModel.authState.collectAsState()

    // 인증 상태 변화에 따른 네비게이션
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(NavDestinations.MAIN_NAV_GRAPH) {
                    popUpTo(NavDestinations.SPLASH) { inclusive = true }
                }
            }
            is AuthState.AuthenticatedWithoutProfile -> {
                navController.navigate(NavDestinations.ONBOARDING_NAV_GRAPH) {
                    popUpTo(NavDestinations.SPLASH) { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(NavDestinations.AUTH_NAV_GRAPH) {
                    popUpTo(NavDestinations.SPLASH) { inclusive = true }
                }
            }
            AuthState.Loading -> {
                // 로딩 중에는 SplashScreen 유지
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 스플래시 화면
        composable(NavDestinations.SPLASH) {
            SplashScreen(viewModel = authViewModel)
        }

        // 인증 네비게이션 그래프
        navigation(
            startDestination = NavDestinations.LOGIN,
            route = NavDestinations.AUTH_NAV_GRAPH
        ) {
            // 로그인 화면
            composable(NavDestinations.LOGIN) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignUp = {
                        navController.navigate(NavDestinations.SIGNUP)
                    },
                )
            }

            // 회원가입 화면
            composable(NavDestinations.SIGNUP) {
                SignUpScreen(
                    viewModel = authViewModel,
                    onSignUpSuccess = {
                        // authViewModel에서 상태 업데이트하고 LaunchedEffect에서 처리
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // 온보딩 네비게이션 그래프
        navigation(
            startDestination = NavDestinations.ONBOARDING,
            route = NavDestinations.ONBOARDING_NAV_GRAPH
        ) {
            // 온보딩 화면
            composable(NavDestinations.ONBOARDING) {
                OnboardingScreen(
                    onGetStarted = {
                        navController.navigate(NavDestinations.USER_INFO)
                    }
                )
            }

            // 사용자 정보 입력 화면
            composable(NavDestinations.USER_INFO) {
                UserInfoScreen(
                    profileViewModel = profileGaitViewModel,
                    isEdit = false,
                    onNavigateNext = {
                        // 프로필 생성 완료 알림
                        authViewModel.notifyProfileCreated()
                        // authViewModel에서 상태 업데이트하고 LaunchedEffect에서 처리
                    }
                )
            }
        }

        // 메인 네비게이션 그래프
        navigation(
            startDestination = NavDestinations.MAIN_NAVIGATION,
            route = NavDestinations.MAIN_NAV_GRAPH
        ) {
            // 메인 네비게이션 컨테이너
            composable(NavDestinations.MAIN_NAVIGATION) {
                MainNavigationScreen(
                    externalNavController = navController,
                    authViewModel = authViewModel
                )
            }

            // 프로필 관리 화면
            profileManagementNavigation(navController, profileGaitViewModel)

            // 다른 메인 화면들 추가...
        }
    }
}

/**
 * 네비게이션 목적지 상수
 */
object NavDestinations {
    // 최상위 목적지
    const val SPLASH = "splash"

    // 네비게이션 그래프
    const val AUTH_NAV_GRAPH = "auth_nav_graph"
    const val ONBOARDING_NAV_GRAPH = "onboarding_nav_graph"
    const val MAIN_NAV_GRAPH = "main_nav_graph"

    // 인증 관련 목적지
    const val LOGIN = "login"
    const val SIGNUP = "signup"

    // 온보딩 관련 목적지
    const val ONBOARDING = "onboarding"
    const val USER_INFO = "user_info"

    // 메인 관련 목적지
    const val MAIN_NAVIGATION = "main_navigation"
    const val PROFILE_MANAGEMENT = "profile_management"
    const val PROFILE_CREATE = "profile_create"
    const val PROFILE_EDIT = "profile_edit/{profileId}"

    // 기타 메인 화면
    const val RECORDING_MODES = "recording_modes"
    const val RECORDING = "recording/{mode}"
    const val RECORDING_RESULTS = "recording_results"
    const val GAIT_ANALYSIS = "gait_analysis"
    const val SETTINGS = "settings"
}