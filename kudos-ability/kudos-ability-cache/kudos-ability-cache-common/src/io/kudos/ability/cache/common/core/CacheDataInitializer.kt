package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.kit.CacheKit
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.BeanPostProcessor


/**
 * 缓存数据初始化器
 * 
 * 用于在系统启动时加载所有配置为启动加载的缓存数据。
 * 
 * 核心功能：
 * 1. 收集缓存处理器：在Bean初始化后收集所有AbstractCacheHandler实例
 * 2. 延迟加载缓存：在所有单例Bean初始化完成后，加载配置为启动加载的缓存
 * 
 * 工作流程：
 * 1. Bean后处理：在postProcessAfterInitialization中收集所有缓存处理器
 * 2. 延迟初始化：实现SmartInitializingSingleton，在所有单例Bean初始化完成后执行
 * 3. 检查配置：遍历所有缓存处理器，检查是否配置了writeOnBoot=true
 * 4. 加载缓存：对于配置了启动加载的缓存，调用reloadAll(false)加载数据
 * 
 * 加载条件：
 * - 缓存配置存在（cacheConfig != null）
 * - 配置了启动加载（writeOnBoot == true）
 * 
 * 延迟加载原因：
 * - 确保数据库等依赖已初始化完成（如Flyway已执行）
 * - 避免在数据库未就绪时尝试加载缓存导致失败
 * - 保证所有Bean都已准备就绪
 * 
 * 注意事项：
 * - 只加载配置了writeOnBoot=true的缓存
 * - reloadAll(false)表示不清除现有缓存，直接加载
 * - 如果缓存配置不存在，会跳过该缓存处理器
 *
 * @author K
 * @since 1.0.0
 */
class CacheDataInitializer : BeanPostProcessor, SmartInitializingSingleton {

    private var cacheHandlers = mutableListOf<AbstractCacheHandler<*>>()

    /**
     * Bean初始化后的处理
     * 
     * 收集所有AbstractCacheHandler实例，用于后续的缓存数据加载。
     * 
     * @param bean Bean实例
     * @param beanName Bean名称
     * @return 处理后的Bean实例
     */
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is AbstractCacheHandler<*>) {
            cacheHandlers.add(bean)
        }
        return bean
    }

    /**
     * 在所有单例Bean初始化完成后加载缓存数据
     * 
     * 遍历所有收集的缓存处理器，对于配置了启动加载的缓存，执行数据加载。
     * 
     * 工作流程：
     * 1. 遍历缓存处理器：对每个收集的AbstractCacheHandler执行检查
     * 2. 获取缓存配置：通过CacheKit获取缓存配置
     * 3. 检查启动加载标志：如果writeOnBoot为true，执行加载
     * 4. 加载缓存数据：调用reloadAll(false)加载数据（不清除现有缓存）
     * 
     * 延迟加载原因：
     * - 确保数据库等依赖已初始化完成
     * - 避免在Flyway等数据库初始化工具执行前加载缓存
     * - 保证所有Bean都已准备就绪
     * 
     * 加载策略：
     * - reloadAll(false)：不清除现有缓存，直接加载新数据
     * - 如果缓存已存在，会被新数据覆盖
     * 
     * 注意事项：
     * - 只加载配置了writeOnBoot=true的缓存
     * - 如果缓存配置不存在，会跳过该处理器
     * - 加载过程可能耗时，建议合理配置启动加载的缓存
     */
    // 所有非懒加载的单例 bean 都实例化完成后，再加载缓存数据。防止类似flyway还未初始化数据库, 就可能有地方先去库里加载缓存的事情发生。
    override fun afterSingletonsInstantiated() {
        cacheHandlers.forEach {
            val cacheConfig = CacheKit.getCacheConfig(it.cacheName())
            if (cacheConfig != null && cacheConfig.writeOnBoot == true) {
                it.reloadAll(false)
            }
        }
    }

}