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
import io.kudos.base.io.scanner.support.Resource
import io.kudos.base.logger.LogFactory
import java.io.IOException
import java.lang.reflect.Modifier
import java.net.URL
import java.util.*
import kotlin.reflect.KClass

/**
 * 类路径扫描器
 * 
 * 用于扫描类路径（classpath）中的资源和类文件，支持文件系统和JAR包两种协议。
 * 
 * 核心功能：
 * 1. 资源扫描：根据路径、前缀和后缀扫描类路径中的资源文件
 * 2. 类扫描：扫描实现指定接口的类，自动过滤抽象类
 * 3. 协议支持：支持file协议（文件系统）和jar/zip/wsjar协议（JAR包）
 * 4. 路径解析：自动解析类路径URL，支持多个类路径位置
 * 
 * 扫描机制：
 * - 使用ClassLoader.getResources获取类路径下的所有URL
 * - 根据URL协议选择对应的扫描器（FileSystemClassPathLocationScanner或JarFileClassPathLocationScanner）
 * - 递归扫描子目录和子包
 * - 根据前缀和后缀过滤资源名称
 * 
 * 资源过滤：
 * - 支持前缀匹配：文件名以指定前缀开头
 * - 支持后缀匹配：文件名以指定后缀结尾
 * - 支持同时匹配前缀和后缀
 * - 如果前缀和后缀都为空，返回所有资源
 * 
 * 类扫描：
 * - 自动过滤抽象类，只返回可实例化的类
 * - 检查类是否实现指定接口
 * - 支持扫描子包中的类
 * - 返回Class数组，便于后续实例化
 * 
 * 使用场景：
 * - 国际化资源文件的扫描
 * - 插件类的自动发现
 * - 配置文件的位置查找
 * - 接口实现类的自动注册
 * 
 * 注意事项：
 * - 使用当前线程的ClassLoader进行扫描
 * - 不支持vfs、bundle等特殊协议（代码中有注释但未实现）
 * - 扫描结果按资源名称排序（使用TreeSet）
 * - 扫描失败不会抛出异常，只会记录日志
 * 
 * @since 1.0.0
 */
object ClassPathScanner {
    
    private val logger = LogFactory.getLog(this)

    /**
     * Scans the classpath for resources under the specified location, starting
     * with the specified prefix and ending with the specified suffix.
     *
     * @param path The path in the classpath to start searching. Subdirectories
     * are also searched.
     * @param prefix The prefix of the resource names to match.
     * @param suffix The suffix of the resource names to match.
     * @return The resources that were found.
     * @throws IOException when the location could not be scanned.
     * @since 1.0.0
     */
	fun scanForResources(path: String, prefix: String, suffix: String): Array<Resource> {
        val resources: MutableSet<Resource> = TreeSet()
        try {
            val resourceNames = findResourceNames(path, prefix, suffix)
            for (resourceName in resourceNames) {
                resources.add(ClassPathResource(resourceName))
                logger.debug("Found resource for $path (Prefix: $prefix, Suffix: $suffix): $resourceName")
            }
        } catch (e: Exception) {
            logger.error(e)
        }
        return resources.toTypedArray()
    }

    /**
     * Scans the classpath for concrete classes under the specified package
     * implementing this interface. Non-instantiable abstract classes are
     * filtered out.
     *
     * @param location The location (package) in the classpath to start
     * scanning. Subpackages are also scanned.
     * @param implementedInterface The interface the matching classes should
     * implement.
     * @return The non-abstract classes that were found.
     * @throws Exception when the location could not be scanned.
     * @since 1.0.0
     */
    fun scanForClasses(location: String, implementedInterface: KClass<*>): Array<Class<*>> {
        logger.debug("Scanning for classes at '" + location + "' (Implementing: '" + implementedInterface.qualifiedName + "')")
        val classes: MutableList<Class<*>> = ArrayList()
        try {
            val resourceNames = findResourceNames(location, "", ".class")
            for (resourceName in resourceNames) {
                val className = toClassName(resourceName)
                val clazz = classLoader.loadClass(className)
                if (Modifier.isAbstract(clazz.modifiers)) {
                    logger.debug("Skipping abstract class: $className")
                    continue
                }
                if (!implementedInterface.java.isAssignableFrom(clazz)) {
                    continue
                }
                Class.forName(className)
//                ClassKit.instantiate(className)
                classes.add(clazz)
                logger.debug("Found class: $className")
            }
        } catch (e: Exception) {
            logger.error(e)
        }
        return classes.toTypedArray()
    }

