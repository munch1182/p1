package com.munch1182.p1

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.munch1182.lib.LocationHelper
import com.munch1182.lib.PermissionHelper
import com.munch1182.p1.ui.theme.P1Theme

// https://developer.android.google.cn/develop/sensors-and-location/location/permissions?hl=zh-cn
class LocationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithBase { Location() }
    }
}

@Composable
fun Location() {
    val ctx = LocalContext.current
    var permissionListStr by remember { mutableStateOf("") }
    var gpsIsOpen by remember { mutableStateOf(false) }
    Button({
        permissionListStr = getPermissionListStr()
        gpsIsOpen = LocationHelper.isGpsOpen
    }) { Text("检查状态") }
    Spacer(Modifier.height(16.dp))
    Button({}) { Text("申请权限") }
    Button({ LocationHelper.openGPS(ctx) }) { Text("申请打开GPS") }
    Text("GPS是否已开启：$gpsIsOpen\n权限列表：\n$permissionListStr")
}

fun getPermissionListStr(): String {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    val map =
        permissions.map { it.replace("android.permission.", "") to PermissionHelper.check(it) }
    return StringBuilder().apply {
        map.forEach {
            append("${it.first}: ${it.second}\n")
        }
    }.toString()
}

@Preview
@Composable
fun LocationPreview() {
    P1Theme { Location() }
}