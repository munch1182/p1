package com.munch1182.p1.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.munch1182.lib.helper.currFM
import com.munch1182.lib.helper.dialog.DialogContainer

@Composable
fun str(id: Int) = stringResource(id)

abstract class BaseActivity : FragmentActivity()

fun DialogContainer.show() = show(currFM.supportFragmentManager)