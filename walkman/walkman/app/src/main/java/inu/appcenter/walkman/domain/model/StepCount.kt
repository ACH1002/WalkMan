package inu.appcenter.walkman.domain.model

data class StepCountData(
    val date: Long,
    val steps: Int,
    val distance: Float,
    val calories: Float
)