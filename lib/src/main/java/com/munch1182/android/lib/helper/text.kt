package com.munch1182.android.lib.helper

import java.util.regex.Pattern

/**
 * 文本分段处理器
 * 支持两种输入模式：
 * 1. 模式A：增量文本片段（不包含之前的文本）
 * 2. 模式B：完整当前文本（包含之前的所有文本）
 */
class TextSegmenter {

    // 句子结束符（包括中英文）
    private val sentenceEndings = setOf('。', '！', '？', '.', '!', '?', ';', '；', '…')

    // 段落分隔符（换行、空行等）
    private val paragraphSeparators = setOf('\n', '\r', '|', '¶', '§')

    // 缓冲区（用于增量模式）
    private var incrementalBuffer = StringBuilder()

    // 上一次处理的完整文本（用于完整文本模式）
    private var lastFullText = ""

    // 用于检测段落
    private val paragraphPattern = Pattern.compile("\\n\\s*\\n")

    /**
     * 模式A：处理增量文本片段（不包含之前的内容）
     * 适用于流式传输场景
     * @param newChunk 新的文本片段（不包含之前已处理的内容）
     * @return 返回完整的句子或段落列表
     */
    fun processIncrementalChunk(newChunk: String): List<String> {
        incrementalBuffer.append(newChunk)
        return extractSegmentsFromBuffer()
    }

    /**
     * 模式B：处理完整当前文本（包含之前的内容）
     * 适用于每次传入完整累积文本的场景
     * @param fullText 当前完整的文本（包含之前已处理的内容）
     * @return 返回自上次处理以来新增的完整句子或段落列表
     */
    fun processFullText(fullText: String): List<String> {
        // 如果这是第一次调用，或者文本比上次的短（可能重置了）
        if (lastFullText.isEmpty() || fullText.length <= lastFullText.length) {
            // 整个文本都是新的
            val newSegments = extractSegmentsFromText(fullText)
            lastFullText = fullText
            return newSegments
        }

        // 检查新文本是否以旧文本开头
        if (fullText.startsWith(lastFullText)) {
            // 提取新增部分
            val newText = fullText.substring(lastFullText.length)
            if (newText.isBlank()) {
                return emptyList() // 没有新内容
            }

            // 处理新增文本
            val newSegments = extractSegmentsFromText(newText)
            lastFullText = fullText
            return newSegments
        } else {
            // 文本不连续（可能是重置或编辑了之前的文本）
            // 需要重新分析整个文本，但要避免重复处理
            return handleNonContinuousText(fullText)
        }
    }

    /**
     * 从缓冲区提取分段（用于增量模式）
     */
    private fun extractSegmentsFromBuffer(): List<String> {
        val segments = mutableListOf<String>()
        val text = incrementalBuffer.toString()

        // 1. 检查是否有完整段落
        val paragraphSegments = splitParagraphs(text)
        if (paragraphSegments.size > 1) {
            // 有完整段落，返回前面的完整段落，保留最后一个不完整的部分
            val completeParagraphs = paragraphSegments.dropLast(1)
            val remainingText = paragraphSegments.last()

            completeParagraphs.forEach { segment ->
                if (segment.isNotBlank()) {
                    segments.add(segment.trim())
                }
            }

            // 更新缓冲区为剩余文本
            incrementalBuffer.clear()
            incrementalBuffer.append(remainingText)
            return segments
        }

        // 2. 如果没有完整段落，尝试按句子分割
        val sentenceSegments = splitSentences(text)
        if (sentenceSegments.size > 1) {
            // 有完整句子，返回前面的完整句子，保留最后一个不完整的句子
            val completeSentences = sentenceSegments.dropLast(1)
            val remainingText = sentenceSegments.last()

            completeSentences.forEach { sentence ->
                if (sentence.isNotBlank() && isMeaningfulSentence(sentence)) {
                    segments.add(sentence.trim())
                }
            }

            // 更新缓冲区
            incrementalBuffer.clear()
            incrementalBuffer.append(remainingText)
            return segments
        }

        // 3. 如果既没有段落也没有完整句子，检查是否有超长文本需要强制分割
        if (segments.isEmpty() && text.length > 100) {
            val forcedSegments = forceSplit(text, 50, 100)
            if (forcedSegments.size > 1) {
                val completeSegments = forcedSegments.dropLast(1)
                val remainingText = forcedSegments.last()

                completeSegments.forEach { segment ->
                    if (segment.isNotBlank()) {
                        segments.add(segment.trim())
                    }
                }

                incrementalBuffer.clear()
                incrementalBuffer.append(remainingText)
            }
        }

        return segments
    }

