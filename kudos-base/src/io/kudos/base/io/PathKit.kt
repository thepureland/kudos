package io.kudos.base.io

import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.reflect.KClass

/**
 * 路径工具类
 *
 * @author K
 * @since 1.0.0
 */
object PathKit {

    /**
     * 获取指定类的类路径，包括包名部分的路径。无论指定的类是否在zip/jar中
     *
     * @param clazz kotlin类
     * @return 类路径, 动态生成的类将返回空串
     * @author K
     * @since 1.0.0
     */
    fun getClasspathIncludePackage(clazz: KClass<*>): String {
        var c = clazz.java
        while (c.isMemberClass || c.isAnonymousClass) {
            c = c.enclosingClass // Get the actual enclosing file
        }
        if (c.protectionDomain.codeSource == null) {
            // This is a proxy or other dynamically generated class, and has no physical container,
            // so just return "".
            return ""
        }
        val path = try {
            val className = """${c.simpleName}.class"""
            val thisClass = c.getResource(className)!!.path
            thisClass.replace(className, "")
        } catch (_: Exception) {
            c.protectionDomain.codeSource.location.path
        }
        return URLDecoder.decode(path, "UTF-8")
    }

    /**
     *  获取指定类的类路径，不包括包名部分的路径。无论指定的类是否在zip/jar中
     *
     *  @param clazz kotlin类
     *  @return 类路径, 动态生成的类将返回空串
     *  @author K
     *  @since 1.0.0
     */
    fun getClasspath(clazz: KClass<*>): String {
        var c = clazz.java
        while (c.isMemberClass || c.isAnonymousClass) {
            c = c.enclosingClass // Get the actual enclosing file
        }
        if (c.protectionDomain.codeSource == null) {
            // This is a proxy or other dynamically generated class, and has no physical container,
            // so just return "".
            return ""
        }
        val path = c.protectionDomain.codeSource.location.path
        return URLDecoder.decode(path, "UTF-8")
    }

    /**
     * 获取资源的路径(支持jar中的资源)
     *
     * @param name 资源名称
     * @return 资源绝对路径
     */
    fun getResourcePath(name: String): String {
        val cl = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader
        val url = cl.getResource(name) ?: error("找不到资源：$name")

        // 能直接映射到文件系统就直接用（最理想）
        if (url.protocol == "file") {
            return File(url.toURI()).path
        }

        // 其它协议（jar/jrt/vfs/bundle 等）一律抽取到临时文件，保证可用
        return extractToTempDir(url, name).toAbsolutePath().toString()
    }

    /**
     * 得到相对路径
     *
     * @param baseDir 基础路径
     * @param file 待操作路径
     * @return 相对路径
     * @author K
     * @since 1.0.0
     */
    fun getRelativePath(baseDir: File, file: File): String {
        if (baseDir == file) {
            return ""
        }
        val templateFile = if (baseDir.parentFile == null) {
            file.absolutePath.substring(baseDir.absolutePath.length)
        } else {
            file.absolutePath.substring(baseDir.absolutePath.length + 1)
        }
        return templateFile.replace("\\", "/")
    }

    /**
     * 得到工程根目录，如果是web项目，得到的是如tomcat的bin目录
     *
     * @return 绝对路径
     * @author K
     * @since 1.0.0
     */
    fun getProjectRootPath(): String = System.getProperty("user.dir")

    /**
     * 得到程序运行时的路径
     *
     * @return 绝对路径
     * @author K
     * @since 1.0.0
     */
    fun getRuntimePath(): String = PathKit::class.java.classLoader.getResource(".")!!.path

    /**
     * 获取系统临时目录
     *
     * @return 系统临时目录
     * @author K
     * @since 1.0.0
     */
    fun getTempDirectoryPath(): String = FileUtils.getTempDirectoryPath()

    /**
     * 获取系统临时目录
     *
     * @return 系统临时目录文件对象
     * @author K
     * @since 1.0.0
     */
    fun getTempDirectory(): File = FileUtils.getTempDirectory()

    /**
     * 获取系统用户根目录
     *
     * @return 系统用户根目录
     * @author K
     * @since 1.0.0
     */
    fun getUserDirectoryPath(): String = FileUtils.getUserDirectoryPath()

    /**
     * 获取系统用户根目录
     *
     * @return 系统用户根目录文件对象
     * @author K
     * @since 1.0.0
     */
    fun getUserDirectory(): File = FileUtils.getUserDirectory()

    private fun extractToTempDir(url: URL, name: String): Path {
        // 关键：用“专用临时目录”，避免把系统 temp 根目录整个复制/扫描时踩权限坑
        val dir = Files.createTempDirectory("resource-").toFile().apply { deleteOnExit() }

        val fileName = name.substringAfterLast('/').ifBlank { "resource.bin" }
        val target = File(dir, fileName).apply { deleteOnExit() }.toPath()

        url.openStream().use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return target
    }

}