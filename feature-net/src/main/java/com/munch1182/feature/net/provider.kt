package com.munch1182.feature.net

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@NavGraph<ExternalModuleGraph>
annotation class FeatureNetGraph

@Destination<FeatureNetGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun NetScreen(navigator: DestinationsNavigator) {
    WebSocketScreen()
}

//@Module
//@InstallIn(ViewModelComponent::class)
//object NetModule {
//    // TODO: add DI bindings
//}