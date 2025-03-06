package com.munch1182.libview.dialog

import com.munch1182.lib.AppHelper

object DialogViewManager {
    private var listener: OnDialogCreateListener? = null
    fun setOnDialogCreateListener(listener: OnDialogCreateListener) {
        this.listener = listener
    }
}

interface OnDialogCreateListener {
    fun onCreateMessage(): MessageDialog
    fun onCreateBottom(): BottomDialog
    fun onCreatePop(): PopDialog
    fun onCreateTop(): TopDialog
    fun onCreateProgress(): ProgressDialog
}

object DialogHelper {
    fun newMessage() = MessageDialog()
    fun newBottom() = BottomDialog()
    fun newPop() = PopDialog()
    fun newProgress() = ProgressDialog()
    fun newTop() = TopDialog()
}

interface IDialog {
    fun show()
    fun create(): IDialog
}

class TopDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}

class PopDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}

class BottomDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}

class MessageDialog : IDialog {

    inner class Builder {

        fun create(
            message: String,
            title: String,
            ok: String = AppHelper.getString(android.R.string.ok),
            cancel: String = AppHelper.getString(android.R.string.cancel)
        ) {

        }

        fun build(): Builder {
            return this
        }
    }

    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}

class ProgressDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}
