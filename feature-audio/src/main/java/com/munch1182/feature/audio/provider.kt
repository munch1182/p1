package com.munch1182.feature.audio

import androidx.compose.runtime.Composable
import com.munch1182.feature.audio.record.RecordScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import com.ramcosta.composedestinations.annotation.NavGraph
import com.ramcosta.composedestinations.annotation.parameters.CodeGenVisibility

@NavGraph<ExternalModuleGraph>
annotation class FeatureAudioGraph

@Destination<FeatureAudioGraph>(start = true, visibility = CodeGenVisibility.INTERNAL)
@Composable
internal fun AudioScreen() {
    //ConvertScreen()
    RecordScreen()
}

//@Module
//@InstallIn(ViewModelComponent::class)
//object AudioModule {
//}