    /**
     * Gets the physical location urls for this logical path on the classpath.
     *
     * @param path The path on the classpath.
     * @return The underlying physical URLs.
     * @throws IOException when the lookup fails.
     */
    fun getLocationUrlsForPath(path: String): List<URL> {
        val locationUrls: MutableList<URL> = ArrayList()
        val urls = classLoader.getResources(path)
        /*if (!urls.hasMoreElements()) {
			throw new SystemException("Unable to determine URL for classpath location: " + path + " (ClassLoader: "
					+ getClassLoader() + ")");
		}*/while (urls.hasMoreElements()) {
            locationUrls.add(urls.nextElement())
        }
        return locationUrls
    }

    /**
     * Converts this resource name to a fully qualified class name.
     *
     * @param resourceName The resource name.
     * @return The class name.
     */
    private fun toClassName(resourceName: String): String {
        val nameWithDots = resourceName.replace("/", ".")
        return nameWithDots.substring(0, nameWithDots.length - ".class".length)
    }

    /**
     * Finds the resources names present at this location and below on the
     * classpath starting with this prefix and ending with this suffix.
     *
     * @param path The path on the classpath to scan.
     * @param prefix The filename prefix to match.
     * @param suffix The filename suffix to match.
     * @return The resource names.
     * @throws IOException when scanning this location failed.
     */
    private fun findResourceNames(path: String, prefix: String, suffix: String): Set<String> {
        val resourceNames: MutableSet<String> = TreeSet()
        val locationsUrls = getLocationUrlsForPath(path)
        for (locationUrl in locationsUrls) {
            logger.debug("Scanning URL: " + locationUrl.toExternalForm())
            val protocol = locationUrl.protocol
            val classPathLocationScanner = createLocationScanner(protocol)
            if (classPathLocationScanner == null) {
                val scanRoot = FileKit.toFile(locationUrl)?.path
                logger.warn("Unable to scan location: $scanRoot (unsupported protocol: $protocol)")
            } else {
                resourceNames.addAll(classPathLocationScanner.findResourceNames(path, locationUrl))
            }
        }
        return filterResourceNames(resourceNames, prefix, suffix)
    }

    /**
     * Creates an appropriate location scanner for this url protocol.
     *
     * @param protocol The protocol of the location url to scan.
     * @return The location scanner or `null` if it could not be created.
     */
    private fun createLocationScanner(protocol: String): IClassPathLocationScanner? {
        if ("file" == protocol) {
            return FileSystemClassPathLocationScanner()
        }
        return if ("jar" == protocol || "zip" == protocol || "wsjar" == protocol) {
            JarFileClassPathLocationScanner()
        } else null

        // if (FeatureDetector.isJBossVFSv3Available() &&
        // "vfs".equals(protocol)) {
        // return new JBossVFSv3ClassPathLocationScanner();
        // }
        //
        // if (FeatureDetector.isOsgiFrameworkAvailable() && (
        // "bundle".equals(protocol) // Felix
        // || "bundleresource".equals(protocol)) //Equinox
        // ) {
        // return new OsgiClassPathLocationScanner();
        // }
    }

    /**
     * @return The classloader to use to scan the classpath.
     */
    private val classLoader: ClassLoader
        get() = Thread.currentThread().contextClassLoader

    /**
     * Filters this list of resource names to only include the ones whose
     * filename matches this prefix and this suffix.
     *
     * @param resourceNames The names to filter.
     * @param prefix The prefix to match.
     * @param suffix The suffix to match.
     * @return The filtered names set.
     */
    private fun filterResourceNames(resourceNames: Set<String>, prefix: String, suffix: String): Set<String> {
        val filteredResourceNames: MutableSet<String> = TreeSet()
        for (resourceName in resourceNames) {
            if (prefix.isBlank() && suffix.isBlank()) {
                filteredResourceNames.add(resourceName)
            } else {
                val fileName = resourceName.substring(resourceName.lastIndexOf("/") + 1)
                if (prefix.isBlank() && fileName.endsWith(suffix)) {
                    filteredResourceNames.add(resourceName)
                } else if (suffix.isBlank() && fileName.startsWith(prefix)) {
                    filteredResourceNames.add(resourceName)
                } else if (fileName.startsWith(prefix) && fileName.endsWith(suffix)
                        && fileName.length >= (prefix + suffix).length) {
                    filteredResourceNames.add(resourceName)
                }

//				if (fileName.startsWith(prefix) && fileName.endsWith(suffix)
//						&& (fileName.length() >= (prefix + suffix).length())) {
//					filteredResourceNames.add(resourceName);
//				} else {
////				LOG.debug("Filtering out resource: " + resourceName + " (filename: " + fileName + ")");
//				}
            }
        }
        return filteredResourceNames
    }
}