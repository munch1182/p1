package com.munch1182.p1.helper

import com.google.gson.Gson
import com.munch1182.lib.base.Loglog
import com.munch1182.p1.helper.NetVideoHelper.ParseResult
import org.jsoup.Jsoup

class NetXHS(private val url: String) : NetVideoHelper.NetParse {

    companion object {
        fun isUrl(url: String): Boolean {
            return url.contains("xiaohongshu") || url.contains("xhslink")
        }
    }

    private class Info(val note: Note) {
        class Note(val noteDetailMap: Map<String, Detail>, val currentNoteId: String)
        class Detail(val note: ItemNote)
        class ItemNote(val title: String, val type: String, val video: Video?)
        class Video(val consumer: Consumer)
        class Consumer(val originVideoKey: String)
    }

    override suspend fun parse(): ParseResult? {
        val doc = Jsoup.connect(url).timeout(3000).get().html()
        val info = "window.__INITIAL_STATE__=(.*?)</script>".toRegex().find(doc)?.value

        if (info != null) {
            val json = info.replace("window.__INITIAL_STATE__=", "").replace("</script>", "")
            val infoVal = Gson().fromJson(json, Info::class.java)

            val itemNode = infoVal.note.noteDetailMap[infoVal.note.currentNoteId]?.note
            val title = itemNode?.title ?: ""
            val key = itemNode?.video?.consumer?.originVideoKey ?: return null
            if (itemNode.type == "video") {
                val durl = "https://sns-video-bd.xhscdn.com/${key}"
                Loglog.logStr("$title: $durl")
                return ParseResult(title, durl)
            }
        }
        return null
    }
}