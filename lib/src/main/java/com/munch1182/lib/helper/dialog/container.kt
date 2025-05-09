package com.munch1182.lib.helper.dialog

import android.os.Bundle
import androidx.activity.ComponentDialog
import androidx.fragment.app.DialogFragment
import com.munch1182.lib.base.DialogProvider

open class DialogContainer : DialogFragment() {
    private var dProvider: DialogProvider? = null
    fun inject(provider: DialogProvider?) = this.apply { this.dProvider = provider }
    override fun onCreateDialog(savedInstanceState: Bundle?) = dProvider?.onCreateDialog(requireContext()) ?: super.onCreateDialog(savedInstanceState)
}

// DialogFragment可以依附于当前Activity而不要求View体系
fun ComponentDialog.container() = DialogContainer().inject { this }