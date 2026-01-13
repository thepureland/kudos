package io.kudos.base.support

import java.util.concurrent.ConcurrentHashMap

/**
 * 对象注册器
 * 
 * 提供基于key的对象注册和查找功能，支持一个key对应多个对象。
 * 
 * 核心功能：
 * 1. 对象注册：根据key注册对象，支持单个和批量注册
 * 2. 对象查找：根据key查找已注册的对象列表
 * 3. 去重机制：自动去重，避免重复注册相同对象
 * 
 * 数据结构：
 * - 使用ConcurrentHashMap存储，key为String，value为MutableList<Any>
 * - 支持一个key对应多个对象
 * - 线程安全，支持并发访问
 * 
 * 使用场景：
 * - 插件注册：根据插件类型注册插件实例
 * - 策略模式：根据策略类型注册策略实现
 * - 扩展点：根据扩展点名称注册扩展实现
 * 
 * 注意事项：
 * - 使用ConcurrentHashMap保证线程安全
 * - 对象比较使用equals方法，需要确保对象正确实现equals
 * - 如果key不存在，lookup返回空列表而不是null
 * 
 * @author K
 * @since 1.0.0
 */
object Registry {

    /**
     * 所有注册的对象Map
     * key为注册键，value为注册的对象列表
     */
    private val map = ConcurrentHashMap<String, MutableList<Any>>()

    /**
     * 根据key查询所注册的对象
     * 
     * 查找指定key对应的所有注册对象。
     * 
     * 工作流程：
     * 1. 从map中查找key对应的列表
     * 2. 如果存在，返回该列表
     * 3. 如果不存在，返回空列表（不返回null）
     * 
     * 返回值：
     * - 如果key已注册对象，返回包含所有对象的列表
     * - 如果key未注册，返回空列表（不是null）
     * 
     * 注意事项：
     * - 返回的列表是实际存储的列表，修改会影响注册表
     * - 如果key不存在，返回的是新创建的空列表，不会添加到map中
     * 
     * @param key 注册键
     * @return 注册的对象列表，如果key不存在则返回空列表
     */
    fun lookup(key: String): MutableList<Any> = map[key] ?: mutableListOf()

    /**
     * 注册单个对象
     * 
     * 将对象注册到指定key下，如果对象已存在则不会重复添加。
     * 
     * 工作流程：
     * 1. 查找key对应的列表（如果不存在则创建空列表）
     * 2. 检查对象是否已存在（使用contains方法）
     * 3. 如果不存在，添加到列表
     * 4. 将列表放回map（确保map中的引用是最新的）
     * 
     * 去重机制：
     * - 使用contains方法检查对象是否已存在
     * - 基于equals方法进行比较
     * - 如果对象已存在，不会重复添加
     * 
     * 注意事项：
     * - 对象需要正确实现equals方法，否则去重可能失效
     * - 即使对象已存在，也会更新map中的引用
     * - 线程安全，支持并发注册
     * 
     * @param key 注册键
     * @param obj 要注册的对象
     */
    fun register(key: String, obj: Any) {
        val resultList: MutableList<Any> = lookup(key)
        if (!resultList.contains(obj)) {
            resultList.add(obj)
        }
        map[key] = resultList
    }

    /**
     * 批量注册对象
     * 
     * 将多个对象注册到指定key下，如果数组为空则直接返回。
     * 
     * 工作流程：
     * 1. 检查数组是否为空，如果为空则直接返回
     * 2. 查找key对应的列表（如果不存在则创建空列表）
     * 3. 将数组转换为列表并添加到结果列表
     * 4. 将列表放回map
     * 
     * 批量处理：
     * - 使用addAll方法一次性添加所有对象
     * - 不会进行去重检查（与单个注册不同）
     * - 如果数组中有重复对象，会全部添加
     * 
     * 注意事项：
     * - 如果数组为空，直接返回，不执行任何操作
     * - 批量注册不会检查对象是否已存在，可能添加重复对象
     * - 如果需要去重，应在调用前自行处理
     * 
     * @param key 注册键
     * @param objs 要注册的对象可变数组
     */
    fun register(key: String, vararg objs: Any) {
        if (objs.isEmpty()) {
            return
        }
        val resultList: MutableList<Any> = lookup(key)
        resultList.addAll(listOf(*objs))
        map[key] = resultList
    }
}