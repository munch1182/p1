package com.munch1182.lib.helper

import android.Manifest
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import androidx.annotation.RequiresPermission
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.Releasable
import com.munch1182.lib.base.log

private typealias ARMUpdate = ARManager<OnUpdateListener<NetStateHelper.NetState>>

/**
 * 获取网络状态并监听后续更改，不应该同时使用多个实例
 *
 * @see register
 *
 */
class NetStateHelper : ARMUpdate by ARDefaultManager(), Releasable {

    private val log = log()
    private var lastNet: NetState = NetState(NetworkType.None, false)
    private val cm by lazy { AppHelper.getSystemService(ConnectivityManager::class.java) }

    @SuppressLint("MissingPermission")
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            log.logStr("onLost")
            updateNet(network)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            log.logStr("onCapabilitiesChanged")
            updateNet(network)
        }
    }

    /**
     * 回调网络状态
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun updateNet(network: Network) {
        val newState = getState(network)
        if (newState != lastNet) {
            lastNet = newState
            log.logStr("updateNet: $newState")
            forEach { it.onUpdate(newState) }
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getState(network: Network? = curr) = NetState(getNetworkType(network), isNetAvailable(network))

    /**
     * 当前使用的网络
     */
    val curr
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE) get() = cm?.activeNetwork

    /**
     * 判断网络使用有可用网络
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetAvailable(net: Network? = curr): Boolean {
        net ?: return false
        return cm?.getNetworkCapabilities(net)?.isNetAvailable() ?: false
    }

    /**
     * 判断当前网络是否包含指定类型
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun hasTransport(net: Network? = curr, netType: NetworkType): Boolean {
        net ?: return false
        val cap = cm?.getNetworkCapabilities(net) ?: return false
        return cap.hasTransport(netType.trans)
    }

    /**
     * 获取网络类型
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetworkType(net: Network? = curr): NetworkType {
        net ?: return NetworkType.None
        val capabilities = cm?.getNetworkCapabilities(net) ?: return NetworkType.None
        return NetworkType.from(capabilities)
    }

    /**
     * 注册网络监听；如果注册太多次，会抛出异常
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun register(handler: Handler? = null): Boolean {
        cm ?: return false
        try {
            if (handler != null) {
                cm.registerDefaultNetworkCallback(callback, handler)
            } else {
                cm.registerDefaultNetworkCallback(callback)
            }
            curr?.let { updateNet(it) }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 取消注册网络监听
     */
    fun unregister() {
        cm?.unregisterNetworkCallback(callback)
    }

    /**
     * 网络类型
     */
    sealed class NetworkType(val trans: Int) {
        object Wifi : NetworkType(NetworkCapabilities.TRANSPORT_WIFI)
        object Cellular : NetworkType(NetworkCapabilities.TRANSPORT_CELLULAR)
        object Ethernet : NetworkType(NetworkCapabilities.TRANSPORT_ETHERNET)
        object Bluetooth : NetworkType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        object Vpn : NetworkType(NetworkCapabilities.TRANSPORT_VPN)
        object None : NetworkType(-1)

        val isConnected get() = this != None

        companion object {
            fun from(capabilities: NetworkCapabilities) = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Wifi
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Cellular
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Ethernet
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> Bluetooth
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> Vpn
                else -> None
            }
        }

        override fun toString() = when (this) {
            Wifi -> "Wifi"
            Cellular -> "Cellular"
            Ethernet -> "Ethernet"
            Bluetooth -> "Bluetooth"
            Vpn -> "Vpn"
            None -> "None"
        }
    }

    data class NetState(val type: NetworkType, val isConnected: Boolean) {
        override fun toString(): String {
            return "${type}($isConnected)"
        }
    }

    override fun release() {
        unregister()
        clear()
    }
}

fun NetworkCapabilities.isNetAvailable() = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)