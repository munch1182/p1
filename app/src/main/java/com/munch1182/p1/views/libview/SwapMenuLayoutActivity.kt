package com.munch1182.p1.views.libview

import android.os.Bundle
import android.view.LayoutInflater
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
        bind.asmlSml.setOnStateChangeListener { if (it == ViewDragHelper.STATE_IDLE) toggleText() }

        bind.asmlRv.layoutManager = LinearLayoutManager(this)
        bind.asmlRv.adapter = SMLAAdapter()
    }

    private fun toggleText() {
        bind.asmlToggle.text = if (bind.asmlSml.isOpen) "收起" else "展开"
    }

    private class SMLAAdapter : RecyclerView.Adapter<SMLAVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SMLAVH {
            return SMLAVH(ItemSwapMenuLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount(): Int {
            return 25
        }

        override fun onBindViewHolder(holder: SMLAVH, position: Int) {
        }
    }

    private class SMLAVH(val bind: ItemSwapMenuLayoutBinding) : RecyclerView.ViewHolder(bind.root)
}