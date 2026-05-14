package com.munch1182.core.common

/**
 * 数据分析追踪器接口。
 *
 * 定义了用于记录用户行为、屏幕访问、用户属性等分析数据的标准方法。
 * 建议即使当前不执行具体的行为分析, 也预先留下实现(使用日志)和调用, 避免后期代码量
 */
interface AnalyticsTracker {

    /**
     * 追踪用户进入某个屏幕（页面）的事件。
     *
     * @param screenName 屏幕（页面）的名称，例如 "HomeScreen"、"ProfileScreen"
     * @param properties 可选的自定义属性键值对，用于描述该屏幕的额外上下文信息，如 `"source"` 到 `"notification"`
     */
    fun trackScreen(screenName: String, properties: Map<String, Any>? = null)

    /**
     * 追踪用户触发的自定义事件。
     *
     * @param eventName 事件名称，例如 "button_click"、"purchase_completed"
     * @param properties 可选的事件属性键值对，用于描述事件的额外详细信息，如 `"item_id"` 到 `12345`
     */
    fun trackEvent(eventName: String, properties: Map<String, Any>? = null)

    /**
     * 追踪与当前用户关联的单个用户属性。
     *
     * 可用于记录用户的静态属性（如会员等级）或动态属性（如最近一次搜索关键词）。
     * 多次调用相同 `key` 会覆盖之前的值。
     *
     * @param key 属性名称，例如 "vip_level"
     * @param value 属性值，类型需与具体分析平台兼容（通常为 String、Number、Boolean 等）
     */
    fun trackUserProperty(key: String, value: Any)

    /**
     * 识别当前用户，并携带初始用户特征。
     *
     * 通常在用户登录后调用，用于将后续所有事件与指定 `userId` 关联。
     * @param userId 用户的唯一标识符（如用户 ID、UUID）
     * @param traits 可选的用户特征初始值键值对，例如 `"email"` 到 `"user@example.com"`、`"plan"` 到 `"premium"`
     */
    fun identify(userId: String, traits: Map<String, Any>? = null)

    /**
     * 批量设置当前用户的多个属性。
     *
     * 与 [trackUserProperty] 的区别在于：本方法可一次性更新多个属性，且语义上更适合表示用户元数据（如偏好设置、账号信息等）。
     * 如果属性已存在，通常会被覆盖。
     *
     * @param properties 用户属性键值对集合，例如 `mapOf("age" to 25, "city" to "Beijing")`
     */
    fun setUserProperties(properties: Map<String, Any>)
}

class DefaultLogAnalyticsTracker(private val log: Logger) : AnalyticsTracker {
    companion object {
        private const val TAG = "AnalyticsTracker"
    }

    override fun trackScreen(screenName: String, properties: Map<String, Any>?) {
        log.i(TAG, "trackScreen: $screenName, properties: $properties")
    }

    override fun trackEvent(eventName: String, properties: Map<String, Any>?) {
        log.i(TAG, "trackEvent: $eventName, properties: $properties")
    }

    override fun trackUserProperty(key: String, value: Any) {
        log.i(TAG, "trackUserProperty: $key, value: $value")
    }

    override fun identify(userId: String, traits: Map<String, Any>?) {
        log.i(TAG, "identify: $userId, traits: $traits")
    }

    override fun setUserProperties(properties: Map<String, Any>) {
        log.i(TAG, "setUserProperties: $properties")
    }
}
