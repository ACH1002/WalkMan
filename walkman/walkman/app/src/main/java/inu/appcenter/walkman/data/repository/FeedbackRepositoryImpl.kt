// data/repository/FeedbackRepositoryImpl.kt
package inu.appcenter.walkman.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import inu.appcenter.walkman.domain.repository.FeedbackRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자에게 피드백을 제공하는 Repository 구현
 */
@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FeedbackRepository {

    companion object {
        private const val TAG = "FeedbackRepository"
    }

    /**
     * 진동 패턴으로 피드백 제공
     * @param pattern 진동 패턴 (밀리초 단위)
     */
    override fun vibrate(pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "진동 피드백 제공 실패", e)
        }
    }

    /**
     * 소리로 피드백 제공
     * @param soundResourceId 소리 리소스 ID
     */

}