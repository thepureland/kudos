package io.kudos.base.lang

import io.kudos.base.logger.LogFactory
import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.Enumeration
import java.util.jar.JarFile
import kotlin.reflect.KClass

/**
 * Package utility.
 *
 * @author K
 * @since 1.0.0
 */
object PackageKit {

    /** Logger */
    private val LOG = LogFactory.getLog(PackageKit::class)

    /**
     * Get all classes under the specified package name.
     *
     * @param pkg standard package name separated by "."
     * @param recursive whether to iterate recursively
     * @return Set(class)
     * @author K
     * @since 1.0.0
     */
    fun getClassesInPackage(pkg: String, recursive: Boolean): Set<KClass<*>> {
        val action = Action(true)
        find(pkg, recursive, action)
        return action.classes.toSet()
    }

    /**
     * Get all packages matching a regular expression.
     * The leading portion of the package must be explicitly specified.
     *
     * @param pkgPattern the package regular expression
     * @param recursive whether to recursively fetch sub-packages
     * @return Set(package name)
     * @author K
     * @since 1.0.0
     */
    fun getPackages(pkgPattern: String, recursive: Boolean): Set<String> {
        val action = Action(false)
        val packagePrefix = getPackagePrefix(pkgPattern)
        find(packagePrefix, recursive, action)
        val pkgs = action.pkgs
        val regExp = pkgPattern.replace("\\*".toRegex(), ".*")
        return pkgs.filter { it.matches(Regex(regExp)) }.toSet()
    }

    /**
     * Extract the "enumerable fixed prefix" from a wildcard package pattern.
     * For an input like `io.kudos.*.bean`, the prefix is truncated to everything before the first segment containing `*`, yielding `io.kudos`.
     *
     * @param pkgPattern the package match expression containing `*`
     * @return the fixed prefix with the trailing dot removed
     * @author K
     * @since 1.0.0
     */
    private fun getPackagePrefix(pkgPattern: String): String =
        pkgPattern.split(".").takeWhile { !it.contains("*") }.joinToString(".")

