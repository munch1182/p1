package com.munch1182.lib.result

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

class PermissionHelper internal constructor(internal val ctx: Context) {

    companion object {

        fun init(act: FragmentActivity) = PermissionHelper(Context(act, act.supportFragmentManager))
        fun init(fg: Fragment) =
            PermissionHelper(Context(fg.requireContext(), fg.childFragmentManager))
    }

    fun permission(permission: Array<String>) =
        ResultExecutor(ctx.apply { this.input = permission })

    internal class Context internal constructor(
        val context: android.content.Context,
        fm: FragmentManager,
    ) : ContractHelper.Context<Array<String>, Map<String, Boolean>>(
        fm, ActivityResultContracts.RequestMultiplePermissions()
    )
}