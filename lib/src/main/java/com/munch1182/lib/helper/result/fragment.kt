package com.munch1182.lib.helper.result

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity


internal class ResultFragment<I, O>(
    contract: ActivityResultContract<I, O>, private val input: I, private val onResult: (O) -> Unit
) : Fragment() {

    companion object {
        const val TAG = "com.munch1182.lib.helper.result.ResultFragment"
    }

    private val resultLauncher = registerForActivityResult(contract) { result ->
        onResult(result)
        removeFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) resultLauncher.launch(input)
    }

    private fun removeFragment() {
        activity?.supportFragmentManager?.let {
            if (it.isDestroyed) return
            it.beginTransaction().remove(this).commitAllowingStateLoss()
        }
    }

    fun start(fm: FragmentActivity) {
        fm.supportFragmentManager.beginTransaction().add(this, TAG).commitNow()
    }
}