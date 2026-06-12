package com.munch1182.lib.bluetooth.le

import com.munch1182.lib.android.Log
import com.munch1182.lib.android.invokeOnCompletion
import com.munch1182.lib.common.launchIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 识别蓝牙数据
 */
interface BLETypeIdentifier<T> {

    /**
     * 从接收到的蓝牙数据包中解析出数据类型标识符 [T]。
     *
     * 该标识符将用于：
     * - 将数据分发到注册了相同 [T] 的 [BLEDataHelper.onType]（接收器）。
     * - 匹配正在等待特定 [T] 响应的命令（通过 [BLECommand.target]）。
     *
     * ## 重要：选择 [T] 的粒度直接影响请求-响应的正确性
     *
     * ### 1. 以**协议类型**作为 [T]（例如枚举值 0x01 表示“温度数据”，0x02 表示“湿度数据”）
     *    优点：简单，大多数 BLE 设备按此设计。
     *    缺点：如果同时发送**两个相同的协议类型请求**（例如两个读取温度的命令），
     *          由于它们的 [BLECommand.target] 相同，收到一个温度响应后，**只有一个请求会得到结果**，
     *          另一个请求会超时或错误接收。**这是设计限制**，需要业务层保证同一协议类型
     *          不会并发发送（例如使用队列串行化请求）。
     *
     * ### 2. 以**请求的唯一 ID** 作为 [T]（例如每次发送命令时动态生成递增 ID，并放在响应包中回传）
     *    优点：完美支持并发请求和响应乱序，每个请求都能正确匹配自己的响应，无冲突。
     *    缺点：需要**设备协议配合**，要求设备在响应数据中携带请求方指定的 ID。
     *
     * ## 实现建议
     * - 如果协议不支持带请求 ID，就使用方法 1，并在业务层确保同一类型的请求串行发送。
     * - 如果协议支持请求 ID，强烈建议使用方法 2，以获得最佳的并发性能。
     *
     * @param data 接收到的原始字节数据（一个或多个包，多次调用可能接收到分包的其中一部分，
     *             是否完整分包由上层调用者决定）。
     * @return 解析出的数据类型标识符，如果数据无效或无法识别类型则返回 `null`。
     *         返回 `null` 时，数据会传递给 [onInvalidData] 回调，不会进入后续分发流程。
     *
     * @see BLEDataHelper.onType
     * @see BLECommand.target
     * @see onInvalidData
     */
    fun identifyType(data: ByteArray): T?

    /**
     * 无效数据，会回调到此方法
     */
    fun onInvalidData(data: ByteArray) {}
}

/**
 * 蓝牙数据发送接口
 */
interface BLEDataSender {
    /**
     * 实际发送的方法，最终会被回调到此方法
     */
    fun send(data: ByteArray): Result<Unit>
}

/**
 * 提供一个接收蓝牙数据的对象
 */
interface BLEDataReceiverProvider {
    /**
     * 返回一个接收蓝牙接收数据的flow
     */
    fun receiveFlowProvider(): Flow<ByteArray>
}

/**
 * 蓝牙发送协议定义
 */
interface BLECommand<T, R> {
    /**
     * 发送的完整数据;
     *
     * 注意： 如果超过了最大mtu大小的数据，则需要手动分包
     */
    val sendData: ByteArray

    /**
     * 数据类型, 用于区分数据
     */
    val type: T

    /**
     * 目标数据类型, 如果为null则发送后无需等待结果立即返回
     */
    val target: T?

    /**
     * 解析[target]返回的数据并返回解析后的值;
     *
     * 关于分包数据：[parseData]会接收到每一包能[BLETypeIdentifier.identifyType]的数据，
     * 如果该数据不完整(分包或者多包数据)，可以返回[DataPackParseResult.NeedMoreData]来等待下一包数据直至接收完;
     */
    suspend fun parseData(data: ByteArray): DataPackParseResult<R>
}

/**
 * 蓝牙协议结果
 */
