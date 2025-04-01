package inu.appcenter.walkman.util

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * 센서 데이터로 부터 보행 분석을 위한 유틸리티 클래스
 */
object GaitAnalysisUtils {

    // 보행 안정성 계산 함수
    fun calculateGaitStability(accelerationData: Array<DoubleArray>): Map<String, Double> {
        // 데이터 유효성 검사
        if (accelerationData.isEmpty() || accelerationData[0].size != 3) {
            throw IllegalArgumentException("가속도 데이터는 (n, 3) 형태여야 합니다.")
        }

        return try {
            // 신호의 크기 계산 (가속도의 크기)
            val magnitude = DoubleArray(accelerationData.size) { i ->
                sqrt(accelerationData[i][0].pow(2) + accelerationData[i][1].pow(2) + accelerationData[i][2].pow(2))
            }

            // 1. 전체적인 움직임의 변동성 (Coefficient of Variation)
            val magnitudeMean = magnitude.average()
            val magnitudeStd = magnitude.standardDeviation()
            val movementVariability = magnitudeStd / magnitudeMean

            // 2. 좌우 흔들림 (Y축 방향의 표준편차)
            val yValues = DoubleArray(accelerationData.size) { i -> accelerationData[i][1] }
            val lateralStability = yValues.standardDeviation()

            // 3. 수직 안정성 (Z축 방향의 표준편차)
            val zValues = DoubleArray(accelerationData.size) { i -> accelerationData[i][2] }
            val verticalStability = zValues.standardDeviation()

            // 4. 신호의 평활도 (Smoothness) - 연속된 샘플 간의 차이
            val magnitudeDiffs = DoubleArray(magnitude.size - 1) { i -> abs(magnitude[i+1] - magnitude[i]) }
            val smoothness = magnitudeDiffs.average()

            // 5. 보행 대칭성 (좌우 움직임의 대칭성)
            val symmetry = yValues.map { abs(it) }.average()

            // 각 지표를 0-1 범위로 정규화
            val normMovementVar = min(movementVariability / 2.0, 1.0)
            val normLateralStab = min(lateralStability / 2.0, 1.0)
            val normSmoothness = min(smoothness, 1.0)
            val normSymmetry = min(symmetry, 1.0)

            // 종합 안정성 점수 계산 (0-100 scale)
            val totalScore = 100 * (1 - listOf(
                normMovementVar,
                normLateralStab,
                normSmoothness,
                normSymmetry
            ).average())

            mapOf(
                "stability_score" to max(0.0, min(100.0, totalScore.round(2))),
                "movement_variability" to movementVariability.round(4),
                "lateral_stability" to lateralStability.round(4),
                "vertical_stability" to verticalStability.round(4),
                "smoothness" to smoothness.round(4),
                "symmetry" to symmetry.round(4)
            )

        } catch (e: Exception) {
            println("계산 중 오류 발생: ${e.message}")
            mapOf(
                "stability_score" to 0.0,
                "movement_variability" to 0.0,
                "lateral_stability" to 0.0,
                "vertical_stability" to 0.0,
                "smoothness" to 0.0,
                "symmetry" to 0.0
            )
        }
    }

