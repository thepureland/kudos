package io.kudos.ms.sys.common.i18n.vo.response
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 国际化详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nDetail (

    /** 主键 */
    override val id: String = "",


    /** 语言_地区 */
    val locale: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 国际化类型字典代码 */
    val i18nTypeDictCode: String = "",

    /** 国际化命名空间 */
    val namespace: String = "",

    /** 国际化key */
    val key: String = "",

    /** 国际化值 */
    val value: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>