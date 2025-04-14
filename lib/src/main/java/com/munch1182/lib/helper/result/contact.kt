package com.munch1182.lib.helper.result

import androidx.activity.result.ActivityResult
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.UnSupportImpl

/**
 * 1. 将所有的ctx包裹对应ctx子类的[ContactHelper.CtxWrapper], 并传回原有ResultHelper的对应类中代替原有的Ctx，用于附加更多的参数
 *
 * 2. [ifOk]函数返回的[ContactHelper]模拟了原有ResultHelper的入口函数，作为实际的组合函数，链接前后顺序
 * @see [ContactHelper.CtxWrapper.newPermission]
 * @see [ContactHelper.CtxWrapper.newIntent]
 * @see [ContactHelper.CtxWrapper.newJudge]
 * @see [ContactHelper.CtxWrapper.contactNext]
 *
 * 3. 当调用ResultHelper.Request.request时，会调用[ContactHelper.CtxWrapper]的对应方法
 * 该方法只处理逻辑：寻找第一个请求并依次调用原有的ctx进行执行，当原有ctx执行完毕后，调用[ifOk]进行判断，成功则执行下一个请求
 * (所有的请求都在最后一个ResultHelper.Request.request的函数内执行)
 * @see [ContactHelper.CtxWrapperLinkImpl.runFromFirst]
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

fun IntentHelper.Request.ifOk(ifOk: IfOk<ActivityResult>): ContactHelper {
    val ctx = if (ctx is ContactHelper.ICtxWrapper) ctx else ContactHelper.ICtxWrapper(ctx)
    return ContactHelper(ctx.apply { this.ifOk = ifOk })
}

fun JudgeHelper.Request.ifOk(ifOk: IfOk<Boolean>): ContactHelper {
    val ctx = if (ctx is ContactHelper.JCtxWrapper) ctx else ContactHelper.JCtxWrapper(ctx)
    return ContactHelper(ctx.apply { this.ifOk = ifOk })
}

class ContactHelper internal constructor(private val ctx: CtxWrapper) {

    fun permission(permission: Array<String>) = ctx.newPermission().permission(permission)
    fun intent(intent: android.content.Intent) = ctx.newIntent().intent(intent)
    fun judge(judge: JudgeHelper.OnJudge) = ctx.newJudge().judge(judge)

    internal class PCtxWrapper internal constructor(
        ctx: PermissionHelper.Ctx, internal var ifOk: IfOk<Map<String, PermissionHelper.Result>>? = null,
    ) : PermissionHelper.Ctx(ctx), CtxWrapper, CtxWrapperLink by CtxWrapperLinkImpl() {
        override fun request(l: OnResultListener<Map<String, PermissionHelper.Result>>) {
            /*super.request(l)*/
            runFromFirst()
        }
    }

    internal class ICtxWrapper internal constructor(
        ctx: IntentHelper.Ctx, internal var ifOk: IfOk<ActivityResult>? = null,
    ) : IntentHelper.Ctx(ctx), CtxWrapper, CtxWrapperLink by CtxWrapperLinkImpl() {
        override fun request(l: OnResultListener<ActivityResult>) {
            /*super.request(l)*/
            runFromFirst()
        }
    }

    internal class JCtxWrapper internal constructor(
        ctx: JudgeHelper.Ctx, internal var ifOk: IfOk<Boolean>? = null,
    ) : JudgeHelper.Ctx(ctx), CtxWrapper, CtxWrapperLink by CtxWrapperLinkImpl() {
        override fun request(l: OnResultListener<Boolean>) {
            /*super.request(l)*/
            runFromFirst()
        }

    }

    internal interface CtxWrapper : CtxWrapperLink {
        fun newPermission() = PermissionHelper(newPCtxWrapper.apply { this@CtxWrapper.contactNext(this) })
        fun newIntent() = IntentHelper(newICtxWrapper.apply { this@CtxWrapper.contactNext(this) })
        fun newJudge() = JudgeHelper(newJCtxWrapper.apply { this@CtxWrapper.contactNext(this) })

        val contractCtx: ContractHelper.Ctx<*, *, *> get() = if (this is ContractHelper.Ctx<*, *, *>) this else throw UnSupportImpl()
        val newPCtxWrapper get() = PCtxWrapper(PermissionHelper.Ctx(contractCtx.act, contractCtx.fm))
        val newICtxWrapper get() = ICtxWrapper(IntentHelper.Ctx(contractCtx.act, contractCtx.fm))
        val newJCtxWrapper get() = JCtxWrapper(JudgeHelper.Ctx(contractCtx.act, contractCtx.fm))

        suspend fun runIfOk(): Boolean = false
    }

    internal interface CtxWrapperLink {
        val next: CtxWrapper?
        fun contactNext(next: CtxWrapper)
        fun runFromFirst()
    }

    internal class CtxWrapperLinkImpl : CtxWrapperLink {
        private var _last: CtxWrapper? = null
        private var _next: CtxWrapper? = null
        override val next: CtxWrapper? get() = _next

        override fun contactNext(next: CtxWrapper) {
            /* if (next !is CtxWrapperLinkImpl) throw UnSupportImpl()
             this._next = next
             next._last = this*/
        }

        override fun runFromFirst() {
            /*var first: CtxWrapperLinkImpl = this
            while (first._last != null) {
                first = first._last!!
            }

            var curr: CtxWrapperLink? = first
            while (curr != null) {
                val res = curr.runIfOk()
                curr = curr.next
            }*/
        }
    }
}

fun ContactHelper.permission(vararg permission: String) = permission(permission.copyInto(Array(permission.size) { "" }))