package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DeleteFileModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the [IDeleteService.isValid] default method.
 *
 * Covers: normal path, null bucket/path combinations, blank result, `..`
 * path-traversal rejection (in bucketName, in filePath, and as a standalone segment),
 * and the Java-interop DefaultImpls static bridge.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class IDeleteServiceTest {

    private val service = object : IDeleteService {
        override fun delete(model: DeleteFileModel): Boolean = true
    }

    private fun model(bucket: String?, path: String?) = DeleteFileModel().apply {
        bucketName = bucket
        filePath = path
    }

    @Test
    fun isValid_acceptsNormalPath() {
        assertTrue(service.isValid(model("bucket", "/dir/a.txt")))
    }

    @Test
    fun isValid_acceptsWhenOnlyBucketPresent() {
        assertTrue(service.isValid(model("bucket", null)))
    }

    @Test
    fun isValid_acceptsWhenOnlyFilePathPresent() {
        assertTrue(service.isValid(model(null, "/a.txt")))
    }

    @Test
    fun isValid_acceptsWhenBothNull() {
        // null + File.separator + null -> the separator alone is not blank
        assertTrue(service.isValid(model(null, null)))
    }

    @Test
    fun isValid_rejectsTraversalInFilePath() {
        assertFalse(service.isValid(model("bucket", "/../etc/passwd")))
        assertFalse(service.isValid(model("bucket", "a/../../b.txt")))
    }

    @Test
    fun isValid_rejectsTraversalInBucketName() {
        assertFalse(service.isValid(model("..", "/a.txt")))
        assertFalse(service.isValid(model("bucket/..", "/a.txt")))
    }

    @Test
    fun delete_fakeImplementationReturnsTrue() {
        assertTrue(service.delete(model("bucket", "/a.txt")))
    }

    @Test
    fun isValid_javaInteropDefaultImplsBridgeDispatchesSameLogic() {
        // The compiler emits IDeleteService$DefaultImpls static bridges for Java callers /
        // older binaries; exercise them the way such a caller would.
        val defaultImpls = Class.forName("io.kudos.ability.file.common.IDeleteService\$DefaultImpls")
        val isValid = defaultImpls.getMethod("isValid", IDeleteService::class.java, DeleteFileModel::class.java)
        assertTrue(isValid.invoke(null, service, model("bucket", "/dir/a.txt")) as Boolean)
        assertFalse(isValid.invoke(null, service, model("bucket", "/../etc/passwd")) as Boolean)
    }

}
