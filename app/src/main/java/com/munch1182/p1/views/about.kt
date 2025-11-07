package com.munch1182.p1.views

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.munch1182.lib.AppHelper
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.versionCodeCompat
import com.munch1182.lib.base.versionName
import com.munch1182.lib.helper.onResult
import com.munch1182.p1.AppVM
import com.munch1182.p1.base.DataHelper
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.mainScreens
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.ClickIcon
import com.munch1182.p1.ui.Items
import com.munch1182.p1.ui.RvPageIter
import com.munch1182.p1.ui.SpacerV
import com.munch1182.p1.ui.theme.PagePaddingModifier

@Composable
fun AboutView(app: AppVM = hiltViewModel()) {
    var name by remember { mutableStateOf("") }
    var isOpen by remember { mutableStateOf(false) }
    val netState by app.netState.collectAsState(null)
    LaunchedEffect(null) {
        (DataHelper.StartIndex.start() ?: mainScreens.firstOrNull()?.first)?.name?.let { name = it }
    }
    Items(Modifier.fillMaxWidth()) {
        Text("${versionName}(${versionCodeCompat})")
        Text("CURR SDK: ${Build.VERSION.SDK_INT}")
        Text("CURR NET: $netState")

        SpacerV()

        Row(verticalAlignment = Alignment.CenterVertically) {
            val r: () -> Unit = {
                showItem(name) {
                    name = it
                    isOpen = false
                }
                isOpen = true
            }
            ClickButton(name, onClick = r)
            ClickIcon(Icons.AutoMirrored.Filled.ArrowLeft, modifier = Modifier.rotate(if (isOpen) 90f else 270f), onClick = r)
        }
    }
}

private fun showItem(name: String, update: (String) -> Unit) {
    val array = mainScreens

    DialogHelper.newBottom(0) { v ->
        var selected by remember { mutableStateOf(name) }
        RvPageIter(array, PagePaddingModifier.defaultMinSize(minHeight = 300.dp)) { i, it ->
            ClickButton(
                it.first.name, colors = if (it.first.name.lowercase() == selected.lowercase()) ButtonDefaults.buttonColors(Color.Red) else ButtonDefaults.buttonColors()
            ) {
                v.update(i)
                selected = it.first.name
            }
        }
    }.onResult {
        update(array.getOrNull(it)?.first?.name ?: name)
        AppHelper.launchIO { DataHelper.StartIndex.save(it) }
    }.show()
}