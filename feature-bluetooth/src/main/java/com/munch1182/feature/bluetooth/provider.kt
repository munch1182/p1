package com.munch1182.feature.bluetooth

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import com.munch1182.core.base.NotSingleton
import com.munch1182.feature.bluetooth.connect.BluetoothConnect
import com.munch1182.feature.bluetooth.scan.BluetoothScan
import com.munch1182.lib.bluetooth.le.DefaultBLEDeviceManager
import com.munch1182.lib.bluetooth.le.IBLEDeviceManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility
import com.ramcosta.composedestinations.generated.bluetooth.destinations.BluetoothConnectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@NavGraph<ExternalModuleGraph>
annotation class FeatureBluetoothGraph

@SuppressLint("MissingPermission")
@Destination<FeatureBluetoothGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothScanScreen(navigator: DestinationsNavigator) {
    BluetoothScan(
        onDeviceClick = { device ->
            navigator.navigate(
                BluetoothConnectScreenDestination(
                    mac = device.address,
                    name = device.name ?: "Unknown"
                )
            )
        }
    )
}

@Destination<FeatureBluetoothGraph>(visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun BluetoothConnectScreen(mac: String, name: String = "Unknown") {
    BluetoothConnect(mac, name)
}

@Module
@InstallIn(ViewModelComponent::class)      // 绑定到 ViewModel 组件
object BluetoothModule {
    @NotSingleton   // 非单例传入
    @Provides
    @ViewModelScoped  // 作用域为 ViewModel 级别
    fun provideNotSingletonBLEManager(): IBLEDeviceManager<String> = DefaultBLEDeviceManager()
}