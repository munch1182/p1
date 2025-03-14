package com.munch1182.lib.result

import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.appDetailsPage
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.lib.base.asPermissionCheckRationale
import com.munch1182.lib.result.PermissionHelper.Ctx
import com.munch1182.lib.result.PermissionHelper.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias PermissionDialogCreator = Context.(Array<String>, State) -> ResultDialog?

/**
 * 权限请求处理，可以处理权限的请求、弹窗、被永久拒绝后的跳转
 *
 * 处理逻辑: [Ctx.requestLogic]
 * 注意：除了[State.Before]时不需要[PermissionDialogCreator]返回不为null的[ResultDialog]外，其余流程中，必须有返回才能继续流程
 * 例如：不调用[PermissionHelper.With.dialogWhen]传入[PermissionDialogCreator]但调用了[PermissionHelper.With.intentIfDeniedForever]传入了[intent]
 *      如果第一次调用发起请求被拒绝，则回调失败。再次调用发起请求被永久拒绝，则回调失败。(不会跳转[intent]因为没有传入[PermissionDialogCreator])
 *
 * 虽然可以传入多个权限，但如无特殊情况，最多传入同组的多个权限，多个权限最好分次请求。
 *
 * 现有bug: 如果第一次发起权限请求时，用户选择取消权限而不是拒绝权限(点击系统权限弹窗外的部分取消权限弹窗)，则会被误判为权限被永久拒绝
 * 原有是使用了[String.asPermissionCheckRationale]来判断永久拒绝，这这种情景下该方法无法区分
 * 暂无法处理
 *
 */
class PermissionHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {
    companion object {
        fun init(act: FragmentActivity) = PermissionHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = PermissionHelper(frag.requireActivity(), frag.childFragmentManager)
    }

    /**
     * 权限的请求顺序与传入的权限顺序无关
     * 如果权限请求有顺序(如一个关键权限+几个附加权限)，不要一次性传入
     */
    fun permission(permissions: Array<String>) = With(Ctx(act, fm).apply { this.input = permissions })

