/**
 * Copyright 2010-2013 Axel Fontaine and the many contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kudos.base.io.scanner.classpath

import io.kudos.base.io.FileKit
import io.kudos.base.logger.LogFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Paths
import java.util.TreeSet

/**
 * ClassPathLocationScanner for the file system.
 */
class FileSystemClassPathLocationScanner : IClassPathLocationScanner {

    private val logger = LogFactory.getLog(this::class)


    @Throws(IOException::class)
    override fun findResourceNames(location: String, locationUrl: URL): Set<String> {
        val filePath = FileKit.toFile(locationUrl)?.path
        if (filePath == null) {
            return TreeSet()
        }
        val folder = File(filePath)
        if (!folder.isDirectory) {
            logger.debug("Skipping path as it is not a directory: $filePath")
            return TreeSet()
        }
        var classPathRootOnDisk = filePath.substring(0, filePath.length - location.length)
        if (!classPathRootOnDisk.endsWith("/")) {
            classPathRootOnDisk = "$classPathRootOnDisk/"
        }
        logger.debug("Scanning starting at classpath root in filesystem: $classPathRootOnDisk")
        return findResourceNamesFromFileSystem(classPathRootOnDisk, location, folder)
    }

    /**
     * Finds all the resource names contained in this file system folder.
     *
     * @param classPathRootOnDisk The location of the classpath root on disk,
     * with a trailing slash.
     * @param scanRootLocation The root location of the scan on the classpath,
     * without leading or trailing slashes.
     * @param folder The folder to look for resources under on disk.
     * @return The resource names;
     * @throws IOException when the folder could not be read.
     */
    /* private -> for testing */
    @Throws(IOException::class)
    fun findResourceNamesFromFileSystem(classPathRootOnDisk: String?, scanRootLocation: String, folder: File): Set<String> {
        logger.debug("Scanning for resources in path: " + folder.path + " (" + scanRootLocation + ")")
        val resourceNames: MutableSet<String> = TreeSet()
        val files = folder.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.canRead()) {
                    if (file.isDirectory) {
                        resourceNames.addAll(findResourceNamesFromFileSystem(classPathRootOnDisk, scanRootLocation, file))
                    } else {
                        resourceNames.add(toResourceNameOnClasspath(classPathRootOnDisk, file))
                    }
                }
            }
        }
        return resourceNames
    }

    /**
     * Converts this file into a resource name on the classpath.
     *
     * @param classPathRootOnDisk The location of the classpath root on disk,
     * with a trailing slash.
     * @param file The file.
     * @return The resource name on the classpath.
     * @throws IOException when the file could not be read.
     */
    @Throws(IOException::class)
    private fun toResourceNameOnClasspath(classPathRootOnDisk: String?, file: File): String {
        if (classPathRootOnDisk.isNullOrEmpty()) {
            return ""
        }
        // 使用 Path 相对路径：在 Windows 上 File.path 为反斜杠，而 URL.file 为正斜杠，
        // 旧实现用 substring 对齐两者会失败，导致资源名始终对不上。
        val root = Paths.get(classPathRootOnDisk.trimEnd('/', '\\')).normalize().toAbsolutePath()
        val absoluteFile = file.toPath().normalize().toAbsolutePath()
        return root.relativize(absoluteFile).toString().replace('\\', '/')
    }

}