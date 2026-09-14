package io.kudos.ms.tag.core.runtime.attribute.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeEvent
import io.kudos.ms.tag.core.runtime.attribute.model.table.TagAttributeEvents
import org.springframework.stereotype.Repository

@Repository
open class TagAttributeEventDao : BaseCrudDao<String, TagAttributeEvent, TagAttributeEvents>()
