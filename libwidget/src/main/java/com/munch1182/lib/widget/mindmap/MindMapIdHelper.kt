package com.munch1182.lib.widget.mindmap

object MindMapIdHelper {

    private const val SPLIT: Char = '_'

    fun newID(from: String?, index: Int): String {
        return from?.let { "$it$SPLIT$index" } ?: "$index"
    }

    fun parentID(id: String): String? {
        if (!id.contains(SPLIT)) return null
        return id.substringBeforeLast(SPLIT)
    }

}