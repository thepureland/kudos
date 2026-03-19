package io.kudos.ms.sys.common.vo.resource.response


/**
 * 系统菜单树结点响应VO
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