    /**
     * 从文本直接提取分段（用于完整文本模式）
     */
    private fun extractSegmentsFromText(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // 先将文本分割成段落
        val paragraphs = splitParagraphs(text)
        val segments = mutableListOf<String>()

        paragraphs.forEach { paragraph ->
            if (paragraph.isBlank()) return@forEach

            // 如果段落较短，直接作为一个整体
            if (paragraph.length <= 100 && !paragraph.contains('。') && !paragraph.contains('.')) {
                if (isMeaningfulSentence(paragraph)) {
                    segments.add(paragraph.trim())
                }
            } else {
                // 否则按句子分割
                val sentences = splitSentences(paragraph)
                sentences.forEach { sentence ->
                    if (sentence.isNotBlank() && isMeaningfulSentence(sentence)) {
                        segments.add(sentence.trim())
                    }
                }
            }
        }

        return segments
    }

    /**
     * 处理不连续的文本（完整文本模式）
     */
    private fun handleNonContinuousText(fullText: String): List<String> {
        // 找出新旧文本的公共前缀
        val commonPrefix = findCommonPrefix(lastFullText, fullText)
        val oldSuffix = lastFullText.substring(commonPrefix.length)
        val newText = fullText.substring(commonPrefix.length)

        // 如果有旧后缀被删除，可能需要调整
        if (oldSuffix.isNotEmpty()) {
            // 文本被编辑了，这里简单处理：重新分析整个文本
            // 在实际应用中，可能需要更复杂的逻辑来处理编辑
            val allSegments = extractSegmentsFromText(fullText)

            // 尝试避免重复输出之前已经输出的内容
            // 这里我们假设如果公共前缀以句子结束符结尾，那么之前的内容已经处理完毕
            val alreadyProcessed = commonPrefix.isNotEmpty() &&
                    (sentenceEndings.any { commonPrefix.endsWith(it.toString()) } ||
                            commonPrefix.contains("\n\n"))

            lastFullText = fullText

            return if (alreadyProcessed) {
                // 只返回新文本的段落
                extractSegmentsFromText(newText)
            } else {
                // 返回整个文本的段落（可能包含重复）
                allSegments
            }
        }

        lastFullText = fullText
        return extractSegmentsFromText(newText)
    }

    /**
     * 找出两个字符串的公共前缀
     */
    private fun findCommonPrefix(str1: String, str2: String): String {
        var i = 0
        val minLength = minOf(str1.length, str2.length)
        while (i < minLength && str1[i] == str2[i]) {
            i++
        }
        return str1.substring(0, i)
    }

    /**
     * 按段落分割文本
     */
    private fun splitParagraphs(text: String): List<String> {
        val matcher = paragraphPattern.matcher(text)
        val paragraphs = mutableListOf<String>()
        var lastEnd = 0

        while (matcher.find()) {
            val paragraph = text.substring(lastEnd, matcher.start())
            paragraphs.add(paragraph)
            lastEnd = matcher.end()
        }

        if (lastEnd < text.length) {
            paragraphs.add(text.substring(lastEnd))
        }

        return paragraphs
    }

    /**
     * 按句子分割文本
     */
    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        var start = 0
        var inQuotation = false
        var quoteChar: Char? = null

        for (i in text.indices) {
            val char = text[i]

            // 处理引号
            when (char) {
                '"', '\'', '「', '」', '『', '』', '《', '》' -> {
                    if (inQuotation && quoteChar == char) {
                        inQuotation = false
                        quoteChar = null
                    } else if (!inQuotation) {
                        inQuotation = true
                        quoteChar = char
                    }
                }
            }

            if (inQuotation) continue

            if (isSentenceEnding(char, text, i)) {
                val sentence = text.substring(start, i + 1)
                if (sentence.trim().isNotEmpty()) {
                    sentences.add(sentence)
                }
                start = i + 1
            }
        }

