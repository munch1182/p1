package com.munch1182.lib.floatwindow

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.children

object FWManager {

    internal val tag = this::class.java.name
    internal val tagKey = R.id.flow_window

    fun checkPermission(context: Context) = Settings.canDrawOverlays(context)

    private fun wm(ctx: Context): WindowManager? {
        return kotlin.runCatching { ctx.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager }
            .getOrNull()
    }

    fun hideAll(context: Context): Boolean {
        val view = findView(context) ?: return false
        if (!view.isAttachedToWindow) {
            return true
        }
        kotlin.runCatching { wm(context)?.removeView(view) }
        return true
    }

    fun findView(context: Context): View? {
        val act = context.findActivity() ?: return null
        val root = act.findViewById<ViewGroup>(android.R.id.content)
        return root.children.find { it.isTagView() }
    }

    internal fun View.isTagView() = getTag(tagKey) == tag
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

interface IFWComponent {
    fun create(): Boolean

    fun show()

    fun hide()

    fun destroy()
}