package pro.felixo.logicalclocks.lamport

/**
 * Convenience function to instantiate a [LamportClock] for timestamps of type [Int].
 */
fun intLamportClock(initialTime: Int, onNewTime: suspend (Int) -> Unit = {}) =
    LamportClock(initialTime, Int::inc, onNewTime)

/**
 * Convenience function to instantiate a [LamportClock] for timestamps of type [Long].
 */
fun longLamportClock(initialTime: Long, onNewTime: suspend (Long) -> Unit = {}) =
    LamportClock(initialTime, Long::inc, onNewTime)
