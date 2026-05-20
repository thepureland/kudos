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
 * 包工具类
 *
 * @author K
 * @since 1.0.0
 */
object PackageKit {

    /** 日志器 */
    private val LOG = LogFactory.getLog(PackageKit::class)

    /**
     * 获取指定包名下的所有类
     *
     * @param pkg 以"."分隔的标准包名
     * @param recursive 是否循环迭代
     * @return Set(类)
     * @author K
     * @since 1.0.0
     */
    fun getClassesInPackage(pkg: String, recursive: Boolean): Set<KClass<*>> {
        val action = Action(true)
        find(pkg, recursive, action)
        return action.classes.toSet()
    }

    /**
     * 根据正则表达式获取匹配的所有包
     * 包的开头部分必须明确指定
     *
     * @param pkgPattern 包正则表达式
     * @param recursive 是否递归地获取子包
     * @return Set(包名)
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
     * 从含通配符的包正则中提取“可枚举的固定前缀”。
     * 把 `io.kudos.*.bean` 这样的输入截取到第一个含 `*` 段之前，得到 `io.kudos`。
     *
     * @param pkgPattern 含 `*` 的包匹配表达式
     * @return 去掉末尾点号的固定前缀
     * @author K
     * @since 1.0.0
     */
    private fun getPackagePrefix(pkgPattern: String): String =
        pkgPattern.split(".").takeWhile { !it.contains("*") }.joinToString(".")

    /**
     * 包扫描的统一入口：对当前线程的 classloader 拿到所有匹配资源，
     * 按协议分发到“文件目录扫描”或“JAR 扫描”。结果写入 [action]。
     *
     * @param packagePrefix 固定包前缀（点号分隔），不能含通配符
     * @param recursive 是否递归扫描子目录
     * @param action 扫描结果收集器
     * @author K
     * @since 1.0.0
     */
    private fun find(packagePrefix: String, recursive: Boolean, action: Action) {
        // 获取包的名字 并进行替换
        val packageDirName = packagePrefix.replace('.', '/')
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        val dirs: Enumeration<URL>
        try {
            dirs = Thread.currentThread().contextClassLoader.getResources(packageDirName)
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                val url = dirs.nextElement()
                // 得到协议的名称
                val protocol = url.protocol
                // 如果是以文件的形式保存在服务器上
                if ("file" == protocol) {
                    // 获取包的物理路径
                    val filePath = URLDecoder.decode(url.file, "UTF-8")
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(packagePrefix, filePath, recursive, action)
                } else if ("jar" == protocol) {
                    // 如果是jar包文件
                    // 定义一个JarFile
                    findAndAddClassesInPackageByJar(packagePrefix, packageDirName, url, recursive, action)
                }
            }
        } catch (e: IOException) {
            LOG.error(e)
        }
    }

    /**
     * 以 JAR 形式扫描指定包前缀，把命中的类或子包收集到 [action]。
     * 根据 [Action.isRetrieveClass] 切换“收类”或“收包”行为；
     * recursive=false 时只收顶层包/类。
     *
     * @param packageName 入参与递归过程中维护的当前包名
     * @param packageDirName 包前缀的目录形式（点号转斜杠）
     * @param url JAR 对应的资源 URL
     * @param recursive 是否递归扫描
     * @param action 扫描结果收集器
     * @author K
     * @since 1.0.0
     */
    private fun findAndAddClassesInPackageByJar(
        packageName: String, packageDirName: String, url: URL, recursive: Boolean, action: Action
    ) {
        var pkgName = packageName
        val jar: JarFile
        try {
            // 获取jar
            jar = (url.openConnection() as JarURLConnection).jarFile
            // 从此jar包 得到一个枚举类
            val entries = jar.entries()
            // 同样的进行循环迭代
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                val entry = entries.nextElement()
                var name = entry.name
                // 如果是以/开头的
                if (name[0] == '/') {
                    // 获取后面的字符串
                    name = name.substring(1)
                }
                // 如果前半部分和定义的包名相同
                if (name.startsWith(packageDirName)) {
                    val idx = name.lastIndexOf('/')
                    // 如果以"/"结尾 是一个包
                    if (idx != -1) {
                        // 获取包名 把"/"替换成"."
                        pkgName = name.substring(0, idx).replace('/', '.')
                    }
                    // 如果可以迭代下去 并且是一个包
                    if (idx != -1 || recursive) {
                        if (action.isRetrieveClass) {
                            // 如果是一个.class文件 而且不是目录
                            if (name.endsWith(".class") && !entry.isDirectory) {
                                // 去掉后面的".class" 获取真正的类名
                                val className = name.substring(pkgName.length + 1, name.length - 6)
                                try {
                                    // 添加到classes
                                    action.addClass(Class.forName("$pkgName.$className").kotlin)
                                } catch (e: ClassNotFoundException) {
                                    LOG.error(e)
                                }
                            }
                        } else { // 获取包名
                            if (entry.isDirectory) {
                                action.addPackage(pkgName)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            // log.error("在扫描用户定义视图时从jar包获取文件出错");
            LOG.error(e)
        }
    }

    /**
     * 以文件的形式来获取包下的所有Class
     */
    private fun findAndAddClassesInPackageByFile(
        packageName: String, packagePath: String, recursive: Boolean, action: Action
    ) {
        // 获取此包的目录 建立一个File
        val dir = File(packagePath)
        // 如果不存在或者 也不是目录就直接返回
        if (dir.exists() && dir.isDirectory) {
            if (!action.isRetrieveClass) {
                action.addPackage(packageName)
            }
        } else {
//			 LOG.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return
        }
        // 如果存在 就获取包下的所有文件 包括目录
        val dirFiles = dir.listFiles { file ->
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            recursive && file.isDirectory || file.name.endsWith(".class")
        }
        // 循环所有文件
        dirFiles.orEmpty().forEach { file ->
            if (file.isDirectory) {
                // 如果是目录 则继续扫描
                findAndAddClassesInPackageByFile("$packageName.${file.name}", file.absolutePath, recursive, action)
            } else if (action.isRetrieveClass) {
                // 如果是java类文件 去掉后面的.class 只留下类名
                val className = file.name.removeSuffix(".class")
                try {
                    // 添加到集合中去
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
     * 包扫描内部收集器。
     * true 时把命中的类放入 [classes]；false 时把命中的子包放入 [pkgs]，
     * 通过同一套扫描代码服务于 [getClassesInPackage] 与 [getPackages] 两个公开入口。
     *
     * @author K
     * @since 1.0.0
     */
    private class Action(retrieveClass: Boolean) {
        /** true: 当前扫描在收集类；false: 当前扫描在收集子包 */
        var isRetrieveClass = true
        /** 命中的类，使用 [LinkedHashSet] 保证发现顺序 */
        val classes = LinkedHashSet<KClass<*>>()
        /** 命中的子包名，使用 [LinkedHashSet] 保证发现顺序 */
        val pkgs = LinkedHashSet<String>()

        /**
         * 收集一个类。
         * @param clazz 命中的类
         */
        fun addClass(clazz: KClass<*>) {
            classes.add(clazz)
        }

        /**
         * 收集一个子包名。
         * @param pkg 命中的包名
         */
        fun addPackage(pkg: String) {
            pkgs.add(pkg)
        }

        init {
            isRetrieveClass = retrieveClass
        }
    }
}