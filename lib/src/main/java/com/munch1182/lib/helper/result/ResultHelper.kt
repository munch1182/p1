package com.munch1182.lib.helper.result

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.isCanAsk
import com.munch1182.lib.base.isGranted
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.removeFragmentIfExists
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.AllowDeniedDialog
import com.munch1182.lib.helper.isAllow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ResultHelper(private val fm: FragmentActivity?) {

    companion object {
        private const val TAG = "com.munch1182.lib.helper.result.ResultHelper"
    }

    /**
     * 请求权限并返回结果
     */
    fun permission(permissions: Array<String>) = PermissionsResultHelper(fm, permissions)

    /**
     * 跳转页面并返回结果
     */
    fun intent(intent: Intent) = IntentResultHelper(fm, intent)

    /**
     * 判断并跳转
     *
     * 适应场景：打开定位/蓝牙等
     */
    fun judge(judge: (Context) -> Boolean, intent: Intent) = JudgeHelper(fm, judge, intent)

    class PermissionsResultHelper(internal val fm: FragmentActivity?, private val permissions: Array<String>) {

        private val result = HashMap<String, PermissionResult>()
        private val canAsk = mutableListOf<String>()
        private var onDialog: ((PermissionDialogTime, Array<String>) -> AllowDeniedDialog?)? = null
        private val noCanAsk get() = canAsk.isEmpty()

        /**
         * 根据时机提示用户授予权限
         *
         * 如果[show]返回不为null，则会显示该[AllowDeniedDialog]并根据[AllowDeniedDialog.result]继续或者终止流程
         */
        fun onDialog(show: (PermissionDialogTime, Array<String>) -> AllowDeniedDialog?): PermissionsResultHelper {
            onDialog = show
            return this
        }

        fun request(callback: (Map<String, PermissionResult>) -> Unit) {
            fm?.lifecycleScope?.launchIO { requestImpl(PermissionDialogTime.BeforeRequest, callback) }
        }

        private suspend fun requestImpl(time: PermissionDialogTime, callback: (Map<String, PermissionResult>) -> Unit) {
            // 分发权限
            dispatchResult(time != PermissionDialogTime.BeforeRequest)
            // 如果以及到最后的执行阶段，或者没有可以请求的权限，或者用户拒绝了权限，则返回结果
            if (time.isOverTime || noCanAsk || !showExplainDialogIfNeed(time, permissions)) return callback.invoke(result)
            // 执行请求
            callPermission()
            requestImpl(time.nextTime, callback)
        }

        /**
         * 返回dialog是否允许请求
         *
         * 如果被拒绝，则不应该继续请求或者跳转
         */
        internal suspend fun showExplainDialogIfNeed(time: PermissionDialogTime, permissions: Array<String>): Boolean {
            return withUI { onDialog?.invoke(time, permissions)?.isAllow() ?: return@withUI !time.isDeniedIfNoDialog }
        }

        /**
         * 执行权限请求
         *
         * 每一次执行都请求全部权限，用以获取权限的真实状态，且不会有其它影响
         */
        private suspend fun callPermission(): Map<String, Boolean> {
            if (permissions.isEmpty() || fm == null) return mapOf()
            return withUI {
                suspendCancellableCoroutine { acc ->
                    val permissionFragment = ResultFragment.newPermissions(permissions) {
                        fm.removeFragmentIfExists(TAG)
                        acc.resume(it)
                    }
                    fm.removeFragmentIfExists(TAG)
                    fm.supportFragmentManager.beginTransaction().add(permissionFragment, TAG).commitAllowingStateLoss()
                }
            }
        }

        private fun dispatchResult(hadAsk: Boolean = true) {
            result.clear()
            canAsk.clear()
            for (permission in permissions) {
                val p = PermissionResult.fromPermission(permission, !hadAsk)
                if (p.isDenied) canAsk.add(permission)
                result[permission] = p
            }
        }
    }

    class IntentResultHelper(internal val fm: FragmentActivity?, private val intent: Intent) {

        private var onDialog: (() -> AllowDeniedDialog?)? = null

        /**
         * 当跳转页面前显示的dialog
         *
         *  如果[show]返回不为null，则会显示该[AllowDeniedDialog]并根据[AllowDeniedDialog.result]继续或者终止流程
         */
        fun onDialog(show: () -> AllowDeniedDialog?): IntentResultHelper {
            this.onDialog = show
            return this
        }

        fun request(callback: (ActivityResult) -> Unit) {
            fm?.lifecycleScope?.launchIO {
                val dialog = withUI { onDialog?.invoke()?.isAllow() } ?: true
                if (!dialog) return@launchIO callback.invoke(ActivityResult(Activity.RESULT_CANCELED, null))
                callback.invoke(callResult())
            }
        }

        private suspend fun callResult(): ActivityResult {
            fm ?: return ActivityResult(Activity.RESULT_CANCELED, null)
            return withUI {
                suspendCancellableCoroutine { acc ->
                    val intentFragment = ResultFragment.newResult(intent) {
                        fm.removeFragmentIfExists(TAG)
                        acc.resume(it)
                    }
                    fm.removeFragmentIfExists(TAG)
                    fm.supportFragmentManager.beginTransaction().add(intentFragment, TAG).commitAllowingStateLoss()
                }
            }
        }
    }

    class JudgeHelper(internal val fm: FragmentActivity?, private val judge: (Context) -> Boolean, private val intent: Intent) {
        private var onDialog: (() -> AllowDeniedDialog?)? = null

        /**
         * 当跳转页面前显示的dialog
         *
         *  如果[show]返回不为null，则会显示该[AllowDeniedDialog]并根据[AllowDeniedDialog.result]继续或者终止流程
         */
        fun onDialog(show: () -> AllowDeniedDialog?): JudgeHelper {
            this@JudgeHelper.onDialog = show
            return this
        }

        fun request(callback: (Boolean) -> Unit) {
            fm ?: return
            if (judge.invoke(fm)) return callback(true)
            fm.lifecycleScope.launchIO {
                val dialog = withUI { onDialog?.invoke()?.isAllow() } ?: true
                if (!dialog) return@launchIO callback.invoke(false)
                callResult()
                callback.invoke(judge.invoke(fm))
            }
        }

        private suspend fun callResult(): ActivityResult {
            fm ?: return ActivityResult(Activity.RESULT_CANCELED, null)
            return withUI {
                suspendCancellableCoroutine { acc ->
                    val judgeFragment = ResultFragment.newResult(intent) {
                        fm.removeFragmentIfExists(TAG)
                        acc.resume(it)
                    }
                    fm.removeFragmentIfExists(TAG)
                    fm.supportFragmentManager.beginTransaction().add(judgeFragment, TAG).commitAllowingStateLoss()
                }
            }
        }
    }

    sealed class PermissionResult {
        object Granted : PermissionResult()
        object Denied : PermissionResult()
        object NeverAsk : PermissionResult()

        val isGranted get() = this == Granted
        val isDenied get() = this == Denied
        val isNeverAsk get() = this == NeverAsk

        companion object {
            fun fromPermission(p: String, noAskAsDenied: Boolean = true): PermissionResult {
                return when {
                    p.isGranted() -> Granted
                    p.isCanAsk() || noAskAsDenied -> Denied
                    else -> NeverAsk
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                Denied -> "Denied"
                Granted -> "Granted"
                NeverAsk -> "NeverAsk"
            }
        }
    }

    sealed class PermissionDialogTime {
        /**
         * 在请求前显示dialog
         */
        object BeforeRequest : PermissionDialogTime()

        /**
         * 请求拒绝的权限前显示dialog
         */
        object Denied : PermissionDialogTime()

        /**
         * 永久拒绝的权限显示dialog
         */
        object NeverAsk : PermissionDialogTime()

        val isBeforeRequest get() = this == BeforeRequest
        val isDenied get() = this == Denied
        val isNeverAsk get() = this == NeverAsk

        /**
         * 如果在该时间没有设置dialog，是否视为被拒绝
         *
         * 当前只有[BeforeRequest]时未设置可以继续
         * 当处于其它时机时必须要显示dialog且选择了允许才继续，否则直接回调结果
         */
        internal val isDeniedIfNoDialog get() = this != BeforeRequest

        internal val nextTime
            get() = when (this) {
                BeforeRequest -> Denied
                Denied -> NeverAsk
                NeverAsk -> NeverAsk
            }

        internal val isOverTime get() = this is NeverAsk
    }
}

class ResultFragment<I, O>(private val input: I, contract: ActivityResultContract<I, O>, onResult: (O) -> Unit) : Fragment() {
    private val contract = registerForActivityResult(contract, onResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contract.launch(input)
    }

    companion object {
        fun newResult(intent: Intent, onResult: (ActivityResult) -> Unit) = ResultFragment(intent, ActivityResultContracts.StartActivityForResult(), onResult)
        fun newPermission(permission: String, onResult: (Boolean) -> Unit) = ResultFragment(permission, ActivityResultContracts.RequestPermission(), onResult)
        fun newPermissions(permissions: Array<String>, onResult: (Map<String, Boolean>) -> Unit) = ResultFragment(permissions, ActivityResultContracts.RequestMultiplePermissions(), onResult)
    }

}