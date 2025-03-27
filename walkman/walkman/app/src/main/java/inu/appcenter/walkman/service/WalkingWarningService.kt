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
        private const val WARNING_DURATION = 5000L // 5초 동안 표시
    }

    private var windowManager: WindowManager? = null
    private var warningView: View? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_WARNING) {
            showWarningOverlay()
        }

        return START_NOT_STICKY
    }

    private fun showWarningOverlay() {
        Log.d(TAG, "showWarningOverlay 호출됨")

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

                // 경고 메시지 설정 (기존 string resource 사용)
                findViewById<TextView>(R.id.tvWarningMessage).text = getString(R.string.walking_warning_message)
            }

            // 뷰 추가
            windowManager?.addView(warningView, layoutParams)
            Log.d(TAG, "경고 창 표시됨")

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

    private fun removeWarningOverlay() {
        try {
            warningView?.let { view ->
                windowManager?.removeView(view)
                warningView = null
                Log.d(TAG, "경고 창 제거됨")
            }
        } catch (e: Exception) {
            Log.e(TAG, "경고 창 제거 중 오류 발생: ${e.message}", e)
        } finally {
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