    // 보행 리듬 계산 함수
    fun calculateGaitRhythm(accelerationData: Array<DoubleArray>, samplingRate: Double = 100.0): Map<String, Double> {
        return try {
            // Z축 가속도 데이터 사용(스마트폰을 보고 있는 경우 Z축이 지면과 수직 방향에 가까움)
            val verticalAcc = DoubleArray(accelerationData.size) { i -> accelerationData[i][2] }
            println("수직 가속도 데이터 범위: ${verticalAcc.minOrNull()?.round(3)} ~ ${verticalAcc.maxOrNull()?.round(3)}")

            // 피크 검출을 위한 데이터 전처리(정규화)
            val verticalAccMean = verticalAcc.average()
            val verticalAccStd = verticalAcc.standardDeviation()
            val normalizedAcc = DoubleArray(verticalAcc.size) { i ->
                (verticalAcc[i] - verticalAccMean) / verticalAccStd
            }

            // 피크(걸음) 검출
            val peaks = findPeaks(
                normalizedAcc,
                height = 0.3,
                minDistance = (samplingRate * 0.3).toInt(),
                prominence = 0.2,
                width = 5
            )

            println("검출된 피크 수: ${peaks.size}")

            if (peaks.size < 2) {
                throw IllegalArgumentException("충분한 걸음 데이터가 없습니다. 검출된 피크: ${peaks.size}개")
            }

            // 걸음 간 시간 간격 계산 (초 단위)
            val stepIntervals = DoubleArray(peaks.size - 1) { i ->
                (peaks[i + 1] - peaks[i]) / samplingRate
            }

            // 이상치 제거
            val meanInterval = stepIntervals.average()
            val stdInterval = stepIntervals.standardDeviation()
            val validIntervals = stepIntervals.filter {
                it > meanInterval - 2.0 * stdInterval && it < meanInterval + 2.0 * stdInterval
            }.toDoubleArray()

            println("걸음 간 시간 간격: 평균 ${validIntervals.average().round(3)}초, 표준편차 ${validIntervals.standardDeviation().round(3)}초")

            // 보행 주기 계산
            val meanStrideTime = validIntervals.average()
            // 보행 주기 변동 계수
            val strideTimeVariability = validIntervals.standardDeviation() / meanStrideTime * 100
            println("보행 주기 변동계수: ${strideTimeVariability.round(2)}%")

            // 리듬 점수 계산
            val maxAcceptableCv = 50.0  // 최대 허용 변동계수
            val cvRatio = strideTimeVariability / maxAcceptableCv
            // 보행 주기 변동 계수가 낮을수록 높은 점수
            val rhythmScore = 100 * exp(-cvRatio)  // 100점 만점으로 환산
            println("규칙성 점수 계산: ${rhythmScore.round(2)} = 100 * exp(-${cvRatio.round(2)})")

            // 연속성 지표 계산 (이상치 제거 후)
            val consecutiveDiffs = DoubleArray(validIntervals.size - 1) { i ->
                abs(validIntervals[i + 1] - validIntervals[i])
            }
            val strideConsistency = if (consecutiveDiffs.isNotEmpty()) consecutiveDiffs.average() else 0.0

            // 분당 걸음 수 계산
            val cadence = 60 / meanStrideTime

            mapOf(
                "rhythm_score" to rhythmScore.round(2),
                "mean_stride_time" to meanStrideTime.round(3),
                "stride_time_variability" to strideTimeVariability.round(2),
                "cadence" to cadence.round(1),
                "stride_consistency" to strideConsistency.round(4),
                "step_count" to peaks.size.toDouble(),
                "valid_step_count" to (validIntervals.size + 1).toDouble()
            )

        } catch (e: Exception) {
            println("리듬 계산 중 오류 발생: ${e.message}")
            mapOf(
                "rhythm_score" to 0.0,
                "mean_stride_time" to 0.0,
                "stride_time_variability" to 0.0,
                "cadence" to 0.0,
                "stride_consistency" to 0.0,
                "step_count" to 0.0,
                "valid_step_count" to 0.0
            )
        }
    }

    // 피크 탐지 함수 (Python의 find_peaks 함수를 단순화해서 구현)
    fun findPeaks(
        signal: DoubleArray,
        height: Double = 0.0,
        minDistance: Int = 1,
        prominence: Double = 0.0,
        width: Int = 1
    ): IntArray {
        val peaks = mutableListOf<Int>()

        // 간단한 피크 탐지 알고리즘
        for (i in 1 until signal.size - 1) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1] && signal[i] >= height) {
                // 기본 조건을 만족하는 피크 후보
                peaks.add(i)
            }
        }

        // 최소 거리 적용 (간단한 구현)
        val filteredPeaks = mutableListOf<Int>()
        var lastPeakIndex = -minDistance

        for (peak in peaks) {
            if (peak - lastPeakIndex >= minDistance) {
                filteredPeaks.add(peak)
                lastPeakIndex = peak
            } else if (signal[peak] > signal[lastPeakIndex]) {
                // 더 높은 피크로 대체
                if (filteredPeaks.isNotEmpty()) {
                    filteredPeaks.removeAt(filteredPeaks.size - 1)
                }
                filteredPeaks.add(peak)
                lastPeakIndex = peak
            }
        }

        return filteredPeaks.toIntArray()
    }

    // 표준편차 확장 함수
    fun DoubleArray.standardDeviation(): Double {
        if (this.isEmpty()) return 0.0
        val mean = this.average()
        val variance = this.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    // 반올림 확장 함수
    fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    /**
     * SensorReading 리스트를 Array<DoubleArray>로 변환
     */
    fun convertSensorReadingsToAccelerationData(readings: List<inu.appcenter.walkman.domain.model.SensorReading>): Array<DoubleArray> {
        return Array(readings.size) { i ->
            val reading = readings[i]
            doubleArrayOf(reading.accX.toDouble(), reading.accY.toDouble(), reading.accZ.toDouble())
        }
    }

    /**
     * 전체 보행 점수 계산 (안정성과 리듬성의 평균)
     */
    fun calculateOverallGaitScore(stabilityScore: Double, rhythmScore: Double): Int {
        return ((stabilityScore + rhythmScore) / 2).toInt()
    }
}