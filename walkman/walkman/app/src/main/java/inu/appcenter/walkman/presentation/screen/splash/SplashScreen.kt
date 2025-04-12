package inu.appcenter.walkman.presentation.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * 스플래시 화면
 * 앱 시작 시 표시되며, 인증 상태를 확인하는 동안 로딩 인디케이터 표시
 */
@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    // 인증 상태 확인
    LaunchedEffect(Unit) {
        // 스플래시 화면을 잠깐 표시하기 위한 딜레이
        delay(1500)
        // 인증 상태 확인 로직 실행
        viewModel.checkAuthState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WalkManColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize()
        ) {
            // 앱 로고
            Image(
                painter = painterResource(id = R.drawable.ic_logo_gaitx),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 앱 이름
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = WalkManColors.Primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 로딩 인디케이터
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = WalkManColors.Primary,
                strokeWidth = 3.dp
            )
        }
    }
}