package com.munch1182.libview.dialog

object DialogHelper {
    fun newMessage(msg: String) = MessageDialog()
    fun newBottom(items: Array<String>) = BottomDialog()
    fun newPop() = PopDialog()
    fun newProgress(min: Int = 0, max: Int = 100) = ProgressDialog()
    fun newTop() = TopDialog()
}

interface IDialog {
    fun show()
    fun dismiss()
    fun create(): IDialog

    fun canCancelNoChose(can: Boolean = false): IDialog {
        return this
    }

    // 拦截取消显示的事件
    fun interceptDismiss(listener: (IDialog) -> Boolean): IDialog {
        return this
    }

    // 显示取消的回调
    fun onDismissListener(listener: (IDialog) -> Unit): IDialog {
        return this
    }
}

interface ITitleDialog {
    fun title(title: String): ITitleDialog

    fun noTitle(no: Boolean = false): ITitleDialog
}

class PopDialog : IDialog {
    override fun show() {
    }

    override fun create(): IDialog {
        return this
    }

    override fun dismiss() {
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

    override fun dismiss() {
    }

    override fun noTitle(no: Boolean): BottomDialog {
        return this
    }
}

class MessageDialog : IDialog, ITitleDialog {

    override fun show() {
    }

    override fun dismiss() {
    }

    override fun create(): IDialog {
        return this
    }

    override fun title(title: String): MessageDialog {
        return this
    }

    override fun noTitle(no: Boolean): MessageDialog {
        return this
    }
}

class TopDialog : IDialog {
    override fun show() {
    }

    override fun dismiss() {
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

    override fun dismiss() {
    }

    override fun create(): IDialog {
        return this
    }

    override fun title(title: String): ITitleDialog {
        return this
    }

    override fun noTitle(no: Boolean): ITitleDialog {
        return this
    }
}
