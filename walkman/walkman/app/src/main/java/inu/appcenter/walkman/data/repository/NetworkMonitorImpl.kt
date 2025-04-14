// data/repository/NetworkMonitorImpl.kt
package inu.appcenter.walkman.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import inu.appcenter.walkman.domain.model.ConnectionType
import inu.appcenter.walkman.domain.model.NetworkConfig
import inu.appcenter.walkman.domain.model.NetworkStatus
import inu.appcenter.walkman.domain.repository.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetworkMonitor 인터페이스의 구현체
 * 안드로이드 시스템의 네트워크 상태를 관찰하고 Flow로 제공합니다.
 */
@Singleton
class NetworkMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkMonitor {
    companion object {
        private const val TAG = "NetworkMonitor"
        private const val PREFS_NAME = "network_prefs"
        private const val KEY_WIFI_ONLY = "wifi_only_upload"
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _networkConfig = MutableStateFlow(
        NetworkConfig(
            wifiOnlyUpload = preferences.getBoolean(KEY_WIFI_ONLY, false)
        )
    )

    private val networkConfig: StateFlow<NetworkConfig> = _networkConfig
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = NetworkConfig(
                wifiOnlyUpload = preferences.getBoolean(KEY_WIFI_ONLY, false)
            )
        )

    /**
     * 네트워크 상태를 Flow로 제공
     * NetworkCallback을 사용하여 네트워크 변경 이벤트를 감지합니다.
     */
    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        // 현재 네트워크 상태 확인 및 초기값 전송
        val initialStatus = getCurrentNetworkStatus()
        trySend(initialStatus)

        // 네트워크 변경 이벤트를 감지하는 콜백
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val status = getNetworkStatusFromCapabilities(capabilities)
                trySend(status)
                Log.d(TAG, "Network available: $status")
            }

            override fun onLost(network: Network) {
                // 다른 네트워크가 있는지 확인 후 상태 업데이트
                val status = getCurrentNetworkStatus()
                trySend(status)
                Log.d(TAG, "Network lost: $status")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // 네트워크 성능이 변경될 때마다 상태 업데이트
                val status = getNetworkStatusFromCapabilities(networkCapabilities)
                trySend(status)
                Log.d(TAG, "Network capabilities changed: $status")
            }
        }

        // 모든 네트워크 타입을 모니터링하기 위한 요청 생성
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // 네트워크 콜백 등록
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Flow가 수집 중단될 때 콜백 해제
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // 중복된 상태 변경 방지

    /**
     * 업로드가 가능한 네트워크 연결 상태인지 확인
     */
    override fun isUploadAllowed(): Flow<Boolean> {
        return combine(networkStatus, networkConfig) { status, config ->
            when (status) {
                is NetworkStatus.Connected -> {
                    // WiFi 전용 업로드 설정이 켜져 있으면 WiFi 연결 시에만 true
                    if (config.wifiOnlyUpload) {
                        status.type == ConnectionType.WIFI
                    } else {
                        // 설정이 꺼져 있으면 모든 연결 타입에서 true
                        true
                    }
                }
                is NetworkStatus.Disconnected -> false
            }
        }.distinctUntilChanged()
    }

    /**
     * 네트워크 환경 설정 업데이트
     * @param config 새로운 네트워크 설정
     */
    fun updateNetworkConfig(config: NetworkConfig) {
        _networkConfig.value = config
        preferences.edit().putBoolean(KEY_WIFI_ONLY, config.wifiOnlyUpload).apply()
    }

    /**
     * WiFi 전용 업로드 설정 변경
     * @param wifiOnly WiFi 전용 업로드 여부
     */
    fun setWifiOnlyUpload(wifiOnly: Boolean) {
        _networkConfig.value = _networkConfig.value.copy(wifiOnlyUpload = wifiOnly)
        preferences.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
    }

    /**
     * 현재 WiFi 전용 업로드 설정 반환
     * @return WiFi 전용 업로드 여부
     */
    fun getWifiOnlyUpload(): Boolean {
        return _networkConfig.value.wifiOnlyUpload
    }

    /**
     * 현재 네트워크 상태를 확인하는 함수
     */
    private fun getCurrentNetworkStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return getNetworkStatusFromCapabilities(capabilities)
    }

    /**
     * NetworkCapabilities로부터 NetworkStatus 생성
     */
    private fun getNetworkStatusFromCapabilities(capabilities: NetworkCapabilities?): NetworkStatus {
        if (capabilities == null) {
            return NetworkStatus.Disconnected
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (!hasInternet) {
            return NetworkStatus.Disconnected
        }

        // 연결 타입 확인
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                ConnectionType.ETHERNET
            else ->
                ConnectionType.OTHER
        }

        return NetworkStatus.Connected(type)
    }

    /**
     * 실제 인터넷 연결을 확인하는 함수 (백그라운드 스레드에서 호출해야 함)
     * @return 인터넷 연결 가능 여부
     */
    override suspend fun isInternetReachable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val timeoutMs = _networkConfig.value.connectionTimeoutMs

                withTimeoutOrNull(timeoutMs) {
                    val socket = Socket()
                    try {
                        // Google의 DNS 서버에 연결 시도
                        socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                        socket.close()
                        true
                    } catch (e: Exception) {
                        false
                    } finally {
                        try {
                            socket.close()
                        } catch (e: Exception) {
                            // 무시
                        }
                    }
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking internet connection", e)
                false
            }
        }
    }
}