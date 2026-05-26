package io.kudos.base.io

import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.reflect.KClass

/**
 * Path utility class.
 *
 * @author K
 * @since 1.0.0
 */
object PathKit {

    /**
     * Returns the classpath for the given class, including the package portion of the path. Works whether or not
     * the class lives inside a zip/jar.
     *
     * @param clazz the kotlin class
     * @return the classpath; an empty string for dynamically generated classes
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
            val thisClass = requireNotNull(c.getResource(className)) {
                "Unable to locate class resource: ${c.name}/$className"
            }.path
            thisClass.replace(className, "")
        } catch (_: IllegalArgumentException) {
            c.protectionDomain.codeSource.location.path
        } catch (_: SecurityException) {
            c.protectionDomain.codeSource.location.path
        }
        return URLDecoder.decode(path, StandardCharsets.UTF_8)
    }

    /**
     *  Returns the classpath for the given class, excluding the package portion of the path. Works whether or not
     *  the class lives inside a zip/jar.
     *
     *  @param clazz the kotlin class
     *  @return the classpath; an empty string for dynamically generated classes
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
        return URLDecoder.decode(path, StandardCharsets.UTF_8)
    }

    /**
     * Gets the path of a resource (supports resources inside a jar).
     *
     * @param name the resource name
     * @return the absolute resource path
     */
    fun getResourcePath(name: String): String {
        val cl = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader
        val url = cl.getResource(name) ?: error("Resource not found: $name")

        // If it maps directly to the filesystem, use it directly (the ideal case)
        if (url.protocol == "file") {
            return File(url.toURI()).path
        }

        // Other protocols (jar/jrt/vfs/bundle, etc.) are always extracted to a temp file to ensure usability
        return extractToTempDir(url, name).toAbsolutePath().toString()
    }

    /**
     * Returns the relative path.
     *
     * @param baseDir the base path
     * @param file the target path
     * @return the relative path
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
     * Returns the project root directory; for a web project this is something like Tomcat's bin directory.
     *
     * @return the absolute path
     * @author K
     * @since 1.0.0
     */
    fun getProjectRootPath(): String = System.getProperty("user.dir")

    /**
     * Returns the runtime path of the program.
     *
     * @return the absolute path
     * @author K
     * @since 1.0.0
     */
    fun getRuntimePath(): String = requireNotNull(PathKit::class.java.classLoader.getResource(".")) {
        "Unable to get runtime path resource: ."
    }.path

    /**
     * Returns the system temp directory.
     *
     * @return the system temp directory
     * @author K
     * @since 1.0.0
     */
    fun getTempDirectoryPath(): String = FileUtils.getTempDirectoryPath()

    /**
     * Returns the system temp directory.
     *
     * @return the system temp directory as a File
     * @author K
     * @since 1.0.0
     */
    fun getTempDirectory(): File = FileUtils.getTempDirectory()

    /**
     * Returns the system user home directory.
     *
     * @return the system user home directory
     * @author K
     * @since 1.0.0
     */
    fun getUserDirectoryPath(): String = FileUtils.getUserDirectoryPath()

    /**
     * Returns the system user home directory.
     *
     * @return the system user home directory as a File
     * @author K
     * @since 1.0.0
     */
    fun getUserDirectory(): File = FileUtils.getUserDirectory()

    /**
     * Extracts a resource served by a non-`file:` protocol (e.g. `jar:`, `jrt:`) into a newly created temporary
     * directory so callers can obtain a readable local path. Both the temp directory and file are registered with
     * [File.deleteOnExit].
     *
     * A dedicated subdirectory is created (rather than copying directly under the system temp root) to avoid
     * permission/race issues with other tools that scan the system temp root.
     *
     * @param url the resource URL
     * @param name the original resource name (used only to name the temp file; a blank name falls back to `resource.bin`)
     * @return the path of the extracted temporary file
     * @author K
     * @since 1.0.0
     */
    private fun extractToTempDir(url: URL, name: String): Path {
        // Key: use a "dedicated temp directory" to avoid permission pitfalls when copying/scanning the whole system temp root
        val dir = Files.createTempDirectory("resource-").toFile().apply { deleteOnExit() }

        val fileName = name.substringAfterLast('/').ifBlank { "resource.bin" }
        val target = File(dir, fileName).apply { deleteOnExit() }.toPath()

        url.openStream().use { input ->
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
        }
        return target
    }

}