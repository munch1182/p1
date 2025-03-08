package com.munch1182.lib.result

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias IntentDialogCreator = Context.() -> ResultDialog?

typealias Judge = (Context) -> Boolean

/**
 * 当未传入[creator]时，如果传入[ignore]为true，则返回true
 * 否则返回false
 * 当传入[creator]时，调用[ResultDialog.show]并返回[ResultDialog.OnChoseListener]的回调
 */
internal suspend fun Context.isIntentDialogContinue(creator: IntentDialogCreator?, ignore: Boolean = false): Boolean {
    val dialog = creator?.invoke(this) ?: return ignore
    withContext(Dispatchers.Main) { dialog.show() }
    return suspendCoroutine { c -> dialog.setOnChoseListener { c.resume(it) } }
}

class IntentHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = IntentHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = IntentHelper(frag.requireActivity(), frag.childFragmentManager)
    }

    fun dialogBefore(dialog: IntentDialogCreator) = Intent(Ctx(act, fm, dialog))

    fun intent(intent: android.content.Intent) = Intent(Ctx(act, fm)).intent(intent)

    class Intent internal constructor(private val ctx: Ctx) {
        fun intent(intent: android.content.Intent) = Request(ctx.apply { this.input = intent })
    }

    class Request internal constructor(internal val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<ActivityResult>) = ctx.request(l)
    }

    internal open class Ctx(act: FragmentActivity, fm: FragmentManager, internal var idc: IntentDialogCreator? = null) :
        ContractHelper.Ctx<android.content.Intent, ActivityResult, ActivityResult>(act, fm, ActivityResultContracts.StartActivityForResult()) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.idc) {
            this.input = ctx.input
            this.mapper = ctx.mapper
        }

        override fun request(l: ContractHelper.OnResultListener<ActivityResult>) {
            act.lifecycleScope.launch {
                if (act.isIntentDialogContinue(idc, true)) {
                    PermissionIntentFragment.get(fm).launchIntent(input, l)
                } else {
                    l.onResult(ActivityResult(FragmentActivity.RESULT_CANCELED, null))
                }
            }
        }

        override fun toString(): String {
            return "IntentHelper <- ($input)"
        }
    }
}

class JudgeIntentHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {
    companion object {
        fun init(act: FragmentActivity) = JudgeIntentHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = JudgeIntentHelper(frag.requireActivity(), frag.childFragmentManager)
    }

    fun judge(judge: Judge) = Dialog(Ctx(act, fm, judge))

    class Dialog internal constructor(private val ctx: Ctx) {
        fun dialogBefore(creator: IntentDialogCreator) = Intent(ctx.apply { this.jdc = creator })
        fun intent(intent: android.content.Intent) = Intent(ctx).intent(intent)
    }

    class Intent internal constructor(private val ctx: Ctx) {
        fun intent(intent: android.content.Intent) = Request(ctx.apply { this.input = intent })
    }

    class Request internal constructor(internal val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<Boolean>) = ctx.request(l)
    }

    internal open class Ctx(act: FragmentActivity, fm: FragmentManager, internal val judge: Judge, internal var jdc: IntentDialogCreator? = null) :
        ContractHelper.Ctx<android.content.Intent, ActivityResult, Boolean>(act, fm, ActivityResultContracts.StartActivityForResult(), mapper = { judge.invoke(act) }) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.judge, ctx.jdc) {
            this.input = ctx.input
            this.mapper = ctx.mapper
        }

        override fun request(l: ContractHelper.OnResultListener<Boolean>) {
            if (judge.invoke(act)) return l.onResult(true)
            act.lifecycleScope.launch {
                if (!act.isIntentDialogContinue(jdc, true)) {
                    return@launch l.onResult(false)
                } else {
                    PermissionIntentFragment.get(fm).launchIntent(input) { l.onResult(mapper!!(it)) }
                }
            }
        }

        override fun toString(): String {
            return "JudgeIntentHelper <- judge <- ($input)"
        }
    }
}