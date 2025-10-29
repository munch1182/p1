package com.munch1182.p1.views

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.isDeveloperMode
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.navigationHeight
import com.munch1182.lib.base.screen
import com.munch1182.lib.base.screenDisplay
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.NetStateHelper
import com.munch1182.lib.helper.onResult
import com.munch1182.lib.helper.result.judge
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.ui.CheckBoxLabel
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.RvPageIter
import com.munch1182.p1.ui.SpacerV
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

class StartIndexVM : ViewModel() {
    private var _startIndex = MutableLiveData(0)
    private var _newList = MutableLiveData<Array<String>>()

    val startIndex: LiveData<Int> = _startIndex
    val newList: LiveData<Array<String>> = _newList

    init {
        viewModelScope.launchIO {
            val list = mainScreens.mapIndexedNotNull { i, it -> if (i == 0) null else it.first.name }.toMutableList().apply { add(0, "首页") }.toTypedArray()
            withUI { _newList.value = list }
            var index = DataHelper.StartIndex.get() ?: 0
            if (index !in 0 until list.size) index = 0
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

@HiltViewModel
class NetStateVM @Inject constructor(private val netStateHelper: NetStateHelper) : ViewModel() {
    private var _netState = MutableLiveData<NetStateHelper.NetworkType>(NetStateHelper.NetworkType.None)
    val netState: LiveData<NetStateHelper.NetworkType> = _netState
    private val onUpdate = OnUpdateListener<NetStateHelper.NetState> { ns ->
        _netState.postValue(if (ns.isConnected) ns.type else NetStateHelper.NetworkType.None)
    }

    init {
        netStateHelper.add(onUpdate)
        netStateHelper.curr?.let { onUpdate.onUpdate(netStateHelper.getState(it)) }
    }

    override fun onCleared() {
        super.onCleared()
        netStateHelper.remove(onUpdate)
    }
}

@Composable
fun AboutView(startIndexVM: StartIndexVM = viewModel(), netStateVM: NetStateVM = hiltViewModel()) {
    val newList by startIndexVM.newList.observeAsState(arrayOf(""))
    val choseIndex by startIndexVM.startIndex.observeAsState(0)
    val netState by netStateVM.netState.observeAsState(NetStateHelper.NetworkType.None)
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
        ClickButton("设置界面") { curr?.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        ClickButton("关于界面") { curr?.startActivity(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)) }
        Column(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(16.dp), horizontalAlignment = Alignment.Companion.Start
        ) {
            Text("CURR NET: $netState")
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
            CheckBoxLabel(item, i == select, Modifier.fillMaxWidth()) {
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
    val curr = curr ?: return
    curr.judge({ isDeveloperMode() }, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)).onDialog { DialogHelper.newMessage("打开开发者模式", "前往设置界面打开开发者模式") }.request { curr.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
}