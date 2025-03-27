package inu.appcenter.walkman.presentation.screen.recordingmodes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.UserInfo
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun UserInfoCard(
    userInfo: UserInfo?,
    onEditClick: () -> Unit
) {
    // 현재 언어에 맞게 성별 텍스트 변환
    val genderText = when(userInfo?.gender) {
        stringResource(id = R.string.gender_male) -> stringResource(id = R.string.gender_male)
        stringResource(id = R.string.gender_female) -> stringResource(id = R.string.gender_female)
        "남성" -> stringResource(id = R.string.gender_male)
        "여성" -> stringResource(id = R.string.gender_female)
        "Male" -> stringResource(id = R.string.gender_male)
        "Female" -> stringResource(id = R.string.gender_female)
        else -> userInfo?.gender ?: ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = WalkManColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 사용자 아이콘
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = stringResource(id = R.string.profile),
                tint = WalkManColors.Primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 사용자 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = userInfo?.name ?: stringResource(id = R.string.default_user),
                    style = MaterialTheme.typography.titleMedium,
                    color = WalkManColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Wrap을 구현하는 FlowRow 대신 텍스트를 개별적으로 나열
                // 첫 줄: 성별
                if (!userInfo?.gender.isNullOrBlank()) {
                    Text(
                        text = genderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WalkManColors.TextSecondary
                    )
                }

                // 두 번째 줄: 키와 몸무게 나란히
                Row(
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (!userInfo?.height.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.profile_height, userInfo?.height ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkManColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!userInfo?.weight.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.profile_weight, userInfo?.weight ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WalkManColors.TextSecondary
                        )
                    }
                }
            }

            // 편집 버튼
            IconButton(
                onClick = onEditClick
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_profile),
                    tint = WalkManColors.Primary
                )
            }
        }
    }
}