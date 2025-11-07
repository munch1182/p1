package com.munch1182.p1

import android.app.Application
import android.os.Handler
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.ThreadHelper
import com.munch1182.lib.base.log
import com.munch1182.lib.helper.ActivityCurrHelper
import com.munch1182.lib.helper.NetStateHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@HiltAndroidApp
class App : Application() {

    private val log = log()


    override fun onCreate() {
        super.onCreate()
        ActivityCurrHelper.add { onCheckInTime(it) }
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

    @Provides
    @Singleton
    fun provideAppHandler(): Handler = ThreadHelper.newHandler("AppHandler")

    @Provides
    @Singleton
    fun provideNetStateHelper(handler: Handler): NetStateHelper = NetStateHelper().apply {
        val register = register(handler)
        Loglog.logStr("AppModule provideNetStateHelper register: $register")
    }

}
