package com.munch1182.p1

import android.os.Bundle
import androidx.compose.foundation.layout.padding
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.setContentWithTheme
import com.munch1182.p1.ui.theme.PagePaddingModifier
import com.munch1182.p1.views.AppView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentWithTheme { AppView(PagePaddingModifier.padding(it)) }
    }
}
