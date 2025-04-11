package com.munch1182.lib.helper.result

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import com.munch1182.lib.base.onDestroyed
import com.munch1182.lib.helper.dialog.DialogProvider
import com.munch1182.lib.helper.dialog.ResultDialog
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


interface AllowDenyDialog : ResultDialog<AllowDenyDialog.Result> {
    sealed class Result {
        data object Allow : Result()
        data object Deny : Result()

        val isAllow: Boolean get() = this is Allow
        val isDeny: Boolean get() = this is Deny
    }
}

suspend fun AllowDenyDialog.isAllow() = suspendCoroutine { c ->
    lifecycle.onDestroyed { c.resume(result?.isAllow) }
    show()
}

@FunctionalInterface
fun interface AllDenyDialogProvider : DialogProvider<AllowDenyDialog, AllowDenyDialog.Result>

class AllowDenyDialogContainer(private val dialog: AlertDialog) : AllowDenyDialog {
    private var _result: AllowDenyDialog.Result = AllowDenyDialog.Result.Deny
    override val result: AllowDenyDialog.Result get() = _result
    override val lifecycle: Lifecycle get() = dialog.lifecycle

    @SuppressLint("ClickableViewAccessibility")
    override fun show() {
        dialog.show()
        dialog.apply {
            getButton(AlertDialog.BUTTON_POSITIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) _result = AllowDenyDialog.Result.Allow
                return@setOnTouchListener false
            }
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) _result = AllowDenyDialog.Result.Deny
                return@setOnTouchListener false
            }
        }
    }
}

fun AlertDialog.asAllowDenyDialog() = AllowDenyDialogContainer(this)