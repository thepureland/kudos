package io.kudos.base.model.contract.common

import java.time.LocalDateTime

/**
 * 可审计的（带有审计相关属性）模型接口
 *
 * @author K
 * @since 1.0.0
 */
interface IAuditable {

    /** 记录创建时间 */
    var createTime: LocalDateTime?

    /** 记录创建者id */
    var createUserId: String?

    /** 记录创建者名称 */
    var createUserName: String?

    /** 记录更新时间 */
    var updateTime: LocalDateTime?

    /** 记录更新者id */
    var updateUserId: String?

    /** 记录更新者名称 */
    var updateUserName: String?

}