package pro.felixo.logicalclocks.vector

import pro.felixo.logicalclocks.LogicalClock

/**
 * A generic [vector clock](https://en.wikipedia.org/wiki/Vector_clock).
 *
 * Vector clocks generate timestamps with precise information about which events were known at the time that an
 * event occurred. Their disadvantage is that they are large and computationally expensive: their size, as well as
 * the computational complexity of comparing two instances, are proportional to the number of nodes in the system.
 *
 * This implementation is thread-safe.
 *
 * Example instantiation where node IDs are of type [String] and logical time is expressed as a `Long`:
 *
 * ```
 * fun stringLongVectorClock(
 *     localNodeId: String,
 *     initialTime: VectorTimestamp<String, Long>,
 *     onNewTime: suspend (VectorTimestamp<String, Long>) -> Unit = {}
 * ): VectorClock<String, Long> = VectorClock(
 *     localNodeId,
 *     initialTime,
 *     Long::inc,
 *     onNewTime
 * )
 * ```
 */
class VectorClock<NodeId : Any, NodeTime : Comparable<NodeTime>>(
    private val localNodeId: NodeId,
    initialTime: VectorTimestamp<NodeId, NodeTime>,
    private val incrementNodeTime: (NodeTime) -> NodeTime,
    private val onNewTime: suspend (VectorTimestamp<NodeId, NodeTime>) -> Unit = {}
) : LogicalClock<VectorTimestamp<NodeId, NodeTime>> {
    init {
        require(localNodeId in initialTime.components.keys) {
            "The initial time must contain the local node ID."
        }
    }

    override var lastTime: VectorTimestamp<NodeId, NodeTime> = initialTime
        private set

    override suspend fun tick(): VectorTimestamp<NodeId, NodeTime> =
        VectorTimestamp(
            lastTime.components.toMutableMap().apply {
                set(localNodeId, incrementNodeTime(lastTime.components.getValue(localNodeId)))
            }
        ).also { update(it) }

    override suspend fun tock(externalTime: VectorTimestamp<NodeId, NodeTime>): VectorTimestamp<NodeId, NodeTime> {
        var updated = false
        val newTime = VectorTimestamp(
            lastTime.components.toMutableMap().apply {
                externalTime.components.forEach { (nodeId, nodeTime) ->
                    if (compareValues(nodeTime, this[nodeId]) == 1) {
                        set(nodeId, nodeTime)
                        updated = true
                    }
                }
            }
        )
        if (updated)
            update(newTime)
        return lastTime
    }

    private suspend fun update(time: VectorTimestamp<NodeId, NodeTime>) {
        lastTime = time
        onNewTime(time)
    }
}
