package io.kudos.context.retry

import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.GenericKit
import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

abstract class AbstractFailedDataHandler<T> : IFailedDataHandler<T> {

    override fun persistFailedData(data: T): String {
        val rootPath = Paths.get(filePath())
        try {
            val dir = rootPath.resolve(businessType)
            if (Files.notExists(dir)) {
                Files.createDirectories(dir)
            }
            val fileName = System.currentTimeMillis().toString() + "-" + UUID.randomUUID() + ".json"
            val file = dir.resolve(fileName)
            val bytes: ByteArray = JsonKit.writeAnyAsBytes(data)
            Files.write(file, bytes)
            return file.toAbsolutePath().toString()
        } catch (e: IOException) {
            throw RuntimeException("Persist failed data error", e)
        }
    }

    override fun handleFailedData(file: File): Boolean {
        val t = readDataFromFile(file)
        return processFailedData(t)
    }

    /**
     * 处理从文件读取的业务
     * @param data
     * @return 是否处理成功，处理成功后文件会被删除
     */
    protected abstract fun processFailedData(data: T): Boolean

    /**
     * 从文件中读取并转换为 T 对象
     */
    protected fun readDataFromFile(file: File): T {
        try {
            val bytes = Files.readAllBytes(file.toPath())
            val dataType = GenericKit.getSuperClassGenricClass(this::class)
            @Suppress("UNCHECKED_CAST")
            return JsonKit.readValue(bytes, dataType) as T
        } catch (e: IOException) {
            throw RuntimeException("Read failed data error", e)
        }
    }

}
