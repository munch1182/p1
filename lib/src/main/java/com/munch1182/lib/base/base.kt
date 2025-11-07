package com.munch1182.lib.base

import androidx.fragment.app.FragmentActivity

fun FragmentActivity.removeFragmentIfExists(tag: String) {
    val fm = supportFragmentManager
    val oldFrag = fm.findFragmentByTag(tag)
    if (oldFrag != null) fm.beginTransaction().remove(oldFrag).commitNow()
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethod(fn: String, paramTypes: Array<Class<*>> = arrayOf(), vararg params: Any): T? {
    try {
        val method = this::class.java.getMethod(fn, *paramTypes)
        return method.invoke(this, *params) as? T
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}