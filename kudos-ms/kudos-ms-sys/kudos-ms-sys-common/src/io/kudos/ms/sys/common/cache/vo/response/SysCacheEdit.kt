package io.kudos.ms.sys.common.cache.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 缓存编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheEdit (

    /** 主键 */
    override val id: String = "",

    /** 名称 */
    val name: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 缓存策略代码 */
    val strategyDictCode: String = "",

    /** 是否启动时写缓存 */
    val writeOnBoot: Boolean = true,

    /** 是否及时回写缓存 */
    val writeInTime: Boolean = true,

    /** 缓存生存时间(秒) */
    val ttl: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否为 Hash 缓存 */
    val hash: Boolean = false,

) : IIdEntity<String>