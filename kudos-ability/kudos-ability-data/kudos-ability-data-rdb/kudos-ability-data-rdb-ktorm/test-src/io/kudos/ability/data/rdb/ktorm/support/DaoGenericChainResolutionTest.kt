package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorm
import io.kudos.ability.data.rdb.ktorm.table.TestTableKtorms
import org.ktorm.schema.Table
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Generic resolution for DAO shapes that are not a single direct `: BaseCrudDao<…>()`.
 *
 * The DAO learns its table and entity from its own type arguments, and used to read them off its
 * **direct** superclass. That works for exactly one shape. Two others occur in practice and both
 * failed: a shared abstract DAO in the middle (the natural way to give a family of tables a common
 * method), and the extra subclass Spring's CGLIB inserts around a proxied bean. Neither is exotic,
 * and the failure was a confusing exception a long way from the cause.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class DaoGenericChainResolutionTest {

    /** A shared abstract DAO: fixes the key type, leaves the entity and table to subclasses. */
    internal abstract class AbstractHalfwayDao<E : IDbEntity<Int, E>, T : Table<E>> : BaseCrudDao<Int, E, T>() {
        fun tableForTest(): T = table()
        fun entityClassForTest() = entityClass()
    }

    /** …and a concrete DAO one level below it, which is where E and T finally get bound. */
    internal open class IndirectDao : AbstractHalfwayDao<TestTableKtorm, TestTableKtorms>()

    /** Two levels below, parameterising nothing in between — the shape CGLIB produces. */
    internal class SubclassOfIndirectDao : IndirectDao()

    /** A DAO that never fixes its type arguments: resolution cannot invent them. */
    internal open class StillGenericDao<E : IDbEntity<Int, E>, T : Table<E>> : BaseCrudDao<Int, E, T>() {
        fun entityClassForTest() = entityClass()
    }

    @Test
    fun resolvesThroughAnIntermediateAbstractDao() {
        val dao = IndirectDao()
        assertSame(TestTableKtorms, dao.tableForTest(), "the table must resolve past the abstract DAO in between")
        assertSame(TestTableKtorm::class, dao.entityClassForTest(), "and so must the entity, via substitution of E")
    }

    @Test
    fun resolvesThroughASubclassThatParameterisesNothing() {
        // The shape a CGLIB proxy creates around a @Repository bean.
        val dao = SubclassOfIndirectDao()
        assertSame(TestTableKtorms, dao.tableForTest())
        assertSame(TestTableKtorm::class, dao.entityClassForTest())
    }

    @Test
    fun aDaoThatNeverFixesItsEntityTypeIsRejectedClearly() {
        // Erasing to the upper bound would "work" here and then fail later with a column error that
        // says nothing about the real problem, so this is refused where it can still be explained.
        val error = assertFailsWith<IllegalStateException> {
            StillGenericDao<TestTableKtorm, TestTableKtorms>().entityClassForTest()
        }
        assertTrue(
            error.message!!.contains("concrete type"),
            "the error must explain that all three type arguments must be concrete: ${error.message}",
        )
    }
}
