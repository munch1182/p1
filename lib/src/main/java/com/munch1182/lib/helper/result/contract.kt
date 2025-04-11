package com.munch1182.lib.helper.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Logger
import com.munch1182.lib.base.OnResultListener


class ContractHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = ContractHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = ContractHelper(frag.requireActivity(), frag.childFragmentManager)

        internal val log = Logger("result", false)
    }

    fun <I, O> contract(contract: ActivityResultContract<I, O>) = Input(Ctx(act, fm, contract))

    class Input<I, O> internal constructor(private val ctx: Ctx<I, O>) {
        fun input(input: I?) = Request(ctx.apply { ctx.input = input })
    }

    class Request<I, O> internal constructor(private val ctx: Ctx<I, O>) {
        fun request(l: OnResultListener<O>) = ctx.request(l)
    }

    internal open class Ctx<I, O> internal constructor(
        internal val act: FragmentActivity,
        internal val fm: FragmentManager,
        internal val contract: ActivityResultContract<I, O>,
        internal var input: I? = null,
    ) {

        internal open fun request(l: OnResultListener<O>) = ContractFragment.get(fm, contract).launch(input, l)
        override fun toString() = "$contract <- ($input)"
    }
}