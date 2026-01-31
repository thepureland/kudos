package io.kudos.ams.msg.common.vo.receivergroup

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 消息接收者群组详情记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class MsgReceiverGroupDetail (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 接收者群组类型字典码 */
    var receiverGroupTypeDictCode: String? = null,

    /** 群组定义的表 */
    var defineTable: String? = null,

    /** 群组名称在具体群组表中的字段名 */
    var nameColumn: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
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
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
