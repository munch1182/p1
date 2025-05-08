package com.munch1182.p1.views.libview

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.launchIO
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityNumberHeightViewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

class NumberHeightViewActivity : BaseActivity() {

    private val bind by bind(ActivityNumberHeightViewBinding::inflate)
    private var loopJob: Job? = null
    private val duration = 200L

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.root.fitsSystemWindows = true
        bind.number.setDuration(duration)
        bind.start.setOnClickListener {
            val isSelected = bind.start.isSelected
            if (!isSelected) {
                start()
                bind.start.text = "stop"
            } else {
                stop()
                bind.start.text = "start"
            }
            bind.start.isSelected = !isSelected
        }

    }

    private fun start() {
        bind.number.start()
        lifecycleScope.launchIO(SupervisorJob().apply { loopJob = this }) {
            while (isActive) {
                delay(duration)
                bind.number.addNumber(Random.nextInt(0, 100))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stop()
    }

    private fun stop() {
        bind.number.stop()
        loopJob?.cancel()
        loopJob = null
    }
}