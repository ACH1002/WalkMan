// WalkingWarningService.kt 파일을 다음과 같이 수정합니다

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
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 걷는 중 소셜미디어 사용 시 경고 오버레이를 표시하는 서비스
 */
@AndroidEntryPoint
class WalkingWarningService : Service() {

    companion object {
        private const val TAG = "WalkingWarningService"
        const val ACTION_SHOW_WARNING = "SHOW_WARNING"
        const val EXTRA_APP_NAME = "app_name"
        private const val WARNING_DURATION = 5000L // 5초 동안 표시
    }

    private var windowManager: WindowManager? = null
    private var warningView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_WARNING) {
            val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: ""
            showWarningOverlay(appName)
        }

        return START_NOT_STICKY
    }

    private fun showWarningOverlay(appName: String = "") {
        Log.d(TAG, "showWarningOverlay 호출됨 - 앱: $appName")

        if (warningView != null) {
            Log.d(TAG, "경고 창이 이미 표시 중입니다")
            return // 이미 표시 중이면 무시
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
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
                    removeWarningOverlay()
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

                // 배경색 변경 - 앱별 색상 지정 가능
                val backgroundColor = getColorForApp(appName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setBackgroundColor(getColor(backgroundColor))
                } else {
                    @Suppress("DEPRECATION")
                    setBackgroundColor(resources.getColor(backgroundColor))
                }
            }

            // 뷰 추가
            windowManager?.addView(warningView, layoutParams)
            Log.d(TAG, "경고 창 표시됨")

            // 애니메이션 효과 적용
            val slideIn = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_top)
            warningView?.startAnimation(slideIn)

            // 5초 후 자동 제거
            serviceScope.launch {
                delay(WARNING_DURATION)
                if (warningView != null) {
                    removeWarningOverlay()
                    Log.d(TAG, "5초 후 경고 창 자동 제거됨")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "경고 창 표시 중 오류 발생: ${e.message}", e)
            removeWarningOverlay()
        }
    }

    // 앱 이름에 따른 배경색 선택
    private fun getColorForApp(appName: String): Int {
        return when {
            appName.contains("Facebook") -> R.color.facebook_color
            appName.contains("Instagram") -> R.color.instagram_color
            appName.contains("YouTube") -> R.color.youtube_color
            appName.contains("TikTok") -> R.color.tiktok_color
            appName.contains("Twitter") || appName.contains("X") -> R.color.twitter_color
            else -> R.color.default_warning_color
        }
    }

    private fun removeWarningOverlay() {
        try {
            warningView?.let { view ->
                // 애니메이션 효과 적용 후 제거
                val slideOut = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_out_top)
                slideOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        try {
                            windowManager?.removeView(view)
                            warningView = null
                            Log.d(TAG, "경고 창 제거됨")
                            stopSelf()
                        } catch (e: Exception) {
                            Log.e(TAG, "애니메이션 후 경고 창 제거 중 오류 발생: ${e.message}", e)
                            stopSelf()
                        }
                    }

                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                view.startAnimation(slideOut)
            } ?: stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "경고 창 제거 중 오류 발생: ${e.message}", e)
            warningView = null
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeWarningOverlay()
    }
}