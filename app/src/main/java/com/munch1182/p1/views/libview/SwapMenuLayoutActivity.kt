package com.munch1182.p1.views.libview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.toast
import com.munch1182.lib.widget.SwapMenuLayout
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivitySwapMenuLayoutBinding
import com.munch1182.p1.databinding.ItemSwapMenuLayoutBinding

class SwapMenuLayoutActivity : BaseActivity() {

    private val bind by bind(ActivitySwapMenuLayoutBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.asmlDel.setOnClickListener { toast("del click") }
        bind.asmlContent1.setOnClickListener { toast("content click") }
        bind.asmlToggle.setOnClickListener { bind.asmlSml.toggle() }
        bind.asmlSml.setOnStateChangeListener { _, state -> if (state == ViewDragHelper.STATE_IDLE) toggleText() }

        bind.asmlRv.layoutManager = LinearLayoutManager(this)
        val adapter = SMLAAdapter(bind.asmlRv)
        bind.asmlRv.adapter = adapter

        bind.asmlToggleRv.setOnClickListener {
            //val index = Random.nextInt(0, 12)
            //adapter.open(index)
        }

        bind.asmlUpdateRv.setOnClickListener {
            //adapter.update("更新内容")
        }
    }

    private fun toggleText() {
        bind.asmlToggle.text = if (bind.asmlSml.isOpen) "收起" else "展开"
    }

    private class SMLAAdapter(private val rv: RecyclerView) : RecyclerView.Adapter<SMLAVH>() {

        private var currOpen: SwapMenuLayout? = null
        private val conteClick = View.OnClickListener { toast("content click") }
        private val delClick = View.OnClickListener { toast("del click") }
        private val list = MutableList(35) { "item $it" }
        private val stateChange = SwapMenuLayout.OnStateChangeListener { view, state ->
            if (state == ViewDragHelper.STATE_IDLE) {
                if (view.isOpen) currOpen = view
            } else if (currOpen != view) {
                currOpen?.close()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMLAVH {
            return SMLAVH(ItemSwapMenuLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: SMLAVH, position: Int) {
            val item = list[position]
            holder.bind.ismlContentTv.text = item
            holder.bind.ismlContent1.setOnClickListener(conteClick)
            holder.bind.ismlDel.setOnClickListener(delClick)
            holder.bind.ismlSml.tag = position
            holder.bind.ismlSml.setOnStateChangeListener(stateChange)
        }
    }

    private class SMLAVH(val bind: ItemSwapMenuLayoutBinding) : RecyclerView.ViewHolder(bind.root)
}