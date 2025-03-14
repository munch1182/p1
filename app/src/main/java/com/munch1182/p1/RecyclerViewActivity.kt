package com.munch1182.p1

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.dp2Px
import com.munch1182.lib.base.middleHeight
import com.munch1182.lib.base.newFLWWLP
import com.munch1182.lib.base.paddingSystemBars
import com.munch1182.libview.recyclerview.RecyclerViewDividerItemDecoration
import com.munch1182.libview.recyclerview.RecyclerViewStickyHearItemDecoration
import com.munch1182.libview.recyclerview.RecyclerViewTitleItemDecoration
import com.munch1182.libview.recyclerview.SideIndexBarView
import kotlin.math.abs

class RecyclerViewActivity : AppCompatActivity() {

    private val vm by viewModels<RecyclerViewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapter = RecyclerViewAdapter()
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this.context)
            this.adapter = adapter
            addItemDecoration(RecyclerViewDividerItemDecoration())
            val helper = TheTitleDrawHelper()
            addItemDecoration(RecyclerViewTitleItemDecoration(helper))
            addItemDecoration(RecyclerViewStickyHearItemDecoration(TheStickyHeaderHelper(helper)))
        }
        val ctx = this
        setContentView(FrameLayout(this).apply {
            addView(rv)
            val endLP = newFLWWLP().apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
            val indexBar = SideIndexBarView(ctx).apply {
                val p8 = 8.dp2Px().toInt()
                setPadding(p8)
            }
            addView(indexBar, endLP)
            paddingSystemBars()
            
        })
        vm.name.observe(this) { adapter.setData(it) }
    }

    class RecyclerViewAdapter(data: Array<String>? = null) : RecyclerView.Adapter<RecyclerViewAdapter.VH>() {

        private val items: MutableList<String> = data?.toMutableList() ?: mutableListOf()

        class VH(view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(TextView(parent.context).apply {
                val p8 = 16.dp2Px().toInt()
                val p16 = p8 * 2
                setPadding(p16, p8, p16, p8)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            })
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as? TextView)?.text = items[position]
        }

        override fun getItemCount() = items.size

        fun setData(value: Array<String>?) {
            val count = itemCount
            items.clear()
            if (value != null) {
                items.addAll(value)
                val newCount = itemCount
                val diff = count - newCount
                if (diff == 0) {
                    notifyItemRangeChanged(0, newCount)
                } else if (diff > 0) {
                    notifyItemRangeRemoved(diff, count - 1)
                    notifyItemRangeChanged(0, diff)
                } else {
                    notifyItemRangeChanged(0, count)
                    notifyItemRangeChanged(count, count + abs(diff))
                }
            } else {
                notifyItemRangeRemoved(0, count)
            }
        }
    }

    class RecyclerViewViewModel : ViewModel() {
        private val _name = MutableLiveData(Array(100) { "Item $it" })
        val name: LiveData<Array<String>> = _name
    }

    class TheTitleDrawHelper : RecyclerViewTitleItemDecoration.TitleDrawHelper {
        override fun isTitle(pos: Int) = pos == 0 || abs(pos) % 10 == 0
        override fun titleHeight() = 50.dp2Px().toInt()
        override fun onDraw(c: Canvas, rect: Rect, paint: Paint, pos: Int) {
            paint.setColor(Color.GRAY)
            c.drawRect(rect, paint)
            paint.setColor(Color.WHITE)
            paint.textSize = 42f
            c.drawText("Title ${pos / 10}", (rect.left + 20.dp2Px()), (rect.middleHeight.toFloat()), paint)
        }
    }

    class TheStickyHeaderHelper(private val h: TheTitleDrawHelper) : RecyclerViewStickyHearItemDecoration.StickyHearDrawHelper {
        override fun isTitle(pos: Int) = h.isTitle(pos)
        override fun headerHeight() = 50.dp2Px().toInt()
        override fun onDraw(c: Canvas, rect: Rect, paint: Paint, nearestPos: Int) {
            h.onDraw(c, rect, paint, nearestPos)
        }
    }
}

