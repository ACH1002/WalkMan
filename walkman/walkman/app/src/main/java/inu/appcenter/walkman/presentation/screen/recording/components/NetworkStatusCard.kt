package inu.appcenter.walkman.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import inu.appcenter.walkman.R
import inu.appcenter.walkman.domain.model.ConnectionType
import inu.appcenter.walkman.domain.model.NetworkStatus
import inu.appcenter.walkman.presentation.theme.WalkManColors

@Composable
fun NetworkStatusCard(
    networkState: NetworkStatus,
    showConnectButton: Boolean = false,
    onConnectClick: () -> Unit = {}
) {
    when (networkState) {
        is NetworkStatus.Disconnected -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.Error.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "네트워크 연결 없음",
                            tint = WalkManColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.network_disconnected),
                            color = WalkManColors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (showConnectButton) {
                        Button(
                            onClick = onConnectClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.connect_network),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        is NetworkStatus.Connected -> {
            // 선택적으로 연결 상태도 표시할 수 있음
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = WalkManColors.Success.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "네트워크 연결됨",
                        tint = WalkManColors.Success,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val networkTypeText = when (networkState.type) {
                        ConnectionType.WIFI -> stringResource(id = R.string.wifi_connected)
                        ConnectionType.MOBILE -> stringResource(id = R.string.mobile_data_connected)
                        else -> stringResource(id = R.string.network_connected)
                    }
                    Text(
                        text = networkTypeText,
                        color = WalkManColors.Success,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}