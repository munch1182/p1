package com.munch1182.lib.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Logger

class ContractHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = ContractHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = ContractHelper(frag.requireActivity(), frag.childFragmentManager)

        internal val logger = Logger("ContractHelper")
    }

    fun <I, O> contract(contract: ActivityResultContract<I, O>) = Result(Ctx(act, fm, contract))

    class Result<I, O> internal constructor(private val ctx: Ctx<I, O, O>) {
        fun input(input: I?) = Input(ctx.apply { ctx.input = input })
    }

    class Input<I, O> internal constructor(private val ctx: Ctx<I, O, O>) {
        fun <O2> mapResult(mapper: (O) -> O2) = Request(ctx.new(mapper))
        fun request(l: OnResultListener<O>) = Request(ctx).request(l)
    }

    class Request<I, O, O2> internal constructor(private val ctx: Ctx<I, O, O2>) {
        fun request(l: OnResultListener<O2>) = ctx.request(l)
    }

    @FunctionalInterface
    fun interface OnResultListener<O> {
        fun onResult(result: O)
    }

    internal open class Ctx<I, O, O2> internal constructor(
        internal val act: FragmentActivity,
        internal val fm: FragmentManager,
        internal val contract: ActivityResultContract<I, O>,
        internal var input: I? = null,
        internal var mapper: ((O) -> O2)? = null
    ) {

        @Suppress("UNCHECKED_CAST")
        open fun request(l: OnResultListener<O2>) {
            mapper?.let { mp ->
                ContractFragment.get(fm, contract).launch(input) { l.onResult(mp.invoke(it)) }
            } ?: ContractFragment.get(fm, contract).launch(input, l as OnResultListener<O>)
        }

        fun <O3> new(mapper: ((O) -> O3)): Ctx<I, O, O3> = Ctx(act, fm, contract, input, mapper)

        override fun toString(): String {
            return "$contract <- ($input) "
        }
    }
}