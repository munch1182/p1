package com.munch1182.lib.android

import android.Manifest
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds

object NetworkMonitor {
    private val connectivityManager by lazy { AppHelper.getSystemService(ConnectivityManager::class.java) }

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus = _networkStatus.asStateFlow()
    private val log = logger()

    // 更新除非流
    private val eventTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile
    private var eventJob: Job? = null

    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null

    /**
     * 当前是否有任意网络可用
     */
    val isNetValidatedFlow: Flow<Boolean> = networkStatus.map { it.isValidated }.distinctUntilChanged()

    @OptIn(FlowPreview::class)
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startMonitoring(scope: CoroutineScope = AppHelper) {
        if (callback != null) return
        eventJob?.cancel()
        eventJob = eventTrigger
            .debounce(300.milliseconds) // 过滤重复判断+延迟判断
            .onEach { updateStatus() }
            .launchIn(scope)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                log.d("onAvailable")
                eventTrigger.tryEmit(Unit)
            }

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onLost(network: Network) {
                super.onLost(network)
                log.d("onLost")
                eventTrigger.tryEmit(Unit)
            }

            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                log.d("onCapabilitiesChanged")
                eventTrigger.tryEmit(Unit)
            }

            // 连接属性变更: ip或者路由属性变更
            @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                log.d("onLinkPropertiesChanged")
                eventTrigger.tryEmit(Unit)
            }
        }
        log.log("start monitoring network, registerNetworkCallback")
        connectivityManager?.registerNetworkCallback(request, callback)
        this.callback = callback
        eventTrigger.tryEmit(Unit)
    }

    fun stopMonitoring() {
        callback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
            callback = null
            log.log("stop monitoring network, unregisterNetworkCallback")
        }
        eventJob?.cancel()
        eventJob = null
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun updateStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        log.log("curr: $activeNetwork")
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val isConnected = caps != null
        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val type = when {
            !isConnected -> NetworkType.NONE
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.OTHER
        }

        _networkStatus.value = NetworkStatus(isConnected, isValidated, type)
    }
}

data class NetworkStatus(
    val isConnected: Boolean = false,                // 是否有网络连接（物理链路）
    val isValidated: Boolean = false,        // 是否通过校验（能访问外网）
    val type: NetworkType = NetworkType.NONE,
)

/**
 * 判断是否是在使用wifi
 */
val NetworkStatus.isUseWifi get() = type == NetworkType.WIFI

enum class NetworkType {
    NONE, WIFI, CELLULAR, ETHERNET, BLUETOOTH, VPN, OTHER
}