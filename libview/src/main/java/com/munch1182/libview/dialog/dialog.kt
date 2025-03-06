package com.munch1182.libview.dialog

import android.content.Context
import com.munch1182.lib.AppHelper

object DialogViewManager {
    internal var listener: OnDialogCreateListener? = null
    fun setOnDialogCreateListener(listener: OnDialogCreateListener) {
        this.listener = listener
    }
}

interface OnDialogCreateListener {
    fun onCreateMessage(context: Context): MessageDialog? = null
    fun onCreateBottom(context: Context): BottomDialog? = null
    fun onCreatePop(context: Context): PopDialog? = null
    fun onCreateTop(context: Context): TopDialog? = null
    fun onCreateProgress(context: Context): ProgressDialog? = null
}

object DialogHelper {
    fun newMessage(msg: String) = MessageDialog()
    fun newBottom(items: Array<String>) = BottomDialog()
    fun newPop() = PopDialog()
    fun newProgress(min: Int = 0, max: Int = 100) = ProgressDialog()
    fun newTop() = TopDialog()
}

interface IDialog {
    fun show()
    fun create(): IDialog

    fun canCancelNoChose(can: Boolean = false): IDialog {
        return this
    }

    // 拦截取消显示的事件
    fun interceptDismiss(listener: (IDialog) -> Boolean): IDialog {
        return this
    }

    // 取消显示的回调
    fun onDismissListener(listener: (IDialog) -> Unit): IDialog {
        return this
    }
}

interface ITitleDialog {
    fun title(title: String): ITitleDialog
}

class PopDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }
}

class BottomDialog : IDialog, ITitleDialog {

    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }

    override fun title(title: String): BottomDialog {
        return this
    }
}

class MessageDialog : IDialog, ITitleDialog {

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

    override fun title(title: String): MessageDialog {
        return this
    }
}

class TopDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }

}

class ProgressDialog : IDialog, ITitleDialog {


    fun update(progress: Int): ProgressDialog {
        return this
    }

    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }

    override fun title(title: String): ITitleDialog {
        return this
    }
}
