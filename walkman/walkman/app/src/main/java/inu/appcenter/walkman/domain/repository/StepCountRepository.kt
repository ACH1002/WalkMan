package inu.appcenter.walkman.domain.repository

import inu.appcenter.walkman.domain.model.StepCountData
import kotlinx.coroutines.flow.Flow

/**
 * 걸음 수 데이터 관리를 위한 레포지토리 인터페이스
 */
interface StepCountRepository {
    /**
     * 특정 날짜의 걸음 수 데이터 가져오기
     * @param date 밀리초 단위의 타임스탬프 (날짜의 시작 시간)
     * @return 해당 날짜의 걸음 수 데이터
     */
    suspend fun getStepsForDay(date: Long): StepCountData

    /**
     * 특정 날짜의 걸음 수 데이터 Flow로 관찰
     * @param date 밀리초 단위의 타임스탬프 (날짜의 시작 시간)
     * @return 해당 날짜의 걸음 수 데이터 Flow
     */
    fun observeStepsForDay(date: Long): Flow<StepCountData>

    /**
     * 지정된 기간의 걸음 수 데이터 가져오기
     * @param startDate 시작 날짜 (밀리초 단위의 타임스탬프)
     * @param endDate 종료 날짜 (밀리초 단위의 타임스탬프)
     * @return 해당 기간의 걸음 수 데이터 목록
     */
    suspend fun getStepsForRange(startDate: Long, endDate: Long): List<StepCountData>

    /**
     * 걸음 수 데이터 저장
     * @param date 날짜 (밀리초 단위의 타임스탬프)
     * @param steps 걸음 수
     * @param distance 이동 거리 (km)
     * @param calories 소모 칼로리 (kcal)
     */
    suspend fun saveStepData(date: Long, steps: Int, distance: Float, calories: Float)

    /**
     * 걸음 수 데이터 초기화 (특정 날짜)
     * @param date 초기화할 날짜 (밀리초 단위의 타임스탬프)
     */
    suspend fun resetStepsForDay(date: Long)

    /**
     * 걸음 수 보정 계수 설정
     * @param factor 보정 계수 (0.5~1.5 사이의 값)
     */
    suspend fun setCalibrationFactor(factor: Float)

    /**
     * 현재 설정된 걸음 수 보정 계수 가져오기
     * @return 보정 계수
     */
    fun getCalibrationFactor(): Float
}