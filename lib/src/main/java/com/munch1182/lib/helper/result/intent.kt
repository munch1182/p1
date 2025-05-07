package com.munch1182.lib.helper.result

import android.app.Activity
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
import com.munch1182.lib.helper.dialog.AllDenyDialogProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IntentHelper internal constructor(internal val ctx: Ctx) {

    companion object {
        fun init(act: FragmentActivity) = IntentHelper(Ctx(act, act.supportFragmentManager))
        fun init(frag: Fragment) = IntentHelper(Ctx(frag.requireActivity(), frag.childFragmentManager))
        internal val log = ContractHelper.log.newLog("intent")
    }

    fun intent(i: Intent) = Dialog(ctx.apply { input = i })

    class Dialog internal constructor(ctx: Ctx) : Request(ctx) {
        fun dialogBefore(dp: AllDenyDialogProvider) = Request(ctx.apply { ctx.dp = dp })
    }

    open class Request internal constructor(internal val ctx: Ctx) : ContractHelper.Request<Intent, ActivityResult, ActivityResult>(ctx)

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager,
        internal var dp: AllDenyDialogProvider? = null
    ) : ContractHelper.Ctx<Intent, ActivityResult, ActivityResult>(act, fm, ActivityResultContracts.StartActivityForResult()) {

        constructor(ctx: Ctx) : this(ctx.act, ctx.fm, ctx.dp) {
            this.input = ctx.input
        }

        override fun requestLaunch(l: OnResultListener<ActivityResult>) = PermissionIntentFragment.get(fm).launch(input!!, l)

        override fun request(l: OnResultListener<ActivityResult>) {
            act.lifecycleScope.launch {
                val dialog = dialogCollapse()
                if (!dialog) return@launch l.onResult(ActivityResult(Activity.RESULT_CANCELED, null)).apply { log.logStr("return after dialog manual cancel") }
                log.logStr("dialog result: true")
                return@launch requestLaunch(l)
            }
        }

        // 无弹窗也会跳转
        private suspend fun dialogCollapse(): Boolean {
            return withContext(Dispatchers.Main) {
                val dialog = dp?.onCreateDialog(act) ?: return@withContext log.logStr("dialog result true event no dialog provider").let { true }
                suspendCoroutine { c ->
                    dialog.lifecycle.onDestroyed { c.resume(dialog.result?.isAllow ?: false) }
                    log.logStr("request dialog before start intent")
                    dialog.show()
                }
            }
        }
    }
}