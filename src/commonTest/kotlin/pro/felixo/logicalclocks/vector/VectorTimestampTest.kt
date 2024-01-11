package pro.felixo.logicalclocks.vector

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

class VectorTimestampTest : FunSpec({
    test("must have at least one component") {
        assertFailure { VectorTimestamp<String, Int>(emptyMap()) }
    }

    context("compares") {
        withData(
            ComparisonCase(VectorTimestamp(mapOf("a" to 1)), VectorTimestamp(mapOf("a" to 1)), 0),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1)), VectorTimestamp(mapOf("a" to 2)), -1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 2)), VectorTimestamp(mapOf("a" to 1)), 1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1)), VectorTimestamp(mapOf("a" to 1, "b" to 1)), -1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1, "b" to 1)), VectorTimestamp(mapOf("a" to 1)), 1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1, "b" to 2)), VectorTimestamp(mapOf("a" to 1, "b" to 2)), 0),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1, "b" to 2)), VectorTimestamp(mapOf("a" to 1, "b" to 3)), -1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1, "b" to 3)), VectorTimestamp(mapOf("a" to 1, "b" to 2)), 1),
            ComparisonCase(VectorTimestamp(mapOf("a" to 2, "b" to 1)), VectorTimestamp(mapOf("a" to 1, "b" to 2)), 0),
            ComparisonCase(VectorTimestamp(mapOf("a" to 2)), VectorTimestamp(mapOf("a" to 1, "b" to 1)), 0),
            ComparisonCase(VectorTimestamp(mapOf("a" to 1, "b" to 1)), VectorTimestamp(mapOf("b" to 2)), 0),
        ) {
            (a, b, expected) ->
            assertThat(a.compareTo(b)).isEqualTo(expected)
        }
    }
}) {
    private data class ComparisonCase(
        val a: VectorTimestamp<String, Int>,
        val b: VectorTimestamp<String, Int>,
        val expected: Int
    )
}
