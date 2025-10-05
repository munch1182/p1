package com.munch1182.lib.base

import androidx.fragment.app.FragmentActivity

fun FragmentActivity.removeFragmentIfExists(tag: String) {
    val fm = supportFragmentManager
    val oldFrag = fm.findFragmentByTag(tag)
    if (oldFrag != null) fm.beginTransaction().remove(oldFrag).commitNow()
}