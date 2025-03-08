package com.munch1182.lib.result

import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.lib.base.asPermissionCheckRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias PermissionDialogCreator = Context.(Array<String>, PermissionHelper.State) -> ResultDialog?

class PermissionHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {
    companion object {
        fun init(act: FragmentActivity) = PermissionHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = PermissionHelper(frag.requireActivity(), frag.childFragmentManager)
    }

    /**
     * 权限的请求顺序与传入的权限顺序无关
     * 如果权限请求有顺序(如一个关键权限+几个附加权限)，不要一次性传入
     */
    fun permission(permissions: Array<String>) = Dialog(Ctx(act, fm).apply { this.input = permissions })

    class Dialog internal constructor(internal val ctx: Ctx) {
        fun dialogWhen(creator: PermissionDialogCreator) = Request(ctx.apply { this.pdc = creator })
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

        val isBefore: Boolean
            get() = this is Before
        val isDenied: Boolean
            get() = this is Denied
        val isDeniedForever: Boolean
            get() = this is DeniedForever

        internal fun nextState(): State {
            return when (this) {
                Before -> Denied
                Denied -> DeniedForever
                DeniedForever -> DeniedForever
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

    internal open class Ctx(act: FragmentActivity, fm: FragmentManager, internal var pdc: PermissionDialogCreator? = null) :
        ContractHelper.Ctx<Array<String>, Map<String, Boolean>, Map<String, Result>>(act, fm, ActivityResultContracts.RequestMultiplePermissions()) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.pdc) {
            this.input = ctx.input
            this.mapper = ctx.mapper
        }

        override fun request(l: ContractHelper.OnResultListener<Map<String, Result>>) {
            act.lifecycleScope.launch { requestLogic(State.Before, l) }
        }

        private suspend fun requestLogic(state: State, l: ContractHelper.OnResultListener<Map<String, Result>>) {
            // 判断权限，返回被拒绝的权限的集合(不包括被永久拒绝的)
            val (request, map) = dispatchPermission(state)
            // 如果只有被授权的和被永久拒绝的，则作为结果返回
            if (request.isEmpty()) return l.onResult(map)
            // 如果还有被拒绝的权限，则判断是否需要弹窗，如果弹窗回调了取消，则返回
            // 如果没有弹窗，除了第一次请求前，其它时候直接返回(也就是说后续要进行权限请求都需要弹窗确认)
            if (!isPermissionDialogContinue(request, state)) return l.onResult(map)
            // 执行权限请求，并利用协程同步返回回调
            collapseRequest(request)
            // 重复步骤
            requestLogic(state.nextState(), l)
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