package pro.felixo.logicalclocks.hybrid

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

typealias Timestamp = HybridTimestamp<Int, Int>

class HybridClockTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val onNewTimeInvocations = ArrayDeque<Timestamp>()
    var currentPhysical = 0

    fun addNewTime(time: Timestamp) { onNewTimeInvocations += time }

    fun defaultClock(
        initial: Timestamp = Timestamp(0, 0),
        onNewTime: suspend (Timestamp) -> Unit = { addNewTime(it) }
    ) =
        HybridClock(initial, 0, { currentPhysical }, Int::inc, onNewTime)

    fun verifyOnNewTimeInvocation(time: Timestamp) = assertThat(onNewTimeInvocations.removeFirst()).isEqualTo(time)
    fun verifyNoOnNewTimeInvocations() = assertThat(onNewTimeInvocations.isEmpty()).isTrue()

    "tick increases physical component if greater, otherwise increments logical component" {
        val clock = defaultClock()
        assertThat(clock.lastTime).isEqualTo(Timestamp(0, 0))
        verifyNoOnNewTimeInvocations()

        assertThat(clock.tick()).isEqualTo(Timestamp(0, 1))
        verifyOnNewTimeInvocation(Timestamp(0, 1))
        assertThat(clock.lastTime).isEqualTo(Timestamp(0, 1))

        currentPhysical = 5
        assertThat(clock.tick()).isEqualTo(Timestamp(5, 0))
        verifyOnNewTimeInvocation(Timestamp(5, 0))
        assertThat(clock.lastTime).isEqualTo(Timestamp(5, 0))

        currentPhysical = 4
        assertThat(clock.tick()).isEqualTo(Timestamp(5, 1))
        verifyOnNewTimeInvocation(Timestamp(5, 1))
        assertThat(clock.lastTime).isEqualTo(Timestamp(5, 1))

        verifyNoOnNewTimeInvocations()
    }

    "tock increases clock" {
        val clock = defaultClock()
        verifyNoOnNewTimeInvocations()

        currentPhysical = 5

        assertThat(clock.tock(Timestamp(0, 1))).isEqualTo(Timestamp(0, 1))
        verifyOnNewTimeInvocation(Timestamp(0, 1))
        assertThat(clock.lastTime).isEqualTo(Timestamp(0, 1))

        assertThat(clock.tock(Timestamp(1, 0))).isEqualTo(Timestamp(1, 0))
        verifyOnNewTimeInvocation(Timestamp(1, 0))
        assertThat(clock.lastTime).isEqualTo(Timestamp(1, 0))

        verifyNoOnNewTimeInvocations()
    }

    "tock does not increase clock if external time is less than current time" {
        val clock = defaultClock(Timestamp(1, 0))
        verifyNoOnNewTimeInvocations()

        currentPhysical = 5

        assertThat(clock.tock(Timestamp(0, 1))).isEqualTo(Timestamp(1, 0))
        verifyNoOnNewTimeInvocations()
        assertThat(clock.lastTime).isEqualTo(Timestamp(1, 0))

        assertThat(clock.tock(Timestamp(1, 0))).isEqualTo(Timestamp(1, 0))
        assertThat(clock.lastTime).isEqualTo(Timestamp(1, 0))
        verifyNoOnNewTimeInvocations()
    }

    "tick updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = defaultClock { throw exception }
        assertFailure { clock.tick() }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(Timestamp(0, 1))
    }

    "tock updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = defaultClock { throw exception }
        assertFailure { clock.tock(Timestamp(5, 5)) }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(Timestamp(5, 5))
    }

    "tick is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = defaultClock(onNewTime = stall)
        launch { clock.tick() }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(Timestamp(0, 2))
    }

    "tick and tock are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = defaultClock(onNewTime = stall)
        launch { clock.tick() }
        launch { clock.tock(Timestamp(5, 5)) }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(Timestamp(5, 5))
    }

    "tock and tick are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = defaultClock(onNewTime = stall)
        launch { clock.tock(Timestamp(5, 5)) }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(Timestamp(5, 6))
    }

    "tock is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Timestamp) -> Unit = { unstall.receive() }

        val clock = defaultClock(onNewTime = stall)
        launch { clock.tock(Timestamp(6, 6)) }
        launch { clock.tock(Timestamp(5, 5)) }
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(Timestamp(6, 6))
    }
})
