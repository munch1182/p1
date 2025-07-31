package com.munch1182.p1.views

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.str
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.Key
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithRv
import com.munch1182.p1.ui.theme.PagePadding

class SettingActivity : BaseActivity() {

    companion object {
        private const val KEY_NAME = "SETTING_NAME"
        private const val SPLIT = "/"
        fun startNames(name: List<Int>) {
            val str = name.joinToString(SPLIT)
            currAct.startActivity(Intent(currAct, SettingActivity::class.java).putExtra(KEY_NAME, str))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val names = intent.getStringExtra(KEY_NAME)?.split(SPLIT)?.map { str(it.toInt()) }?.toTypedArray() ?: emptyArray<String>()
        setContentWithRv { Views(names) }
    }

    @Composable
    private fun Views(names: Array<String>) {
        var choseLauncher by remember { mutableStateOf("") }
        val selected by remember { derivedStateOf { choseLauncher.isNotEmpty() } }
        var scope = rememberCoroutineScope()
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClickButton("选择启动") {
                DialogHelper.newBottom(names) { choseLauncher = names[it] }.show()
            }
            Text(choseLauncher, modifier = Modifier.padding(horizontal = PagePadding))
        }
        LaunchedEffect(Unit) { choseLauncher = DataHelper.get(Key.KEY_LAUNCHER, "")?.takeIf { it.isNotEmpty() } ?: "MAIN" }
        DisposableEffect(selected) {
            onDispose { scope.launchIO { DataHelper.put(Key.KEY_LAUNCHER, choseLauncher) } }
        }
    }
}