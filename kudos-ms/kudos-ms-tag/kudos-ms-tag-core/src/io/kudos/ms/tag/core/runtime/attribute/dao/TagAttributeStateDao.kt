package io.kudos.ms.tag.core.runtime.attribute.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeState
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeStates
import org.springframework.stereotype.Repository

@Repository
open class TagAttributeStateDao : BaseCrudDao<String, TagAttributeState, TagAttributeStates>()
