package com.munch1182.p1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.munch1182.core.ui.PrimaryButton
import com.munch1182.core.ui.theme.Dimens
import com.munch1182.core.ui.theme.paddingPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction

private val homeItems: List<Direction>
    get() {
        val list = mutableListOf<Direction>()
        NavGraphs.app.destinations.forEach {
            if (it is Direction) list.add(it)

        }
        NavGraphs.app.nestedNavGraphs.forEach {
            for (it in it.destinations) {
                if (it is Direction) list.add(it)
            }
        }
        return list
    }

@Destination<AppGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    LazyColumn(
        Modifier
            .fillMaxSize() // 全局高度
            .paddingPage(), verticalArrangement = Arrangement.spacedBy(Dimens.PaddingItem)
    ) {
        items(homeItems, key = { it.route }) { item ->
            val name = if (item.route.contains("/")) {
                item.route.split("/")[0]
            } else {
                item.route.removeSuffix("_screen")
            }
            PrimaryButton(name.uppercase()) {
                navigator.navigate(item)
            }
        }
    }
}