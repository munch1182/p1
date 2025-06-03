package com.munch1182.p1.views.libview

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.munch1182.lib.base.withCatch
import com.munch1182.lib.widget.mindmap.MindMapFromStart2EndStyle
import com.munch1182.lib.widget.mindmap.MindMapView
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Split
import com.munch1182.p1.ui.setContentWithRv

class MindMapActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
    }

    @Composable
    private fun Views() {
        var styleIndex by remember { mutableIntStateOf(0) }
        ClickButton("下一个样式") { }

        Split()

        AndroidView(
            factory = {
                MindMapView(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            update = { v ->
            })
    }

    private val styles = arrayOf(MindMapFromStart2EndStyle)

    private fun nextStyle(curr: Int): MindMapView.NodeStyle {
        return styles.getOrNull(curr) ?: styles[0]
    }

    private fun newTestData(): MindMapView.Node? {
        return withCatch { Gson().fromJson(json, MindMapView.Node::class.java) }
    }

    private val json = "{" +
            "  \"name\": \"高效学习\"," +
            "  \"children\": [" +
            "    {" +
            "      \"name\": \"时间规划与知识吸收质量\"," +
            "      \"children\": [" +
            "        {\"name\": \"番茄工作法保持专注周期\"}," +
            "        {\"name\": \"每日重点任务清单\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"知识获取渠道\"," +
            "      \"children\": [" +
            "        {\"name\": \"主题阅读筛选经典书籍\"}," +
            "        {\"name\": \"慕课平台补充最新行业课程\"}," +
            "        {\"name\": \"培养信息溯源意识避免被误导\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"整理环节\"," +
            "      \"children\": [" +
            "        {\"name\": \"康奈尔笔记法记录核心观点\"}," +
            "        {\"name\": \"思维导图梳理逻辑结构\"}," +
            "        {\"name\": \"视觉化图表提升记忆效率\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"刻意练习\"," +
            "      \"children\": [" +
            "        {\"name\": \"拆分复杂技能（如语言学习：高频词汇→连读发音）\"}," +
            "        {\"name\": \"模拟考试获取即时反馈\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"定期复盘\"," +
            "      \"children\": [" +
            "        {\"name\": \"学习日志分析时间投入产出比\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"输出成果\"," +
            "      \"children\": [" +
            "        {\"name\": \"撰写文章检验理解深度\"}," +
            "        {\"name\": \"参加实践项目验证方法论有效性\"}" +
            "      ]" +
            "    }," +
            "    {" +
            "      \"name\": \"其他注意事项\"," +
            "      \"children\": [" +
            "        {\"name\": \"学习环境：光线明暗/座椅舒适度\"}," +
            "        {\"name\": \"补充坚果类食物维持大脑供能\"}" +
            "      ]" +
            "    }" +
            "  ]" +
            "}"
}