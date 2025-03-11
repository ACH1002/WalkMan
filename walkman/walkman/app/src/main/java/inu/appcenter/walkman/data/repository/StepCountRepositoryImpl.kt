package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import inu.appcenter.walkman.domain.model.StepCountData
import inu.appcenter.walkman.domain.repository.StepCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepCountRepositoryImpl @Inject constructor(
    context: Context
) : StepCountRepository {

    companion object {
        private const val TAG = "StepCountRepositoryImpl"
        private const val PREFS_NAME = "step_count_prefs"
        private const val KEY_STEPS_PREFIX = "steps_"
        private const val KEY_DISTANCE_PREFIX = "distance_"
        private const val KEY_CALORIES_PREFIX = "calories_"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 메모리 캐시 - 날짜별 데이터 관찰을 위한 StateFlow 맵
    private val stepCountFlows = ConcurrentHashMap<Long, MutableStateFlow<StepCountData>>()

    override suspend fun getStepsForDay(date: Long): StepCountData {
        val dateKey = formatDateKey(date)

        val steps = prefs.getInt("${KEY_STEPS_PREFIX}$dateKey", 0)
        val distance = prefs.getFloat("${KEY_DISTANCE_PREFIX}$dateKey", 0f)
        val calories = prefs.getFloat("${KEY_CALORIES_PREFIX}$dateKey", 0f)

        return StepCountData(date, steps, distance, calories)
    }

    override fun observeStepsForDay(date: Long): Flow<StepCountData> {
        // 해당 날짜의 Flow가 없으면 새로 생성하고 초기값 설정
        val flow = stepCountFlows.getOrPut(date) {
            MutableStateFlow(StepCountData(date, 0, 0f, 0f))
        }

        // 초기값을 현재 저장된 값으로 업데이트
        updateFlowWithCurrentData(date)

        return flow
    }

    override suspend fun getStepsForRange(startDate: Long, endDate: Long): List<StepCountData> {
        val result = mutableListOf<StepCountData>()
        var currentDate = startDate

        // 시작일부터 종료일까지 하루씩 증가하며 데이터 조회
        while (currentDate <= endDate) {
            result.add(getStepsForDay(currentDate))

            // 다음 날로 이동
            currentDate = nextDay(currentDate)
        }

        return result
    }

    override suspend fun saveStepData(date: Long, steps: Int, distance: Float, calories: Float) {
        val dateKey = formatDateKey(date)

        prefs.edit().apply {
            putInt("${KEY_STEPS_PREFIX}$dateKey", steps)
            putFloat("${KEY_DISTANCE_PREFIX}$dateKey", distance)
            putFloat("${KEY_CALORIES_PREFIX}$dateKey", calories)
            apply()
        }

        // 메모리 캐시 업데이트
        updateStepCountFlow(date, steps, distance, calories)

        Log.d(TAG, "Saved step data for $dateKey: steps=$steps, distance=$distance, calories=$calories")
    }

    override suspend fun resetStepsForDay(date: Long) {
        saveStepData(date, 0, 0f, 0f)
        Log.d(TAG, "Reset step data for ${formatDateKey(date)}")
    }

    // 날짜를 YYYYMMDD 형식의 문자열로 변환
    private fun formatDateKey(date: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return String.format("%04d%02d%02d", year, month, day)
    }

    // 다음 날짜 계산
    private fun nextDay(date: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }

    // 현재 저장된 데이터로 Flow 업데이트
    private fun updateFlowWithCurrentData(date: Long) {
        val dateKey = formatDateKey(date)

        val steps = prefs.getInt("${KEY_STEPS_PREFIX}$dateKey", 0)
        val distance = prefs.getFloat("${KEY_DISTANCE_PREFIX}$dateKey", 0f)
        val calories = prefs.getFloat("${KEY_CALORIES_PREFIX}$dateKey", 0f)

        updateStepCountFlow(date, steps, distance, calories)
    }

    // StepCountData StateFlow 업데이트
    private fun updateStepCountFlow(date: Long, steps: Int, distance: Float, calories: Float) {
        stepCountFlows[date]?.value = StepCountData(date, steps, distance, calories)
    }
}