package io.kudos.context.config

import org.springframework.core.env.PropertySource

/**
 * 组件配置内容查找器
 * 用于解决，通过配置文件以及配置中心同时指定配置文件不生效的问题
 *
 * @author hanson
 * @since 1.0.0
 */
interface IConfigDataFinder {
    /**
     * @param name 配置文件名
     * @return 返回数据源
     */
    fun findConfigData(name: String?): PropertySource<*>?
}
