package com.munch1182.p1.views.libview

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.dp2PX
import com.munch1182.lib.base.drawTextInStartXCenterY
import com.munch1182.lib.base.height
import com.munch1182.lib.base.lpW
import com.munch1182.lib.base.middleHeight
import com.munch1182.lib.base.sp2Px
import com.munch1182.lib.widget.recyclerview.RecyclerViewDividerItemDecoration
import com.munch1182.lib.widget.recyclerview.RecyclerViewStickyHearItemDecoration
import com.munch1182.lib.widget.recyclerview.RecyclerViewTitleItemDecoration
import com.munch1182.lib.widget.recyclerview.SideIndexBarView
import com.munch1182.lib.widget.recyclerview.newAsStickyHeader
import com.munch1182.p1.base.BaseActivity

class RecyclerviewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply {
            fitsSystemWindows = true
            clipToPadding = true
            addView(RecyclerView(this@RecyclerviewActivity).apply { handleRv(this) })
            addView(SideIndexBarView(context).apply { setPadding(8.dp2PX.toInt()) }, FrameLayout.LayoutParams(lpW, lpW).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL })
        })
    }

    private fun handleRv(rv: RecyclerView) {
        val list = Array(100) { "$it$it$it" }
        val titleHelper = object : RecyclerViewTitleItemDecoration.TitleDrawHelper {
            private val paint = Paint().apply { textSize = 18.sp2Px }

            override fun isTitle(pos: Int) = pos % 10 == 0
            override fun titleHeight() = (32.dp2PX + paint.height).toInt()
            override fun onDraw(c: Canvas, rect: Rect, paint: Paint, pos: Int) {
                this.paint.setColor(Color.GRAY)
                c.drawRect(rect, this.paint)
                this.paint.setColor(Color.BLACK)
                c.drawTextInStartXCenterY("Title ${if (pos < 10) "0" else list[pos].getOrNull(0)}", (rect.left + 32.dp2PX), rect.middleHeight.toFloat(), this.paint)
            }
        }
        val stickTitleHelper = titleHelper.newAsStickyHeader()

        rv.layoutManager = LinearLayoutManager(this)

        rv.addItemDecoration(RecyclerViewDividerItemDecoration())
        rv.addItemDecoration(RecyclerViewTitleItemDecoration(titleHelper))
        rv.addItemDecoration(RecyclerViewStickyHearItemDecoration(stickTitleHelper))

        val adapter = Adapter()
        rv.adapter = adapter
        adapter.set(list)
    }

    private class Adapter : RecyclerView.Adapter<Adapter.VH>() {

        class VH(val view: TextView) : RecyclerView.ViewHolder(view)

        private val data = mutableListOf<String>()

        fun set(data: Array<String>) {
            this.data.clear()
            this.data.addAll(data)
            notifyItemRangeInserted(0, data.size)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.view.text = data[position]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(TextView(parent.context).apply {
                val p16 = 16.dp2PX.toInt()
                setPadding(p16 * 2, p16, p16 * 2, p16)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            })
        }

        override fun getItemCount() = data.size
    }
}
