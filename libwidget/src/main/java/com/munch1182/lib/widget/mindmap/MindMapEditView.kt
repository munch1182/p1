package com.munch1182.lib.widget.mindmap

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import com.munch1182.lib.base.log
import com.munch1182.lib.base.mapByMatrix
import com.munch1182.lib.base.newCornerDrawable
import kotlin.math.hypot

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

    private val log = log()

    init {
        val matrix = mp.matrix
        val radius = matrix.mapRadius(node.radius)

        setBackgroundDrawable(newCornerDrawable(corner = radius, strokeWidth = 2, strokeColor = Color.BLUE))
        setTextSize(TypedValue.COMPLEX_UNIT_PX, node.textSize * newTextScale(matrix))
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER_VERTICAL
        imeOptions = EditorInfo.IME_ACTION_DONE
        requestFocus()
    }

    private fun newTextScale(matrix: Matrix): Float {
        val vector = floatArrayOf(0f, 1f)
        val mappedVector = floatArrayOf(0f, 1f)
        matrix.mapVectors(mappedVector, vector)
        val scale = hypot(mappedVector[0], mappedVector[1])
        return scale
    }
}