sealed class DataResult<out R> {
    /**
     * 返回完整数据;
     */
    data class Full<R>(val data: R?) : DataResult<R>()

    /**
     * 解析失败
     */
    data class Fail(val msg: String) : DataResult<Nothing>()

    /**
     * 请求冲突：当上一个 [BLECommand.target] 相同的请求还未完成时，再次发起相同 [target] 的请求，
     * 后一个请求不会实际发送，也不会等待响应，而是直接返回此类型。
     *
     * 调用方收到 [Conflict] 后，可以根据业务需要决定：
     * - 延迟重试（不推荐，可能反复冲突）。
     * - 等待前一个请求完成后再发起新请求（例如使用队列串行化）。
     * - 若协议允许且响应数据可共享，可复用前一个请求的结果（由调用方自行实现）。
     */
    data class Conflict(val target: Any) : DataResult<Nothing>()

    /**
     * 等待结果超时
     */
    object TimeOut : DataResult<Nothing>()

    /**
     * 成功则返回数据, 否则返回null;
     */
    val getOrNull get() = if (this is Full<R>) data else null
}

/**
 * 蓝牙包解析结果
 */
sealed class DataPackParseResult<out T> {
    /**
     * 已接收到完整数据并返回，不再等待后续数据
     */
    data class Full<T>(val data: T?) : DataPackParseResult<T>()

    /**
     * 解析失败
     */
    data class Fail(val msg: String) : DataPackParseResult<Nothing>()

    /**
     * 需要更多数据，继续监听后续数据；注意如果超时仍会提前返回
     */
    object NeedMoreData : DataPackParseResult<Nothing>()

    /**
     * 返回数据，否则返回null
     */
    val getOrNull get() = if (this is Full<T>) data else null
}

/**
 * 蓝牙数据发送与解析的共用逻辑
 *
 * @param scope 协程作用域
 * @param identifier 数据类型识别
 * @param sender 数据发送器
 * @param receiver 提供一个响应数据流
 *
 * @see BLECommand
 * @see send 发送协议，等待符合[BLECommand.target]的包并返回[BLECommand.parseData]的值
 * @see onType 监听目标的数据接收
 */
