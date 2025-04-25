package com.munch1182.p1.views

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.munch1182.lib.base.Logger
import com.munch1182.lib.helper.currAct
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.StateButton
import com.munch1182.p1.ui.setContentWithRv
import kotlin.random.Random

class ServerActivity : BaseActivity() {
    private val log = Logger("BindServer")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentWithRv { Views() }
        currAct.bindService(Intent(currAct, BindServer::class.java), conn, Context.BIND_AUTO_CREATE)
    }

    private var server: BindServer? = null

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log.logStr("connect: onServiceConnected")
            server = (service as? BindServer.BindServerBinder)?.service
            log.logStr("connect: getService $server")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            log.logStr("connect: onServiceDisconnected")
        }
    }

    @Composable
    private fun Views() {
        var isBind by remember { mutableStateOf(false) }
        var getValue by remember { mutableIntStateOf(-1) }
        StateButton(if (!isBind) "绑定服务" else "解绑服务", isBind) {
            isBind = !isBind
            if (isBind) {
                runCatching { currAct.bindService(Intent(currAct, BindServer::class.java), conn, Context.BIND_AUTO_CREATE) }
            } else {
                getValue = -1
                runCatching { currAct.unbindService(conn) }
            }
        }
        if (isBind) ClickButton("获取值") {
            getValue = server?.rand ?: -1
            log.logStr("getValue: $getValue")
        }
        if (getValue != -1) Text("getValue: $getValue")
    }
}

class BindServer : Service() {
    private val log = Logger("BindServer")

    private val binder = BindServerBinder()

    inner class BindServerBinder : Binder() {
        val service: BindServer
            get() = this@BindServer
    }

    val rand: Int
        get() = Random.nextInt(100)

    override fun onCreate() {
        super.onCreate()
        log.logStr("onCreate")
    }

    override fun onBind(intent: Intent?): IBinder {
        log.logStr("onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log.logStr("onStartCommand $flags $startId")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        log.logStr("onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        log.logStr("onDestroy")
    }
}