package com.munch1182.lib.net

import com.munch1182.lib.base.OnUpdateListener
import com.munch1182.lib.base.withIO
import com.munch1182.lib.helper.ARDefaultManager
import com.munch1182.lib.helper.ARManager
import com.munch1182.lib.net.WebSocketService.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketService(
    private val scope: CoroutineScope, private val client: OkHttpClient = httpClient.build()
) : ARManager<OnUpdateListener<State>> by ARDefaultManager() {
    private var _socket: WebSocket? = null
    private val _msg = MutableSharedFlow<State>()

    suspend fun connect(request: Request) = withIO {
        try {
            _socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    super.onOpen(webSocket, response)
                    updateState(State.Open)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    super.onMessage(webSocket, text)
                    updateState(State.Message(text))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    super.onFailure(webSocket, t, response)
                    updateState(State.Error(t))
                    close(reason = "close as onFailure: ${t.message}")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    super.onClosed(webSocket, code, reason)
                    _socket = null
                }
            })
            updateState(State.Connecting)
        } catch (e: Exception) {
            updateState(State.Error(e))
        }
        return@withIO _msg.asSharedFlow()
    }

    private fun updateState(state: State) {
        forEach { it.onUpdate(state) } // 回调不经过flow的转换或者延迟
        scope.launch { _msg.emit(state) }
    }

    fun close(code: Int = 1000, reason: String = "close()"): Boolean {
        val result = _socket?.close(code, reason) ?: true
        _socket = null
        return result
    }

    suspend fun send(msg: String) = withIO {
        return@withIO _socket?.send(msg) ?: false
    }

    sealed class State {
        object Connecting : State()
        object Open : State()
        class Message(val msg: String) : State()
        class Error(val t: Throwable) : State()
    }
}