package com.munch1182.p1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.p1.ui.PrimaryButton
import com.munch1182.p1.ui.theme.Dimens
import com.munch1182.p1.ui.theme.paddingPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction

private val homeItems = listOf<Direction>(
    SettingScreenDestination,
    AboutScreenDestination, // ScreenDestination是composedestinations使用ksp生成的实现
)

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    LazyColumn(
        Modifier
            .fillMaxSize() // 全局高度
            .paddingPage(), verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)
    ) {
        items(homeItems, key = { it.route }) { item ->
            PrimaryButton(item.route.removeSuffix("_screen").uppercase()) {
                navigator.navigate(item)
            }
        }
    }
}