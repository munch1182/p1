package com.munch1182.p1.base

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.munch1182.lib.helper.currAsFM
import com.munch1182.lib.helper.dialog.DialogContainer

@Composable
fun str(id: Int) = stringResource(id)

abstract class BaseActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContext(newBase))
    }
}

fun DialogContainer.show() = show(currAsFM.supportFragmentManager)

fun <VB : ViewBinding> Activity.bind(inflater: (LayoutInflater) -> VB): Lazy<VB> {
    return lazy { inflater(layoutInflater).apply { setContentView(root) } }
}
