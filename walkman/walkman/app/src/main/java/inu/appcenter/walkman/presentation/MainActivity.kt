package inu.appcenter.walkman.presentation

import android.Manifest
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import inu.appcenter.walkman.domain.repository.NotificationRepository
import inu.appcenter.walkman.presentation.navigation.GaitxNavGraph
import inu.appcenter.walkman.presentation.theme.WalkManTheme
import inu.appcenter.walkman.presentation.viewmodel.MainViewModel
import inu.appcenter.walkman.service.AppUsageTrackingService
import inu.appcenter.walkman.service.StepCounterService
import inu.appcenter.walkman.service.StepCounterService.Companion.CHANNEL_ID
import inu.appcenter.walkman.util.LanguageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.os.Process
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import inu.appcenter.walkman.BuildConfig
import inu.appcenter.walkman.service.StepCounterService.Companion.TAG

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var languageManager: LanguageManager

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

    override fun attachBaseContext(newBase: Context) {
        // 언어 설정 적용
        languageManager = LanguageManager(newBase)
        super.attachBaseContext(languageManager.updateConfiguration(newBase))
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

        startAppUsageTracking()

        checkAndRequestUsageStatsPermission()

        if (hasUsageStatsPermission()) {
            startAppUsageTracking()
        } else {
            // 권한이 없으면 권한 요청
            requestUsageStatsPermission()
        }

        // 언어 매니저 초기화
        languageManager = LanguageManager(this)

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

        addDebugFloatingButton()

        enableEdgeToEdge()

        // 필요한 권한 요청
        requestPermissions()

        // MainActivity.kt의 setContent 부분 수정
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            WalkManTheme(
                darkTheme = false,
                hideNavigationBar = true
            ) {
                GaitxNavGraph(
                    startDestination = when {
                        uiState.shouldShowOnboarding -> "onboarding"
                        uiState.isUserProfileComplete -> "main_navigation" // 프로필이 완성되면 바로 홈으로
                        else -> "user_info" // 프로필이 없으면 정보 입력 화면으로
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            shouldRequestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 권한 요청
        if (permissions.isNotEmpty()) {
            permissionsLauncher.launch(permissions.toTypedArray())
        } else if (arePermissionsGranted()) {
            // 이미 모든 권한이 있는 경우 서비스 시작
            startStepCounterService()
        }
    }

    private fun checkNotificationSettings() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)

            // 채널이 존재하고 알림이 차단된 경우
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                AlertDialog.Builder(this)
                    .setTitle("알림 설정 필요")
                    .setMessage("걸음 수를 측정하기 위해서는 알림 설정이 필요합니다. 설정으로 이동하여 알림을 허용해주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        } else {
            // Android O 미만에서는 앱 전체 알림 설정 확인
            if (!notificationManager.areNotificationsEnabled()) {
                AlertDialog.Builder(this)
                    .setTitle("알림 설정 필요")
                    .setMessage("걸음 수를 측정하기 위해서는 알림 설정이 필요합니다. 설정으로 이동하여 알림을 허용해주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
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

        // Android 13 이상에서 알림 권한 확인
        val notificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 미만에서는 필요 없음
        }

        return activityRecognitionGranted && fineLocationGranted && notificationPermissionGranted
    }

    private fun startStepCounterService() {
        coroutineScope.launch {
            try {
                val isNotificationEnabled = notificationRepository.isNotificationEnabled().first()

                if (isNotificationEnabled && arePermissionsGranted()) {
                    // 알림 설정이 활성화된 경우에만 서비스 시작
                    val serviceIntent = Intent(this@MainActivity, StepCounterService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting service", e)
            }
        }
    }


    private fun startAppUsageTracking() {
        Log.d(TAG, "Starting AppUsageTrackingService")
        Log.d(TAG, "Usage stats permission: ${hasUsageStatsPermission()}")

        try {
            val intent = Intent(this, AppUsageTrackingService::class.java).apply {
                action = AppUsageTrackingService.ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "AppUsageTrackingService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "앱 사용 추적 서비스 시작 실패", e)
            Toast.makeText(this, "앱 사용 추적 서비스를 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
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

            val hasPermission = mode == AppOpsManager.MODE_ALLOWED

            // 더 자세한 로깅 추가
            Log.d(TAG, "Usage stats permission check details:")
            Log.d(TAG, "App UID: ${Process.myUid()}")
            Log.d(TAG, "Package Name: $packageName")
            Log.d(TAG, "Permission Mode: $mode")
            Log.d(TAG, "Has Permission: $hasPermission")

            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            Log.e(TAG, "Exception details:", e)
            false
        }
    }

    private fun checkUsageStatsPermission() {
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

        if (mode != AppOpsManager.MODE_ALLOWED) {
            // 권한 요청 대화상자 표시
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 앱 종료 시 서비스 중지
        val intent = Intent(this, AppUsageTrackingService::class.java).apply {
            action = AppUsageTrackingService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION)
    }

    private fun checkAndRequestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            AlertDialog.Builder(this)
                .setTitle("사용 통계 접근 권한 필요")
                .setMessage("걷는 동안 소셜미디어 사용량을 측정하기 위해 '사용 통계 접근' 권한이 필요합니다. 설정 화면에서 GAITX 앱에 권한을 허용해주세요.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    try {
                        // 명시적으로 사용 통계 접근 설정 화면으로 이동
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION)
                    } catch (e: Exception) {
                        Log.e(TAG, "권한 설정 화면 오픈 중 오류 발생", e)
                        Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            // 권한이 있는 경우 서비스 시작
            startAppUsageTracking()
        }
    }

    private fun showUsageStatsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("앱 사용 추적 권한 필요")
            .setMessage("걷는 동안 소셜 미디어 사용을 추적하기 위해 '앱 사용 통계' 권한이 필요합니다. 설정 화면에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivityForResult(intent, REQUEST_USAGE_STATS_PERMISSION)
            }
            .setNegativeButton("취소") { _, _ ->
                // 권한 거부 시 기본 동작 처리
                Toast.makeText(this, "앱 사용 추적 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_USAGE_STATS_PERMISSION) {
            if (hasUsageStatsPermission()) {
                // 권한 획득 성공 시 앱 사용 추적 시작
                startAppUsageTracking()
            } else {
                // 권한 거부 시 사용자에게 알림
                Toast.makeText(this, "앱 사용 추적 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startWalkingSimulation(isWalking: Boolean) {
        // Create intent to simulate walking
        val intent = Intent(this, StepCounterService::class.java).apply {
            action = if (isWalking) "SIMULATE_WALKING_START" else "SIMULATE_WALKING_STOP"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Also broadcast a step detection event for immediate feedback
        if (isWalking) {
            val stepIntent = Intent(StepCounterService.STEP_DETECTION_ACTION).apply {
                putExtra("step_count", 1)
            }
            sendBroadcast(stepIntent)
        }
    }

    private fun testWalkingDetection() {
        // Show dialog to select test walking state
        AlertDialog.Builder(this)
            .setTitle("테스트: 걷기 시뮬레이션")
            .setMessage("걷기 상태를 시뮬레이션하여 앱 기능을 테스트합니다.")
            .setPositiveButton("걷기 시작") { _, _ ->
                // Start the walking simulation
                startWalkingSimulation(true)
                Toast.makeText(this, "걷기 시뮬레이션 시작됨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("걷기 중지") { _, _ ->
                // Stop the walking simulation
                startWalkingSimulation(false)
                Toast.makeText(this, "걷기 시뮬레이션 중지됨", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("취소", null)
            .show()
    }

    companion object {
        private const val REQUEST_USAGE_STATS_PERMISSION = 1001
    }

    private fun addDebugFloatingButton() {
        // Only add debug button in debug builds
        if (BuildConfig.DEBUG) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            val debugButton = Button(this).apply {
                text = "DEBUG"
                setBackgroundColor(Color.parseColor("#80FF0000"))
                alpha = 0.7f
                setOnClickListener {
                    testWalkingDetection()
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                x = 10
                y = 100
            }

            try {
                windowManager.addView(debugButton, params)
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not add debug button", e)
            }
        }
    }
}