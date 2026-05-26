package io.kudos.base.support

import kotlin.math.ceil

/**
 * Group executor.
 *
 * Splits a collection into groups of a specified size, then runs a given operation against each group.
 * Useful for batch processing large amounts of data while avoiding OOM or timeouts that may occur when
 * processing everything at once.
 *
 * Core capabilities:
 * 1. Partition the collection: split the collection into multiple sublists of size `groupSize`.
 * 2. Per-group execution: invoke the operation function on each group in order.
 * 3. Automatic sizing: the number of groups is computed automatically; the last group may be smaller than `groupSize`.
 *
 * Typical use cases:
 * - Batch database updates: avoid timeouts that may occur when updating large datasets at once.
 * - Batch file processing: avoid loading a large number of files into memory at once.
 * - Batch API calls: avoid being rate-limited by sending all requests at once.
 *
 * Grouping algorithm:
 * - group count = ceil(collection size / group size)
 * - start index of each group = group index * group size
 * - end index of each group = start index + group size (last group uses the collection size)
 *
 * Notes:
 * - Operations are executed sequentially, not in parallel.
 * - If an operation throws, execution of subsequent groups is interrupted.
 * - The last group may contain fewer than `groupSize` elements.
 *
 * @param E the collection element type
 */
class GroupExecutor<E>(
    /** The collection to be partitioned. */
    private val elems: Collection<E>,
    /** Size of each group; defaults to 1000. */
    private val groupSize: Int = 1000,
    /** Operation to execute on each group; receives the partitioned sublist. */
    private val operation: (List<E>) -> Unit
) {

    /**
     * Execute the grouped operation.
     *
     * Partitions the collection and then invokes the operation function on each group in order.
     *
     * Workflow:
     * 1. Compute the number of groups: use `ceil` (round up) to make sure every element is grouped.
     * 2. Convert to a list: convert the collection to an ArrayList so that `subList` can be used.
     * 3. Iterate groups:
     *    - Compute the start and end index for each group.
     *    - Obtain the group's sublist via `subList`.
     *    - Invoke the operation.
     *
     * Group computation:
     * - group count = ceil(collection size / group size)
     * - start index of group i = i * group size
     * - end index of group i = start index + group size (last group uses the collection size)
     *
     * Edge cases:
     * - The end index of the last group is simply the collection size, ensuring all remaining elements are included.
     * - If the collection is empty, no operation runs.
     * - If `groupSize` exceeds the collection size, only a single group is produced.
     *
     * Execution order:
     * - Groups are executed sequentially, not in parallel.
     * - If the operation on a group throws, subsequent groups are interrupted.
     * - Each group's operation is independent and does not affect the others.
     *
     * Performance considerations:
     * - Uses `subList` to avoid data copying for better performance.
     * - Choose `groupSize` based on the actual scenario to balance memory and performance.
     * - Suitable for batch operations on large datasets.
     *
     * Notes:
     * - `subList` returns a view backed by the original list; modifications affect the original list.
     * - If the operation needs to mutate data, consider copying the sublist first.
     * - Exception handling should be performed inside `operation` to avoid interrupting the entire flow.
     */
    fun execute() {
        val size = elems.size
        val groupCount = ceil(size / groupSize.toDouble()).toInt()
        val elemList: List<E> = ArrayList(elems)
        for (index in 0 until groupCount) {
            val from = index * groupSize
            val end = if (index == groupCount - 1) elemList.size else from + groupSize
            val subList = elemList.subList(from, end)
            operation(subList)
        }
    }


}
