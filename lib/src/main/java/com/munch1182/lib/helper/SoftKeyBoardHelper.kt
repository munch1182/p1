package com.munch1182.lib.helper

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.inputmethod.InputMethodManager
import com.munch1182.lib.base.ctx
import kotlin.math.abs

class SoftKeyBoardHelper(private val rootView: View) {

    constructor(window: Window) : this(window.decorView)

    companion object {

        val im get() = ctx.getSystemService(InputMethodManager::class.java)

        fun hide(window: Window) {
            im?.hideSoftInputFromWindow(window.currentFocus?.windowToken, 0)
        }

        fun hide(view: View) {
            im?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        fun show(view: View) {
            view.requestFocus()
            im?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }

        fun enableEditMode(view: View, enable: Boolean = true) {
            view.isFocusable = enable
            view.isFocusableInTouchMode = enable
        }
    }

    private var rootVisibleHeight = 0
    private var judgeHeight = 200
    private var l: KeyBoardChangeListener? = null

    private val listener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)

        val h = rect.height()

        if (rootVisibleHeight == 0) {
            rootVisibleHeight = h
            return@OnGlobalLayoutListener
        }

        if (rootVisibleHeight == h) {
            return@OnGlobalLayoutListener
        }

        if (abs(rootVisibleHeight - h) > judgeHeight) {
            l?.onKeyBoardChange(rootVisibleHeight - h)
            rootVisibleHeight = h
            return@OnGlobalLayoutListener
        }
    }

    fun setKeyBoardChangeListener(l: KeyBoardChangeListener): SoftKeyBoardHelper {
        this.l = l
        return this
    }

    fun listen() {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    fun unListen() {
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    fun interface KeyBoardChangeListener {
        fun onKeyBoardChange(change: Int)
    }
}