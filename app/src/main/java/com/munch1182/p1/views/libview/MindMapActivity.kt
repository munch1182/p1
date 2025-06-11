package com.munch1182.p1.views.libview

import android.os.Bundle
import com.google.gson.Gson
import com.munch1182.lib.widget.mindmap.MindMapView
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityMindmapBinding

class MindMapActivity : BaseActivity() {

    private val bind by bind(ActivityMindmapBinding::inflate)
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.root.fitsSystemWindows = true
        bind.btn.setOnClickListener { changeNode() }
        changeNode()
    }

    private fun changeNode() {
        bind.mindmap.update {
            val arr = arrayOf(json, json1)
            val jsonStr = arr[index % arr.size]
            setNode(Gson().fromJson(jsonStr, MindMapView.Node::class.java))
            index += 1
        }
    }

    private val json1 = "{" +
            "  \"name\": \"高效学习\"," +
            "  \"children\": [" +
            "        {\"name\": \"补充坚果类食物维持大脑供能\"}," +
            "        {\"name\": \"每日重点任务清单\"}," +
            "        {\"name\": \"培养信息溯源意识避免被误导\"}" +
            "  ]" +
            "}"

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
