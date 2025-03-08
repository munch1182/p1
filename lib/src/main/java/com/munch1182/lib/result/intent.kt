package com.munch1182.lib.result

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IntentHelper private constructor(internal val ctx: Context) {
    companion object {
        fun init(act: FragmentActivity) = IntentHelper(Context(act, act.supportFragmentManager))
        fun init(frag: Fragment) =
            IntentHelper(Context(frag.requireActivity(), frag.childFragmentManager))
    }

    fun judgeFirst(judgeFirst: JudgeFirst) = Judge(ctx.apply { this.judgeFirst = judgeFirst })

    fun intent(intent: Intent) = ResultExecutor(ctx.apply { this.input = intent })

    class Judge internal constructor(private val context: Context) {
        fun intent(intent: Intent) = JudgeExecutor(context.apply { this.input = intent })

        /**
         * 当第一次[judgeFirst]判断失败时则会显示此方法返回的弹窗，弹窗未取消则会跳转[intent]，否则直接结束流程回调false
         * 未调用此方法或者此方法返回null时正常跳转[intent]
         */
        fun dialogIfNeed(creator: DialogCreator) = Judge(context.apply { this.creator = creator })
    }

    internal class Context(
        val ctx: FragmentActivity,
        fm: FragmentManager,
        var judgeFirst: JudgeFirst? = null,
        var creator: DialogCreator? = null,
    ) : ContractHelper.Context<Intent, ActivityResult>(
        fm, ActivityResultContracts.StartActivityForResult(),
    )

    @FunctionalInterface
    fun interface DialogCreator {
        fun create(ctx: android.content.Context): ResultDialog?
    }
}

class JudgeExecutor internal constructor(internal val ctx: IntentHelper.Context) {
    fun request(listener: ContractHelper.OnResultListener<Boolean>) {
        val judgeFirst = ctx.judgeFirst ?: return
        if (judgeFirst.invoke(ctx.ctx)) {
            return listener.onResult(true)
        }
        ctx.ctx.lifecycleScope.launch {
            if (dialogContinue() == false) return@launch listener.onResult(false)
            ResultExecutor(ctx).request { listener.onResult(judgeFirst.invoke(ctx.ctx)) }
        }
    }

    private suspend fun dialogContinue(): Any {
        val dialog = ctx.creator?.create(ctx.ctx) ?: return true
        return suspendCoroutine { c ->
            dialog.create(ctx.ctx).setOnChoseListener { c.resume(it) }.show()
        }
    }
}