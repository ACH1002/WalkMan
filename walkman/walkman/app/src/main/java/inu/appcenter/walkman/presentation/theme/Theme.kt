package inu.appcenter.walkman.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 앱에서 사용할 컬러 정의
private val Purple = Color(0xFF6200EE)
private val PurpleDark = Color(0xFF5000CA)
private val PurpleLight = Color(0xFFE3D0FF)
private val LightBackground = Color.White
private val TextColorPrimary = Color(0xFF212121)
private val TextColorSecondary = Color(0xFF757575)

// 라이트 컬러 스킴 정의
private val LightColorScheme = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = PurpleLight,
    onSecondary = TextColorPrimary,
    background = LightBackground,
    surface = Color(0xFFF5F5F5),
    onBackground = TextColorPrimary,
    onSurface = TextColorPrimary,
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun WalkManTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    hideNavigationBar: Boolean = false, // 새로 추가된 매개변수
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 상태바 색상 설정 - 투명
            window.statusBarColor = Color.Transparent.toArgb()

            // 시스템 바에 표시되는 아이콘 색상 설정
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme

            // 내비게이션 바 관련 설정
            if (hideNavigationBar) {
                // 네비게이션 바를 숨기는 추가 설정
                window.navigationBarColor = Color.Transparent.toArgb()
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// 컬러 상수들을 외부에서 접근할 수 있도록 공개
object WalkManColors {
    val Primary = Purple
    val PrimaryDark = PurpleDark
    val PrimaryLight = PurpleLight
    val Background = LightBackground
    val TextPrimary = TextColorPrimary
    val TextSecondary = TextColorSecondary
    val Surface = Color(0xFFF5F5F5)
    val CardBackground = Color(0xFFF9F9F9)
    val Divider = Color(0xFFEEEEEE)
    val Success = Color(0xFF4CAF50)
    val Error = Color(0xFFB00020)
}