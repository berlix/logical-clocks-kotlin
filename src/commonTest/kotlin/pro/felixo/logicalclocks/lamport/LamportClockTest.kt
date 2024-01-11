package pro.felixo.logicalclocks.lamport

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameAs
import assertk.assertions.isTrue
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class LamportClockTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val onNewTimeInvocations = ArrayDeque<Int>()
    fun onNewTime(time: Int) { onNewTimeInvocations += time }

    fun defaultClock() = intLamportClock(0, ::onNewTime)
    fun verifyOnNewTimeInvocation(time: Int) = assertThat(onNewTimeInvocations.removeFirst()).isEqualTo(time)
    fun verifyNoOnNewTimeInvocations() = assertThat(onNewTimeInvocations.isEmpty()).isTrue()

    "tick increments clock" {
        val clock = defaultClock()
        assertThat(clock.lastTime).isEqualTo(0)
        verifyNoOnNewTimeInvocations()
        assertThat(clock.tick()).isEqualTo(1)
        verifyOnNewTimeInvocation(1)
        assertThat(clock.lastTime).isEqualTo(1)
        assertThat(clock.tick()).isEqualTo(2)
        verifyOnNewTimeInvocation(2)
        assertThat(clock.lastTime).isEqualTo(2)
        verifyNoOnNewTimeInvocations()
    }

    "tock increases clock" {
        val clock = defaultClock()
        verifyNoOnNewTimeInvocations()
        assertThat(clock.tock(5)).isEqualTo(5)
        verifyOnNewTimeInvocation(5)
        assertThat(clock.lastTime).isEqualTo(5)
        assertThat(clock.tock(6)).isEqualTo(6)
        verifyOnNewTimeInvocation(6)
        assertThat(clock.lastTime).isEqualTo(6)
        verifyNoOnNewTimeInvocations()
    }

    "tock does not increase clock if external time is less than current time" {
        val clock = defaultClock()
        verifyNoOnNewTimeInvocations()
        assertThat(clock.tock(5)).isEqualTo(5)
        verifyOnNewTimeInvocation(5)
        assertThat(clock.lastTime).isEqualTo(5)
        assertThat(clock.tock(3)).isEqualTo(5)
        assertThat(clock.lastTime).isEqualTo(5)
        verifyNoOnNewTimeInvocations()
    }

    "tick updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = intLamportClock(0) { throw exception }
        assertFailure { clock.tick() }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(1)
    }

    "tock updates internal state even if onNewTime fails, but exception is not swallowed" {
        val exception = Exception()
        val clock = intLamportClock(0) { throw exception }
        assertFailure { clock.tock(5) }.isSameAs(exception)
        assertThat(clock.lastTime).isEqualTo(5)
    }

    "tick is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Int) -> Unit = { unstall.receive() }

        val clock = intLamportClock(0, stall)
        launch { clock.tick() }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(2)
    }

    "tick and tock are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Int) -> Unit = { unstall.receive() }

        val clock = intLamportClock(0, stall)
        launch { clock.tick() }
        launch { clock.tock(5) }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(5)
    }

    "tock and tick are concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Int) -> Unit = { unstall.receive() }

        val clock = intLamportClock(0, stall)
        launch { clock.tock(5) }
        launch { clock.tick() }
        unstall.send(Unit)
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(6)
    }

    "tock is concurrency-safe" {
        val unstall = Channel<Unit>()
        val stall: suspend (Int) -> Unit = { unstall.receive() }

        val clock = intLamportClock(0, stall)
        launch { clock.tock(6) }
        launch { clock.tock(5) }
        unstall.send(Unit)
        assertThat(clock.lastTime).isEqualTo(6)
    }
})
