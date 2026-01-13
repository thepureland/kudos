package io.kudos.context.retry

import io.kudos.base.data.json.JsonKit
import io.kudos.base.lang.GenericKit
import kotlinx.io.IOException
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * 失败数据处理器抽象类
 * 
 * 提供失败数据的持久化和读取功能，子类需要实现具体的处理逻辑。
 * 
 * 核心功能：
 * 1. 数据持久化：将失败数据序列化为JSON并保存到本地文件
 * 2. 数据读取：从文件中读取JSON数据并反序列化为对象
 * 3. 处理委托：将具体的处理逻辑委托给子类实现
 * 
 * 文件命名规则：
 * - 格式：{时间戳}-{UUID}.json
 * - 例如：1704067200000-550e8400-e29b-41d4-a716-446655440000.json
 * - 时间戳用于排序，UUID确保唯一性
 * 
 * 文件存储结构：
 * - 根目录：{filePath()}
 * - 业务目录：{filePath()}/{businessType}
 * - 文件路径：{filePath()}/{businessType}/{时间戳}-{UUID}.json
 * 
 * 注意事项：
 * - 子类需要实现processFailedData方法，定义具体的处理逻辑
 * - 文件读写使用JSON格式，确保数据可读性和可恢复性
 * - 使用泛型确保类型安全
 */
abstract class AbstractFailedDataHandler<T> : IFailedDataHandler<T> {

    /**
     * 持久化失败数据到本地文件
     * 
     * 将失败数据序列化为JSON格式并保存到本地文件系统。
     * 
     * 工作流程：
     * 1. 构建文件路径：{filePath()}/{businessType}
     * 2. 创建目录：如果目录不存在，自动创建
     * 3. 生成文件名：{时间戳}-{UUID}.json
     * 4. 序列化数据：将数据对象序列化为JSON字节数组
     * 5. 写入文件：将字节数组写入文件
     * 6. 返回文件路径：返回文件的绝对路径
     * 
     * 文件命名：
     * - 时间戳：System.currentTimeMillis()，用于排序
     * - UUID：UUID.randomUUID()，确保唯一性
     * - 格式：时间戳-UUID.json
     * 
     * 异常处理：
     * - 如果IO操作失败，会抛出RuntimeException包装IOException
     * - 确保调用方能够感知到持久化失败
     * 
     * @param data 待持久化的失败数据对象
     * @return 保存文件的绝对路径
     * @throws RuntimeException 如果文件操作失败
     */
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

    /**
     * 处理失败数据文件
     * 
     * 从文件中读取失败数据，调用子类的processFailedData方法进行处理。
     * 
     * 工作流程：
     * 1. 读取文件：调用readDataFromFile从文件中读取并反序列化数据
     * 2. 处理数据：调用子类实现的processFailedData方法处理数据
     * 3. 返回结果：返回处理是否成功
     * 
     * 返回值：
     * - true：处理成功，调用方会删除文件
     * - false：处理失败，文件会保留等待下次重试
     * 
     * @param file 失败数据文件
     * @return true表示处理成功，false表示处理失败
     */
    override fun handleFailedData(file: File): Boolean {
        val t = readDataFromFile(file)
        return processFailedData(t)
    }

    /**
     * 处理从文件读取的业务数据
     * 
     * 子类需要实现此方法，定义具体的失败数据处理逻辑。
     * 
     * 实现要求：
     * - 处理成功后返回true，文件会被删除
     * - 处理失败后返回false，文件会保留等待下次重试
     * - 如果处理过程中抛出异常，会被上层捕获并记录日志
     * 
     * @param data 从文件中读取并反序列化的数据对象
     * @return true表示处理成功，false表示处理失败
     */
    protected abstract fun processFailedData(data: T): Boolean

    /**
     * 从文件中读取并转换为T对象
     * 
     * 读取JSON文件内容，反序列化为指定类型的对象。
     * 
     * 工作流程：
     * 1. 读取文件字节：使用Files.readAllBytes读取文件所有字节
     * 2. 获取泛型类型：通过反射获取子类的泛型参数类型
     * 3. 反序列化：使用JsonKit.readValue将字节数组反序列化为对象
     * 4. 类型转换：将反序列化结果转换为泛型类型T
     * 
     * 类型获取：
     * - 使用GenericKit.getSuperClassGenricClass获取泛型类型
     * - 支持运行时获取泛型参数的实际类型
     * 
     * 异常处理：
     * - 如果IO操作失败，会抛出RuntimeException包装IOException
     * - 如果反序列化失败，会抛出相应的序列化异常
     * 
     * @param file 待读取的文件
     * @return 反序列化后的数据对象
     * @throws RuntimeException 如果文件读取或反序列化失败
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
