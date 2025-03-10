package com.munch1182.lib.result

import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

class ContractHelper internal constructor(private val fm: FragmentManager) {

    fun <I, O> contract(contracts: ActivityResultContract<I, O>): Input<I, O> {
        return Input(Context(fm, contracts))
    }

    class Input<I, O> internal constructor(private val context: Context<I, O>) {
        fun input(input: I?) = ResultExecutor(context.apply { this.input = input })
    }

    companion object {
        fun init(act: FragmentActivity) = ContractHelper(act.supportFragmentManager)
        fun init(frag: Fragment) = ContractHelper(frag.childFragmentManager)
    }

    @FunctionalInterface
    fun interface OnResultListener<T> {
        fun onResult(result: T)
    }

    internal open class Context<I, O>(
        val fm: FragmentManager,
        var contract: ActivityResultContract<I, O>? = null,
        var input: I? = null
    )
}

open class ResultExecutor<I, O> internal constructor(internal val ctx: ContractHelper.Context<I, O>) {

    @Suppress("UNCHECKED_CAST")
    open fun request(listener: ContractHelper.OnResultListener<O>) {
        val frag = getFragment()
        if (frag is ContractFragment<*, *>) {
            (frag as ContractFragment<I, O>).launch(ctx.input, listener)
        }
    }

    internal open fun getFragment(): Fragment {
        val fm = ctx.fm
        val contract = ctx.contract ?: throw IllegalStateException("contract is null")
        return fm.findFragmentByTag(ContractFragment.TAG)?.let {
            // 更新fragment，否则无法更新传入的contract
            if (it.isAdded) fm.beginTransaction().remove(it).commitNowAllowingStateLoss()
            null
            // Fragment直接传参的方法需要保证使用期间页面不被重建
        } ?: ContractFragment(contract).apply {
            fm.beginTransaction().add(this, ContractFragment.TAG).commitNowAllowingStateLoss()
        }
    }
}
