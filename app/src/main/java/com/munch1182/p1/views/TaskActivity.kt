package com.munch1182.p1.views

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.base.asStateFlow
import com.munch1182.lib.base.log
import com.munch1182.lib.base.toDateStr
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.setContentWithScroll
import com.munch1182.p1.ui.theme.FontManySize
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

class TaskActivity : BaseActivity() {
    private val vm by viewModels<VM>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithScroll { Views() }
    }

    @Composable
    private fun Views() {
        val list = vm.output.collectAsState()
        val ls = rememberLazyListState()
        ClickButton("添加任务") { vm.newTask() }
        ClickButton("移除所有未完成的任务") { vm.clear() }
        LaunchedEffect(list.value.size) {
            ls.scrollToItem(list.value.size, 0)
        }
        LazyColumn(state = ls) {
            items(list.value.size) {
                val bean = list.value[it]
                val color = when (bean.type) {
                    Info.Type.Normal -> Color.Black
                    Info.Type.Remain -> Color.Red
                    Info.Type.Wait -> Color.Blue
                    Info.Type.Add -> Color.Cyan
                }
                Text(bean.text, fontSize = FontManySize, color = color)
            }
        }
    }

    class VM : ViewModel() {
        private val log = this.log(false)
        private val queue by lazy { LinkedBlockingQueue<Runnable>() }
        private val executeOutput = mutableListOf<Info>()

        private val _output = MutableStateFlow<Array<Info>>(arrayOf())
        val output = _output.asStateFlow()

        fun clear() = queue.clear()
        fun newTask() {
            val r = InfoRunnable(::newText)
            newText("add task ${r.taskName} in thread(${Thread.currentThread().name})", Info.Type.Add)
            queue.put(r)
        }


        init {
            viewModelScope.launch(CoroutineName("task queue") + Dispatchers.IO) {
                try {
                    while (this.coroutineContext.isActive) {
                        val size = queue.size
                        newText("remain $size task", Info.Type.Remain)
                        if (size == 0) newText("thread wait", Info.Type.Wait)
                        queue.take().run()
                    }
                } catch (_: Exception) {
                    log.logStr("LinkedBlockingQueue quit by interrupt")
                }
                log.logStr("LinkedBlockingQueue break")
            }
        }

        private fun newText(text: String, type: Info.Type = Info.Type.Normal) {
            log.logStr(text)
            executeOutput.add(Info(text, type))
            viewModelScope.launch { _output.emit(executeOutput.toTypedArray()) }
        }
    }

    class InfoRunnable(private val newText: (String) -> Unit) : Runnable {
        private val start = System.currentTimeMillis()
        val taskName = start.toDateStr("mm:ss.SSS")

        override fun run() {
            val curr = System.currentTimeMillis()
            val waitTime = curr - start
            newText("Task($taskName) execute in thread(${Thread.currentThread().name})(wait ${waitTime}ms)")
            runBlocking { delay(Random.nextLong(1, 5) * 1000L) }
            runBlocking {
                val cost = System.currentTimeMillis() - start
                newText("Task($taskName) over (execute cost ${cost}ms)")
            }
        }
    }

    data class Info(val text: String, val type: Type) {
        sealed class Type {
            data object Normal : Type()
            data object Wait : Type()
            data object Remain : Type()
            data object Add : Type()
        }
    }
}