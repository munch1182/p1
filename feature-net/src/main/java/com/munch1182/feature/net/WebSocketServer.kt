package com.munch1182.feature.net

import com.munch1182.lib.android.logger
import io.ktor.http.ContentType
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * 服务器状态枚举
 */


class WebSocketServer(
    val host: String = "0.0.0.0",
    val port: Int = 1234,
    private val maxConnections: Int = Int.MAX_VALUE,
) {
    enum class ServerState {
        STARTING,   // 正在启动
        RUNNING,    // 运行中
        STOPPING,   // 正在关闭
        STOPPED     // 已关闭（最终状态）
    }

    private val log = logger()

    // 状态流
    private val _state = MutableStateFlow(ServerState.STOPPED)
    val state: StateFlow<ServerState> = _state.asStateFlow()

    // 便捷属性（可选）
    val isRunning: Boolean get() = state.value == ServerState.RUNNING

    // 服务器引擎
    private var engine: EmbeddedServer<*, *>? = null

    // 会话存储（线程安全）
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    // 当前连接计数
    private val connectionCount = AtomicInteger(0)

    // 保护所有状态和资源的互斥锁
    private val mutex = Mutex()

    // 生成会话ID
    private fun generateSessionId() = java.util.UUID.randomUUID().toString()

    // ---------- 公开的发送方法（保持不变） ----------
    suspend fun broadcast(frame: Frame): Result<Unit> = try {
        sessions.values.forEach { session ->
            try {
                session.send(frame)
            } catch (e: Exception) {
                // 忽略发送失败
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendTo(sessionId: String, frame: Frame): Result<Unit> = try {
        val session = sessions[sessionId] ?: return Result.failure(IllegalStateException("Session not found"))
        session.send(frame)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun send2First(frame: Frame): Result<Unit> = try {
        val session = sessions.values.firstOrNull()
            ?: return Result.failure(IllegalStateException("No active connection"))
        session.send(frame)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun send2First(text: String) = send2First(Frame.Text(text))

    fun connectionCount(): Int = connectionCount.get()

    // ---------- 生命周期管理 ----------
    /**
     * 异步启动服务器（立即返回）。
     * 若当前处于 STARTING、RUNNING 或 STOPPING，会先执行优雅关闭，等待完成后重新启动。
     * 启动结果通过 [state] 流反馈：STARTING → RUNNING（成功）或 STARTING → STOPPED（失败）。
     */
    suspend fun run() {
        // 1. 确保服务器处于可启动状态（IDLE 或 STOPPED），否则先停止
        while (true) {
            val currentState = mutex.withLock { _state.value }
            when (currentState) {
                ServerState.STOPPED -> break // 可以直接启动
                ServerState.STARTING, ServerState.RUNNING -> {
                    log.d("Server is $currentState, stopping gracefully before restart")
                    stopInternal(graceful = true) // 优雅关闭
                    // 停止后继续循环，检查状态是否为 STOPPED
                }

                ServerState.STOPPING -> {
                    log.d("Server is stopping, waiting...")
                    delay(100) // 轮询等待停止完成
                    // 继续循环
                }
            }
        }

        // 2. 开始启动
        mutex.withLock { _state.value = ServerState.STARTING }
        log.d("Starting server on $host:$port")

        try {
            engine = embeddedServer(Netty, port = port) {
                install(WebSockets) {
                    pingPeriod = 30.seconds
                    timeout = 30.seconds
                    maxFrameSize = Long.MAX_VALUE
                }

                routing {
                    get("/health") { call.respondText("ok") }

                    get("/ws") {
                        val html = createWebSocketHtml("/")
                        call.respondText(html, ContentType.Text.Html)
                    }

                    webSocket("/") {
                        // 连接数限制
                        val currentCount = connectionCount.incrementAndGet()
                        if (currentCount > maxConnections) {
                            connectionCount.decrementAndGet()
                            send(Frame.Close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Too many connections")))
                            return@webSocket
                        }

                        val sessionId = generateSessionId()
                        sessions[sessionId] = this
                        log.d("Session $sessionId connected")
                        send(Frame.Text("Connected with ID: $sessionId"))

                        try {
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        send(Frame.Text("UnSupport: $text"))
                                        log.d("[$sessionId]: Received text: $text")
                                    }

                                    is Frame.Binary -> { /* 忽略 */
                                    }

                                    is Frame.Ping -> { /* 自动响应 */
                                    }

                                    is Frame.Pong -> { /* 忽略 */
                                    }

                                    is Frame.Close -> break
                                }
                            }
                        } catch (e: Exception) {
                            log.e("WebSocket error for $sessionId: ${e.message}")
                        } finally {
                            sessions.remove(sessionId)
                            connectionCount.decrementAndGet()
                            log.d("Session $sessionId disconnected")
                        }
                    }
                }
            }.start(wait = false)

            mutex.withLock { _state.value = ServerState.RUNNING }
            log.d("Server started successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            log.e("Failed to start server: ${e}")
            mutex.withLock {
                engine = null
                _state.value = ServerState.STOPPED
            }
        }
    }

    /**
     * 立即关闭服务器（强制断开所有连接）
     */
    suspend fun close(): Result<Unit> = try {
        stopInternal(graceful = false)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 优雅关闭（等待连接自然结束，超时后强制关闭）
     */
    suspend fun closeGracefully(timeoutMillis: Long = 10000): Result<Unit> = try {
        stopInternal(graceful = true, gracefulTimeoutMillis = timeoutMillis)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- 内部关闭实现 ----------
    /**
     * 内部关闭逻辑，更新状态为 STOPPING → STOPPED。
     * @param graceful 是否优雅关闭（等待连接断开）
     * @param gracefulTimeoutMillis 优雅关闭超时时间（毫秒）
     */
    private suspend fun stopInternal(graceful: Boolean, gracefulTimeoutMillis: Long = 10000) {
        // 如果已经处于停止状态，直接返回
        mutex.withLock {
            if (_state.value == ServerState.STOPPED) {
                return
            }
            _state.value = ServerState.STOPPING
        }

        try {
            // 发送关闭帧
            val closeFrame = Frame.Close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutting down"))
            sessions.values.forEach { session ->
                try {
                    session.send(closeFrame)
                } catch (e: Exception) { /* 忽略 */
                }
            }

            if (graceful) {
                // 等待连接数降为0或超时
                val start = System.currentTimeMillis()
                while (connectionCount.get() > 0 && System.currentTimeMillis() - start < gracefulTimeoutMillis) {
                    delay(100)
                }
            }

            // 强制清理剩余
            sessions.clear()
            connectionCount.set(0)

            // 停止引擎
            engine?.stop(5000, 5000)
            engine = null

            mutex.withLock {
                _state.value = ServerState.STOPPED
            }
            log.d("Server stopped")
        } catch (e: Exception) {
            log.e("Error during server stop", e)
            // 异常情况下也确保状态为 STOPPED
            mutex.withLock {
                engine = null
                sessions.clear()
                connectionCount.set(0)
                _state.value = ServerState.STOPPED
            }
            throw e // 重新抛出，让调用方处理
        }
    }
}