package io.kudos.ams.sys.common.vo.i18n

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 国际化查询记录
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysI18nDetail : IdJsonResult<String>() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2


    /** 语言_地区 */
    var locale: String? = null

    /** 语言_地区 */
    var moduleCode: String? = null

    /** 国际化类型字典代码 */
    var i18nTypeDictCode: String? = null

    /** 国际化key */
    var key: String? = null

    /** 国际化值 */
    var value: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    /** 创建用户 */
    var createUser: String? = null

    /** 创建时间 */
    var createTime: LocalDateTime? = null

    /** 更新用户 */
    var updateUser: String? = null

    /** 更新时间 */
    var updateTime: LocalDateTime? = null

}