package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener
import com.munch1182.lib.base.mapByMatrix
import com.munch1182.lib.base.newCornerDrawable
import kotlin.math.min

@SuppressLint("ViewConstructor")
class MindMapEditView(private val mp: MindMapView, private val node: MindMapView.NodeView) : AppCompatEditText(mp.context) {

    companion object {

        fun newNode(mp: MindMapView, node: MindMapView.NodeView): MindMapEditView {
            val et = MindMapEditView(mp, node)

            val rect = node.contentRect.mapByMatrix(mp.matrix)

            val lp = MarginLayoutParams(rect.width().toInt(), rect.height().toInt())
            et.setText(node.name)
            et.setSelection(node.name.length)
            et.layoutParams = lp
            et.translationX = rect.left
            et.translationY = rect.top

            val paddingRect = RectF(0f, 0f, node.hContentPadding.toFloat(), node.vContentPadding.toFloat())
            val newPaddingRect = paddingRect.mapByMatrix(mp.matrix)
            val w = newPaddingRect.width().toInt()
            val h = newPaddingRect.height().toInt()
            et.setPadding(w, h, w, h)

            return et
        }
    }

    init {
        val matrix = mp.matrix
        val radius = matrix.mapRadius(node.radius)

        setBackgroundDrawable(newCornerDrawable(corner = radius, strokeWidth = 2, strokeColor = Color.BLUE))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, node.textSize * mp.newTextScale())
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER_VERTICAL
        imeOptions = EditorInfo.IME_ACTION_DONE
        setSingleLine()

        requestFocus()
        val maxW = mp.width - node.wPadding * 2f
        addTextChangedListener(afterTextChanged = {
            val str = it?.toString() ?: return@addTextChangedListener
            if (str == node.name) return@addTextChangedListener
            var newWidth = paint.measureText(str) + paddingLeft + paddingRight
            if (newWidth > maxW && width == newWidth.toInt()) return@addTextChangedListener

            // 一次性通过输入法输入超过最大宽度字符的情形
            newWidth = min(newWidth, maxW)
            updateNewWidth(newWidth)
            post { updateNewWidthForLoc(width.toFloat()) }
        })
    }

    // 更新宽度后，需要子节点让位
    private fun updateNewWidthForLoc(w: Float) {
        mp.setEditMode()
        val xOffset = (mp.width - this.width) / 2f - translationX
        translationX += xOffset

        node.editRectF?.let { it.right = it.left + w / mp.newWidthScale() }
        mp.matrix.postTranslate(xOffset, 0f)
        mp.updateChildAsParentEdit(node)
        mp.invalidate()
    }

    private fun updateNewWidth(newWidth: Float) {
        // 只增长不缩减
        if (newWidth <= width) return
        val lp = layoutParams ?: return
        lp.width = newWidth.toInt()
        layoutParams = lp
    }
}