package com.munch1182.lib.widget.mindmap

object MindMapIdHelper {

    private const val SPLIT: Char = '_'

    const val ROOT_ID: String = "0"

    fun newID(from: String?, index: Int): String {
        return from?.let { "$it$SPLIT$index" } ?: "$index"
    }

    fun parentID(id: String): String? {
        if (!id.contains(SPLIT)) return null
        return id.substringBeforeLast(SPLIT)
    }

    fun sortById(id: String): Int {
        return index(id)
    }

    private fun index(id: String): Int {
        return id.substringAfterLast(SPLIT).toIntOrNull() ?: -1
    }
}