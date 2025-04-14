package inu.appcenter.walkman.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.screen.gaitanalysis.UpdatedGaitAnalysisScreen
import inu.appcenter.walkman.presentation.screen.home.HomeScreen
import inu.appcenter.walkman.presentation.screen.mypage.MyPageScreen
import inu.appcenter.walkman.presentation.screen.mypage.settings.language.LanguageSettingsScreen
import inu.appcenter.walkman.presentation.screen.mypage.settings.notification.NotificationSettingsScreen
import inu.appcenter.walkman.presentation.screen.recording.RecordingScreen
import inu.appcenter.walkman.presentation.screen.recordingmodes.RecordingModesScreen
import inu.appcenter.walkman.presentation.screen.result.RecordingResultsScreen
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

sealed class MainNavigationItem(
    val route: String,
    val titleResId: Int,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    object Home : MainNavigationItem(
        route = "home",
        titleResId = R.string.navigation_home,
        selectedIcon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
        unselectedIcon = { Icon(Icons.Outlined.Home, contentDescription = "홈") }
    )

    object MBTI : MainNavigationItem(
        route = "mbti",
        titleResId = R.string.navigation_mbti,
        selectedIcon = { Icon(Icons.Filled.Psychology, contentDescription = "MBTI") },
        unselectedIcon = { Icon(Icons.Outlined.Psychology, contentDescription = "MBTI") }
    )

    object GaitAnalysis : MainNavigationItem(
        route = "gait_analysis",
        titleResId = R.string.navigation_gait_analysis,
        selectedIcon = { Icon(Icons.Filled.ShowChart, contentDescription = "보행 분석") },
        unselectedIcon = { Icon(Icons.Outlined.ShowChart, contentDescription = "보행 분석") }
    )

    object MyPage : MainNavigationItem(
        route = "my_page",
        titleResId = R.string.navigation_mypage,
        selectedIcon = { Icon(Icons.Filled.Person, contentDescription = "마이페이지") },
        unselectedIcon = { Icon(Icons.Outlined.Person, contentDescription = "마이페이지") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    externalNavController: NavHostController,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val recordingViewModel: RecordingViewModel = hiltViewModel()
    val profileGaitViewModel: ProfileGaitViewModel = hiltViewModel()

    // 로그아웃 상태 구독
    val authUiState by authViewModel.uiState.collectAsState()

    // 로그아웃 완료시 처리
    LaunchedEffect(authUiState.logoutCompleted) {
        if (authUiState.logoutCompleted) {
            // 로그아웃 완료 플래그 리셋
            authViewModel.resetLogoutCompleted()
        }
    }

    val navigationItems = listOf(
        MainNavigationItem.Home,
        MainNavigationItem.MBTI,
        MainNavigationItem.GaitAnalysis,
        MainNavigationItem.MyPage
    )

    // 현재 백스택 엔트리 가져오기
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // 현재 라우트 추출
    val currentRoute = navBackStackEntry?.destination?.route

    // 측정 관련 화면인지 확인
    val isRecordingRelatedScreen = remember(currentRoute) {
        currentRoute?.startsWith("recording") ?: false
    }

    // 선택된 탭 상태 관리
    var selectedTab by remember { mutableStateOf(MainNavigationItem.Home.route) }

    // 현재 경로가 바텀 탭 중 하나일 때만 선택된 탭 업데이트
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && navigationItems.any { it.route == currentRoute }) {
            selectedTab = currentRoute
        }
    }

    Scaffold(
        topBar = {
            when (currentRoute) {
                MainNavigationItem.Home.route -> {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(Color.Transparent),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    "GAITX",
                                    color = WalkManColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = WalkManColors.Background
                        )
                    )
                }
                // Other route-specific TopAppBars
                // ...
            }
        },
        bottomBar = {
            // 측정 관련 화면에서는 하단 바를 숨기지 않고, 홈 탭을 선택된 상태로 표시
            NavigationBar(
                containerColor = WalkManColors.Background,
                tonalElevation = 8.dp,
                modifier = Modifier.height(56.dp)
            ) {
                navigationItems.forEach { item ->
                    val selected = item.route == selectedTab

                    NavigationBarItem(
                        icon = {
                            Box(modifier = Modifier.size(25.dp)) {
                                if (selected) {
                                    item.selectedIcon()
                                } else {
                                    item.unselectedIcon()
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(item.titleResId),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WalkManColors.Primary,
                            selectedTextColor = WalkManColors.Primary,
                            indicatorColor = WalkManColors.Primary.copy(alpha = 0.1f),
                            unselectedIconColor = WalkManColors.TextSecondary,
                            unselectedTextColor = WalkManColors.TextSecondary
                        ),
                        onClick = {
                            // 이미 해당 탭에 있는 경우 아무 동작 안함
                            if (selectedTab == item.route) return@NavigationBarItem

                            selectedTab = item.route

                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainNavigationItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainNavigationItem.Home.route) {
                HomeScreen(
                    onStartNewRecording = {
                        // 선택된 탭은 계속 홈 탭으로 유지
                        selectedTab = MainNavigationItem.Home.route
                        navController.navigate("recording_modes")
                    },
                    onNavigateToNotificationSettings = {
                        navController.navigate("notification_settings")
                    }
                )
            }

            composable(MainNavigationItem.MBTI.route) {
                // MBTI 화면
            }

            composable(MainNavigationItem.GaitAnalysis.route) {
                UpdatedGaitAnalysisScreen(
                    onNavigateToProfiles = {
                        // 프로필 관리 화면으로 이동
                        externalNavController.navigate(NavDestinations.PROFILE_MANAGEMENT)
                    },
                    onStartNewRecording = {
                        // 선택된 탭은 계속 홈 탭으로 유지
                        selectedTab = MainNavigationItem.GaitAnalysis.route
                        navController.navigate("recording_modes")
                    }
                )
            }

            composable(MainNavigationItem.MyPage.route) {
                // 마이페이지 화면
                MyPageScreen(
                    navController = navController,
                    onLogOut = {
                        // 로그아웃 수행
                        authViewModel.signOut()
                    },
                    onClickProfileEdit = {
                        // 프로필 관리 화면으로 이동
                        externalNavController.navigate(NavDestinations.PROFILE_MANAGEMENT)
                    }
                )
            }

            composable("language_settings") {
                LanguageSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 측정 관련 화면들도 추가
            composable("recording_modes") {
                RecordingModesScreen(
                    viewModel = recordingViewModel,
                    onNavigateToRecording = { mode ->
                        navController.navigate("recording/${mode.name}")
                    },
                    onNavigateToResults = {
                        navController.navigate("recording_results")
                    },
                    onNavigateToUserInfo = { profile ->
                        // 프로필 편집 화면으로 이동
                        externalNavController.navigate("profile_edit/${profile.id}")
                    },
                    onBackPressed = {
                        // 측정 모드 화면에서 뒤로가기 시 홈 화면으로 이동
                        navController.popBackStack(MainNavigationItem.Home.route, false)
                    }
                )
            }

            composable("recording/{mode}") { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode")?.let {
                    RecordingMode.valueOf(it)
                } ?: RecordingMode.POCKET

                RecordingScreen(
                    viewModel = recordingViewModel,
                    mode = mode,
                    onNavigateBack = {
                        // 측정 화면에서 뒤로가기 시 측정 모드 선택 화면으로 이동
                        navController.popBackStack()
                    }
                )
            }

            composable("recording_results") {
                RecordingResultsScreen(
                    viewModel = recordingViewModel,
                    onNavigateHome = {
                        // 측정 완료 후 메인 네비게이션의 홈으로 돌아감
                        navController.navigate(MainNavigationItem.Home.route) {
                            // 백스택에서 측정 관련 화면들 모두 제거
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }

            composable("notification_settings") {
                NotificationSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}