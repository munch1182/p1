package com.munch1182.lib.result

import android.app.Activity
import androidx.activity.result.ActivityResult
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * [ifOk]系列方法不会实际执行，知道[request]才会实际执行
 */
fun PermissionHelper.With.ifOk(ifOk: (Map<String, PermissionHelper.Result>) -> Boolean): ContactHelper {
    if (ctx is PCTXWrapper) {
        return ContactHelper(ctx.apply { this.ifOk = ifOk })
    }
    return ContactHelper(PCTXWrapper(ctx, ifOk))
}

fun PermissionHelper.Request.ifOk(ifOk: (Map<String, PermissionHelper.Result>) -> Boolean): ContactHelper {
    if (ctx is PCTXWrapper) {
        return ContactHelper(ctx.apply { this.ifOk = ifOk })
    }
    return ContactHelper(PCTXWrapper(ctx, ifOk))
}

fun PermissionHelper.Dialog.ifOk(ifOk: (Map<String, PermissionHelper.Result>) -> Boolean): ContactHelper {
    if (ctx is PCTXWrapper) {
        return ContactHelper(ctx.apply { this.ifOk = ifOk })
    }
    return ContactHelper(PCTXWrapper(ctx, ifOk))
}


fun IntentHelper.Request.ifOk(ifOk: (ActivityResult) -> Boolean): ContactHelper {
    if (ctx is ICTXWrapper) {
        return ContactHelper(ctx.apply { this.ifOk = ifOk })
    }
    return ContactHelper(ICTXWrapper(ctx, ifOk))
}

fun JudgeIntentHelper.Request.ifOk(ifOk: (Boolean) -> Boolean): ContactHelper {
    if (ctx is JCTXWrapper) {
        return ContactHelper(ctx.apply { this.ifOk = ifOk })
    }
    return ContactHelper(JCTXWrapper(ctx, ifOk))
}


internal interface IWrapper {
    // 该参数只用于ctx基本值传递，IWrapper子类本身就是继承了对应Ctx的对象
    val fromCtx: ContractHelper.Ctx<*, *, *>
    val link: LinkHelper

    fun newPCTWrapper(permissions: Array<String>): PCTXWrapper {
        val pctx = PermissionHelper.Ctx(fromCtx.act, fromCtx.fm).apply { input = permissions }
        val pctxWrapper = PCTXWrapper(pctx)
        this += pctxWrapper
        return pctxWrapper
    }

    fun newICTWrapper(dialog: IntentDialogCreator? = null): ICTXWrapper {
        val ictx = IntentHelper.Ctx(fromCtx.act, fromCtx.fm, dialog)
        val ictxWrapper = ICTXWrapper(ictx)
        this += ictxWrapper
        return ictxWrapper
    }

    fun newJCTXWrapper(judge: Judge): JCTXWrapper {
        val jctx = JudgeIntentHelper.Ctx(fromCtx.act, fromCtx.fm, judge)
        val jctxWrapper = JCTXWrapper(jctx)
        this += jctxWrapper
        return jctxWrapper
    }

    operator fun plusAssign(w: IWrapper) {
        if (link.next != null || w.link.last != null) throw IllegalStateException("link error")
        link.next = w
        w.link.last = this
    }

    suspend fun runIfOk() = false

    suspend fun runAllIfOkUntilSelf(): Boolean {
        var w: IWrapper? = this
        while (w?.link?.last != null) {
            w = w.link.last!!
        }
        // 从第一个开始，依次调用，单此调用不会走进此方法，所以不多判断w==this
        while (w != null) {
            // 中间部分任一失败则返回false
            val c = w.runIfOk()
            ContractHelper.logger.logStr("runIfOk: $w($c)")
            if (!c) return false
            w = w.link.next
            // 最后一个即是调用者，返回让其自己调用来返回回调
            if (w == this) break
        }
        return true
    }
}

