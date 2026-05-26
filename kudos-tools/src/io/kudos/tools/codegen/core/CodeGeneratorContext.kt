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
    lateinit var columns: List<ColumnInfo>
    lateinit var config: Config
    lateinit var templateModelCreator: TemplateModelCreator

}