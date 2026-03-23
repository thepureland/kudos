package io.kudos.ability.data.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestBuiltInTableKtormDao
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [BaseCrudService] 测试（含 IHasBuiltIn 删除约束）
 */
@EnableKudosTest
internal open class BaseCrudServiceTest {

    @Resource
    private lateinit var testBuiltInCrudService: TestBuiltInCrudService

    @Resource
    private lateinit var testBuiltInDao: TestBuiltInTableKtormDao

    @Resource
    private lateinit var testTableCrudService: TestTableCrudService

    /** 与 data.sql 中 test_built_in_ktorm 初始数据一致 */
    private val idCustomA = 901
    private val idBuiltIn = 902
    private val idCustomB = 903

    private fun nameEqSearchPayload(value: String): MutableListSearchPayload =
        MutableListSearchPayload().apply {
            setCriterions(listOf(Criterion(TestBuiltInTableKtorm::name.name, OperatorEnum.EQ, value)))
        }

    @Test
    @Transactional
    open fun deleteById_nonBuiltInTable_delegatesToDao() {
        assertTrue(testTableCrudService.deleteById(-1))
        assertNull(testTableCrudService.get(-1))
    }

    @Test
    @Transactional
    open fun deleteById_builtInTable_deletesWhenNotBuiltIn() {
        assertTrue(testBuiltInCrudService.deleteById(idCustomA))
        assertNull(testBuiltInDao.get(idCustomA))
    }

    @Test
    @Transactional
    open fun deleteById_builtInTable_returnsFalseWhenMissing() {
        assertFalse(testBuiltInCrudService.deleteById(999999))
    }

    @Test
    @Transactional
    open fun deleteById_builtInTable_throwsWhenBuiltIn() {
        val ex = assertFailsWith<ServiceException> {
            testBuiltInCrudService.deleteById(idBuiltIn)
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
        assertNotNull(testBuiltInDao.get(idBuiltIn))
    }

    @Test
    @Transactional
    open fun delete_entity_builtInTable_sameAsDeleteById() {
        val row = testBuiltInDao.get(idBuiltIn)!!
        val ex = assertFailsWith<ServiceException> {
            testBuiltInCrudService.delete(row)
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    @Test
    @Transactional
    open fun batchDelete_builtInTable_deletesAllNonBuiltIn() {
        assertEquals(2, testBuiltInCrudService.batchDelete(listOf(idCustomA, idCustomB)))
        assertNull(testBuiltInDao.get(idCustomA))
        assertNull(testBuiltInDao.get(idCustomB))
        assertNotNull(testBuiltInDao.get(idBuiltIn))
    }

    @Test
    @Transactional
    open fun batchDelete_builtInTable_throwsWhenMixingBuiltIn() {
        val ex = assertFailsWith<ServiceException> {
            testBuiltInCrudService.batchDelete(listOf(idCustomA, idBuiltIn))
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
        assertNull(testBuiltInDao.get(idCustomA))
        assertNotNull(testBuiltInDao.get(idBuiltIn))
    }

    @Test
    @Transactional
    open fun batchDeleteCriteria_builtInTable_deletesMatchingNonBuiltIn() {
        val criteria = Criteria.of(TestBuiltInTableKtorm::name.name, OperatorEnum.EQ, "custom_del_a")
        assertEquals(1, testBuiltInCrudService.batchDeleteCriteria(criteria))
        assertNull(testBuiltInDao.get(idCustomA))
    }

    @Test
    @Transactional
    open fun batchDeleteCriteria_builtInTable_throwsWhenOnlyBuiltInMatches() {
        val criteria = Criteria.of(TestBuiltInTableKtorm::name.name, OperatorEnum.EQ, "built_in_row")
        val ex = assertFailsWith<ServiceException> {
            testBuiltInCrudService.batchDeleteCriteria(criteria)
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
        assertNotNull(testBuiltInDao.get(idBuiltIn))
    }

    @Test
    @Transactional
    open fun batchDeleteCriteria_builtInTable_returnsZeroWhenNoMatch() {
        val criteria = Criteria.of(TestBuiltInTableKtorm::name.name, OperatorEnum.EQ, "no_such_name")
        assertEquals(0, testBuiltInCrudService.batchDeleteCriteria(criteria))
    }

    @Test
    @Transactional
    open fun batchDeleteWhen_builtInTable_deletesByPayload() {
        assertEquals(1, testBuiltInCrudService.batchDeleteWhen(nameEqSearchPayload("custom_del_b")))
        assertNull(testBuiltInDao.get(idCustomB))
    }

    @Test
    @Transactional
    open fun batchDeleteWhen_builtInTable_throwsWhenResultContainsBuiltIn() {
        val ex = assertFailsWith<ServiceException> {
            testBuiltInCrudService.batchDeleteWhen(nameEqSearchPayload("built_in_row"))
        }
        assertEquals(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE, ex.errorCode)
    }

    @Test
    @Transactional
    open fun batchDeleteWhen_builtInTable_requiresListSearchPayload() {
        assertFailsWith<IllegalArgumentException> {
            testBuiltInCrudService.batchDeleteWhen(object : ISearchPayload {})
        }
    }
}
