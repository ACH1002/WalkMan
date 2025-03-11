package inu.appcenter.walkman.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.screen.home.HomeScreen
import inu.appcenter.walkman.presentation.screen.recording.RecordingScreen
import inu.appcenter.walkman.presentation.screen.recordingmodes.RecordingModesScreen
import inu.appcenter.walkman.presentation.screen.result.RecordingResultsScreen
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel

sealed class MainNavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: @Composable () -> Unit,
    val unselectedIcon: @Composable () -> Unit
) {
    object Home : MainNavigationItem(
        route = "home",
        title = "홈",
        selectedIcon = { Icon(Icons.Filled.Home, contentDescription = "홈") },
        unselectedIcon = { Icon(Icons.Outlined.Home, contentDescription = "홈") }
    )

    object MBTI : MainNavigationItem(
        route = "mbti",
        title = "MBTI",
        selectedIcon = { Icon(Icons.Filled.Psychology, contentDescription = "MBTI") },
        unselectedIcon = { Icon(Icons.Outlined.Psychology, contentDescription = "MBTI") }
    )

    object MLModel : MainNavigationItem(
        route = "ml_model",
        title = "모델 관리",
        selectedIcon = { Icon(Icons.Filled.Settings, contentDescription = "모델 관리") },
        unselectedIcon = { Icon(Icons.Outlined.Settings, contentDescription = "모델 관리") }
    )

    object MyPage : MainNavigationItem(
        route = "my_page",
        title = "마이페이지",
        selectedIcon = { Icon(Icons.Filled.Person, contentDescription = "마이페이지") },
        unselectedIcon = { Icon(Icons.Outlined.Person, contentDescription = "마이페이지") }
    )
}

@Composable
fun MainNavigationScreen() {
    val navController = rememberNavController()
    val recordingViewModel: RecordingViewModel = hiltViewModel()

    val navigationItems = listOf(
        MainNavigationItem.Home,
        MainNavigationItem.MBTI,
        MainNavigationItem.MLModel,
        MainNavigationItem.MyPage
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = WalkManColors.Background,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navigationItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationBarItem(
                        icon = {
                            if (selected) {
                                item.selectedIcon()
                            } else {
                                item.unselectedIcon()
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
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
                            navController.navigate(item.route) {
                                // 백 스택에서 시작 목적지까지 팝
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // 같은 항목을 다시 선택할 때 중복 항목 피하기
                                launchSingleTop = true
                                // 이전에 선택한 항목의 상태 복원
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
                        navController.navigate("recording_modes")
                    }
                )
            }

            composable(MainNavigationItem.MBTI.route) {
            }

            composable(MainNavigationItem.MLModel.route) {
            }

            composable(MainNavigationItem.MyPage.route) {
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
                    onNavigateToUserInfo = {
                        navController.navigate("user_info?isEdit=true")
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
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    }
}