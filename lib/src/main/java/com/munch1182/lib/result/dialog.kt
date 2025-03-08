package com.munch1182.lib.result

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog

interface ResultDialog {
    fun create(context: Context): ResultDialog = this
    fun show()
    fun setOnChoseListener(l: OnChoseListener): ResultDialog

    @FunctionalInterface
    fun interface OnChoseListener {
        fun onChose(isContinue: Boolean)
    }
}

open class DialogWrapper(private val dialog: AlertDialog) : ResultDialog {
    private var l: ResultDialog.OnChoseListener? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun show() {
        dialog.show()
        dialog.apply {
            getButton(AlertDialog.BUTTON_POSITIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) l?.onChose(true)
                return@setOnTouchListener false
            }
            getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnTouchListener { _, event ->
                if (event?.action == MotionEvent.ACTION_UP) l?.onChose(false)
                return@setOnTouchListener false
            }
        }
    }

    override fun setOnChoseListener(l: ResultDialog.OnChoseListener): DialogWrapper {
        this.l = l
        return this
    }

}

fun AlertDialog.asResultDialog() = DialogWrapper(this)