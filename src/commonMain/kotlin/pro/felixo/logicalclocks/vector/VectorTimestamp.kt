package pro.felixo.logicalclocks.vector

/**
 * A timestamp for use with [VectorClock].
 *
 * [components] is a map from node IDs to timestamps. The timestamp of a node is the number of events that have
 * occurred on that node.
 *
 * [components] must be non-empty.
 *
 * This implementation is [Comparable]. Two instances of [VectorTimestamp] are considered equal if they are either
 * identical or concurrent.
 */
data class VectorTimestamp<NodeId : Any, Timestamp : Comparable<Timestamp>>(
    val components: Map<NodeId, Timestamp>
) : Comparable<VectorTimestamp<NodeId, Timestamp>> {

    init {
        require(components.isNotEmpty()) { "VectorTimestamp must have at least one component" }
    }

    /**
     * Convenience constructor for creating a [VectorTimestamp] from a list of node IDs and timestamps.
     *
     * [components] must be non-empty.
     */
    constructor(vararg components: Pair<NodeId, Timestamp>) : this(components.toMap())

    /**
     * Two instances of [VectorTimestamp] are considered equal if they are either identical or concurrent.
     */
    override operator fun compareTo(other: VectorTimestamp<NodeId, Timestamp>): Int {
        var accumulator = 0
        (components.keys + other.components.keys).forEach { key ->
            val comparison = compareValues(components[key], other.components[key])
            if (comparison != 0)
                if (comparison == -accumulator)
                    return 0
                else
                    accumulator = comparison
        }
        return accumulator
    }
}
