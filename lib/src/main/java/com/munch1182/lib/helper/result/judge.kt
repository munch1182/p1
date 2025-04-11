package com.munch1182.lib.helper.result


import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.OnResultListener
import com.munch1182.lib.base.newLog
import com.munch1182.lib.base.onDestroyed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JudgeHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = JudgeHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = JudgeHelper(frag.requireActivity(), frag.childFragmentManager)
        internal val log = ContractHelper.log.newLog("judge")
    }

    fun judge(judge: OnJudge) = Input(Ctx(act, fm, judge))

    class Input internal constructor(private val ctx: Ctx) {
        fun intent(i: Intent) = Dialog(ctx.apply { input = i })
    }

    class Dialog internal constructor(private val ctx: Ctx) : Req by Request(ctx) {
        fun dialogWhen(dp: IntentCanLaunchDialogProvider) = Request(ctx.apply { ctx.dp = dp })
    }

    interface Req {
        fun request(l: OnResultListener<Boolean>)
    }

    class Request internal constructor(private val ctx: Ctx) : Req {
        override fun request(l: OnResultListener<Boolean>) = ctx.requestIntent(l)
    }

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager, internal var judge: OnJudge, internal var dp: IntentCanLaunchDialogProvider? = null
    ) : ContractHelper.Ctx<Intent, ActivityResult>(act, fm, ActivityResultContracts.StartActivityForResult()) {
        override fun request(l: OnResultListener<ActivityResult>) = PermissionIntentFragment.get(fm).launch(input!!, l)
        internal fun requestIntent(l: OnResultListener<Boolean>) {
            act.lifecycleScope.launch(Dispatchers.IO) {
                val result = judgeCircle(State.Before)
                log.logStr("complete: judge: $result")
                l.onResult(result)
            }
        }

        private suspend fun judgeCircle(state: State): Boolean {
            if (judge.onJudge(act).apply { log.logStr("state:$state: judge: $this") }) return true
            val dialogAllow = dialogCollapse(state)
            log.logStr("state:$state: dialogResult: $dialogAllow")
            if (!dialogAllow) return false
            log.logStr("start intent request")
            withContext(Dispatchers.Main) { suspendCoroutine { c -> request { c.resume(it) } } }
            log.logStr("start intent request back")
            return judgeCircle(state.nextState)
        }

        private suspend fun dialogCollapse(state: State): Boolean {
            return withContext(Dispatchers.Main) {
                val dialog = dp?.onCreateDialog(act, state) ?: return@withContext state.isBefore.apply { PermissionHelper.log.logStr("dialog result $this as state is $state") }
                suspendCoroutine { c ->
                    dialog.lifecycle.onDestroyed { c.resume(dialog.result?.isAllow ?: false) }
                    PermissionHelper.log.logStr("request dialog when $state")
                    dialog.show()
                }
            }
        }
    }

    @FunctionalInterface
    fun interface IntentCanLaunchDialogProvider {
        fun onCreateDialog(ctx: Context, state: State): AllowDenyDialog?
    }

    @FunctionalInterface
    fun interface OnJudge {
        fun onJudge(ctx: Context): Boolean
    }

    sealed class State {
        data object Before : State()
        data object After : State()

        val isBefore: Boolean get() = this is Before
        val isAfter: Boolean get() = this is After

        internal val nextState: State
            get() = when (this) {
                Before -> After
                After -> After
            }
    }
}