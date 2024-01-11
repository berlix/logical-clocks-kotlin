package pro.felixo.logicalclocks

/**
 * The interface describing all logical clocks in this library.
 */
interface LogicalClock<Timestamp : Comparable<Timestamp>> {
    /**
     * The current logical timestamp of this clock.
     */
    val lastTime: Timestamp

    /**
     * Returns a logical timestamp that is greater than the current logical timestamp of this clock and updates
     * the clock's internal state accordingly.
     */
    suspend fun tick(): Timestamp

    /**
     * Updates the internal state of the clock to reflect the occurrence of an external event with the given logical
     * timestamp.
     *
     * Returns the new logical timestamp of the clock, which may be different from the argument [externalTime].
     */
    suspend fun tock(externalTime: Timestamp): Timestamp
}
