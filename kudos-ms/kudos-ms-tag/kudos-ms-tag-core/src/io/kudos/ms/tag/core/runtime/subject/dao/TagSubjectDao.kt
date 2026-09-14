package io.kudos.ms.tag.core.runtime.subject.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import io.kudos.ms.tag.core.runtime.subject.model.table.TagSubjects
import org.springframework.stereotype.Repository

@Repository
open class TagSubjectDao : BaseCrudDao<String, TagSubject, TagSubjects>()
