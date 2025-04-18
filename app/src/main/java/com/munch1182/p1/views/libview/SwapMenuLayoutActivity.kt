package com.munch1182.p1.views.libview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.widget.SwapMenuLayout
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.base.toast
import com.munch1182.p1.databinding.ActivitySwapMenuLayoutBinding
import com.munch1182.p1.databinding.ItemSwapMenuLayoutBinding
import kotlin.random.Random

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
            val index = Random.nextInt(0, 12)
            adapter.open(index)
        }

        bind.asmlUpdateRv.setOnClickListener {
            adapter.update("更新内容 ${Random.nextInt(100, 999)}")
        }
    }

    private fun toggleText() {
        bind.asmlToggle.text = if (bind.asmlSml.isOpen) "收起" else "展开"
    }

    private class SMLAAdapter(private val rv: RecyclerView) : RecyclerView.Adapter<SMLAVH>() {

        private val conteClick = View.OnClickListener { toast("content click") }
        private val delClick = View.OnClickListener { toast("del click") }
        private val list = MutableList(35) { "item $it" }
        private val limitHelper = SwapMenuLayout.LimitHelper()

        fun open(index: Int) {
            (rv.findViewHolderForAdapterPosition(index) as? SMLAVH?)?.apply {
                bind.ismlSml.open()
            }
        }

        fun update(content: String) {
            limitHelper.currPos?.let {
                list[it] = content
                notifyItemChanged(it, true)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMLAVH {
            return SMLAVH(ItemSwapMenuLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = list.size
        override fun onBindViewHolder(holder: SMLAVH, position: Int) {
            updateViews(holder, position)
            holder.bind.ismlContent1.setOnClickListener(conteClick)
            holder.bind.ismlDel.setOnClickListener(delClick)
            limitHelper.bind(holder.bind.ismlSml, position)
        }

        private fun updateViews(holder: SMLAVH, position: Int) {
            val item = list[position]
            holder.bind.ismlContentTv.text = item
        }

        override fun onBindViewHolder(holder: SMLAVH, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                updateViews(holder, position)
                if (holder.bind.ismlSml.isOpen) {
                    holder.bind.ismlSml.close()
                }
            }
        }
    }

    private class SMLAVH(val bind: ItemSwapMenuLayoutBinding) : RecyclerView.ViewHolder(bind.root)
}