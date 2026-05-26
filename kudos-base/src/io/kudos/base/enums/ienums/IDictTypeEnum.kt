package io.kudos.base.enums.ienums

/**
 * Created by (admin) on 6/12/15.
 * Dict type enum interface.
 */
interface IDictTypeEnum {
    /**
     * The owning module.
     */
    val module: IModuleEnum

    /**
     * Returns the type.
     */
    val type: String

    /**
     * Returns the description.
     */
    val desc: String

    /**
     * Parent dict.
     */
    val parentType: IDictTypeEnum?
}
