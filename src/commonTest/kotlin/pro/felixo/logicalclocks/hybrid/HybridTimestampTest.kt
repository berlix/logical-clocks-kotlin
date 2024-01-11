package pro.felixo.logicalclocks.hybrid

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

class HybridTimestampTest : FunSpec({
    context("compares") {
        withData(
            ComparisonCase(HybridTimestamp(0, 0), HybridTimestamp(0, 0), 0),
            ComparisonCase(HybridTimestamp(0, 0), HybridTimestamp(0, 1), -1),
            ComparisonCase(HybridTimestamp(0, 1), HybridTimestamp(0, 0), 1),
            ComparisonCase(HybridTimestamp(0, 0), HybridTimestamp(1, 0), -1),
            ComparisonCase(HybridTimestamp(1, 0), HybridTimestamp(0, 0), 1),
            ComparisonCase(HybridTimestamp(0, 1), HybridTimestamp(1, 0), -1),
            ComparisonCase(HybridTimestamp(1, 0), HybridTimestamp(0, 1), 1),
            ComparisonCase(HybridTimestamp(0, 0), HybridTimestamp(1, 1), -1),
            ComparisonCase(HybridTimestamp(1, 1), HybridTimestamp(0, 0), 1)
        ) { (a, b, expected) ->
            assertThat(a.compareTo(b)).isEqualTo(expected)
        }
    }
}) {
    private data class ComparisonCase(
        val a: HybridTimestamp<Int, Int>,
        val b: HybridTimestamp<Int, Int>,
        val expected: Int
    )
}
