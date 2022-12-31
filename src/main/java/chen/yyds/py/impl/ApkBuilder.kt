package impl

import com.intellij.openapi.diagnostic.Logger

object ApkBuilder {
    private val LOGGER = Logger.getInstance(ApkBuilder::class.java)

    fun test() {
        val testFileContent = this.javaClass.classLoader.getResource("/other/other.xml")?.readText()
        LOGGER.warn("测试读取文件:" + testFileContent)
    }
}