package inu.appcenter.walkman.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import inu.appcenter.walkman.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 걷는 중 소셜미디어 사용 시 경고 오버레이를 표시하는 서비스
 */
class WalkingWarningService : Service() {

    companion object {
        private const val TAG = "WalkingWarningService"
        const val ACTION_SHOW_WARNING = "SHOW_WARNING"
        const val EXTRA_APP_NAME = "app_name"
        private const val WARNING_DURATION = 7000L // 7초 동안 표시
    }

    private var windowManager: WindowManager? = null
    private var warningView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoRemoveJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_WARNING) {
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
            showWarningOverlay(appName)
        }

        return START_NOT_STICKY
    }

    private fun showWarningOverlay(appName: String = "") {
        // 기존 작업 취소 및 기존 뷰 제거
        autoRemoveJob?.cancel()
        removeExistingOverlay()

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                format = PixelFormat.TRANSLUCENT
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP
            }

            // 경고 창 레이아웃 인플레이트
            warningView = LayoutInflater.from(this).inflate(R.layout.overlay_walking_warning, null).apply {
                // 닫기 버튼 이벤트 설정
                findViewById<Button>(R.id.btnDismiss).setOnClickListener {
                    removeOverlay()
                }

                // 앱 아이콘 설정
                val iconView = findViewById<ImageView>(R.id.ivAppIcon)
                val iconResId = getAppIconResource(appName)
                if (iconResId != 0) {
                    iconView.setImageResource(iconResId)
                    iconView.visibility = View.VISIBLE
                } else {
                    iconView.visibility = View.GONE
                }

                // 경고 메시지 설정
                val messageView = findViewById<TextView>(R.id.tvWarningMessage)
                if (appName.isNotEmpty()) {
                    // 앱별 맞춤 메시지
                    messageView.text = getString(R.string.walking_warning_message_with_app, appName)
                } else {
                    // 기본 메시지
                    messageView.text = getString(R.string.walking_warning_message)
                }

                // 배경색 변경 - 앱별 색상 지정
                val backgroundColor = getColorForApp(appName)
                setBackgroundColor(ContextCompat.getColor(this@WalkingWarningService, backgroundColor))
            }

            // 윈도우에 뷰 추가
            windowManager?.addView(warningView, layoutParams)

            // 자동 제거 작업 시작
            autoRemoveJob = serviceScope.launch {
                delay(WARNING_DURATION)
                withContext(Dispatchers.Main) {
                    removeOverlay()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "경고창 표시 중 오류 발생", e)
            removeOverlay()
        }
    }



    private fun removeExistingOverlay() {
        try {
            if (warningView != null) {
                windowManager?.removeView(warningView)
                warningView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "기존 오버레이 제거 중 오류", e)
        }
    }

    private fun removeOverlay() {
        try {
            // 자동 제거 작업 취소
            autoRemoveJob?.cancel()

            // 뷰가 없으면 서비스 중지
            if (warningView == null) {
                stopSelf()
                return
            }

            // 윈도우 매니저에서 뷰 제거
            windowManager?.removeView(warningView)
            warningView = null

            // 서비스 중지
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "오버레이 제거 중 오류", e)
            warningView = null
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    // 앱 아이콘 리소스 ID 가져오기
    private fun getAppIconResource(appName: String): Int {
        return when {
            appName.contains("YouTube", ignoreCase = true) -> R.drawable.ic_youtube
            appName.contains("Facebook", ignoreCase = true) -> R.drawable.ic_facebook
            appName.contains("Instagram", ignoreCase = true) -> R.drawable.ic_instagram
            appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> R.drawable.ic_twitter
            appName.contains("TikTok", ignoreCase = true) -> R.drawable.ic_tiktok
            else -> 0 // 기본값은 아이콘 없음
        }
    }

    // 앱 이름에 따른 배경색 선택
    private fun getColorForApp(appName: String): Int {
        return when {
            appName.contains("Facebook", ignoreCase = true) -> R.color.facebook_color
            appName.contains("Instagram", ignoreCase = true) -> R.color.instagram_color
            appName.contains("YouTube", ignoreCase = true) -> R.color.youtube_color
            appName.contains("TikTok", ignoreCase = true) -> R.color.tiktok_color
            appName.contains("Twitter", ignoreCase = true) || appName.contains("X", ignoreCase = true) -> R.color.twitter_color
            else -> R.color.default_warning_color
        }
    }
}