class BLEDataHelper<T : Any>(
    private val scope: CoroutineScope,
    private val identifier: BLETypeIdentifier<T>,
    private val sender: BLEDataSender,
    receiver: BLEDataReceiverProvider
) {
    companion object {
        private const val TAG = "BLEDataHelper"
    }

    // 等待响应的请求映射：target -> 请求上下文
    private val pendingMap = ConcurrentHashMap<T, BleCommandCtx<T, *>>()

    // 接收数据流，按类型分流
    private val typedDataFlow = receiver.receiveFlowProvider().mapNotNull { data ->
        val type = identifier.identifyType(data)  // 可能抛异常，但让异常直接抛出，不捕获
        if (type == null) {
            identifier.onInvalidData(data)
            Log.d(TAG, "InvalidData: ${data.toHexString()}")
            null
        } else {
            type to data
        }
    }

    // 管理订阅（线程安全）
    private val subscriptions = CopyOnWriteArrayList<Subscription<T>>()

    // 串行化发送的互斥锁
    private val sendMutex = Mutex()

    init {
        // 数据分发协程
        scope.launchIO {
            typedDataFlow.collect { (type, data) ->
                subscriptions.forEach { sub ->
                    if (sub.target == null || sub.target == type) {
                        scope.launch { runCatching { sub.onData(data) } } // 分发数据
                    }
                }
                pendingMap[type]?.let { ctx ->
                    val completeResult = try {
                        when (val result = ctx.cmd.parseData(data)) {
                            is DataPackParseResult.Fail -> DataResult.Fail(result.msg)
                            is DataPackParseResult.Full<*> -> DataResult.Full(result.data)
                            DataPackParseResult.NeedMoreData -> return@let // 继续等待更多数据，不做任何事
                        }
                    } catch (e: Exception) {
                        DataResult.Fail(e.message ?: "Unknown parseData error")
                    }
                    ctx.complete(completeResult)
                    pendingMap.remove(type)
                }
            }
        }

        // 作用域取消时的清理
        scope.invokeOnCompletion {
            pendingMap.values.forEach { ctx ->
                ctx.complete(DataResult.Fail("Scope cancelled"))
            }
            pendingMap.clear()
            subscriptions.clear()
        }
    }


    /**
     * 根据数据包类型监听数据接收。
     *
     * 当设备上报的数据经 [BLETypeIdentifier.identifyType] 识别出的类型与 [type] 匹配时（或 [type] 为 `null` 时匹配所有数据），
     * 会回调 [block] 并传入原始字节数据。
     * 此方法不会受到其它方法影响接收的数据，也不会影响其它方法接收到的数据；
     *
     *
     * ### 生成周期说明
     * 返回一个取消函数（`() -> Unit`）。
     * 未调用该函数则会一直接收数据。
     * 调用该函数可移除本次注册的监听器，并停止接收后续数据。
     * 当[scope]取消时，仍未被取消的监听器也会被自动取消；
     *
     * 典型用法：
     * ```kotlin
     * val cancel = helper.onType(MyType.TEMP) { data ->
     *     // 处理温度数据
     * }
     * // 当不再需要监听时：
     * cancel()
     * ```
     *
     * ### 重复注册的处理
     * - 如果使用**完全相同的** `(type, block)` 多次调用本函数，每次调用都会创建一个独立的订阅。
     * - 因此，同一个 `block` 可能会被多次添加，当匹配的数据到达时，该 [block] 会被调用**多次**（每次订阅独立回调）。
     * - 如果需要避免重复，建议调用方自行维护去重逻辑，或在添加前先手动取消已有的相同订阅。
     *
     * @param type 要监听的数据类型；若为 `null` 则监听所有类型的数据。
     * @param block 数据接收回调，参数为原始字节数组。回调在 [scope] 指定的协程上下文中异步执行。
     * @return 取消函数，调用后该订阅立即失效。多次调用取消函数是安全的，重复取消不会有副作用。
     */
    fun onType(type: T? = null, block: (ByteArray) -> Unit): () -> Unit {
        val sub = Subscription(type, block)
        subscriptions.add(sub)
        return { subscriptions.remove(sub) }
    }

    /**
     * 执行命令并返回结果。
     *
     * ### 调用方行为（重要）
     * - 本函数是 **挂起函数**，调用它的协程会 **挂起等待** 结果返回，然后才继续执行后续代码。
     *
     * ### 内部发送与响应的串行/并行规则
     * - **数据发送是串行的**：所有命令按调用顺序依次发送（前一个 `sender.send` 完成后才发下一个）。
     * - **发送后不等待响应**：发送完成后，若需要等待响应（`target != null`）会立即注册等待，并**继续处理队列中的下一个命令**（不会阻塞整个发送循环）。
     *   - 因此，不同 `target` 的命令可以**并发等待**响应，但相同 `target` 的命令不能并发（第二个会直接返回[DataResult.Conflict]）。
     *   - **重要**：调用方必须保证相同 `target` 的请求串行化发送（例如使用队列或互斥锁），否则后一个请求会立即收到 `Conflict`。
     * - **发送与调用方挂起无关**：调用方的挂起等待不会影响下一个发送执行。
     *
     * ### 设备并发能力与业务层策略
     * - 即使不同 `target` 的命令可以在本类内部并发等待，**最终能否真正并行处理取决于设备端**。
     * - 许多 BLE 设备不支持同时处理多个协议请求（即同一时刻只能处理一个命令，直到响应返回后才能接收下一个命令）。
     * - 如果设备不支持并发（即一次只能处理一个请求），调用方应在业务层做好串行化，例如：
     *   - 使用一个共享的队列或 `Mutex`，确保同一设备同一时刻只发送一个 `send` 调用。
     *   - 或者根据协议类型（如“占用型协议”）选择不同的并行策略。
     * - **建议**：在编写业务代码前，查阅设备协议文档，确认设备是否支持命令并发。若不支持，请自行实现请求串行化。
     *
     * ### 结果返回时机
     * - **`target == null`**（无需响应）：`sender.send` 执行完成后立即返回结果（成功或失败）。
     * - **`target != null`**（需要响应）：
     *   - 等待设备上报匹配 `target` 的数据，且 `parseData` 返回 `Full` 或 `Fail`。
     *   - 若 `parseData` 返回 `NeedMoreData`，会持续等待后续数据包直至解析完成。
     *   - 若超过 `timeout` 未接收到带有目标T的数据包返回，或者多包组合超过了设定的超时时间，返回 `TimeOut`。
     *
     * ### 线程/协程上下文
     * - 调用方：可在任意协程中调用，挂起恢复时回到调用前的协程上下文（即调用时的调度器）。
     * - 内部执行：发送及数据解析在[BLEDataHelper.scope]指定的协程上下文中异步执行。
     *
     * @param command 待执行的命令，注意如果是多包数据，超时时间是整体时间而不是每个包的时间；
     * @param timeout 等待响应的超时时间（仅当 `target != null` 时生效），默认 15 秒
     * @return 结果：`Full(data)`, `Fail(msg)`, `TimeOut`
     */
    suspend fun <R> send(command: BLECommand<T, R>, timeout: Duration = 15000.milliseconds): DataResult<R> {
        val ctx = BleCommandCtx<T, R>(command)

        // 串行化发送：确保同一时刻只有一个命令在发送
        sendMutex.withLock {
            // 1. 实际发送数据
            val sendResult = runCatching { sender.send(command.sendData) }
            if (sendResult.isFailure) return DataResult.Fail(sendResult.exceptionOrNull()?.message ?: "send failed")
            // 2. 不需要等待响应，直接返回成功
            val target = command.target ?: return DataResult.Full(null)
            // 3. 检查是否已有相同 target 的请求在等待
            if (pendingMap.containsKey(target)) return DataResult.Conflict(target)
            // 4. 注册等待响应的上下文
            pendingMap[target] = ctx
        }

        // 发送已完成，现在开始等待响应（带超时）
        val result = withTimeoutOrNull(timeout) { ctx.await() }
        if (result == null) {
            // 超时：移除 pendingMap 并通知 ctx
            pendingMap.remove(command.target) //无需再通知ctx，因为ctx已经超时了不会再进行阻塞
            return DataResult.TimeOut
        }
        return result
    }

    // 请求上下文，持有命令、结果 Deferred
    private class BleCommandCtx<T, R>(
        val cmd: BLECommand<T, *>,
        private val deferred: CompletableDeferred<DataResult<R>> = CompletableDeferred()
    ) {
        /**
         * 阻塞直到收到结果或者异常
         */
        suspend fun await(): DataResult<R> = deferred.await()

        /**
         * 完成请求，用于取消阻塞
         *
         * 因为请求是一对一的，所以类型转换是安全的
         */
        fun complete(result: DataResult<*>) {
            @Suppress("UNCHECKED_CAST")
            deferred.complete(result as DataResult<R>)
        }
    }

    // 内部包装：一个类型可以对应多个回调
    private data class Subscription<T>(val target: T?, val onData: (ByteArray) -> Unit)
}

/**
 * 监听下一包的数据，然后取消监听; 如果超时，返回null；
 */
suspend fun <T : Any> BLEDataHelper<T>.onNext(type: T? = null, timeout: Duration = 3000.milliseconds) = withTimeoutOrNull(timeout) {
    suspendCancellableCoroutine { cont ->
        val cancelOnType = onType(type) { data ->
            if (cont.isActive) {
                cont.resume(data)
                cancel()
            }
        }
        cont.invokeOnCancellation { cancelOnType() }
    }
}