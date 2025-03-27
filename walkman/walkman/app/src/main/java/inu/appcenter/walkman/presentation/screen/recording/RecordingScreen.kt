package inu.appcenter.walkman.presentation.screen.recording

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.RecordingMode
import inu.appcenter.walkman.presentation.screen.recording.components.SensorValueRow
import inu.appcenter.walkman.presentation.theme.WalkManColors
import inu.appcenter.walkman.presentation.viewmodel.RecordingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel,
    mode: RecordingMode,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentReading by viewModel.currentReading.collectAsState()
    val context = LocalContext.current

    // 스크롤 상태 추가
    val scrollState = rememberScrollState()

    var inputText by remember { mutableStateOf("") }
    var recordingTimeElapsed by remember { mutableStateOf(0) }
    val recordingTimeMax = 90 // 최대 녹화 시간(초)

    // 측정이 90초 동안 진행되었는지 추적
    var fullMeasurementCompleted by remember { mutableStateOf(false) }

    // 중간에 측정 중단했는지 추적
    var measurementCancelled by remember { mutableStateOf(false) }

    // 센서 데이터 포맷팅 캐싱
    val sensorDataFormatted = remember(currentReading) {
        currentReading?.let { reading ->
            SensorDataFormatted(
                accX = String.format("%.2f", reading.accX),
                accY = String.format("%.2f", reading.accY),
                accZ = String.format("%.2f", reading.accZ),
                gyroX = String.format("%.2f", reading.gyroX),
                gyroY = String.format("%.2f", reading.gyroY),
                gyroZ = String.format("%.2f", reading.gyroZ)
            )
        }
    }

    // 비디오 플레이어 상태
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // 코루틴 스코프 선언 위치 이동
    val scope = rememberCoroutineScope()

    // 진동 함수
    val vibratePhone = {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 측정 완료 진동 패턴: 500ms 진동, 300ms 대기, 500ms 진동
            val vibrationEffect = VibrationEffect.createWaveform(
                longArrayOf(0, 500, 300, 500), -1
            )
            vibrator.vibrate(vibrationEffect)
        } else {
            // API 26 미만에서는 deprecated API 사용
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 300, 500), -1)
        }
    }

    // 사용자 정의 StopRecording 함수
    // 이 함수는 측정 상태에 따라 데이터를 저장할지 결정
    val customStopRecording = { shouldUpload: Boolean ->
        if (uiState.isRecording) {
            if (shouldUpload) {
                // 90초 측정 완료: 진동 피드백 실행
                vibratePhone()

                // 진동이 완료될 시간을 확보하기 위해 코루틴 내에서 처리
                scope.launch {
                    // 진동이 충분히 울릴 수 있는 시간 확보 (대략 1.5초)
                    delay(1500)

                    // 실제 stopRecording 호출 (업로드 시작)
                    viewModel.stopRecording()

                    // 이제 화면 이동은 성공 메시지가 표시될 때만 수행
                    // (LaunchedEffect(uiState.successMessage)에서 처리됨)
                }
            } else {
                // 업로드하지 않고 중단
                scope.launch {
                    viewModel.cancelRecording()
                    measurementCancelled = true
                }
            }

            // 녹화 중지 시 영상도 일시정지
            if (mode == RecordingMode.VIDEO) {
                exoPlayer?.pause()
            }
        }
    }

    // 뒤로 가기 핸들러
    val handleBackPress: () -> Unit = {
        // 녹화 중이면 중단 (false = 업로드 하지 않음)
        if (uiState.isRecording) {
            customStopRecording(false)
        }

        // 비디오 재생 중지 및 해제
        exoPlayer?.pause()

        // 뒤로 가기 실행
        onNavigateBack()
    }

    // 녹화 시간 카운터
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            // 측정 시작 시 상태 초기화
            recordingTimeElapsed = 0
            fullMeasurementCompleted = false
            measurementCancelled = false

            while (recordingTimeElapsed < recordingTimeMax && uiState.isRecording) {
                delay(1000) // 1초 대기
                recordingTimeElapsed++
            }

            // 시간이 다 되면 자동 중지 및 업로드
            if (recordingTimeElapsed >= recordingTimeMax && uiState.isRecording) {
                fullMeasurementCompleted = true
                customStopRecording(true) // true = 업로드 실행, 진동 후 업로드 완료 시 이동
            }
        }
    }

    // 수동으로 측정을 중지했을 때 처리
    val handleStopRecording = {
        if (uiState.isRecording) {
            if (recordingTimeElapsed >= recordingTimeMax) {
                // 90초 이상 측정 완료: 업로드 실행
                fullMeasurementCompleted = true
                customStopRecording(true)
            } else {
                // 중간에 중단: 업로드 하지 않음
                measurementCancelled = true
                customStopRecording(false)
            }
        }
    }

    // 성공 메시지 감지를 위한 효과
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            // 성공 메시지가 표시되면 - 데이터 업로드가 완료된 상태
            // 사용자가 성공 메시지를 볼 수 있도록 충분한 시간 제공
            delay(2000) // 2초 동안 성공 메시지 표시
            viewModel.clearMessages()
            onNavigateBack() // 데이터 업로드 완료 후 RecordModeScreen으로 이동
        }
    }

    // 오류 메시지 처리
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            delay(3000) // 오류 메시지는 3초 후 사라짐
            viewModel.clearMessages()
        }
    }

    // 비디오 플레이어 초기화
    LaunchedEffect(Unit) {
        if (mode == RecordingMode.VIDEO) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val videoUri = Uri.parse("android.resource://${context.packageName}/raw/documentary_sample")
                val mediaItem = MediaItem.fromUri(videoUri)
                setMediaItem(mediaItem)
                prepare()
                repeatMode = Player.REPEAT_MODE_ALL // 반복 재생
                volume = 0f // 볼륨 설정
            }
        }
    }

    // 컴포넌트가 dispose될 때 ExoPlayer 자원 해제
    DisposableEffect(Unit) {
        onDispose {
            // 녹화 중이면 중단 (업로드 하지 않음)
            if (uiState.isRecording) {
                customStopRecording(false)
            }
            // ExoPlayer 해제
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when(mode) {
                                RecordingMode.POCKET -> stringResource(id = R.string.recording_pocket_mode)
                                RecordingMode.VIDEO -> stringResource(id = R.string.recording_video_mode)
                                RecordingMode.TEXT -> stringResource(id = R.string.recording_text_mode)
                            },
                            color = WalkManColors.Primary,
                            fontWeight = FontWeight.Bold,
                            // 타이틀 텍스트 크기 줄이기
                            fontSize = 18.sp,
                            modifier = Modifier,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = handleBackPress,
                            // 아이콘 버튼 크기 줄이기
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.btn_back),
                                tint = WalkManColors.TextPrimary,
                                // 아이콘 크기 줄이기
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WalkManColors.Background
                ),
                // 커스텀 수정자를 통해 높이 줄이기
                modifier = Modifier.height(70.dp), // 기본값은 일반적으로 64dp
                // 컨텐츠 패딩 조정
                windowInsets = WindowInsets(0,10,0,0)
            )
        },
        containerColor = WalkManColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 모드별 콘텐츠
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when(mode) {
                        RecordingMode.VIDEO -> {
                            // 다큐멘터리 비디오 플레이어
                            if (exoPlayer != null) {
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    factory = { ctx ->
                                        StyledPlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = true // 재생 컨트롤러 표시
                                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.video_loading),
                                        color = WalkManColors.TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(id = R.string.recording_video_desc),
                                color = WalkManColors.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 주의사항
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WalkManColors.Primary.copy(alpha = 0.1f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(id = R.string.video_recording_caution),
                                    color = WalkManColors.Primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        RecordingMode.POCKET -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = stringResource(id = R.string.person_icon),
                                        tint = WalkManColors.TextSecondary,
                                        modifier = Modifier.size(64.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(id = R.string.recording_pocket_desc),
                                        color = WalkManColors.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        RecordingMode.TEXT -> {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text(stringResource(id = R.string.text_input_label), color = WalkManColors.TextSecondary) },
                                placeholder = { Text(stringResource(id = R.string.text_input_placeholder), color = WalkManColors.TextSecondary) },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    unfocusedTextColor = WalkManColors.TextPrimary,
                                    focusedTextColor = WalkManColors.TextPrimary,
                                    cursorColor = WalkManColors.Primary,
                                    focusedBorderColor = WalkManColors.Primary,
                                    unfocusedBorderColor = WalkManColors.TextSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                maxLines = 8
                            )
                        }
                    }
                }
            }

            // 센서 데이터 표시 (디버깅용, 실제 앱에서는 숨김 가능)
            if (currentReading != null && uiState.isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            stringResource(id = R.string.sensor_data),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = WalkManColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 고정된 너비로 센서 데이터 표시하여 떨림 방지
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 가속도계 열
                            Column(modifier = Modifier.width(160.dp)) {
                                Text(stringResource(id = R.string.accelerometer), fontWeight = FontWeight.Bold, color = WalkManColors.TextPrimary)
                                SensorValueRow("X:", sensorDataFormatted?.accX ?: "0.00", "m/s²")
                                SensorValueRow("Y:", sensorDataFormatted?.accY ?: "0.00", "m/s²")
                                SensorValueRow("Z:", sensorDataFormatted?.accZ ?: "0.00", "m/s²")
                            }

                            // 자이로스코프 열
                            Column(modifier = Modifier.width(160.dp)) {
                                Text(stringResource(id = R.string.gyroscope), fontWeight = FontWeight.Bold, color = WalkManColors.TextPrimary)
                                SensorValueRow("X:", sensorDataFormatted?.gyroX ?: "0.00", "rad/s")
                                SensorValueRow("Y:", sensorDataFormatted?.gyroY ?: "0.00", "rad/s")
                                SensorValueRow("Z:", sensorDataFormatted?.gyroZ ?: "0.00", "rad/s")
                            }
                        }
                    }
                }
            }

            // 녹화 진행 상태
            if (uiState.isRecording) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.CardBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { recordingTimeElapsed.toFloat() / recordingTimeMax.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = WalkManColors.Primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.data_collection_in_progress),
                                color = WalkManColors.TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = stringResource(id = R.string.time_remaining, recordingTimeMax - recordingTimeElapsed),
                                color = WalkManColors.Primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 업로드 안내 정보
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(id = R.string.info),
                                tint = WalkManColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_time_info),
                                color = WalkManColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 성공/오류 메시지
            if (uiState.successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Success.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = uiState.successMessage!!,
                        color = WalkManColors.Success,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = WalkManColors.Error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // 측정 중단 메시지
            if (measurementCancelled && !uiState.isRecording && !uiState.isUploading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.recording_cancelled),
                            color = WalkManColors.Error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.recording_cancelled_message),
                            color = WalkManColors.TextPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 측정 완료 메시지
            if (fullMeasurementCompleted && !uiState.isRecording && !uiState.isUploading && uiState.successMessage == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WalkManColors.Success.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.recording_completed_90s),
                            color = WalkManColors.Success,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 측정 버튼
            Button(
                onClick = {
                    if (uiState.isRecording) {
                        handleStopRecording()
                    } else {
                        viewModel.startRecording(mode)
                        // 녹화 시작 시 영상도 재생
                        if (mode == RecordingMode.VIDEO) {
                            exoPlayer?.play()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording) WalkManColors.Error else WalkManColors.Primary,
                    disabledContainerColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isUploading
            ) {
                Text(
                    text = if (uiState.isRecording) stringResource(id = R.string.btn_stop_recording) else stringResource(id = R.string.btn_start_recording),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 상태 표시
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WalkManColors.Error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(WalkManColors.Error, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.recording_in_progress),
                        color = WalkManColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.isUploading) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = WalkManColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = WalkManColors.Primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(id = R.string.uploading_data),
                        color = WalkManColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}



// 포맷팅된 센서 데이터를 저장하는 데이터 클래스
data class SensorDataFormatted(
    val accX: String,
    val accY: String,
    val accZ: String,
    val gyroX: String,
    val gyroY: String,
    val gyroZ: String
)