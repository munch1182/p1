package com.munch1182.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/** 深色主题主色。 */
val Purple80 = Color(0xFFD0BCFF)
/** 深色主题辅色。 */
val PurpleGrey80 = Color(0xFFCCC2DC)
/** 深色主题点缀色。 */
val Pink80 = Color(0xFFEFB8C8)

/** 浅色主题主色。 */
val Purple40 = Color(0xFF6650a4)
/** 浅色主题辅色。 */
val PurpleGrey40 = Color(0xFF625b71)
/** 浅色主题点缀色。 */
val Pink40 = Color(0xFF7D5260)

/**
 * 随机生成一个代表颜色的long值
 */
fun colorRandom() = Random.nextLong(from = 0x00000000, until = 0xFFFFFFFFL)
