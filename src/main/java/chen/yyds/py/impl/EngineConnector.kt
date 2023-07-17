package impl

import chen.yyds.py.Notifyer
import chen.yyds.py.impl.RpcDataModel
import com.google.common.util.concurrent.Atomics
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.WindowManager
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.ktor.websocket.serialization.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.concurrent.thread

@OptIn(ExperimentalSerializationApi::class)
abstract class EngineConnector {
    private val LOGGER = Logger.getInstance(
        EngineConnector::class.java
    )

    private val RPC_TIMEOUT = 12_000L
    private val RPC_PORT = 61140
    private val logQueue: LinkedBlockingQueue<String> = LinkedBlockingQueue()

    private val deviceIp:AtomicReference<String> = AtomicReference("192.168.1.2");

    private var mApiClient: HttpClient? = null
    private var mLogClient: HttpClient? = null

    private var mApiSession: DefaultClientWebSocketSession? = null
    private var mLogSession: DefaultClientWebSocketSession? = null

    @JvmField
    public val isApiConnecting: AtomicBoolean = AtomicBoolean(false)
    @JvmField
    public val isLogConnecting: AtomicBoolean = AtomicBoolean(false)

    private val reqQueue: LinkedBlockingQueue<RpcDataModel> = LinkedBlockingQueue()

    private val resQueue: ConcurrentSkipListMap<String, RpcDataModel> = ConcurrentSkipListMap<String, RpcDataModel>()

    private val jobs:HashSet<Job> = HashSet()

    suspend fun DefaultClientWebSocketSession.pullRes() {
        try {
            while (true) {
                val response = receiveDeserialized<RpcDataModel>()
                resQueue[response.uuid] = response
                delay(200)
                LOGGER.warn("Receive: ${response}")
                addDebugLogToQueue("[插件调试输出]从远程设备收到:$response\n")
            }
        } catch (e: Exception) {
            LOGGER.warn("Error while fetch response ${e.message}", e)
        }
    }

    suspend fun DefaultClientWebSocketSession.postReq() {
        try {
            while (true) {
                if (reqQueue.isEmpty()) {
                    delay(1000)
                    continue
                }
                val req = reqQueue.poll()
                LOGGER.warn("Send to Device: $req")
                sendSerialized(req)
            }
        } catch (e: Exception) {
            LOGGER.warn("Error while fetch postReq ${e.message}")
        }
    }

    fun engineTimeoutApiCallOrNull(request: RpcDataModel): RpcDataModel? {
        reqQueue.add(request)
        val start = System.currentTimeMillis()
        while (!resQueue.containsKey(request.uuid)) {
            if (System.currentTimeMillis() - start > RPC_TIMEOUT) {
                return null
            }
        }
        return resQueue.remove(request.uuid)
    }
    fun addDebugLogToQueue(log: String) {
        logQueue.offer("来自插件:" + log)
    }

    fun engineWaitApiCall(request: RpcDataModel): RpcDataModel {
        reqQueue.add(request)
        addDebugLogToQueue("[插件调试输出]发送到远程请求设备:$request")
        while (!resQueue.containsKey(request.uuid)) {
            Thread.sleep(1000)
        }
        return resQueue.remove(request.uuid)!!
    }

    // 首次初始化 - 连接
    public fun ensureConnect() {
        thread {
            while (true) {
                if (!isApiConnecting.get()) {
                    addDebugLogToQueue("[插件调试输出]控制断连, 正在重新连接 ${deviceIp}\n")
                    thread {
                        startConnectApiJob()
                    }
                }
                if (!isLogConnecting.get()) {
                    addDebugLogToQueue("[插件调试输出]日志断连, 正在重新连接 ${deviceIp}\n")
                    thread {
                        startConnectLogJob()
                    }
                }
                Thread.sleep(10_000)
            }
        }
    }

    private fun startConnectApiJob() {
//        val handler = CoroutineExceptionHandler { _, e ->
//            LOGGER.error("==========CoroutineExceptionHandler==========", e)
//        }

        mApiClient = HttpClient(Java) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 3600_000
                connectTimeoutMillis = 3600_000
                socketTimeoutMillis = 3600_000
            }
            install(Logging)
        }

        runBlocking {
                isApiConnecting.set(true)
                LOGGER.warn("Api C START >>>")
                val mApiSession = mApiClient!!.webSocketSession(
                    method = HttpMethod.Get,
                    host = deviceIp.get(),
                    port = RPC_PORT,
                    path = "/api"
                )
                val j1 = launch(Dispatchers.IO) { mApiSession.pullRes() }
                val j2 = launch(Dispatchers.IO) { mApiSession.postReq() }
                jobs.add(j1)
                jobs.add(j2)
                joinAll(j1, j2)
                LOGGER.warn("Api C End")
                mApiSession.close()
                // Notifyer.notifyError("开发助手", "已断开设备API链接!")
        }
        isApiConnecting.set(false)
    }

    fun disConnect() {
        runBlocking {
            jobs.forEach { it.cancel("disConnect() Manually!") }
            jobs.clear()

            mApiSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "@-@"))
            mApiClient?.close()

            mLogClient?.close()
            mLogSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "@-@"))

            isLogConnecting.set(false)
            isApiConnecting.set(false)
            LOGGER.warn("disConnect()")
        }
    }

    fun reConnect(ip: String) {
        runBlocking {
            setDeviceIp(ip)
            disConnect()
        }
    }

    fun setDeviceIp(ip: String) {
        deviceIp.set(ip)
    }

    fun getDeviceIp(): String {
        return deviceIp.get()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun startConnectLogJob() {
        val handler = CoroutineExceptionHandler { _, e ->
            LOGGER.warn("==========CoroutineExceptionHandler==========", e)
        }

        mLogClient = HttpClient(Java) {
            install(WebSockets)
            install(HttpTimeout) {
                requestTimeoutMillis = 3600_000
                connectTimeoutMillis = 3600_000
                socketTimeoutMillis = 3600_000
            }
            install(Logging)
        }

        runBlocking {
            launch(handler) {
                LOGGER.warn("LOG C START >>>")
                mLogClient!!.webSocket(
                    method = HttpMethod.Get,
                    host = deviceIp.get(),
                    port = 61140,
                    path = "/log"
                ) {
                    isLogConnecting.set(true)
                    mLogSession = this
                    val logjob = launch(handler) {
                        for (frame in incoming) {
                            // LOGGER.warn("receive log frame: ${frame.frameType} ${frame.fin}")
                            when (frame) {
                                is Frame.Text -> {
                                    val logText = frame.readText()
                                    logQueue.offer(logText)
                                    LOGGER.warn("(((( $logText")
                                }
                                else -> {
                                }
                            }
                        }
                    }
                    jobs.add(logjob)
                    logjob.join()
                }
            }
        }
        isLogConnecting.set(false)
        LOGGER.warn("LOG C END")
        // Notifyer.notifyError("开发助手", "已断开设备日志链接!")
    }

    fun nextLog(): String? {
        return logQueue.poll()
    }

    fun logHasNext(): Boolean {
        return !logQueue.isEmpty()
    }
}