package inu.appcenter.walkman.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import inu.appcenter.walkman.data.repository.NotificationRepositoryImpl
import inu.appcenter.walkman.service.WalkingDetectorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 기기 부팅 완료 시 걷기 감지 서비스를 자동으로 시작하는 브로드캐스트 리시버
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed, checking notification settings")

            // 약간의 지연 후 설정 확인 (시스템 부팅 완료 확보)
            Thread.sleep(5000)

            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    // Hilt 사용이 불가능한 BroadcastReceiver에서는 수동으로 레포지토리 생성
                    val notificationRepo = NotificationRepositoryImpl(context)

                    // 현재 설정 상태 확인
                    val isEnabled = notificationRepo.isNotificationEnabled().first()

                    if (isEnabled) {
                        // 걷기 감지 서비스 시작
                        val serviceIntent = Intent(context, WalkingDetectorService::class.java)
                        serviceIntent.action = WalkingDetectorService.ACTION_START

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        Log.d(TAG, "Walking detector service started after boot (settings enabled)")
                    } else {
                        Log.d(TAG, "Walking detector service not started (settings disabled)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check settings after boot", e)
                }
            }
        }
    }
}