package com.munch1182.lib.helper.result


import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

class IntentHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = IntentHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = IntentHelper(frag.requireActivity(), frag.childFragmentManager)
    }

    fun intent(i: Intent) = Request(Ctx(act, fm).apply { input = i })

    class Request internal constructor(private val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<ActivityResult>) = ctx.request(l)
    }

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager,
    ) : ContractHelper.Ctx<Intent, ActivityResult>(act, fm, ActivityResultContracts.StartActivityForResult()) {
        override fun request(l: ContractHelper.OnResultListener<ActivityResult>) = PermissionIntentFragment.get(fm).launch(input!!, l)
    }
}