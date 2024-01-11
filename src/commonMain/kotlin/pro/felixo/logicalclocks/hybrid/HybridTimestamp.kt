package pro.felixo.logicalclocks.hybrid

import pro.felixo.logicalclocks.vector.VectorTimestamp

/**
 * A timestamp for use with [HybridClock].
 *
 * A hybrid timestamp is a combination of a physical timestamp and a logical timestamp.
 *
 * This implementation is [Comparable]. Since the point of a hybrid timestamp is to provide an ordering to concurrent
 * events, only identical [HybridTimestamp] instances are considered equal. This is different from the behaviour of
 * [VectorTimestamp], which considers concurrent timestamps equal.
 */
data class HybridTimestamp<Physical : Comparable<Physical>, Logical : Comparable<Logical>>(
    val physical: Physical,
    val logical: Logical
) : Comparable<HybridTimestamp<Physical, Logical>> {

    override fun compareTo(other: HybridTimestamp<Physical, Logical>): Int =
        physical.compareTo(other.physical).takeIf { it != 0 } ?: logical.compareTo(other.logical)
}
