package io.kudos.base.enums.ienums

/**
 * Create by (admin) on 6/12/15.
 * 字典枚举接口
 */
interface IDictTypeEnum {
    /**
     * 所属模块
     */
    val module: IModuleEnum

    /**
     * 获取类型
     */
    val type: String

    /**
     * 获取描述
     */
    val desc: String

    /**
     * 父字典
     */
    val parentType: IDictTypeEnum?
}
