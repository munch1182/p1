package com.munch1182.lib.widget.mindmap

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.munch1182.lib.base.drawTextInCenter
import com.munch1182.lib.widget.mindmap.MindMapView.NodeView
import kotlin.math.abs
import kotlin.math.min

object MindMapCommon {

    private val tmpPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 绘制编辑节点的默认实现
     */
    fun drawEditNode(view: MindMapView, canvas: Canvas, node: NodeView) {
        val level = node.level

        /* val contentRect = node.contentRect
        val hPadding = contentRect.height() * 0.2f
        val wPadding = node.wPadding

        setupSelectedBorderPaint(level)
        val radius = node.radius
        canvas.drawRoundRect(contentRect, radius, radius, paint)

        setupCursorPaint(level)
        if (node.editName == null) {
            canvas.drawTextInCenter(node.name, node.contentRect.centerX(), node.contentRect.centerY(), paint)
        } else {
            canvas.drawTextInStartXCenterY(node.editName ?: "", node.contentRect.left, contentRect.centerY(), paint)
        }

        if (node.showCursor) {
            val cursorX = contentRect.left + paint.measureText(node.editName) + wPadding
            tmpRect.set(cursorX, contentRect.top + hPadding, cursorX, contentRect.bottom - hPadding)
            canvas.drawLine(tmpRect.left, tmpRect.top, tmpRect.left, tmpRect.bottom, paint)
        }*/

        setupLinkPointPaint(level)
        node.linkPoint?.drawLink(canvas, paint, level)
    }

    /**
     * 绘制连接线的默认实现
     */
    fun drawLink(canvas: Canvas, paint: Paint, level: Int, lp: MindMapView.LinkPoint) {
        val sX = lp.sX
        val eX = lp.eX
        val sY = lp.sY
        val eY = lp.eY

        if (abs(sY - eY) < 5f) {
            canvas.drawLine(sX, sY, eX, sY, paint)
            return
        }

        /*val sX = min(sX, eX)
        val eX = max(this.sX, eX)
        val sY = min(sY, eY)
        val eY = max(this.sY, eY)*/

        val disX = abs(eX - sX)
        val disY = abs(eY - sY)

        val dis = min(disX, disY)
        // 因为是最短的距离
        tmpPath.reset()


        val offsetStartX = dis * 0.1f
        val tbOffset = if (sY > eY) -1f else 1f

        val sc1x = sX + offsetStartX * 1.5f - disY * 0.01f
        val sc1y = sY + offsetStartX * 0.3f * tbOffset
        val sc2x = sX + offsetStartX * 3f + disY * 0.01f
        val sc2y = sY + offsetStartX * 2f * tbOffset
        val cX = sX + (eX - sX) / 3f + offsetStartX
        val cY = sY + (eY - sY) / 3f + offsetStartX * tbOffset
        tmpPath.moveTo(sX, sY)
        tmpPath.cubicTo(sc1x, sc1y, sc2x, sc2y, cX, cY)

        var offsetEnd = dis * 0.1f
        offsetEnd *= 3f
        val ec1x = eX - offsetEnd
        val ec1y = eY
        val ec2x = eX - offsetEnd - offsetEnd / 2f
        val ec2y = eY
        tmpPath.cubicTo(ec1x, ec1y, ec2x, ec2y, eX, eY)
        canvas.drawPath(tmpPath, paint)

        /*paint.setColor(Color.BLACK)
        canvas.drawCircle(sc1x, sc1y, 1f, paint)
        canvas.drawCircle(sc2x, sc2y, 1f, paint)
        canvas.drawCircle(ec1x, ec1y, 1f, paint)
        canvas.drawCircle(ec2x, ec2y, 1f, paint)
        paint.setColor(Color.CYAN)
        canvas.drawCircle(cX, cY, 1f, paint)*/
    }

    /**
     * 绘制节点的默认实现
     */
    fun drawNode(canvas: Canvas, node: NodeView) {
        setupTextPaint(node.level)
        canvas.drawTextInCenter(node.name, node.contentRect.centerX(), node.contentRect.centerY(), paint)

        setupBorderPaint(node.level)
        val radius = node.radius
        canvas.drawRoundRect(node.contentRect, radius, radius, paint)

        setupLinkPointPaint(node.level)
        node.linkPoint?.drawLink(canvas, paint, node.level)
    }

    private fun setupLinkPointPaint(level: Int) {
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.strokeCap = Paint.Cap.ROUND
    }

    private fun setupTextPaint(level: Int) {
        paint.color = Color.BLACK
        paint.textSize = 36f
        paint.style = Paint.Style.FILL
    }

    private fun setupCursorPaint(level: Int) {
        setupTextPaint(level)
        paint.strokeWidth = 2f
        paint.color = Color.BLUE
    }

    private fun setupSelectedBorderPaint(level: Int) {
        setupBorderPaint(level)
        paint.color = Color.BLUE
        paint.strokeWidth = 2f
    }

    private fun setupBorderPaint(level: Int) {
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
    }
}