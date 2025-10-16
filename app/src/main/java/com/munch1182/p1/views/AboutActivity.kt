package com.munch1182.p1.views

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.isDeveloperMode
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.currAct
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.judge
import com.munch1182.p1.MainActivity
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.CheckBoxLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.RvPageIter
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.setContentWithRv

class AboutActivity : BaseActivity() {
    private val startIndexVM by viewModels<StartIndexVM>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Click() }
    }

    @Composable
    fun Click() {
        val newList by startIndexVM.newList.observeAsState(arrayOf(""))
        val choseIndex by startIndexVM.startIndex.observeAsState(0)
        var isExpand by remember { mutableStateOf(false) }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("启动页面：${newList.getOrNull(choseIndex)}")
                ClickIcon(
                    Icons.Filled.ArrowDropDown, modifier = Modifier
                        .rotate(if (isExpand) 0f else 180f)
                        .size(16.dp)
                ) {
                    isExpand = !isExpand
                    if (isExpand) showChoseStartDialog(newList, choseIndex) {
                        isExpand = false
                        startIndexVM.saveStartIndex(it)
                    }
                }
            }
            SpacerV()
            ClickButton("开发者选项界面") { toDeveloperSettings() }
            ClickButton("设置界面") { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            ClickButton("关于界面") { startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(16.dp), horizontalAlignment = Alignment.Companion.Start
            ) {
                Text(screenStr())
                Text("CURR SDK: ${Build.VERSION.SDK_INT}")
                Text("${versionName}(${versionCodeCompat})")
            }
        }
    }

    private fun showChoseStartDialog(data: Array<String>, select: Int, on: (Int) -> Unit) {
        DialogHelper.newBottom(select) { res ->
            var select by remember { mutableIntStateOf(select) }
            RvPageIter(data) { i, item ->
                CheckBoxLabel(item, i == select) {
                    select = i
                    res.update(i)
                }
            }
        }.onResult(on).show()
    }

    private fun screenStr(): String {
        val sc = screen()
        val sd = screenDisplay()
        val equalsHeight = sc.height() == (sd.heightPixels + statusHeight())
        val navHeight = if (equalsHeight) 0 else navigationHeight()
        return "${sc.width()}(${sd.widthPixels}) x ${sc.height()}(${statusHeight()} + ${sd.heightPixels} + $navHeight)"
    }

    fun toDeveloperSettings() {
        judge({ isDeveloperMode() }, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)).onDialog { DialogHelper.newMessage("打开开发者模式", "前往设置界面打开开发者模式") }.request { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
    }
}

class StartIndexVM : ViewModel() {
    private var _startIndex = MutableLiveData(0)
    private var _newList = MutableLiveData<Array<String>>()

    val startIndex: LiveData<Int> = _startIndex
    val newList: LiveData<Array<String>> = _newList

    init {
        viewModelScope.launchIO {
            val list = arrayOf("首页", *MainActivity.items.map {
                val first = it.first
                first as? String ?: (first as? Int)?.let { i -> currAct.getString(i) } ?: first.toString()
            }.toTypedArray())
            withUI { _newList.value = list }
            var index = DataHelper.StartIndex.get() ?: 0
            if (index < 0) index = 0
            if (index >= list.size) index = list.size - 1
            withUI { _startIndex.value = index }
        }
    }

    fun saveStartIndex(index: Int) {
        viewModelScope.launchIO {
            DataHelper.StartIndex.save(index)
            withUI { _startIndex.value = index }
        }
    }

}