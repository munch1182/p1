package com.munch1182.lib.helper.result

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import com.munch1182.lib.helper.dialog.ResultDialog


interface PermissionCanRequestDialog : ResultDialog<PermissionCanRequestDialog.PermissionCanRequest> {
    sealed class PermissionCanRequest {
        data object Allow : PermissionCanRequest()
        data object Deny : PermissionCanRequest()

        val isAllow: Boolean get() = this is Allow
        val isDeny: Boolean get() = this is Deny
    }
}

class PermissionCanRequestDialogContainer(private val dialog: AlertDialog) : PermissionCanRequestDialog {
    private var _result: PermissionCanRequestDialog.PermissionCanRequest = PermissionCanRequestDialog.PermissionCanRequest.Deny
    override val result: PermissionCanRequestDialog.PermissionCanRequest get() = _result
    override val owner: LifecycleOwner = dialog

    @SuppressLint("ClickableViewAccessibility")
    override fun show() {
        dialog.show()
        dialog.apply {
            getButton(AlertDialog.BUTTON_POSITIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) _result = PermissionCanRequestDialog.PermissionCanRequest.Allow
                return@setOnTouchListener false
            }
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) _result = PermissionCanRequestDialog.PermissionCanRequest.Deny
                return@setOnTouchListener false
            }
        }
    }
}

fun AlertDialog.asPermissionDialog() = PermissionCanRequestDialogContainer(this)