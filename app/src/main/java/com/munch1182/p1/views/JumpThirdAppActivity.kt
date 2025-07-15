package com.munch1182.p1.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv

class JumpThirdAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        ClickButton("高德地图") { launchGD2Navigation(40.057406655722, 116.2964407172, true) }
        ClickButton("美团外卖") { launchMT2WaimaiSearch("肯德基") }
        ClickButton("美团打车") { launchMT2Taxi() }
        ClickButton("支付宝滴滴") { launchAliPay2DiDi() }
    }

    private fun launchAliPay2DiDi() {
        val uri = "alipays://platformapi/startapp?appId=20000778".toUri()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchMT2Taxi() {
        val uri = "imeituan://www.meituan.com/cab/home".toUri()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun launchMT2WaimaiSearch(keyword: String) {
        val uri = "meituanwaimai://waimai.meituan.com/search?query=${keyword}".toUri()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    /**
     * 启动导航到该点经纬度
     * @param lat 终点经纬
     * @param needOffset 是否需要需要国测加密；如果已经加密的，则不需要
     * @see [https://lbs.amap.com/api/amap-mobile/guide/android/navigation]
     */
    private fun launchGD2Navigation(lat: Double, lon: Double, needOffset: Boolean) {
        val uri = "androidamap://navi?sourceApplication=p1&lat=${lat}&lon=${lon}&dev=${if (needOffset) 1 else 0}".toUri()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}