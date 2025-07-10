package com.munch1182.p1.views.libview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.dp2PX
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.log
import com.munch1182.lib.base.newCornerDrawable
import com.munch1182.lib.base.newRandom
import com.munch1182.lib.base.statusHeight
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.SoftKeyBoardHelper
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityAichatBinding
import com.munch1182.p1.databinding.ItemAichatBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class AiChatActivity : BaseActivity() {

    private val bind by bind(ActivityAichatBinding::inflate)
    private val vm by viewModels<AiChatVM>()
    private val softHelper by lazy { SoftKeyBoardHelper(window.decorView) }
    private val log = log()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bind.send.setOnClickListener { send() }
        val adapter = bind.rv.setup(this)

        softHelper.listen(this).setKeyBoardChangeListener {
            val lp = bind.bottomSpace.layoutParams
            lp.height = if (it > 0) it else 0
            bind.bottomSpace.post { bind.bottomSpace.requestLayout() }
        }

        lifecycleScope.launchIO {
            vm.data.collect {
                when (it) {
                    null -> {}
                    is AiChatData.Ask -> adapter.addAsk(it)
                    is AiChatData.Answer -> adapter.addAnswer(it)
                    is AiChatData.Over -> adapter.answerOver()
                }
            }
        }
        val dp16 = 16.dp2PX.toInt()
        bind.rv.setPadding(dp16, statusHeight() + dp16, dp16, dp16)
    }


    private fun send() {
        val str = bind.input.text.toString()
        if (str.isNotEmpty()) {
            vm.send(str)
            SoftKeyBoardHelper.hide(window)
        }
    }

    private fun RecyclerView.setup(ctx: Context): AiAdapter {
        layoutManager = LinearLayoutManager(ctx)
        val aiAdapter = AiAdapter()
        adapter = aiAdapter
        return aiAdapter
    }

    private class AiAdapter : RecyclerView.Adapter<AiAdapter.VH>() {
        class VH(val bind: ItemAichatBinding) : RecyclerView.ViewHolder(bind.root) {
            val tv get() = bind.content

            fun initBgByType() {
                val dp16 = 16.dp2PX
                val lp = (bind.content.layoutParams as? FrameLayout.LayoutParams)
                when (ViewType.from(itemViewType)) {
                    ViewType.Receive -> {
                        bind.content.background = newCornerDrawable(dp16, dp16, 0f, dp16, strokeWidth = 2)
                        lp?.gravity = Gravity.END
                    }

                    ViewType.Send -> {
                        bind.content.background = newCornerDrawable(dp16, dp16, dp16, 0f, strokeWidth = 2)
                        lp?.gravity = Gravity.START
                    }
                }
            }
        }

        private val list = mutableListOf<AiChatData.AiReceive>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemAichatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = list.size

        override fun getItemViewType(position: Int): Int {
            return when (list[position]) {
                is AiChatData.Answer -> ViewType.Receive.type
                is AiChatData.Ask -> ViewType.Send.type
                AiChatData.Over -> ViewType.Receive.type
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.initBgByType()
            when (val data = list[position]) {
                is AiChatData.Answer -> holder.tv.text = data.content
                is AiChatData.Ask -> holder.tv.text = data.content
                AiChatData.Over -> {}
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                if (payloads.firstOrNull() == true) {
                    val data = list[position] as? AiChatData.Answer ?: return
                    holder.tv.text = "${holder.tv.text}${data.content}"
                }
            }
        }

        suspend fun addAsk(it: AiChatData.Ask) {
            list.add(it)
            withUI { notifyItemInserted(list.size) }
        }

        suspend fun addAnswer(it: AiChatData.Answer) {
            val last = list.lastOrNull()
            var index = list.size
            if (last != null && last.isSame(it)) {
                index = list.size - 1
                list[index] = last.addAnswer(it)
                withUI { notifyItemChanged(index, true) }
            } else {
                list.add(it)
                withUI { notifyItemInserted(index) }
            }
        }

        fun answerOver() {

        }

        sealed class ViewType(val type: Int) {
            data object Send : ViewType(TYPE_SEND)
            data object Receive : ViewType(TYPE_RECEIVE)

            companion object {
                private const val TYPE_SEND = 1
                private const val TYPE_RECEIVE = 2
                fun from(type: Int) = when (type) {
                    TYPE_SEND -> Send
                    TYPE_RECEIVE -> Receive
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

}

object AiChatData {
    sealed class AiReceive {
        fun isSame(it: Answer) = this is Answer && id == it.id
        fun addAnswer(it: Answer): Answer {
            if (this !is Answer) return it
            return Answer(id, content + it.content)
        }

        override fun toString(): String {
            return when (this) {
                is Ask -> "Ask($content)"
                is Answer -> "Answer($id, $content)"
                is Over -> "Over"
            }
        }
    }

    class Ask(val content: String) : AiReceive()
    class Answer(val id: Int, val content: String) : AiReceive()
    data object Over : AiReceive()
}


class AiChatVM : ViewModel() {
    private val id = AtomicInteger(0)
    private val log = log()

    private val _data = MutableStateFlow<AiChatData.AiReceive?>(null)
    val data = _data.asStateFlow()

    fun send(str: String) {
        viewModelScope.launchIO {
            _data.emit(AiChatData.Ask(str))

            delay(Random.nextLong(100L, 300L) * 2L)
            val newId = id.getAndIncrement()
            val count = Random.nextInt(100, 500)
            repeat(count) {
                val answer = newRandom()
                _data.emit(AiChatData.Answer(newId, answer))
                delay(Random.nextLong(100L, 200L))
            }
            _data.emit(AiChatData.Over)
        }
    }
}