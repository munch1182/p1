package com.munch1182.lib.helper.dialog

import android.os.Bundle
import androidx.activity.ComponentDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.DialogProvider

class DialogContainer(private val dp: DialogProvider? = null) {
    fun show(manager: FragmentManager, tag: String? = null) = DF().inject(dp).show(manager, tag)
}

class DF : DialogFragment() {
    private var dProvider: DialogProvider? = null
    fun inject(provider: DialogProvider?) = this.apply { this.dProvider = provider }
    override fun onCreateDialog(savedInstanceState: Bundle?) = dProvider?.onCreateDialog(requireContext()) ?: super.onCreateDialog(savedInstanceState)
}

fun ComponentDialog.container() = DialogContainer { this }