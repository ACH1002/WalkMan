package inu.appcenter.walkman.presentation.screen.gaitanalysis

import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.theme.WalkManTheme
import inu.appcenter.walkman.presentation.viewmodel.DailyGaitData
import inu.appcenter.walkman.presentation.viewmodel.GaitAnalysisUiState
import java.util.Calendar
import java.util.Date

/**
 * GaitAnalysisScreen의 Preview를 제공하는 클래스
 */
class GaitAnalysisPreviewProvider : PreviewParameterProvider<GaitAnalysisUiState> {
    override val values: Sequence<GaitAnalysisUiState> = sequenceOf(
        // 기본 데이터를 가진 상태
        GaitAnalysisUiState(
            isLoading = false,
            stabilityScore = 78,
            rhythmScore = 85,
            overallScore = 82,
            weeklyData = getMockWeeklyData(),
            lastUpdated = Calendar.getInstance().time,
            error = null
        ),
        // 로딩 중인 상태
        GaitAnalysisUiState(
            isLoading = true
        ),
        // 에러 상태
        GaitAnalysisUiState(
            isLoading = false,
            error = "데이터를 불러오는 데 실패했습니다."
        )
    )

    /**
     * 테스트용 주간 데이터 생성
     */
    private fun getMockWeeklyData(): List<DailyGaitData> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6) // 일주일 전부터 시작

        return List(7) { index ->
            val date = calendar.time
            val stability = 60 + (index * 5) // 점점 증가하는 패턴
            val rhythm = 65 + (index * 4)

            calendar.add(Calendar.DAY_OF_YEAR, 1) // 다음 날로 이동

            DailyGaitData(
                date = Date(date.time),
                stabilityScore = stability,
                rhythmScore = rhythm,
                overallScore = (stability + rhythm) / 2
            )
        }
    }
}

/**
 * GaitAnalysisScreen Preview
 */
@Preview(
    name = "Gait Analysis Screen",
    showBackground = true,
    showSystemUi = true
)
@Composable
fun GaitAnalysisScreenPreview(
    @PreviewParameter(GaitAnalysisPreviewProvider::class) uiState: GaitAnalysisUiState
) {
    WalkManTheme {
        GaitAnalysisScreenWithState(uiState = uiState)
    }
}

/**
 * GaitAnalysisScreen의 다양한 상태를 위한 래퍼 컴포저블
 */
@Composable
fun GaitAnalysisScreenWithState(uiState: GaitAnalysisUiState) {
    // 실제 ViewModel이 아닌 미리 정의된 uiState를 사용하는 Screen 구현
    // GaitAnalysisScreen을 수정하여 ViewModel 대신 uiState를 직접 받도록 리팩토링하거나
    // 이 곳에서 UI 로직을 복제할 수 있음

    // 여기서는 기존 GaitAnalysisScreen을 직접 호출
    // 실제 통합 시에는 ViewModel의 상태를 넘기도록 수정 필요
    GaitAnalysisScreen()
}

/**
 * 각 UI 요소에 대한 개별 Preview들
 */
@Preview(showBackground = true)
@Composable
fun GaitScoreCardPreview() {
    WalkManTheme {
        GaitScoreCard(
            title = "보행 안정성",
            score = 78,
            animatedScore = 0.78f,
            description = "보행 중 흔들림이 적고 균형감이 우수합니다.",
            iconContent = {
                androidx.compose.material.icons.Icons.Default.DirectionsWalk
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImprovementItemPreview() {
    WalkManTheme {
        ImprovementItem(
            title = "보행 안정성 향상",
            description = "발 뒤꿈치부터 지면에 닿도록 하고, 상체를 똑바로 유지하세요."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LegendItemPreview() {
    WalkManTheme {
        LegendItem(color = WalkManColors.Primary, text = "안정성")
    }
}