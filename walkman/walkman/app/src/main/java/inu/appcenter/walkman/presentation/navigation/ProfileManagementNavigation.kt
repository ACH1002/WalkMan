package inu.appcenter.walkman.presentation.navigation

import android.annotation.SuppressLint
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import inu.appcenter.walkman.presentation.screen.profile.ProfileCreateEditScreen
import inu.appcenter.walkman.presentation.screen.profile.UserProfileManagementScreen
import inu.appcenter.walkman.presentation.viewmodel.ProfileGaitViewModel

/**
 * 프로필 관리 화면 네비게이션 확장 함수
 */
@SuppressLint("StateFlowValueCalledInComposition")
fun NavGraphBuilder.profileManagementNavigation(
    navController: NavHostController,
    profileViewModel: ProfileGaitViewModel
) {
    // 프로필 관리 화면
    composable(NavDestinations.PROFILE_MANAGEMENT) {
        UserProfileManagementScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToCreateProfile = {
                navController.navigate(NavDestinations.PROFILE_CREATE)
            },
            onNavigateToEditProfile = { profile ->
                navController.navigate("profile_edit/${profile.id}")
            }
        )
    }

    // 프로필 생성 화면
    composable(NavDestinations.PROFILE_CREATE) {
        ProfileCreateEditScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    // 프로필 편집 화면
    composable(
        route = NavDestinations.PROFILE_EDIT,
        arguments = listOf(
            navArgument("profileId") {
                type = NavType.StringType
            }
        )
    ) { backStackEntry ->
        val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
        val profileToEdit = profileViewModel.uiState.value.userProfiles.find { it.id == profileId }

        ProfileCreateEditScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            profileToEdit = profileToEdit
        )
    }
}