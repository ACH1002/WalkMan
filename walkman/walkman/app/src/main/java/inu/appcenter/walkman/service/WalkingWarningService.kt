package inu.appcenter.walkman.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import dagger.hilt.android.AndroidEntryPoint
import inu.appcenter.walkman.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WalkingWarningService : Service() {

    companion object {
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
        if (warningView != null) return // 이미 표시 중이면 무시

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

        warningView = LayoutInflater.from(this).inflate(R.layout.overlay_walking_warning, null).apply {
            findViewById<Button>(R.id.btnDismiss).setOnClickListener {
                removeWarningOverlay()
            }
        }

        windowManager?.addView(warningView, layoutParams)

        // 5초 후 자동 제거
        serviceScope.launch {
            delay(WARNING_DURATION)
            removeWarningOverlay()
        }
    }

    private fun removeWarningOverlay() {
        warningView?.let { view ->
            windowManager?.removeView(view)
            warningView = null
        }

        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeWarningOverlay()
    }
}