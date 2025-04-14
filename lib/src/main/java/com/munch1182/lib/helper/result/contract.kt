package com.munch1182.lib.helper.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.UnSupportImpl


class ContractHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = ContractHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = ContractHelper(frag.requireActivity(), frag.childFragmentManager)

        internal val log = Logger("result", false)
    }

    fun <I, O> contract(contract: ActivityResultContract<I, O>) = Input(Ctx(act, fm, contract))

    class Input<I, O> internal constructor(private val ctx: Ctx<I, O, O>) {
        fun input(input: I?) = Request(ctx.apply { ctx.input = input })
    }

    class Request<I, O> internal constructor(private val ctx: Ctx<I, O, O>) {
        fun request(l: OnResultListener<O>) = ctx.request(l)
    }

    internal open class Ctx<INPUT, OUTPUT, LISTENER> internal constructor(
        internal val act: FragmentActivity,
        internal val fm: FragmentManager,
        private val contract: ActivityResultContract<INPUT, OUTPUT>,
        internal var input: INPUT? = null,
    ) {

        internal open fun requestLaunch(l: OnResultListener<OUTPUT>) = ContractFragment.get(fm, contract).launch(input, l)

        internal open fun request(l: OnResultListener<LISTENER>) {
            @Suppress("UNCHECKED_CAST")
            val ll = l as? OnResultListener<OUTPUT> ?: throw UnSupportImpl()
            requestLaunch(ll)
        }

        override fun toString() = "$contract <- ($input)"
    }
}