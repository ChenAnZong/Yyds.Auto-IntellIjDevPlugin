package chen.yyds.py.impl

import chen.yyds.py.Notifyer
import com.intellij.openapi.diagnostic.Logger
import impl.EngineConnector
import java.io.File

object EngineImplement : EngineConnector() {
    private val LOGGER = Logger.getInstance(
        EngineImplement::class.java
    )

    fun notifyStartProject(projectName:String) {
        val st = System.currentTimeMillis()
        val res = engineWaitApiCall(RpcDataModel(
            method = RPC_METHOD.ENGINE_PROJECT_START,
            mapData = hashMapOf(RPC_MAP_KEY.ENGINE_START_PROJECT_NAME to projectName)))
        if (res.isSuccess()) {
            Notifyer.notifyInfo("工程 $projectName  运行完毕 耗时${System.currentTimeMillis() - st} Ms")
        }
    }

    fun notifyStopProject() {
        engineTimeoutApiCallOrNull(RpcDataModel(method =RPC_METHOD.ENGINE_ABORT))
    }
    /**
     * 获取手机截图 保存在电脑一个固定的位置 再返回路径
     */
    fun getScreenShot():String? {
        val folder = System.getProperty("user.home") + "/Desktop/Yyds.Auto"
        with(File(folder)) {
            if (!exists()) mkdirs()
        }
        val screenFile = File(folder, "截图.jpg")

        val request = RpcDataModel(method = RPC_METHOD.AUTO_API_SCREEN_SHOT)
        LOGGER.warn("echo: $request")
        val response = engineTimeoutApiCallOrNull(request) ?: return null
        if (response.isSuccess()) {
            response.writeOutBinData(screenFile.absolutePath)
        } else {
            return null
        }
        return screenFile.absolutePath
    }

    fun getUiaDump():String? {
        val folder = System.getProperty("user.home") + "/Desktop/Yyds.Py"
        with(File(folder)) {
            if (!exists()) mkdirs()
        }
        val hierarchyFile = File(folder, "控件信息.xml")
        val response = engineTimeoutApiCallOrNull(RpcDataModel(method =RPC_METHOD.AUTO_API_UI_DUMP))
        if (response != null && response.isSuccess()) {
            hierarchyFile.writeBytes(response.binData)
        } else {
            return null
        }
        return hierarchyFile.absolutePath
    }

    fun getForeground():String {
        val response = engineTimeoutApiCallOrNull(RpcDataModel(method =RPC_METHOD.AUTO_API_FOREGROUND))
        if (response != null && response.isSuccess()) {
            return response.getResult()!!
        }
        return ""
    }

    fun runCodeSnippet(code:String) {
        val st = System.currentTimeMillis()
        // 自动去掉前面空格
        val response = engineTimeoutApiCallOrNull(RpcDataModel(
            method = RPC_METHOD.ENGINE_CODE_RUN,
            mapData = hashMapOf(RPC_MAP_KEY.ENGINE_RUN_CODE to code.trimIndent())
        ))
        if (response != null && response.isSuccess()) {
            Notifyer.notifyInfo("代码运行完毕 耗时 ${System.currentTimeMillis() - st} Ms")
        } else {
            Notifyer.notifyError("代码运行失败!")
        }
    }

    fun click(x:Int, y:Int):Boolean {
        val response = engineTimeoutApiCallOrNull(RpcDataModel(
            method = RPC_METHOD.ENGINE_CLICK,
            mapData = hashMapOf(RPC_MAP_KEY.ENGINE_CLICK_X to x.toString(),
                                RPC_MAP_KEY.ENGINE_CLICK_Y to y.toString())
        ))
        return response?.isSuccess() ?: false
    }

    fun sendEntireProject(tempZip:String, projectName: String, files: Array<File>) {
        try {
            ZipUtility.zip(files.toList(), tempZip)
            LOGGER.warn("文件压缩完毕: $tempZip")
            // 插件开发-解压测试
            // ZipUtility.unzip(tempZip, "C:/testPyUnZip")
        } catch (e:Exception) {
            LOGGER.error(e)
        }
        val st = System.currentTimeMillis()
        val response = engineWaitApiCall(RpcDataModel(
            method = RPC_METHOD.File_ZIP_RECEIVE,
            mapData = hashMapOf(RPC_MAP_KEY.SEND_ZIP_NAME to projectName),
            binData = File(tempZip).readBytes()
        ))
        val lost = System.currentTimeMillis() - st
        Notifyer.notifyInfo("发送工程到设备完成:$projectName 工程大小: ${File(tempZip).length()/1024}Kb 耗时: ${lost}Ms")
        // 删除临时压缩文件
        File(tempZip).delete()
    }
}