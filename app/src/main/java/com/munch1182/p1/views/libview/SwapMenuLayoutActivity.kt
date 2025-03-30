package com.munch1182.p1.views.libview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.toast
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
        bind.asmlRv.adapter = SMLAAdapter(bind.asmlRv)
    }

    private fun toggleText() {
        bind.asmlToggle.text = if (bind.asmlSml.isOpen) "收起" else "展开"
    }

    private class SMLAAdapter(rv: RecyclerView) : RecyclerView.Adapter<SMLAVH>() {

        private val conteClick = View.OnClickListener { toast("content click") }
        private val delClick = View.OnClickListener { toast("del click") }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMLAVH {
            return SMLAVH(ItemSwapMenuLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return 55
        }

        override fun onBindViewHolder(holder: SMLAVH, position: Int) {
            holder.bind.ismlContent1.setOnClickListener(conteClick)
            holder.bind.ismlDel.setOnClickListener(delClick)
        }
    }

    private class SMLAVH(val bind: ItemSwapMenuLayoutBinding) : RecyclerView.ViewHolder(bind.root)
}