    /**
     * Unified entry point for package scanning: obtain all matching resources from the current thread's classloader,
     * then dispatch to "file directory scan" or "JAR scan" based on the protocol. Results are written into [action].
     *
     * @param packagePrefix the fixed package prefix (dot-separated); must not contain wildcards
     * @param recursive whether to scan subdirectories recursively
     * @param action the scan result collector
     * @author K
     * @since 1.0.0
     */
    private fun find(packagePrefix: String, recursive: Boolean, action: Action) {
        // Get the package name and substitute
        val packageDirName = packagePrefix.replace('.', '/')
        // Define an enumeration of URLs and iterate to process the contents of this directory
        val dirs: Enumeration<URL>
        try {
            dirs = Thread.currentThread().contextClassLoader.getResources(packageDirName)
            // Iterate
            while (dirs.hasMoreElements()) {
                // Get the next element
                val url = dirs.nextElement()
                // Get the protocol name
                val protocol = url.protocol
                // If it is stored as a file on the server
                if ("file" == protocol) {
                    // Get the physical path of the package
                    val filePath = URLDecoder.decode(url.file, "UTF-8")
                    // Scan all files under the package as files and add them to the collection
                    findAndAddClassesInPackageByFile(packagePrefix, filePath, recursive, action)
                } else if ("jar" == protocol) {
                    // If it is a JAR file
                    // Define a JarFile
                    findAndAddClassesInPackageByJar(packagePrefix, packageDirName, url, recursive, action)
                }
            }
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    /**
     * Scan the specified package prefix inside a JAR and collect matching classes or sub-packages into [action].
     * Switch between "collect classes" and "collect packages" behavior via [Action.isRetrieveClass];
     * when recursive=false, only the top-level package/classes are collected.
     *
     * @param packageName the current package name maintained during input and recursion
     * @param packageDirName the directory form of the package prefix (dots converted to slashes)
     * @param url the resource URL of the JAR
     * @param recursive whether to scan recursively
     * @param action the scan result collector
     * @author K
     * @since 1.0.0
     */
    private fun findAndAddClassesInPackageByJar(
        packageName: String, packageDirName: String, url: URL, recursive: Boolean, action: Action
    ) {
        var pkgName = packageName
        val jar: JarFile
        try {
            // Get the jar
            jar = (url.openConnection() as JarURLConnection).jarFile
            // Get an enumeration of entries from this jar
            val entries = jar.entries()
            // Iterate similarly
            while (entries.hasMoreElements()) {
                // Get an entry from the jar; may be a directory or other file inside the jar such as META-INF
                val entry = entries.nextElement()
                var name = entry.name
                // If it starts with /
                if (name[0] == '/') {
                    // Take the remainder
                    name = name.substring(1)
                }
                // If the prefix matches the defined package name
                if (name.startsWith(packageDirName)) {
                    val idx = name.lastIndexOf('/')
                    // If it ends with "/", it is a package
                    if (idx != -1) {
                        // Get the package name by replacing "/" with "."
                        pkgName = name.substring(0, idx).replace('/', '.')
                    }
                    // If we can recurse and this is a package
                    if (idx != -1 || recursive) {
                        if (action.isRetrieveClass) {
                            // If it is a .class file and not a directory
                            if (name.endsWith(".class") && !entry.isDirectory) {
                                // Strip ".class" to get the actual class name
                                val className = name.substring(pkgName.length + 1, name.length - 6)
                                try {
                                    // Add to classes
                                    action.addClass(Class.forName("$pkgName.$className").kotlin)
                                } catch (e: ClassNotFoundException) {
                                    LOG.error(e)
                                }
                            }
                        } else { // Collect the package name
                            if (entry.isDirectory) {
                                action.addPackage(pkgName)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            // log.error("Error fetching files from jar while scanning user-defined views");
            LOG.error(e)
        }
    }

    /**
     * Get all classes under a package via the filesystem.
     */
    private fun findAndAddClassesInPackageByFile(
        packageName: String, packagePath: String, recursive: Boolean, action: Action
    ) {
        // Build a File for the package directory
        val dir = File(packagePath)
        // If it does not exist, or is not a directory, return immediately
        if (dir.exists() && dir.isDirectory) {
            if (!action.isRetrieveClass) {
                action.addPackage(packageName)
            }
        } else {
//            LOG.warn("No files found under user-defined package " + packageName);
            return
        }
        // If it exists, list all files under the package, including directories
        val dirFiles = dir.listFiles { file ->
            // Custom filter: recurse into directories (if recursive), or .class files (compiled Java classes)
            recursive && file.isDirectory || file.name.endsWith(".class")
        }
        // Iterate over all files
        dirFiles.orEmpty().forEach { file ->
            if (file.isDirectory) {
                // If it is a directory, continue scanning
                findAndAddClassesInPackageByFile("$packageName.${file.name}", file.absolutePath, recursive, action)
            } else if (action.isRetrieveClass) {
                // If it is a Java class file, strip the .class suffix and keep only the class name
                val className = file.name.removeSuffix(".class")
                try {
                    // Add to the collection
                    action.addClass(
                        Thread.currentThread().contextClassLoader.loadClass("$packageName.$className").kotlin
                    )
                } catch (e: ClassNotFoundException) {
                    LOG.error(e)
                }
            }
        }
    }

    /**
     * Internal collector used by package scanning.
     * When true, matched classes are placed into [classes]; when false, matched sub-packages are placed into [pkgs].
     * This allows the same scanning code to serve both [getClassesInPackage] and [getPackages] public entry points.
     *
     * @author K
     * @since 1.0.0
     */
    private class Action(retrieveClass: Boolean) {
        /** true: the current scan is collecting classes; false: the current scan is collecting sub-packages */
        var isRetrieveClass = true
        /** matched classes; uses [LinkedHashSet] to preserve discovery order */
        val classes = LinkedHashSet<KClass<*>>()
        /** matched sub-package names; uses [LinkedHashSet] to preserve discovery order */
        val pkgs = LinkedHashSet<String>()

        /**
         * Collect a class.
         * @param clazz the matched class
         */
        fun addClass(clazz: KClass<*>) {
            classes.add(clazz)
        }

        /**
         * Collect a sub-package name.
         * @param pkg the matched package name
         */
        fun addPackage(pkg: String) {
            pkgs.add(pkg)
        }

        init {
            isRetrieveClass = retrieveClass
        }
    }
}
