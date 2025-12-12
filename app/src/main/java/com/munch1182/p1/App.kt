package com.munch1182.p1

import android.app.Application
import android.os.Handler
import androidx.lifecycle.ViewModel
import com.munch1182.android.lib.helper.UsbHelper
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ActivityCurrHelper
import com.munch1182.lib.helper.NetStateHelper
import com.munch1182.lib.helper.asFlow
import com.munch1182.p1.receiver.UserUsbHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidApp
class App : Application() {

    private val log = log()

    override fun onCreate() {
        super.onCreate()
        ActivityCurrHelper.add { onCheckInTime(it) }
        AppHelper.launchIO {
            UsbHelper.registerUsbStateUpdate()
            UsbHelper.getDevs(UserUsbHelper.VENDOR_ID, UserUsbHelper.PRODUCT_ID)?.firstOrNull()?.let {
                UserUsbHelper.init(it)
            }
        }
    }

    /**
     * 当回到前台时需要实时检查的部分
     */
    private fun onCheckInTime(notFront: Boolean) {
        log.logStr("App onCheckInTime: $notFront")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val log = log()

    @Provides
    @Singleton
    fun provideAppHandler(): Handler = ThreadHelper.newHandler("AppHandler")

    @Provides
    @Singleton
    fun provideNetStateHelper(handler: Handler): NetStateHelper = NetStateHelper().apply {
        try {
            val register = register(handler)
            log.logStr("register NetState: ${register}, $curr")
        } catch (e: Exception) {
            e.printStackTrace()
            log.logStr("register NetState fail: $e")
        }
    }
}

@Singleton
class NetStateFlow @Inject constructor(private val netHelper: NetStateHelper) {
    private val _netState by lazy {
        netHelper.asFlow().shareIn(AppHelper, SharingStarted.Eagerly, 1)
    }

    private fun updateNetNow() {
        val curr = netHelper.curr?.let { netHelper.getState(it) }
        if (curr != null) netHelper.forEach { it.onUpdate(curr) }
    }

    val netState get() = _netState.apply { updateNetNow() }
}

/**
 * 为了支持在方法的参数中注入，使用一个ViewModel来包装全局不变参数
 */
@HiltViewModel
class AppVM @Inject constructor(private val netFlow: NetStateFlow) : ViewModel() {
    val netState get() = netFlow.netState
}