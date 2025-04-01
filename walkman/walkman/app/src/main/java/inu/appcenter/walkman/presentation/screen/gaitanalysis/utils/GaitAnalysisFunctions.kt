package inu.appcenter.walkman.presentation.screen.gaitanalysis.utils

object GaitAnalysisFunctions {
    /**
     * 안정성 점수에 따른 설명 텍스트
     */
    fun getStabilityDescription(score: Int): String {
        return when {
            score >= 90 -> "보행 중 흔들림이 거의 없고 균형감이 매우 우수합니다."
            score >= 80 -> "보행 중 흔들림이 적고 균형감이 우수합니다."
            score >= 70 -> "전반적으로 안정적인 보행 패턴을 보입니다."
            score >= 60 -> "약간의 불안정함이 있으나 양호한 수준입니다."
            score >= 50 -> "보행 시 불안정함이 감지됩니다."
            else -> "보행 안정성이 부족합니다. 개선이 필요합니다."
        }
    }

    /**
     * 리듬성 점수에 따른 설명 텍스트
     */
    fun getRhythmDescription(score: Int): String {
        return when {
            score >= 90 -> "매우 일정한 보폭과 속도로 걷는 능력이 탁월합니다."
            score >= 80 -> "일정한 보폭과 속도로 걷는 능력이 우수합니다."
            score >= 70 -> "보행 리듬이 대체로 일정합니다."
            score >= 60 -> "약간의 불규칙함이 있으나 양호한 리듬감입니다."
            score >= 50 -> "보행 리듬에 불규칙함이 감지됩니다."
            else -> "보행 리듬이 불규칙합니다. 개선이 필요합니다."
        }
    }

    /**
     * 점수에 따른 개선 제안 목록
     */
    fun getImprovementSuggestions(stabilityScore: Int, rhythmScore: Int): List<Pair<String, String>> {
        val suggestions = mutableListOf<Pair<String, String>>()

        // 안정성 관련 제안
        if (stabilityScore < 70) {
            suggestions.add(
                Pair(
                    "보행 안정성 향상",
                    "발 뒤꿈치부터 지면에 닿도록 하고, 상체를 똑바로 유지하세요. 균형 운동을 통해 안정성을 키울 수 있습니다."
                )
            )
        }

        // 리듬성 관련 제안
        if (rhythmScore < 70) {
            suggestions.add(
                Pair(
                    "보행 리듬 개선",
                    "일정한 속도로 걷기 위해 메트로놈 앱을 사용해보세요. 규칙적인 걸음걸이를 연습하면 리듬감이 향상됩니다."
                )
            )
        }

        // 데이터 수집 관련 제안
        suggestions.add(
            Pair(
                "더 정확한 분석을 위해",
                "더 정확한 분석을 위해 주 3회 이상 VIDEO 모드에서 걷기 데이터를 수집하세요."
            )
        )

        return suggestions
    }
}