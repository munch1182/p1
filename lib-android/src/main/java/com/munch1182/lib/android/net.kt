package com.munch1182.lib.android

import android.Manifest
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object NetworkMonitor {
    private val connectivityManager by lazy { AppHelper.getSystemService(ConnectivityManager::class.java) }

    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus = _networkStatus.asStateFlow()
    private val log = logger()

    private val updateHandler = Handler(Looper.getMainLooper()) // 主线程更新
    private val updateRunnable = Runnable { updateStatus() }
    private var callback: ConnectivityManager.NetworkCallback? = null
    private val monitorLock = Any()

    /**
     * 当前是否有任意网络可用
     */
    val isNetValidatedFlow: Flow<Boolean> = networkStatus.map { it.isValidated }.distinctUntilChanged()

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun startMonitoring() {
        synchronized(monitorLock) {
            if (callback != null) return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            val callback = object : ConnectivityManager.NetworkCallback() {

                @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    log.d("onAvailable")
                    scheduleUpdate()
                }

                @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                override fun onLost(network: Network) {
                    super.onLost(network)
                    log.d("onLost")
                    scheduleUpdate()
                }

                @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    log.d("onCapabilitiesChanged")
                    scheduleUpdate()
                }

                // 连接属性变更: ip或者路由属性变更
                @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                    log.d("onLinkPropertiesChanged")
                    scheduleUpdate()
                }
            }
            log.log("start monitoring network, registerNetworkCallback")
            connectivityManager?.registerNetworkCallback(request, callback)
            this.callback = callback
        }
        scheduleUpdate()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun stopMonitoring() {
        synchronized(monitorLock) {
            callback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
                callback = null
                log.log("stop monitoring network, unregisterNetworkCallback")
            }
        }
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun scheduleUpdate() {
        updateHandler.removeCallbacks(updateRunnable)
        updateHandler.postDelayed(updateRunnable, 300)
    }

    @SuppressLint("MissingPermission")
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