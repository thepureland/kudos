package io.kudos.base.io

import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass


/**
 * 类和资源查找工具类
 *
 * @author K
 * @since 1.0.0
 */
object ScanKit {

    /**
     * 查找带有指定注解的类。
     *
     * @param basePackage 需要扫描的包名
     * @param annotationClass 要查找的注解类。
     * @param recursive 是否递归扫描其子包,默认递归
     * @return 带有指定注解的类列表。
     * @author K
     * @since 1.0.0
     */
    fun <T : Annotation> findClassesWithAnnotation(
        basePackage: String,
        annotationClass: KClass<T>,
        recursive: Boolean = true
    ): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo().enableAnnotationInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getClassesWithAnnotation(annotationClass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * 查找指定父类的子类。
     *
     * @param basePackage 需要扫描的包名
     * @param superclass 要查找子类的父类,不能为接口,接口请用findImplementations方法.
     * @param recursive 是否递归扫描其子包,默认递归
     * @return 指定父类的子类列表。
     * @author K
     * @since 1.0.0
     */
    fun findSubclassesOf(basePackage: String, superclass: KClass<*>, recursive: Boolean = true): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getSubclasses(superclass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * 查找实现了指定接口的类。
     *
     * @param basePackage 需要扫描的包名
     * @param interfaceClass 要查找实现类的接口,不能为类,类请用findSubclassesOf方法.
     * @param recursive 是否递归扫描其子包,默认递归
     * @return 实现了指定接口的类列表。
     * @author K
     * @since 1.0.0
     */
    fun findImplementations(
        basePackage: String,
        interfaceClass: KClass<*>,
        recursive: Boolean = true
    ): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        return classGraph.scan()
            .use { scanResult ->
                scanResult.getClassesImplementing(interfaceClass.qualifiedName).loadClasses().map { it.kotlin }
            }
    }

    /**
     * 查找类名匹配指定模式的类。
     *
     * @param basePackage 需要扫描的包名
     * @param pattern 要匹配的类名模式（正则表达式）。
     * @param recursive 是否递归扫描其子包,默认递归
     * @return 类名匹配指定模式的类列表。
     * @author K
     * @since 1.0.0
     */
    fun findClassesMatching(basePackage: String, pattern: String, recursive: Boolean = true): List<KClass<*>> {
        var classGraph = ClassGraph().enableClassInfo()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        val regexPattern = pattern.toRegex()
        return classGraph.scan()
            .use { scanResult ->
                scanResult.allClasses
                    .filter { it.simpleName.matches(regexPattern) }
                    .loadClasses().map { it.kotlin }
            }
    }

    /**
     * 查找资源文件名匹配指定模式的资源。
     *
     * @param basePackage 需要扫描的包名
     * @param pattern 要匹配的资源文件名模式（正则表达式）。
     * @param recursive 是否递归扫描其子包,默认递归
     * @return 资源文件名匹配指定模式的资源列表（路径）。
     * @author K
     * @since 1.0.0
     */
    fun findResourcesMatching(basePackage: String, pattern: String, recursive: Boolean = true): List<String> {
        var classGraph = ClassGraph()
        classGraph = if (recursive) {
            classGraph.acceptPackages(basePackage)
        } else {
            classGraph.acceptPackagesNonRecursive(basePackage)
        }
        val regexPattern = pattern.toRegex()
        return classGraph.scan()
            .use { scanResult ->
                scanResult.allResources
                    .filter { it.path.matches(regexPattern) }
                    .map { it.path }
            }
    }

}