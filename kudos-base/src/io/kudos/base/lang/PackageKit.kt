package io.kudos.base.lang

import org.soul.base.lang.PackageTool
import kotlin.reflect.KClass

/**
 * 包工具类
 *
 * @author K
 * @since 1.0.0
 */
object PackageKit {

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
        return PackageTool.getClassesInPackage(pkg, recursive).map { it.kotlin }.toSet()
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
        return PackageTool.getPackages(pkgPattern, recursive)
    }

}