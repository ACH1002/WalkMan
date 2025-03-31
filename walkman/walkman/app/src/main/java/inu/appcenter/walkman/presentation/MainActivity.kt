package inu.appcenter.walkman.presentation

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.BuildConfig
import inu.appcenter.walkman.WalkManApplication
import inu.appcenter.walkman.presentation.navigation.GaitxNavGraph
import inu.appcenter.walkman.presentation.theme.WalkManTheme
import inu.appcenter.walkman.presentation.viewmodel.MainViewModel
import inu.appcenter.walkman.service.WalkingDetectorService
import inu.appcenter.walkman.util.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
private const val REQUEST_USAGE_STATS_PERMISSION = 1001

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var languageManager: LanguageManager

    // 권한 요청 런처
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startWalkingDetectorService()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // 언어 설정 적용
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 스플래시 스크린 설정
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.uiState.value.isLoading
            }
        }

        super.onCreate(savedInstanceState)

        // 언어 매니저 초기화
        languageManager = LanguageManager(this)

        // 상태바 설정
        // 상태바 설정 수정 부분
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // 시스템 바 색상 및 가시성 설정
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true // 상태 바 아이콘 색상 (true: 어두운 색, false: 밝은 색)
            isAppearanceLightNavigationBars = true // 내비게이션 바 아이콘 색상
            hide(WindowInsetsCompat.Type.systemBars()) // 시스템 바 숨기기
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // 스와이프 시 일시적으로 표시
        }

        // 필요한 권한 확인 및 요청
        checkAndRequestPermissions()

        // 앱 사용 통계 권한 확인
        checkAndRequestUsageStatsPermission()

        // 디버그 모드일 경우 테스트 버튼 추가
        if (BuildConfig.DEBUG) {
            addDebugControls()
        }

        // 메인 UI 설정
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            WalkManTheme(
                darkTheme = false,
                hideNavigationBar = true
            ) {
                GaitxNavGraph(
                    startDestination = when {
                        uiState.shouldShowOnboarding -> "onboarding"
                        uiState.isUserProfileComplete -> "main_navigation"
                        else -> "user_info"
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

        // 권한이 있을 경우에만 설정에 따라 서비스 시작 또는 중지
        if (hasUsageStatsPermission() && arePermissionsGranted()) {
            checkNotificationSettingsAndUpdateService()
        }
    }

    private fun checkNotificationSettingsAndUpdateService() {
        // 코루틴 스코프 생성
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            try {
                // NotificationRepository 인스턴스 가져오기
                val notificationRepo = (application as? WalkManApplication)?.notificationRepository
                    ?: return@launch

                // 현재 설정 상태 확인
                val isEnabled = notificationRepo.isNotificationEnabled().first()

                if (isEnabled) {
                    startWalkingDetectorService()
                } else {
                    stopWalkingDetectorService()
                }
            } catch (e: Exception) {
                Log.e(TAG, "설정 확인 중 오류 발생", e)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // 위치 권한 추가
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 위치 권한에 대한 설명 다이얼로그 추가
            AlertDialog.Builder(this)
                .setTitle("위치 권한 필요")
                .setMessage("걷기 패턴 분석과 정확한 이동 경로 추적을 위해 위치 정보가 필요합니다. 사용자의 동의를 부탁드립니다.")
                .setPositiveButton("허용") { _, _ ->
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    permissionsLauncher.launch(permissions.toTypedArray())
                }
                .setNegativeButton("거부", null)
                .show()
        }

        // 센서 권한 (Android 10 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        // 알림 권한 (Android 13 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 오버레이 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
        }

        // 권한 요청
        if (permissions.isNotEmpty()) {
            permissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun showOverlayPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("화면 오버레이 권한 필요")
            .setMessage("걷는 중 소셜 미디어 사용 시 경고를 표시하기 위해 화면 위에 표시 권한이 필요합니다.")
            .setPositiveButton("설정") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun checkAndRequestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            AlertDialog.Builder(this)
                .setTitle("앱 사용 통계 권한 필요")
                .setMessage("걷는 중 소셜 미디어 사용을 감지하기 위해 앱 사용 통계 접근 권한이 필요합니다.")
                .setPositiveButton("설정") { _, _ ->
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION)
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            startWalkingDetectorService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    packageName
                )
            }

            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "사용 통계 권한 확인 오류", e)
            false
        }
    }

    private fun arePermissionsGranted(): Boolean {
        // 활동 인식 권한 확인
        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10 미만에서는 필요 없음
        }

        // 알림 권한 확인
        val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 미만에서는 필요 없음
        }

        // 오버레이 권한 확인
        val overlayPermissionGranted = Settings.canDrawOverlays(this)

        return activityRecognitionGranted && notificationPermissionGranted && overlayPermissionGranted
    }

    private fun startWalkingDetectorService() {
        if (!arePermissionsGranted() || !hasUsageStatsPermission()) {
            Log.d(TAG, "필요한 권한이 없어 서비스를 시작할 수 없습니다")
            return
        }

        try {
            val intent = Intent(this, WalkingDetectorService::class.java).apply {
                action = WalkingDetectorService.ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "걷기 감지 서비스 시작")
        } catch (e: Exception) {
            Log.e(TAG, "걷기 감지 서비스 시작 실패", e)
            Toast.makeText(this, "걷기 감지 서비스를 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_USAGE_STATS_PERMISSION) {
            if (hasUsageStatsPermission()) {
                startWalkingDetectorService()
            } else {
                Toast.makeText(this, "앱 사용 통계 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addDebugControls() {
        // 디버그 모드에서 테스트용 컨트롤 추가
        // 실제 구현은 UI 요구사항에 맞게 조정 필요
    }

    override fun onDestroy() {
        // 앱 종료 시 서비스 중지 (선택적)
        // stopWalkingDetectorService()
        super.onDestroy()
    }

    private fun stopWalkingDetectorService() {
        try {
            val intent = Intent(this, WalkingDetectorService::class.java).apply {
                action = WalkingDetectorService.ACTION_STOP
            }
            stopService(intent)
            Log.d(TAG, "걷기 감지 서비스 중지")
        } catch (e: Exception) {
            Log.e(TAG, "걷기 감지 서비스 중지 실패", e)
        }
    }
}