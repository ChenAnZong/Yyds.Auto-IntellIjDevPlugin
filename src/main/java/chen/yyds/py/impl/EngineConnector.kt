package impl

import chen.yyds.py.impl.RpcDataModel
import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.*
import io.ktor.websocket.*
import io.ktor.websocket.serialization.*
import io.netty.util.Timeout
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@OptIn(ExperimentalSerializationApi::class)
abstract class EngineConnector {
    private val LOGGER = Logger.getInstance(
        EngineConnector::class.java
    )

    private val RPC_TIMEOUT = 5_000L
    private val RPC_PORT = 1140
    private val logQueue: LinkedBlockingQueue<String> = LinkedBlockingQueue()

    @Volatile
    private var deviceIp = "192.168.31.125"

    private var mApiClient: HttpClient? = null
    private var mApiSession: DefaultClientWebSocketSession? = null

    private var mLogClient: HttpClient? = null
    private var mLogSession: DefaultClientWebSocketSession? = null
    private val isConnecting: AtomicBoolean = AtomicBoolean(false)

    private val reqQueue: LinkedBlockingQueue<RpcDataModel> = LinkedBlockingQueue()

    private val resQueue: ConcurrentSkipListMap<String, RpcDataModel> = ConcurrentSkipListMap<String, RpcDataModel>()

    suspend fun DefaultClientWebSocketSession.pullRes() {
        try {
            while (true) {
                val response = receiveDeserialized<RpcDataModel>()
                resQueue[response.uuid] = response
                delay(200)
                // LOGGER.warn("Receive: ${frame.frameType} fin:${frame.fin} data size: ${frame.data.size}")
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
        ensureConnect()
        reqQueue.add(request)
        val start = System.currentTimeMillis()
        while (!resQueue.containsKey(request.uuid)) {
            if (System.currentTimeMillis() - start > RPC_TIMEOUT) {
                return null
            }
        }
        return resQueue.remove(request.uuid)
    }

    fun engineWaitApiCall(request: RpcDataModel): RpcDataModel {
        ensureConnect()
        reqQueue.add(request)
        while (!resQueue.containsKey(request.uuid)) {
            Thread.sleep(1000)
        }
        return resQueue.remove(request.uuid)!!
    }

    // 首次初始化 - 连接
    private fun ensureConnect() {
        if (mApiClient == null || !isConnecting.get()) {
            thread {
                startConnectApiJob()
            }
            thread {
                startConnectLogJob()
            }
        }
    }

    private fun startConnectApiJob() {
//        val handler = CoroutineExceptionHandler { _, e ->
//            LOGGER.error("==========CoroutineExceptionHandler==========", e)
//        }

        mApiClient = HttpClient {
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
                isConnecting.set(true)
                LOGGER.warn("S===")
                val mApiSession = mApiClient!!.webSocketSession(
                    method = HttpMethod.Get,
                    host = deviceIp,
                    port = RPC_PORT,
                    path = "/api"
                )
                val j1 = launch(Dispatchers.IO) { mApiSession.pullRes() }
                val j2 = launch(Dispatchers.IO) { mApiSession.postReq() }
                j1.join()
                j2.join()
                LOGGER.warn("E===")
                mApiSession.close()
                LOGGER.warn("Disconnect from $deviceIp")
                isConnecting.set(false)
        }
    }

    fun reConnect(ip: String) {
        runBlocking {
            deviceIp = ip
            mApiSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "@-@"))
            mApiClient?.close()
            mApiClient = null
            mApiSession = null

            mLogClient?.close()
            mLogSession?.close(CloseReason(CloseReason.Codes.GOING_AWAY, "@-@"))
            mLogClient = null
            mLogSession = null
            ensureConnect()
        }
    }

    fun disConnectAll() {
        runBlocking {
            mApiSession?.close()
            mApiClient?.close()
            mApiClient = null
            mApiClient = null
        }
    }

    fun setDeviceIp(ip: String) {
        deviceIp = ip
    }

    fun getDeviceIp(): String {
        return deviceIp
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun startConnectLogJob() {
        val handler = CoroutineExceptionHandler { _, e ->
            LOGGER.warn("==========CoroutineExceptionHandler==========", e)
        }

        mLogClient = HttpClient {
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
                while (true) {
                    LOGGER.warn("SL===")
                    mLogClient!!.webSocket(
                        method = HttpMethod.Get,
                        host = deviceIp,
                        port = 1140,
                        path = "/log"
                    ) {
                        mLogSession = this
                        launch(handler) {
                            for (frame in incoming) {
                                LOGGER.warn("receive log frame: ${frame.frameType} ${frame.fin}")
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
                        }.join()
                        LOGGER.warn("EL===")
                        mLogClient!!.close()
                    }
                }
            }
        }
        LOGGER.warn("startConnectLogJob gone")
    }

    fun nextLog(): String? {
        return logQueue.poll()
    }

    fun logHasNext(): Boolean {
        return !logQueue.isEmpty()
    }
}