package com.munch1182.p1

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.munch1182.lib.keepScreenOn
import com.munch1182.lib.view.ClickItemTouchHelper
import kotlin.random.Random

class SlideMenuViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keepScreenOn()
        val adapter = SlideMenuViewAdapter()
        setContentView(RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@SlideMenuViewActivity)
            this.adapter = adapter
            ClickItemTouchHelper(this)
        })

        adapter.submitList(List(100) { it * 2 })
    }
}

class SlideMenuViewAdapter : BaseQuickAdapter<Int, QuickViewHolder>() {

    private val listener by lazy {
        View.OnClickListener {
            Log.d("SlideItemHelperCallback", "aaaa: ")
            val v = it ?: return@OnClickListener
            val pos = v.tag as? Int ?: return@OnClickListener
            Toast.makeText(v.context, "pos $pos", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: Int?) {
        holder.setText(R.id.slide_menu_content, "$item $position")
        holder.itemView.translationX = 0f
        holder.itemView.findViewById<View>(R.id.slide_menu_del).tag = position
    }

    override fun onCreateViewHolder(
        context: Context, parent: ViewGroup, viewType: Int
    ) = QuickViewHolder(R.layout.item_slide_menu, parent).apply {
        itemView.setBackgroundColor(randomColor())
        itemView.findViewById<View>(R.id.slide_menu_del).apply {
            setOnClickListener(listener)
        }
    }
}


private fun randomColor(): Int {
    return Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
}
