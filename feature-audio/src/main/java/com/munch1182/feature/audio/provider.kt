package com.munch1182.feature.audio

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@NavGraph<ExternalModuleGraph>
annotation class FeatureAudioGraph

@Destination<FeatureAudioGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun AudioScreen(navigator: DestinationsNavigator) {
    // TODO: implement entry composable
}

@Module
@InstallIn(ViewModelComponent::class)
object AudioModule {
    // TODO: add DI bindings
}