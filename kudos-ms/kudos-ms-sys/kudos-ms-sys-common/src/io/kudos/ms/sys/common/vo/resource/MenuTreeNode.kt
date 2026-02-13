package io.kudos.ms.sys.common.vo.resource


/**
 * 系统菜单树结点，用于前端菜单的展现
 *
 * @author K
 * @since 1.0.0
 */
open class MenuTreeNode : BaseMenuTreeNode() {

    /** url */
    var index: String? = null

    /** 图标 */
    var icon: String? = null

}