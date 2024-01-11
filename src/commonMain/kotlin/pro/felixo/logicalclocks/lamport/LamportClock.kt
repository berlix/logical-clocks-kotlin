package pro.felixo.logicalclocks.lamport

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pro.felixo.logicalclocks.LogicalClock

/**
 * A generic [Lamport clock](https://en.wikipedia.org/wiki/Lamport_timestamp).
 *
 * A Lamport clock is a simple logical clock that can be used to provide a causal ordering of events. Its
 * potential disadvantage is that it may order concurrent events in a way that is undesirable for a given application.
 *
 * This implementation is thread-safe.
 */
class LamportClock<LamportTimestamp : Comparable<LamportTimestamp>>(
    initialTime: LamportTimestamp,
    private var increment: (LamportTimestamp) -> LamportTimestamp,
    private val onNewTime: suspend (LamportTimestamp) -> Unit = {}
) : LogicalClock<LamportTimestamp> {
    private val mutex = Mutex()

    override var lastTime: LamportTimestamp = initialTime
        private set

    override suspend fun tick(): LamportTimestamp = mutex.withLock {
        increment(lastTime).also { update(it) }
    }

    override suspend fun tock(externalTime: LamportTimestamp): LamportTimestamp = mutex.withLock {
        if (externalTime > lastTime) { update(externalTime) }
        lastTime
    }

    private suspend fun update(time: LamportTimestamp) {
        lastTime = time
        onNewTime(time)
    }
}
