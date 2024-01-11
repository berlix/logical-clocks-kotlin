package pro.felixo.logicalclocks.vector

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

typealias Timestamp = VectorTimestamp<String, Int>

class VectorClockTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val onNewTimeInvocations = ArrayDeque<Timestamp>()
    fun onNewTime(time: Timestamp) { onNewTimeInvocations += time }
    fun verifyOnNewTimeInvocation(time: Timestamp) = assertThat(onNewTimeInvocations.removeFirst()).isEqualTo(time)
    fun verifyNoOnNewTimeInvocations() = assertThat(onNewTimeInvocations.isEmpty()).isTrue()

    "throws if initial time does not contain local component" {
        assertFailure { VectorClock("local", VectorTimestamp(mapOf("a" to 1)), Int::inc) }
    }

    "tick increments local component" {
        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc, ::onNewTime)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 0))
        verifyNoOnNewTimeInvocations()
        assertThat(clock.tick()).isEqualTo(VectorTimestamp("local" to 1))
        verifyOnNewTimeInvocation(VectorTimestamp("local" to 1))
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 1))
        assertThat(clock.tick()).isEqualTo(VectorTimestamp("local" to 2))
        verifyOnNewTimeInvocation(VectorTimestamp("local" to 2))
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 2))
        verifyNoOnNewTimeInvocations()
    }

    "tock increases each component to maximum of local and external time" {
        val clock = VectorClock("local", VectorTimestamp("local" to 0, "a" to 5), Int::inc, ::onNewTime)
        verifyNoOnNewTimeInvocations()
        assertThat(clock.tock(VectorTimestamp("local" to 1, "a" to 4)))
            .isEqualTo(VectorTimestamp("local" to 1, "a" to 5))

        verifyOnNewTimeInvocation(VectorTimestamp("local" to 1, "a" to 5))
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 1, "a" to 5))
        assertThat(clock.tock(VectorTimestamp("a" to 6, "b" to 0)))
            .isEqualTo(VectorTimestamp("local" to 1, "a" to 6, "b" to 0))
        verifyOnNewTimeInvocation(VectorTimestamp("local" to 1, "a" to 6, "b" to 0))
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 1, "a" to 6, "b" to 0))
        verifyNoOnNewTimeInvocations()
    }

    "tick updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc) { throw exception }
        assertFailure { clock.tick() }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 1))
    }

    "tock updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc) { throw exception }
        assertFailure { clock.tock(VectorTimestamp("a" to 1)) }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 0, "a" to 1))
    }

    "tick is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc, stall)
        launch { clock.tick() }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 2))
    }

    "tick and tock are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc, stall)
        launch { clock.tick() }
        launch { clock.tock(VectorTimestamp("local" to 1, "a" to 1)) }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 1, "a" to 1))
    }

    "tock and tick are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc, stall)
        launch { clock.tock(VectorTimestamp("local" to 1, "a" to 1)) }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 2, "a" to 1))
    }

    "tock is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = VectorClock("local", VectorTimestamp("local" to 0), Int::inc, stall)
        launch { clock.tock(VectorTimestamp("a" to 2)) }
        launch { clock.tock(VectorTimestamp("a" to 1)) }
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(VectorTimestamp("local" to 0, "a" to 2))
    }
})
