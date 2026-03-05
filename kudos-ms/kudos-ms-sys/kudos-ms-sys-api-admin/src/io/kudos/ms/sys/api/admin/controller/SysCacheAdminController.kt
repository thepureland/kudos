package io.kudos.ms.sys.api.admin.controller

import io.kudos.ability.web.springmvc.controller.BaseCrudController
import io.kudos.ms.sys.common.vo.cache.SysCacheDetail
import io.kudos.ms.sys.common.vo.cache.SysCachePayload
import io.kudos.ms.sys.common.vo.cache.SysCacheRecord
import io.kudos.ms.sys.common.vo.cache.SysCacheSearchPayload
import io.kudos.ms.sys.core.service.impl.SysCacheService
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 缓存管理控制器
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/sys/cache")
open class SysCacheAdminController :
    BaseCrudController<String, ISysCacheService, SysCacheSearchPayload, SysCacheRecord, SysCacheDetail, SysCachePayload>() {

    /**
     * 重载指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    @GetMapping("/reload")
    fun reload(name: String, key: String) {
        return service.reload(name, key)
    }

    /**
     * 重载指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    @GetMapping("/reloadAll")
    fun reloadAll(name: String) {
        return service.reloadAll(name)
    }

    /**
     * 踢除指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    @DeleteMapping("/evict")
    fun evict(name: String, key: String) {
        return service.evict(name, key)
    }

    /**
     * 踢除指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    @DeleteMapping("/evictAll")
    fun evictAll(name: String) {
        return service.evictAll(name)
    }

    /**
     * 检测指定名称和key的缓存项是否存在
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    @GetMapping("/existsKey")
    fun existsKey(name: String, key: String): Boolean {
        return service.existsKey(name, key)
    }

    /**
     * 获取指定名称和key的缓存项的值的json表示
     *
     * @param name 缓存名称
     * @param key 缓存key
     * @return value的json串,value为null或出错返回空串
     */
    @GetMapping("/getValueJson")
    fun getValueJson(name: String, key: String): String {
        return service.getValueJson(name, key)
    }

}