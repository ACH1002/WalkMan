package inu.appcenter.walkman.presentation.screen.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            imageId = R.drawable.ic_logo_gaitx,
            title = stringResource(id = R.string.onboarding_title_1),
            description = stringResource(id = R.string.onboarding_desc_1)
        ),
        OnboardingPage(
            imageId = R.drawable.ic_logo_gaitx,
            title = stringResource(id = R.string.onboarding_title_2),
            description = stringResource(id = R.string.onboarding_desc_2)
        ),
        OnboardingPage(
            imageId = R.drawable.ic_logo_gaitx,
            title = stringResource(id = R.string.onboarding_title_3),
            description = stringResource(id = R.string.onboarding_desc_3)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalkManColors.Background)
    ) {
        // 페이저
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { position ->
            OnboardingPage(
                page = pages[position],
                pageCount = pages.size,
                position = position,
                onNextPage = {
                    if (position < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(position + 1)
                        }
                    }
                },
                onGetStarted = onGetStarted
            )
        }

        // 인디케이터
        Row(
            Modifier
                .height(48.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) {
                    WalkManColors.Primary
                } else {
                    Color.LightGray
                }
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPage,
    pageCount: Int,
    position: Int,
    onNextPage: () -> Unit,
    onGetStarted: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 이미지
        Image(
            painter = painterResource(id = page.imageId),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 제목
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = WalkManColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 설명
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = WalkManColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 버튼
        Button(
            onClick = if (position == pageCount - 1) onGetStarted else onNextPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WalkManColors.Primary
            )
        ) {
            Text(
                text = if (position == pageCount - 1) {
                    stringResource(id = R.string.get_started)
                } else {
                    stringResource(id = R.string.next)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

data class OnboardingPage(
    val imageId: Int,
    val title: String,
    val description: String
)