internal class PCTXWrapper internal constructor(
    override val fromCtx: PermissionHelper.Ctx,
    internal var ifOk: ((Map<String, PermissionHelper.Result>) -> Boolean)? = null,
    override val link: LinkHelper = LinkHelper()
) : PermissionHelper.Ctx(fromCtx), IWrapper {

    override fun request(l: ContractHelper.OnResultListener<Map<String, PermissionHelper.Result>>) {
        act.lifecycleScope.launch {
            // 最后一个要自己调用返回回调
            if (runAllIfOkUntilSelf()) return@launch toCtx().request(l)
            l.onResult(buildMap { input!!.forEach { p -> put(p, PermissionHelper.Result.Denied) } })
        }
    }

    override suspend fun runIfOk(): Boolean {
        val res = PermissionHelper.Request(toCtx()).request()
        return ifOk?.invoke(res) ?: false
    }

    private fun toCtx() = PermissionHelper.Ctx(act, fm, pdc).apply {
        this.input = this@PCTXWrapper.input
        this.mapper = this@PCTXWrapper.mapper
    }

    override fun toString(): String {
        return "PCTXWrapper(${super.toString()})"
    }
}

internal class ICTXWrapper internal constructor(
    override val fromCtx: IntentHelper.Ctx,
    internal var ifOk: ((ActivityResult) -> Boolean)? = null,
    override val link: LinkHelper = LinkHelper(),
) : IntentHelper.Ctx(fromCtx), IWrapper {
    override fun request(l: ContractHelper.OnResultListener<ActivityResult>) {
        act.lifecycleScope.launch {
            // 最后一个要自己调用返回回调
            if (runAllIfOkUntilSelf()) return@launch toCtx().request(l)
            l.onResult(ActivityResult(Activity.RESULT_CANCELED, null))
        }
    }

    override fun toString(): String {
        return "ICTXWrapper(${super.toString()})"
    }

    override suspend fun runIfOk(): Boolean {
        val res = IntentHelper.Request(toCtx()).request()
        return ifOk?.invoke(res) ?: false
    }

    private fun toCtx() = IntentHelper.Ctx(act, fm, idc).apply {
        this.input = this@ICTXWrapper.input
        this.mapper = this@ICTXWrapper.mapper
    }
}

internal class JCTXWrapper internal constructor(
    override val fromCtx: JudgeIntentHelper.Ctx,
    internal var ifOk: ((Boolean) -> Boolean)? = null,
    override val link: LinkHelper = LinkHelper()
) : JudgeIntentHelper.Ctx(fromCtx), IWrapper {
    override fun request(l: ContractHelper.OnResultListener<Boolean>) {
        act.lifecycleScope.launch {
            // 最后一个要自己调用返回回调
            if (runAllIfOkUntilSelf()) return@launch toCtx().request(l)
            l.onResult(false)
        }
    }

    override fun toString(): String {
        return "JCTXWrapper(${super.toString()})"
    }

    override suspend fun runIfOk(): Boolean {
        val res = JudgeIntentHelper.Request(toCtx()).request()
        return ifOk?.invoke(res) ?: false
    }

    private fun toCtx() = JudgeIntentHelper.Ctx(act, fm, judge, jdc).apply {
        this.input = this@JCTXWrapper.input
        this.mapper = this@JCTXWrapper.mapper
    }
}

class ContactHelper internal constructor(private val wrapper: IWrapper) {
    fun permission(permissions: Array<String>) = PermissionHelper.With(wrapper.newPCTWrapper(permissions))
    fun dialogBefore(dialog: IntentDialogCreator) = IntentHelper.Intent(wrapper.newICTWrapper(dialog))
    fun intent(intent: android.content.Intent) = IntentHelper.Intent(wrapper.newICTWrapper()).intent(intent)
    fun judge(judge: Judge) = JudgeIntentHelper.Dialog(wrapper.newJCTXWrapper(judge))

    override fun toString(): String {
        return wrapper.toString()
    }
}

internal class LinkHelper {
    var last: IWrapper? = null
    var next: IWrapper? = null
}