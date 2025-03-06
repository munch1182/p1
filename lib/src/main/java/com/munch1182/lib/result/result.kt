package com.munch1182.lib.result

import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.findActivity
import com.munch1182.lib.helper.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ResultHelper private constructor(
    private val fm: FragmentManager,
    private val builder: Builder,
    private val l: OnResultListener
) {


    companion object {

        fun init(activity: FragmentActivity) =
            Builder(activity, activity.supportFragmentManager)

        fun init(fragment: Fragment) =
            Builder(fragment.requireContext(), fragment.childFragmentManager)

        // 还未发起任何请求或者跳转
        const val STATE_BEFORE_REQUEST = 0

        // 发起了请求，但被拒绝了
        const val STATE_DENIED = 1

        // 发起了请求，但被拒绝了，并且选择了不再询问
        const val STATE_NOT_ASK_FOREVER = 2
    }

    private val fragment: InvisibleFragment
        get() {
            var fragment = fm.findFragmentByTag(InvisibleFragment.TAG) as? InvisibleFragment
            if (fragment == null) {
                fragment = InvisibleFragment()
                fm.beginTransaction()
                    .add(fragment, InvisibleFragment.TAG)
                    .commitNowAllowingStateLoss()
            }
            return fragment
        }

    private val scope: CoroutineScope
        get() = fragment.scope

    internal fun request() {
        scope.launch(Dispatchers.Main) {
            builder.reqs.forEach {
                val ctx = builder.context
                if (it is INTENT && it.judge?.invoke(ctx) == true) {
                    return@forEach
                }
                it.update(STATE_BEFORE_REQUEST)
                // 请求前的权限解释
                val isCancel = builder.explain?.invoke(it)?.create(ctx)?.showExplain() ?: false
                if (isCancel) return@forEach

                it.requestLogic()
            }
        }
    }

    /**
     * 循环请求和弹窗
     * 如果被拒绝且不再询问，则返回
     * 如果循环此次超过limit，则返回
     */
    private suspend fun Request.requestLogic(limit: Int = 3) {
        if (limit <= 0) return
        val isIntentOk = request()
        if (state == STATE_NOT_ASK_FOREVER) {
            return
        }
        val dialog = builder.explain?.invoke(this)
        if (dialog != null) {
            // 请求前的权限解释
            val isCancel = dialog.create(builder.context).showExplain()
            if (isCancel) return
        }
        val newReq = judge(builder.context, isIntentOk)
        newReq?.requestLogic(limit - 1)
    }

    private suspend fun Request.request(): Boolean {
        return suspendCoroutine { c ->
            when (this) {
                is PERMISSION -> fragment.startPermission(permission) { c.resume(true) }
                is INTENT -> fragment.startActivity4Result(intent) { isOk, _ -> c.resume(isOk) }
            }
        }
    }

    private suspend fun IResultDialog.showExplain(): Boolean {
        return suspendCoroutine { c -> addOnDismissListener { c.resume(!it) }.show() }
    }


    interface OnResultListener {
        fun onGrantAll() {}
        fun onDenied(denied: Array<Request>) {}
        fun onGranted(granted: Array<Request>) {}
    }

    sealed class Request(@State internal var state_: Int) {
        internal companion object {
            fun build(permission: Array<String>) = PERMISSION(permission, STATE_BEFORE_REQUEST)
            fun build(intent: Intent) = INTENT(null, intent, STATE_BEFORE_REQUEST)
            fun build(judge: (Context) -> Boolean, intent: Intent) =
                INTENT(judge, intent, STATE_BEFORE_REQUEST)
        }

        internal fun update(state: Int) {
            this.state_ = state
        }

        internal fun judge(context: Context, isIntentOk: Boolean): Request? {
            when (this) {
                is PERMISSION -> {
                    val act = context.findActivity() ?: return null
                    val res = PermissionHelper.collectAllRationale(act, permission)
                    if (res.rationale.isEmpty() && res.denied.isEmpty()) {
                        return null
                    }
                    return build(res.denied)
                }

                is INTENT -> {
                    val judge = judge?.invoke(context) ?: isIntentOk
                    return if (judge) null else this
                }
            }
        }

        @State
        val state: Int
            get() = state_
    }

    class PERMISSION(val permission: Array<String>, @State state: Int) :
        Request(state)

    class INTENT(
        val judge: ((Context) -> Boolean)?,
        val intent: Intent,
        @State state: Int
    ) : Request(state)

    @Target(
        AnnotationTarget.TYPE,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.PROPERTY,
    )
    @IntDef(STATE_BEFORE_REQUEST, STATE_DENIED, STATE_NOT_ASK_FOREVER)
    annotation class State


    class Builder internal constructor(
        internal val context: Context,
        private val fm: FragmentManager
    ) {
        internal val reqs = arrayListOf<Request>()
        internal var explain: ((Request) -> IResultDialog)? = null

        fun with(vararg permission: String): Builder {
            reqs.add(Request.build(arrayOf(*permission)))
            return this
        }

        fun with(intent: Intent): Builder {
            reqs.add(Request.build(intent))
            return this
        }

        fun with(judge: (Context) -> Boolean, intent: Intent): Builder {
            reqs.add(Request.build(judge, intent))
            return this
        }

        /**
         * 除了个别权限需要重点多次提醒外，不建议反复弹出提醒权限
         * 而是应该在调用该功能的时候弹出弹窗，每次根据功能(和是否被永久拒绝)弹出(不一样的)弹窗，并且被拒绝就终止流程
         */
        fun explain(explain: (request: Request) -> IResultDialog): Builder {
            this.explain = explain
            return this
        }

        fun request(request: OnResultListener) {
            ResultHelper(fm, this, request).request()
        }

        fun request(onGrantAll: () -> Unit) {
            request(object : OnResultListener {
                override fun onGrantAll() {
                    onGrantAll()
                }
            })
        }
    }

    interface IResultDialog {
        fun create(context: Context): IResultDialog {
            return this
        }

        fun show()

        fun addOnDismissListener(listener: (confirm: Boolean) -> Unit): IResultDialog {
            return this
        }
    }
}





