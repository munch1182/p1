package com.munch1182.lib.base

import android.graphics.Rect
import android.view.View

fun View.toRect() = Rect(left, top, right, bottom)