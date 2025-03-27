package inu.appcenter.walkman.presentation.screen.mypage.settings.language

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.util.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    var selectedLanguage by remember { mutableStateOf(languageManager.getLanguageCode()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_language),
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back),
                            tint = WalkManColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                )
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 언어 선택 라디오 버튼
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 영어 선택 옵션
                    LanguageOption(
                        languageCode = LanguageManager.LANGUAGE_ENGLISH,
                        languageName = stringResource(id = R.string.language_en),
                        isSelected = selectedLanguage == LanguageManager.LANGUAGE_ENGLISH,
                        onSelect = { code ->
                            selectedLanguage = code
                            languageManager.setLanguage(code)
                            // 언어 변경 후 앱 재시작 필요
                            languageManager.restartApp(context)
                        }
                    )

                    Divider(
                        color = WalkManColors.Divider,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 한국어 선택 옵션
                    LanguageOption(
                        languageCode = LanguageManager.LANGUAGE_KOREAN,
                        languageName = stringResource(id = R.string.language_ko),
                        isSelected = selectedLanguage == LanguageManager.LANGUAGE_KOREAN,
                        onSelect = { code ->
                            selectedLanguage = code
                            languageManager.setLanguage(code)
                            // 언어 변경 후 앱 재시작 필요
                            languageManager.restartApp(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.language_change_notice),
                style = MaterialTheme.typography.bodySmall,
                color = WalkManColors.TextSecondary
            )
        }
    }
}

