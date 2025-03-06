package com.munch1182.lib.result

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

class IntentHelper private constructor(internal val context: Context) {
    companion object {
        fun init(act: FragmentActivity) = IntentHelper(Context(act, act.supportFragmentManager))
        fun init(frag: Fragment) =
            IntentHelper(Context(frag.requireContext(), frag.childFragmentManager))
    }

    fun judgeFirst(judgeFirst: JudgeFirst) = Judge(context.apply { this.judgeFirst = judgeFirst })

    fun intent(intent: Intent) = ResultExecutor(context.apply { this.input = intent })

    class Judge internal constructor(private val context: Context) {
        fun intent(intent: Intent) = JudgeExecutor(context.apply { this.input = intent })
    }

    internal class Context(
        val context: android.content.Context,
        fm: FragmentManager,
        var judgeFirst: JudgeFirst? = null,
    ) : ContractHelper.Context<Intent, ActivityResult>(
        fm, ActivityResultContracts.StartActivityForResult(),
    )
}

class JudgeExecutor internal constructor(internal val ctx: IntentHelper.Context) {
    fun request(listener: ContractHelper.OnResultListener<Boolean>) {
        val judgeFirst = ctx.judgeFirst ?: return
        if (judgeFirst.invoke(ctx.context)) {
            return listener.onResult(true)
        }
        ResultExecutor(ctx).request { listener.onResult(judgeFirst.invoke(ctx.context)) }
    }
}