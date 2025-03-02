package com.munch1182.lib

import android.graphics.Color
import androidx.annotation.ColorInt
import kotlin.random.Random


val randomColor: Int
    @ColorInt
    get() = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))