package io.kudos.tools.codegen.core

import io.kudos.tools.codegen.model.vo.ColumnInfo
import io.kudos.tools.codegen.model.vo.Config


/**
 * Code generator context.
 *
 * @author K
 * @since 1.0.0
 */
object CodeGeneratorContext {

    lateinit var tableName: String
    lateinit var tableComment: String

    /**
     * Business module of the current table: the first-level package directory the generated code goes under
     * (`common/<bizModule>/...`, `core/<bizModule>/...`, ...).
     *
     * Several tables usually share one business module (`sys_dict` + `sys_dict_item` -> `dict`,
     * `user_account` + `user_account_third` + `user_org_user` -> `account`), and that grouping cannot be
     * derived from the table name — it is a human decision entered in the wizard and persisted on
     * `code_gen_object`. Blank means "fall back to [TemplateModelCreator.defaultBizModule]".
     */
    var bizModule: String = ""

    lateinit var columns: List<ColumnInfo>
    lateinit var config: Config
    lateinit var templateModelCreator: TemplateModelCreator

}