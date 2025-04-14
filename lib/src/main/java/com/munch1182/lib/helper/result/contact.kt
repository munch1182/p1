package com.munch1182.lib.helper.result

import androidx.activity.result.ActivityResult
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.UnSupportImpl
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *
 * 使用：
 * 1. 正常使用[PermissionHelper]/[IntentHelper]/[JudgeHelper]，并在request位置调用[ifOk]函数作为代替
 * 2. 用[ContactHelper.requestAll]执行，当所以[ifOk]判断成功后，该回调返回true，任一[ifOk]判断失败则立刻回调false
 *
 * [IfOk]不会执行请求，原有的[PermissionHelper.Request.request]只会执行自己的请求
 *
 * 1. 将所有的ctx包裹对应ctx子类的[ContactHelper.CtxWrapper], 并传回原有ResultHelper的对应类中代替原有的Ctx，用于附加更多的参数
 *
 * 2. [ifOk]函数返回的[ContactHelper]模拟了原有ResultHelper的入口函数，作为实际的组合函数，链接前后顺序
 * @see [ContactHelper.CtxWrapper.newPermission]
 * @see [ContactHelper.CtxWrapper.newIntent]
 * @see [ContactHelper.CtxWrapper.newJudge]
 * @see [ContactHelper.CtxWrapper.plusAssign]
 *
 * 3. 当调用[ContactHelper.requestAll]，开始执行逻辑：
 * 寻找第一个请求并依次调用原有的ctx进行执行，当原有ctx执行完毕后，调用[ifOk]进行判断，成功则执行下一个请求
 * @see ContactHelper.requestAll
 * @see ContactHelper.CtxWrapper.runFromFirst
 *
 */
@FunctionalInterface
fun interface IfOk<T> {
    fun ifOk(any: T): Boolean
}

/**
 *当[ifOk]返回true时，继续执行下一个请求
 * 该方法不会立即执行请求
 */
fun PermissionHelper.Request.ifOk(ifOk: IfOk<Map<String, PermissionHelper.Result>>): ContactHelper {
    // 第一个ifOk不是ContactHelper包裹的Ctx，则创建一个包裹的Ctx
    val ctx = if (ctx is ContactHelper.PCtxWrapper) ctx else ContactHelper.PCtxWrapper(ctx)
    return ContactHelper(ctx.apply { this.ifOk = ifOk })
}

fun PermissionHelper.Request.ifAllGranted() = ifOk { it.isAllGranted }

fun IntentHelper.Request.ifOk(ifOk: IfOk<ActivityResult>): ContactHelper {
    val ctx = if (ctx is ContactHelper.ICtxWrapper) ctx else ContactHelper.ICtxWrapper(ctx)
    return ContactHelper(ctx.apply { this.ifOk = ifOk })
}

fun JudgeHelper.Request.ifOk(ifOk: IfOk<Boolean>): ContactHelper {
    val ctx = if (ctx is ContactHelper.JCtxWrapper) ctx else ContactHelper.JCtxWrapper(ctx)
    return ContactHelper(ctx.apply { this.ifOk = ifOk })
}

fun JudgeHelper.Request.ifTrue() = ifOk { it }
fun JudgeHelper.Request.ifFalse() = ifOk { it }

class ContactHelper internal constructor(private val ctx: CtxWrapper) {

    fun permission(permission: Array<String>) = ctx.newPermission().permission(permission)
    fun intent(intent: android.content.Intent) = ctx.newIntent().intent(intent)
    fun judge(judge: JudgeHelper.OnJudge) = ctx.newJudge().judge(judge)

    /**
     * 将该方法独立出来，而不调用原request，用于统一回调
     * 因此，最后一个请求也需要调用[ifOk]
     */
    fun requestAll(l: OnResultListener<Boolean>) {
        ctx.contractCtx.act.launchIO { l.onResult(ctx.runFromFirst()) }
    }

    internal class PCtxWrapper internal constructor(
        ctx: PermissionHelper.Ctx, internal var ifOk: IfOk<Map<String, PermissionHelper.Result>>? = null,
        override val link: CtxWrapperLinkManager = CtxWrapperLinkManager()
    ) : PermissionHelper.Ctx(ctx), CtxWrapper {

        override suspend fun runIfOk(): Boolean {
            val execute = suspendCoroutine { c -> super.request { c.resume(it) } }
            return ifOk?.ifOk(execute) ?: false
        }

        override fun toString() = "PermissionCtxWrapper(${super.toString()})"
    }

    internal class ICtxWrapper internal constructor(
        ctx: IntentHelper.Ctx, internal var ifOk: IfOk<ActivityResult>? = null,
        override val link: CtxWrapperLinkManager = CtxWrapperLinkManager()
    ) : IntentHelper.Ctx(ctx), CtxWrapper {

        override suspend fun runIfOk(): Boolean {
            val execute = suspendCoroutine { c -> super.request { c.resume(it) } }
            return ifOk?.ifOk(execute) ?: false
        }

        override fun toString() = "IntentCtxWrapper(${super.toString()})"
    }

    internal class JCtxWrapper internal constructor(
        ctx: JudgeHelper.Ctx, internal var ifOk: IfOk<Boolean>? = null,
        override val link: CtxWrapperLinkManager = CtxWrapperLinkManager()
    ) : JudgeHelper.Ctx(ctx), CtxWrapper {

        override suspend fun runIfOk(): Boolean {
            val execute = suspendCoroutine { c -> super.request { c.resume(it) } }
            return ifOk?.ifOk(execute) ?: false
        }

        override fun toString() = "JudgeCtxWrapper(${super.toString()})"
    }

    internal interface CtxWrapper {
        fun newPermission() = PermissionHelper(newPCtxWrapper.apply { this@CtxWrapper += this })
        fun newIntent() = IntentHelper(newICtxWrapper.apply { this@CtxWrapper += this })
        fun newJudge() = JudgeHelper(newJCtxWrapper.apply { this@CtxWrapper += this })

        val contractCtx: ContractHelper.Ctx<*, *, *> get() = if (this is ContractHelper.Ctx<*, *, *>) this else throw UnSupportImpl()
        val newPCtxWrapper get() = PCtxWrapper(PermissionHelper.Ctx(contractCtx.act, contractCtx.fm))
        val newICtxWrapper get() = ICtxWrapper(IntentHelper.Ctx(contractCtx.act, contractCtx.fm))
        val newJCtxWrapper get() = JCtxWrapper(JudgeHelper.Ctx(contractCtx.act, contractCtx.fm))

        suspend fun runIfOk(): Boolean = false

        val link: CtxWrapperLinkManager

        operator fun plusAssign(ctx: CtxWrapper) {
            this.link.next = ctx
            ctx.link.last = this
        }

        suspend fun runFromFirst(): Boolean {
            val log = log(false)
            log.logStr("find fist")
            var first: CtxWrapper = this
            while (first.link.last != null) {
                first = first.link.last!!
            }
            log.logStr("find fist: $first")

            log.logStr("start run loop")
            var curr: CtxWrapper? = first
            while (curr != null) {
                log.logStr("run: $curr")
                val ifOk = curr.runIfOk()
                log.logStr("run: $curr => $ifOk")
                if (!ifOk) {
                    log.logStr("break run loop: false")
                    return false
                }
                curr = curr.link.next
            }
            log.logStr("end run loop: true")
            return true
        }
    }

    internal class CtxWrapperLinkManager {
        var next: CtxWrapper? = null
        var last: CtxWrapper? = null
    }
}

fun ContactHelper.permission(vararg permission: String) = permission(permission.copyInto(Array(permission.size) { "" }))