package impl

import chen.yyds.py.impl.ZipUtility
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset

fun byteArrayToInt(b: ByteArray): Int {
    return b[3].toInt() and 0xFF or (
            b[2].toInt() and 0xFF shl 8) or (
            b[1].toInt() and 0xFF shl 16) or (
            b[0].toInt() and 0xFF shl 24)
}

fun intToByteArray(a: Int): ByteArray {
    return byteArrayOf(
            (a shr 24 and 0xFF).toByte(),
            (a shr 16 and 0xFF).toByte(),
            (a shr 8 and 0xFF).toByte(),
            (a and 0xFF).toByte()
    )
}

object CmdCode {
    const val CODE_RUN:Byte = 1
    const val CODE_STOP:Byte = 2
    const val CODE_RECV_FILE:Byte = 3
    const val CODE_RECV_ZIP_FILE:Byte = 33
    const val CODE_CATCH_DEBUG_LOG:Byte = 5
    const val CODE_SCREENSHOT:Byte = 6
    const val CODE_UIA_DUMP:Byte = 10
    const val CODE_CODE_SNIPPET:Byte = 22
    const val CODE_FOREGROUND:Byte = 12
}

fun OutputStream.sendString(content:String) {
    val temp = content.toByteArray(Charset.forName("utf-8"))
    this.write(intToByteArray(temp.size))
    this.write(temp)
    this.flush()
}

fun OutputStream.sendFile(file:File) {
    val temp = file.readBytes()
    this.write(intToByteArray(temp.size))
    this.write(temp)
    this.flush()
}

fun InputStream.readInt():Int {
    var hasRead = 0
    val cache = ByteArray(4)
    while (hasRead != 4) {
        hasRead += this.read(cache, hasRead, 4 - hasRead)
    }
    return byteArrayToInt(cache)
}

fun InputStream.readBytesFixLength():ByteArray {
    val length = readInt()
    var hasRead = 0
    if (length == 0) {
        return ByteArray(0)
    }
    val cache = ByteArray(length)
    while (hasRead != length) {
        hasRead += this.read(cache, hasRead, length - hasRead)
    }
    return cache
}

fun InputStream.readString(length:Int):String {
    var hasRead = 0
    if (length == 0) {
        return ""
    }
    val cache = ByteArray(length)
    while (hasRead != length) {
        hasRead += this.read(cache, hasRead, length - hasRead)
    }
    return String(cache)
}

fun InputStream.readStringFixLength():String {
    return readString(readInt())
}

object EngineConnector {
    private val LOGGER: Logger = Logger.getInstance(EngineConnector::class.java)

    private var deviceIp = ""

    public fun setDeviceIp(ip:String) {
        deviceIp = ip;
    }

    public fun checkClientSocketOk():Boolean {
        return try {
            return Socket(deviceIp, 1140).isConnected
        } catch (ignore:Throwable) {
            return false;
        }
    }

    private fun getClientConSocket():Socket {
        val socket = Socket(deviceIp, 1140)
        if (!socket.isConnected || socket.isOutputShutdown) {
            throw RuntimeException("连接错误!")
        }
        return socket
    }

    public fun getClientLogSocket():Socket {
        val socket = Socket(deviceIp, 1142)

        if (!socket.isConnected || socket.isOutputShutdown) {
            throw RuntimeException("连接错误!")
        }
        return socket
    }

    public fun notifyStartProject(projectName:String) {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(1)
        socketSendStream.write(data)
        Thread.sleep(300)
        socketSendStream.write(projectName.toByteArray())
        socket.close()
        socketSendStream.close()
    }

    public fun notifyStopProject() {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_STOP)
        socketSendStream.write(data)
        socket.close()
        socketSendStream.close()
    }

    public fun notifyCrawlLogcat() {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_CATCH_DEBUG_LOG)
        socketSendStream.write(data)
        socket.close()
        socketSendStream.close()
    }

    /**
     * 获取手机截图 保存在电脑一个固定的位置 再返回路径
     */
    public fun getScreenShot():String? {
        val socket = getClientConSocket()

        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_SCREENSHOT)
        socketSendStream.write(data)
        val socketRecvStream = socket.getInputStream()
        val screenShot = socketRecvStream.readBytesFixLength()
        if (screenShot.isEmpty()) {
            return null
        }
        LOGGER.warn("getScreenShot Ok size=" + screenShot.size)
        val folder = System.getProperty("user.home") + "/Desktop/Yyds.Auto"
        with(File(folder)) {
            if (!exists()) mkdirs()
        }
        val screenFile = File(folder, "截图.jpg")
        screenFile.writeBytes(screenShot)
        return screenFile.absolutePath
    }

    public fun getUiaDump():String? {
        val socket = getClientConSocket()

        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_UIA_DUMP)
        socketSendStream.write(data)
        val socketRecvStream = socket.getInputStream()
        val dumpData = socketRecvStream.readBytesFixLength()
        if (dumpData.isEmpty()) {
            return null
        }
        val folder = System.getProperty("user.home") + "/Desktop/Yyds.Py"
        with(File(folder)) {
            if (!exists()) mkdirs()
        }
        val hierarchyFile = File(folder, "控件信息.xml")
        hierarchyFile.writeBytes(dumpData)
        return hierarchyFile.absolutePath
    }

    public fun getForeground():String {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_FOREGROUND)
        socketSendStream.write(data)
        val socketRecvStream = socket.getInputStream()
        return socketRecvStream.readStringFixLength()
    }

    public fun runCodeSnippet(code:String) {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        val data = byteArrayOf(CmdCode.CODE_CODE_SNIPPET)
        socketSendStream.write(data)
        socketSendStream.sendString(code)
        socketSendStream.flush()
    }

    public fun sendEntireProject(tempZip:String, projectName: String, files: Array<File>) {
        val socket = getClientConSocket()
        val socketSendStream = socket.getOutputStream()
        try {
            ZipUtility.zip(files.toList(), tempZip)
            LOGGER.warn("文件压缩完毕: $tempZip")
            // 插件开发-解压测试
            // ZipUtility.unzip(tempZip, "C:/testPyUnZip")
        } catch (e:Exception) {
            LOGGER.error(e)
        }
        socketSendStream.write(byteArrayOf(CmdCode.CODE_RECV_ZIP_FILE))
        socketSendStream.sendString(projectName)
        socketSendStream.sendFile(File(tempZip))
        // 删除临时压缩文件
        File(tempZip).delete()
    }
}