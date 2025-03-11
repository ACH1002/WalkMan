package inu.appcenter.walkman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import inu.appcenter.walkman.service.StepCounterService

/**
 * 기기 부팅 완료 시 걸음 수 측정 서비스를 자동으로 시작하는 브로드캐스트 리시버
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, starting step counter service")

            // 약간의 지연 후 서비스 시작 (시스템 부팅 완료 확보)
            Thread.sleep(5000)

            // StepCounterService 시작
            val serviceIntent = Intent(context, StepCounterService::class.java)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "Step counter service started after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service after boot", e)
            }
        }
    }
}