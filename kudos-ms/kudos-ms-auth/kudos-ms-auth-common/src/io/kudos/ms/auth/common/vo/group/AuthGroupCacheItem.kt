package io.kudos.ms.auth.common.vo.group

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 用户组缓存项
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class AuthGroupCacheItem (

    /** 主键 */
    override var id: String = EMPTY_ID,

    //region your codes 1

    /** 用户组编码 */
    var code: String? = null,

    /** 用户组名称 */
    var name: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subsysCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(EMPTY_ID)

    /**
     * 以非空语义访问 id。
     *
     * 默认值可能是空字符串（用于无参构造/反射创建），业务侧可通过该属性要求“非空白 id”。
     */
    var requiredId: String
        get() = id.takeIf { it.isNotBlank() }
            ?: error("AuthGroupCacheItem.id 为空白，当前对象可能尚未持久化。")
        set(value) {
            require(value.isNotBlank()) { "AuthGroupCacheItem.id 不能为空白字符串。" }
            id = value
        }

    //endregion your codes 3

    companion object {
        private const val serialVersionUID = 1L
        private const val EMPTY_ID = ""
    }

}
