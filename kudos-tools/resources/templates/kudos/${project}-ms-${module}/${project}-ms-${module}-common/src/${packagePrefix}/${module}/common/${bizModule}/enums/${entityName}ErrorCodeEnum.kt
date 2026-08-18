package ${packagePrefix}.${module}.common.${bizModule}.enums

import io.kudos.base.enums.ienums.IErrorCodeEnum


<@generateClassComment "Error codes for "+(table.comment!table.name)+"."/>
//region your codes 1
enum class ${entityName}ErrorCodeEnum(
//endregion your codes 1
    /** Error code */
    override val code: String,
    /** Default display text */
    override val defaultDisplayText: String,
) : IErrorCodeEnum {

    /** Undefined error */
    UNSPECIFIED("UNSPECIFIED", "Undefined error"),

    /** Lookup by primary key failed */
    ${shortEntityName?upper_case}_NOT_FOUND("${shortEntityName?upper_case}_NOT_FOUND", "${table.comment!table.name} does not exist"),

    /** Unique key conflict */
    ${shortEntityName?upper_case}_ALREADY_EXISTS("${shortEntityName?upper_case}_ALREADY_EXISTS", "${table.comment!table.name} already exists"),

    //region your codes 2

    //endregion your codes 2
    ;

    override val i18nKeyPrefix: String
        get() = "${module}.error-msg.${bizModule}"

    //region your codes 3

    //endregion your codes 3

}