        if (start < text.length) {
            val remaining = text.substring(start)
            sentences.add(remaining)
        }

        return sentences
    }

    /**
     * 判断是否是句子结束
     */
    private fun isSentenceEnding(char: Char, text: String, index: Int): Boolean {
        if (!sentenceEndings.contains(char)) return false

        if (char == '.') {
            if (index > 0 && text[index - 1].isLetter()) {
                val prevWord = getPreviousWord(text, index - 1)
                if (isCommonAbbreviation(prevWord)) {
                    return false
                }
            }
        }

        if (char == '.' && index > 0 && index < text.length - 1) {
            if (text[index - 1].isDigit() && text[index + 1].isDigit()) {
                return false
            }
        }

        if (index < text.length - 1) {
            val nextChar = text[index + 1]
            if (sentenceEndings.contains(nextChar)) {
                return true
            }
        }

        return true
    }

    /**
     * 获取前一个单词
     */
    private fun getPreviousWord(text: String, endIndex: Int): String {
        val builder = StringBuilder()
        var i = endIndex
        while (i >= 0 && (text[i].isLetter() || text[i] == '.')) {
            builder.insert(0, text[i])
            i--
        }
        return builder.toString()
    }

    /**
     * 常见缩写列表
     */
    private fun isCommonAbbreviation(word: String): Boolean {
        val abbreviations = setOf(
            "Mr", "Mrs", "Dr", "Prof", "etc", "e.g", "i.e", "vs",
            "A.M", "P.M", "U.S", "U.K", "Inc", "Corp", "Ltd"
        )
        return abbreviations.any { word.contains(it, ignoreCase = true) }
    }

    /**
     * 强制分割超长文本
     */
    private fun forceSplit(text: String, minLength: Int, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)

        val segments = mutableListOf<String>()
        var currentIndex = 0

        while (currentIndex < text.length) {
            var endIndex = currentIndex + maxLength

            if (endIndex >= text.length) {
                segments.add(text.substring(currentIndex))
                break
            }

            var splitIndex = -1
            for (i in endIndex downTo currentIndex + minLength) {
                if (i < text.length &&
                    (sentenceEndings.contains(text[i]) ||
                            text[i] == ',' || text[i] == '，' ||
                            text[i] == ' ' || text[i] == '、')
                ) {
                    splitIndex = i + 1
                    break
                }
            }

            if (splitIndex == -1) {
                splitIndex = endIndex
            }

            segments.add(text.substring(currentIndex, splitIndex))
            currentIndex = splitIndex
        }

        return segments
    }

    /**
     * 判断是否是有效的句子
     */
    private fun isMeaningfulSentence(sentence: String): Boolean {
        val trimmed = sentence.trim()
        if (trimmed.length < 2) return false

        if (trimmed.all { it.isWhitespace() || sentenceEndings.contains(it) }) return false

        val meaninglessStarts = listOf("然后", "而且", "但是", "不过", "另外", "例如", "比如")
        if (meaninglessStarts.any { trimmed.startsWith(it) && trimmed.length < 10 }) return false

        return true
    }

    /**
     * 获取缓冲区中的剩余文本（用于增量模式）
     */
    fun flushIncremental(): String? {
        val remaining = incrementalBuffer.toString().trim()
        incrementalBuffer.clear()
        return remaining.ifBlank { null }
    }

    /**
     * 重置处理器（清除所有状态）
     */
    fun reset() {
        incrementalBuffer.clear()
        lastFullText = ""
    }

    /**
     * 获取当前缓冲区内容（增量模式）
     */
    fun getIncrementalBuffer(): String {
        return incrementalBuffer.toString()
    }

    /**
     * 获取上次处理的完整文本（完整文本模式）
     */
    fun getLastFullText(): String {
        return lastFullText
    }
}