package chen.yyds.py.impl

import kotlinx.serialization.Serializable
import java.io.File
import java.util.*
import kotlin.collections.HashMap


object RPC_METHOD {
    const val ENGINE_PROJECT_START = "engine.project.start"
    const val ENGINE_CODE_RUN = "engine.run.code"
    const val ENGINE_ABORT = "engine.project.abort"
    const val ENGINE_GET_RUNNING_STATUS = "engine.running.status"
    const val ENGINE_CLICK = "engine.click"
    const val FILE_RECEIVE = "file.receive"
    const val File_ZIP_RECEIVE = "file.zip.receive"
    const val AUTO_API_SCREEN_SHOT = "api.screenshot"
    const val AUTO_API_UI_DUMP = "api.ui.dump"
    const val AUTO_API_SHELL = "api.shell"
    const val AUTO_API_FOREGROUND = "foreground"
    const val DISCONNECT = "bye"
    const val HEARTBEAT = "beat"
}


object RPC_MAP_KEY {
    const val ENGINE_START_PROJECT_NAME = "start.project.name"
    const val ENGINE_CURRENT_PROJECT_NAME = "current.project.name"
    const val ENGINE_IS_PROJECT_RUNNING = "is.project.running"
    const val ENGINE_RUN_CODE = "run.code.snippet"
    const val ENGINE_RUN_SHELL = "run.shell"
    const val ENGINE_CLICK_X = "x"
    const val ENGINE_CLICK_Y = "y"
    const val SEND_ZIP_NAME = "send.zip.name"
    const val FILE_NAME = "file.name"
    const val FILE_PATH = "path"
}


@Serializable
data class RpcDataModel(
    val uuid:String = UUID.randomUUID().toString(),
    val method:String,
    val mapData: HashMap<String, String> = hashMapOf(),
    var binData: ByteArray = byteArrayOf()
) {
    fun setSuccess(isSuccess:Boolean) {
        mapData["is_success"] = isSuccess.toString()
    }

    fun isSuccess():Boolean {
        return mapData["is_success"] == "true"
    }

    fun setDesc(desc:String) {
        mapData["desc"] = desc
    }

    fun getDesc():String? {
        return mapData["desc"]
    }

    fun setResult(res:String) {
        mapData["result"] = res
    }

    fun getResult():String? {
        return mapData["result"]
    }

    fun setBinDataFile(file:File) {
        binData = file.readBytes()
    }

    fun writeOutBinData(path:String) {
        File(path).writeBytes(binData)
    }

    fun addBoolean(key:String, bol:Boolean) {
        mapData[key] = bol.toString()
    }

    fun addString(key:String, value:String) {
        mapData[key] = value
    }

    fun getString(key:String):String? {
        return mapData[key]
    }

    fun getBoolean(key:String):Boolean? {
        mapData[key] ?: return null
        return mapData[key].equals("true", true)
    }

    fun writeOutBinData(file:File) {
        file.writeBytes(binData)
    }

    override fun toString(): String {
        return "$uuid=>$method map=${mapData} bin.size=${binData.size}"
    }

    companion object {
        fun initResponseFromRequest(rpc:RpcDataModel):RpcDataModel {
            return RpcDataModel(
                rpc.uuid, rpc.method, hashMapOf<String,String>()
            )
        }
    }
}

