package com.munch1182.lib.helper.result

import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.asPermissionCheck
import com.munch1182.lib.base.asPermissionCheckRationale
import com.munch1182.lib.base.newLog
import com.munch1182.lib.base.onDestroyed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionHelper internal constructor(internal val ctx: Ctx) {

    companion object {
        fun init(act: FragmentActivity) = PermissionHelper(Ctx(act, act.supportFragmentManager))
        fun init(frag: Fragment) = PermissionHelper(Ctx(frag.requireActivity(), frag.childFragmentManager))
        internal val log = ContractHelper.log.newLog("permission")
    }

    fun permission(permission: Array<String>) = Input(ctx.apply { input = permission })

    class Input internal constructor(ctx: Ctx) : Request(ctx) {
        // 不建议在[State.Before]时显示弹窗，否则需要解释[State.Before]的弹窗和[State.DeniedForever]的弹窗前后依次出现的语境
        fun dialogWhen(dp: PermissionCanRequestDialogProvider?) = Intent(ctx.apply { this.dp = dp })
    }

    class Intent internal constructor(ctx: Ctx) : Request(ctx) {
        fun manualIntent(intent: android.content.Intent = appSetting()) = Request(ctx.apply { this.intent = intent })
    }

    open class Request internal constructor(internal val ctx: Ctx) {
        open fun request(l: OnResultListener<Map<String, Result>>) = ctx.request(l)
    }

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager,
        var dp: PermissionCanRequestDialogProvider? = null, var intent: android.content.Intent? = null
    ) : ContractHelper.Ctx<Array<String>, Map<String, Boolean>, Map<String, Result>>(act, fm, ActivityResultContracts.RequestMultiplePermissions()) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.dp, ctx.intent) {
            this.input = ctx.input
        }

        override fun requestLaunch(l: OnResultListener<Map<String, Boolean>>) = PermissionIntentFragment.get(fm).launch(input!!, l)

        override fun request(l: OnResultListener<Map<String, Result>>) {
            act.lifecycleScope.launch(Dispatchers.IO) {
                var res = requestCircle(State.Before)
                res = needToIntent(res)
                log.logStr("complete: $res")
                l.onResult(res)
            }
        }

        private suspend fun requestCircle(state: State): Map<String, Result> {
            val (req, res) = dispatchPermissionResult(state)
            log.logStr("state:$state: dispatchPermissionResult: req: ${req}, $res")
            if (req.isEmpty()) return res.apply { log.logStr("now: no denied to req") }
            val dialogAllow = dialogCollapse(state, req.toTypedArray())
            log.logStr("state:$state: dialogResult: $dialogAllow")
            if (!dialogAllow) return res.apply { log.logStr("now: dialog denied") }
            if (state.isDeniedForever) return res.apply { log.logStr("now: state $state") }
            log.logStr("start permission request")
            requestCollapse()
            log.logStr("start permission request back")
            return requestCircle(state.nextState())
        }

        private suspend fun needToIntent(res: Map<String, Result>): Map<String, Result> {
            val state = State.DeniedForever
            val intent = intent ?: return res.apply { log.logStr("now no intent") }
            val dfReq = res.filter { it.value == Result.DeniedForever }.keys.toTypedArray()
            if (dfReq.isEmpty()) return res.apply { log.logStr("now: no deniedforever to handle") }
            val dialogAllow = dialogCollapse(state, dfReq)
            log.logStr("state:$state: dialogResult: $dialogAllow")
            if (!dialogAllow) return res.apply { log.logStr("now: dialog denied") }
            log.logStr("start intent: $intent")
            suspendCoroutine { c -> IntentHelper(IntentHelper.Ctx(act, fm)).intent(intent).request { c.resume(it) } }
            log.logStr("start intent back")
            val (_, newRes) = dispatchPermissionResult(state)
            log.logStr("state:$state: dispatchPermissionResult: $newRes")
            return newRes
        }

        // 为了避免无法取消的无限请求，如果在被拒绝及之后没有设置用户提示，默认等同与用户拒绝
        private suspend fun dialogCollapse(state: State, permission: Array<String>): Boolean {
            if (permission.isEmpty()) return true
            return withContext(Dispatchers.Main) {
                val dialog = dp?.onCreateDialog(act, state, permission)
                    ?: return@withContext state.isBefore.apply { log.logStr("dialog result $this as state is $state") }
                suspendCoroutine { c ->
                    dialog.lifecycle.onDestroyed { c.resume(dialog.result?.isAllow ?: false) }
                    log.logStr("request dialog for ${permission.joinToString()} when $state")
                    dialog.show()
                }
            }
        }

        private suspend fun requestCollapse(): Map<String, Boolean> {
            return withContext(Dispatchers.Main) { suspendCoroutine { c -> requestLaunch { c.resume(it) } } }
        }

        private fun dispatchPermissionResult(state: State): Pair<MutableList<String>, HashMap<String, Result>> {
            val permissions = input!!
            val result = HashMap<String, Result>(permissions.size)
            val request = mutableListOf<String>()
            permissions.forEach {
                result[it] = if (it.asPermissionCheck()) {
                    Result.Granted
                } else if (!state.isBefore && !it.asPermissionCheckRationale(act)) {
                    Result.DeniedForever
                } else {
                    request.add(it)
                    Result.Denied
                }
            }
            return request to result
        }
    }

    @FunctionalInterface
    fun interface PermissionCanRequestDialogProvider {
        fun onCreateDialog(ctx: Context, state: State, permission: Array<String>): AllowDenyDialog?
    }

    sealed class State {

        data object Before : State() // 在第一次判断权限后第一次请求前
        data object Denied : State() //被判断权限拒绝时
        data object DeniedForever : State() // 被判断为权限永久拒绝时

        val isBefore: Boolean get() = this is Before
        val isDenied: Boolean get() = this is Denied
        val isDeniedForever: Boolean get() = this is DeniedForever

        internal fun nextState(): State {
            return when (this) {
                Before -> Denied
                Denied -> Denied // Denied不会自动更改到DeniedForever，需要逻辑判断后更改
                DeniedForever -> DeniedForever
            }
        }
    }

    sealed class Result {
        data object Granted : Result()
        data object Denied : Result()
        data object DeniedForever : Result()

        val isGranted: Boolean get() = this is Granted
        val isDenied: Boolean get() = this is Denied
        val isDeniedForever: Boolean get() = this is DeniedForever
    }
}