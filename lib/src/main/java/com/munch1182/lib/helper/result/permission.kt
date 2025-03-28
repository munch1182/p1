package com.munch1182.lib.helper.result

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Logger

class PermissionHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = PermissionHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = PermissionHelper(frag.requireActivity(), frag.childFragmentManager)
        internal val log = Logger("permission: ")
    }

    fun permission(p: Array<String>) = Request(Ctx(act, fm).apply { input = p })

    class Request internal constructor(private val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<Map<String, Boolean>>) = ctx.request(l)
    }

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager
    ) : ContractHelper.Ctx<Array<String>, Map<String, Boolean>>(act, fm, ActivityResultContracts.RequestMultiplePermissions())
}