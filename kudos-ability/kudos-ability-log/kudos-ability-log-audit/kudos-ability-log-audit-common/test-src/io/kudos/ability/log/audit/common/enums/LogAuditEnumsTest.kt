package io.kudos.ability.log.audit.common.enums

import io.kudos.base.enums.impl.KudosModuleEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for the three log-audit enums.
 *
 * Coverage:
 *  - [OperationTypeEnum]: every code is a pure-numeric string (contract demanded by `BaseLog.toSysLogVo`'s
 *    `Integer.valueOf`), codes are unique, displayText mapping, and the [OperationTypeEnum.dictType] getter.
 *  - [LogParamTypeEnum]: code/displayText mapping.
 *  - [LogAuditDictTypeEnum]: module / type / desc / parentType projection.
 *
 * @author K
 * @since 1.0.0
 */
internal class LogAuditEnumsTest {

    @Test
    fun operationType_allCodesAreNumericAndUnique() {
        val codes = OperationTypeEnum.entries.map { it.code }
        // Integer.valueOf must not throw for any entry — toSysLogVo depends on it
        codes.forEach { Integer.valueOf(it) }
        assertEquals(codes.size, codes.toSet().size, "operation-type codes must be unique")
    }

    @Test
    fun operationType_codeAndDisplayTextMapping() {
        assertEquals("0", OperationTypeEnum.OTHER.code)
        assertEquals("1", OperationTypeEnum.QUERY.code)
        assertEquals("2", OperationTypeEnum.CREATE.code)
        assertEquals("3", OperationTypeEnum.UPDATE.code)
        assertEquals("4", OperationTypeEnum.DELETE.code)
        assertEquals("5", OperationTypeEnum.LOGIN.code)
        assertEquals("6", OperationTypeEnum.LOGOUT.code)
        assertEquals("create", OperationTypeEnum.CREATE.displayText)
        assertEquals("logout", OperationTypeEnum.LOGOUT.displayText)
    }

    @Test
    fun operationType_dictTypeAlwaysOperationType() {
        OperationTypeEnum.entries.forEach {
            assertSame(LogAuditDictTypeEnum.OPERATION_TYPE, it.dictType,
                "every operation type belongs to the OPERATION_TYPE dict: $it")
        }
    }

    @Test
    fun logParamType_codeAndDisplayTextMapping() {
        assertEquals("1", LogParamTypeEnum.STRING.code)
        assertEquals("2", LogParamTypeEnum.CURRENCY.code)
        assertEquals("3", LogParamTypeEnum.DATE.code)
        assertEquals("String", LogParamTypeEnum.STRING.displayText)
        assertEquals("Currency", LogParamTypeEnum.CURRENCY.displayText)
        assertEquals("Date", LogParamTypeEnum.DATE.displayText)
        assertEquals(3, LogParamTypeEnum.entries.size)
    }

    @Test
    fun logAuditDictType_projection() {
        val e = LogAuditDictTypeEnum.OPERATION_TYPE
        assertSame(KudosModuleEnum.LOG_AUDIT, e.module)
        assertEquals("operation_type", e.type)
        assertEquals("Operation type", e.desc)
        assertNull(e.parentType, "OPERATION_TYPE is a root dict type")
        assertEquals(1, LogAuditDictTypeEnum.entries.size)
    }
}