    class With internal constructor(internal val ctx: Ctx) {
        fun dialogWhen(creator: PermissionDialogCreator) = Dialog(ctx.apply { this.pdc = creator })
        fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) = Request(ctx).request(l)
        fun intentIfDeniedForever(intent: android.content.Intent = appDetailsPage) = Intent(ctx.apply { this.deniedIntent = intent })
    }

    class Intent internal constructor(internal val ctx: Ctx) {
        fun dialogWhen(creator: PermissionDialogCreator) = Request(ctx.apply { this.pdc = creator })
        fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) = Request(ctx).request(l)
    }

    class Dialog internal constructor(internal val ctx: Ctx) {
        fun intentIfDeniedForever(intent: android.content.Intent = appDetailsPage) = Request(ctx.apply { this.deniedIntent = intent })
        fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) = Request(ctx).request(l)
    }

    class Request internal constructor(internal val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) = ctx.request(l)
    }

    sealed class State {


        // 在第一次判断权限后第一次请求前
        data object Before : State()

        // 在第一次请求后被判断拒绝
        data object Denied : State()

        // 在第一次请求后被判断永久拒绝
        // 或第二次请求权限时选择了永久拒绝
        data object DeniedForever : State()

        // 被永久拒绝准备跳转intent时
        data object Intent : State()


        val isBefore: Boolean
            get() = this is Before
        val isDenied: Boolean
            get() = this is Denied
        val isDeniedForever: Boolean
            get() = this is DeniedForever
        val isIntent: Boolean
            get() = this is Intent

        internal fun nextState(): State {
            return when (this) {
                Before -> Denied
                Denied -> DeniedForever
                DeniedForever -> DeniedForever // DeniedForever不会自然跳转到Intent阶段，除非调用了[intentIfDeniedForever]
                Intent -> Intent
            }
        }
    }

    sealed class Result {
        data object Granted : Result()
        data object Denied : Result()
        data object DeniedForever : Result()

        val isGranted: Boolean
            get() = this is Granted
        val isDenied: Boolean
            get() = this is Denied
        val isDeniedForever: Boolean
            get() = this is DeniedForever
    }

    internal open class Ctx(
        act: FragmentActivity, fm: FragmentManager,
        internal var pdc: PermissionDialogCreator? = null,
        internal var deniedIntent: android.content.Intent? = null
    ) :
        ContractHelper.Ctx<Array<String>, Map<String, Boolean>, Map<String, Result>>(act, fm, ActivityResultContracts.RequestMultiplePermissions()) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.pdc) {
            this.input = ctx.input
            this.mapper = ctx.mapper
        }

        override fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) {
            act.lifecycleScope.launch {
                val res = deniedIntent?.let { requestToIntent(it) } ?: requestLogic(State.Before)
                l.onResult(res)
            }
        }

        private suspend fun requestToIntent(intent: android.content.Intent): Map<String, Result> {
            // 如果有intent需要跳转，逻辑附加在最后不更改原有逻辑
            val res = requestLogic(State.Before)
            val deniedForever = res.mapNotNull { p -> if (p.value.isDeniedForever) p.key else null }.toTypedArray()
            if (deniedForever.isNotEmpty()) {
                // 必须要弹窗才能跳转
                if (isPermissionDialogContinue(deniedForever, State.Intent)) {
                    // 跳转intent
                    IntentHelper(act, fm).intent(intent).request()
                    // 跳转之后所有权限都需要重新判断，但是不必再请求了
                    return dispatchPermission(State.Intent).second
                }
            }
            return res
        }

        private suspend fun requestLogic(state: State): Map<String, Result> {
            // 判断权限，返回被拒绝的权限的集合(不包括被永久拒绝的)
            val (request, map) = dispatchPermission(state)
            Loglog.log("${state} -> ${map}")
            // 如果只有被授权的和被永久拒绝的，则作为结果返回
            if (request.isEmpty()) return map
            // 如果还有被拒绝的权限，则判断是否需要弹窗，如果弹窗回调了取消，则返回
            // 如果没有弹窗，除了第一次请求前，其它时候直接返回(也就是说后续要进行权限请求都需要弹窗确认)
            if (!isPermissionDialogContinue(request, state)) return map
            // 执行权限请求，并利用协程同步返回回调
            val a = collapseRequest(request)
            Loglog.log("${a}")
            // 重复步骤
            return requestLogic(state.nextState())
        }

        private fun dispatchPermission(state: State): Pair<Array<String>, HashMap<String, Result>> {
            val map = hashMapOf<String, Result>()
            val request = arrayListOf<String>()
            this.input?.forEach {
                map[it] = if (it.asPermissionCheck()) {
                    Result.Granted
                    // 只有在请求一次权限后，此判断才能用于判断是否是永久拒绝
                    // @see [com.munch1182.lib.base.PermissionKt.checkPermissionRationale] 权限被取消时会误判
                } else if (!state.isBefore && !it.asPermissionCheckRationale(act)) {
                    Result.DeniedForever
                } else {
                    request.add(it)
                    Result.Denied
                }
            }
            return request.toTypedArray() to map
        }

        private suspend fun collapseRequest(input: Array<String>): Map<String, Boolean> {
            return suspendCoroutine { c -> PermissionIntentFragment.get(fm).launchPermission(input) { c.resume(it) } }
        }

        /**
         * 当未传入[pdc]时，如果[state]为[PermissionHelper.State.isBefore]则返回true
         * 否则返回false
         * 当传入[pdc]时，调用[ResultDialog.show]并返回[ResultDialog.OnChoseListener]的回调
         * 就是说，[PermissionHelper.State.isBefore]之后的请求，都至少要有[pdc]传入且在弹窗中选择了继续才会返回true
         * 如果必须请求，可在弹窗中不允许取消(虽然这没什么意义)
         */
        private suspend fun isPermissionDialogContinue(permissions: Array<String>, state: State): Boolean {
            val dialog = pdc?.invoke(act, permissions, state) ?: return state.isBefore
            withContext(Dispatchers.Main) { dialog.show() }
            return suspendCoroutine { c -> dialog.setOnChoseListener { c.resume(it) } }
        }

        override fun toString(): String {
            return "PermissionHelper <- ${input?.joinToString(prefix = "[", postfix = "]")}"
        }
    }
}