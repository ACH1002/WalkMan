package inu.appcenter.walkman.presentation


import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.presentation.navigation.GaitxNavGraph
import inu.appcenter.walkman.presentation.theme.WalkManTheme
import inu.appcenter.walkman.presentation.viewmodel.MainViewModel
import inu.appcenter.walkman.service.StepCounterService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // 권한 요청 런처
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        // 필요한 경우 권한 결과 처리
        if (allGranted) {
            // 권한 획득 후 서비스 시작
            startStepCounterService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 스플래시 스크린 설정
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                // 초기 데이터 로딩이 완료될 때까지 스플래시 유지
                viewModel.uiState.value.isLoading
            }
        }

        super.onCreate(savedInstanceState)

        // 상단 상태바를 투명하게 설정
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 네비게이션 바 숨기기
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        // Android 10 이상에서 상단, 하단 바 숨기기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        enableEdgeToEdge()

        // 필요한 권한 요청
        requestPermissions()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            WalkManTheme(
                darkTheme = false,
                hideNavigationBar = true // 새로 추가된 매개변수
            ) {
                GaitxNavGraph(
                    startDestination = when {
                        uiState.shouldShowOnboarding -> "onboarding"
                        uiState.isUserProfileComplete && uiState.isDataCollectionComplete -> "main_navigation"
                        else -> "recording_modes"
                    },
                    onOnboardingComplete = {
                        viewModel.markOnboardingShown()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 앱이 활성화될 때 서비스 상태 확인
        if (arePermissionsGranted()) {
            startStepCounterService()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        // 저장소 권한 (Android 10 이하)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q &&
            shouldRequestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // 위치 권한
        if (shouldRequestPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // 활동 인식 권한 (Android 10 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            shouldRequestPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // 권한 요청
        if (permissions.isNotEmpty()) {
            permissionsLauncher.launch(permissions.toTypedArray())
        } else if (arePermissionsGranted()) {
            // 이미 모든 권한이 있는 경우 서비스 시작
            startStepCounterService()
        }
    }

    private fun shouldRequestPermission(permission: String): Boolean {
        return checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun arePermissionsGranted(): Boolean {
        // 필수 권한 확인
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10 미만에서는 필요 없음
        }

        val fineLocationGranted =
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return activityRecognitionGranted && fineLocationGranted
    }

    private fun startStepCounterService() {
        // 걸음 수 카운터 서비스 시작
        val serviceIntent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}