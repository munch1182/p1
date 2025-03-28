package com.munch1182.lib.helper.result


import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.munch1182.lib.base.Logger

class JudgeHelper internal constructor(private val act: FragmentActivity, private val fm: FragmentManager) {

    companion object {
        fun init(act: FragmentActivity) = JudgeHelper(act, act.supportFragmentManager)
        fun init(frag: Fragment) = JudgeHelper(frag.requireActivity(), frag.childFragmentManager)
        internal val log = Logger("judge: ")
    }

    fun judge(judge: OnJudge) = Input(Ctx(act, fm, judge))

    class Input internal constructor(private val ctx: Ctx) {
        fun intent(i: Intent) = Request(ctx.apply { input = i })
    }

    class Request internal constructor(private val ctx: Ctx) {
        fun request(l: ContractHelper.OnResultListener<Boolean>) = ctx.requestIntent(l)
    }

    internal open class Ctx internal constructor(
        act: FragmentActivity, fm: FragmentManager, internal var judge: OnJudge
    ) : ContractHelper.Ctx<Intent, ActivityResult>(act, fm, ActivityResultContracts.StartActivityForResult()) {
        fun requestIntent(l: ContractHelper.OnResultListener<Boolean>) {
            if (judge.onJudge(act).apply { log.logStr("first judge: $this") }) return l.onResult(true)
            PermissionIntentFragment.get(fm).launch(input!!) {
                l.onResult(judge.onJudge(act).apply { log.logStr("second judge: $this") })
            }
        }
    }

    @FunctionalInterface
    fun interface OnJudge {
        fun onJudge(ctx: Context): Boolean
    }
}