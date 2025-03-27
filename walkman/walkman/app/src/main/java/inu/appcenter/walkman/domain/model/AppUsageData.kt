package inu.appcenter.walkman.domain.model

data class AppUsageData(
    val packageName: String,    // 앱 패키지 이름
    val appName: String,        // 앱 표시 이름
    val usageDurationMs: Long,  // 사용 시간(밀리초)
    val lastUsedTimestamp: Long // 마지막 사용 시간
)
