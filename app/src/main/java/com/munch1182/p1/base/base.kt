package com.munch1182.p1.base

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.currAsFM

@Composable
fun str(id: Int) = stringResource(id)

abstract class BaseActivity : FragmentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.attachBaseContext(newBase))
    }
}

fun DialogFragment.show() = show(currAsFM.supportFragmentManager, null)

fun <VB : ViewBinding> Activity.bind(inflater: (LayoutInflater) -> VB): Lazy<VB> {
    return lazy { inflater(layoutInflater).apply { setContentView(root) } }
}

fun toast(msg: String) {
    currAct.runOnUiThread {
        Toast.makeText(currAct, msg, Toast.LENGTH_SHORT).show()
    }
}
