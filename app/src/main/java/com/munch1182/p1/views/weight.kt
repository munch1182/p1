package com.munch1182.p1.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.withUI
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.databinding.ViewSoundWaveBinding
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import kotlinx.coroutines.delay

@Composable
fun WeightView() {
    Items(Modifier.fillMaxWidth()) {
        ClickButton("SoundWave", onClick = ::showWave)
    }
}

private fun showWave() {
    DialogHelper.newBottom((ViewSoundWaveBinding::inflate), onViewCreated = { bind, fg ->
        bind.wave.setAnimationSpeed(40L)
        bind.wave.setAmplitudes(FloatArray(20) { 0.1f })
        fg.lifecycleScope.launchIO {
            for (i in 0 until 30000) {
                val amplitude = Math.random().toFloat()
                if (i in 500..600) {
                    withUI { bind.wave.addAmplitude(0f) }
                } else {
                    withUI { bind.wave.addAmplitude(amplitude) }
                }
                delay(40L)
            }
        }
    }).show()
}