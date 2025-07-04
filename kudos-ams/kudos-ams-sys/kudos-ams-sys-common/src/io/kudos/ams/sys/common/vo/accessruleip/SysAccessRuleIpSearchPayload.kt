package io.kudos.ams.sys.common.vo.accessruleip

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * ip访问规则查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysAccessRuleIpSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysAccessRuleIpRecord::class

}