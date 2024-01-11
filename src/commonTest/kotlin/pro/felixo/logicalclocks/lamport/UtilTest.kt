package pro.felixo.logicalclocks.lamport

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import io.kotest.core.spec.style.StringSpec

class UtilTest : StringSpec({
    "intLamportClock instantiates a LamportClock for Int timestamps" {
        var newTime: Int? = null
        val clock = intLamportClock(5) { newTime = it }
        assertThat(newTime).isNull()
        clock.tick()
        assertThat(newTime).isEqualTo(6)
        clock.tock(5)
        assertThat(newTime).isEqualTo(6)
        clock.tock(7)
        assertThat(newTime).isEqualTo(7)
    }

    "longLamportClock instantiates a LamportClock for Long timestamps" {
        var newTime: Long? = null
        val clock = longLamportClock(5) { newTime = it }
        assertThat(newTime).isNull()
        clock.tick()
        assertThat(newTime).isEqualTo(6L)
        clock.tock(5L)
        assertThat(newTime).isEqualTo(6L)
        clock.tock(7L)
        assertThat(newTime).isEqualTo(7L)
    }
})
