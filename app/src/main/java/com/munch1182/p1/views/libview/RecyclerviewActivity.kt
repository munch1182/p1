package com.munch1182.p1.views.libview

import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.AppHelper
import com.munch1182.lib.widget.recyclerview.RecyclerViewDividerItemDecoration
import com.munch1182.p1.base.BaseActivity

class RecyclerviewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply {
            addView(RecyclerView(this@RecyclerviewActivity).apply { handleRv(this) })
            fitsSystemWindows = true
        })
    }

    private fun handleRv(rv: RecyclerView) {
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(RecyclerViewDividerItemDecoration())

        val adapter = Adapter()
        rv.adapter = adapter

        adapter.set(Array(100) { "$it$it$it" })
    }

    private class Adapter : RecyclerView.Adapter<Adapter.VH>() {

        class VH(val view: TextView) : RecyclerView.ViewHolder(view) {}

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

val Number.dp2PX
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), AppHelper.resources.displayMetrics
    )