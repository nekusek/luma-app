package pl.zse.dziennik.model

import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.*

class GradePolicyTest {
    private fun grade(raw: String, numeric: Double?, weight: Int = 1) = Grade(raw, "s", "it", "Programowanie", raw, numeric, weight, "Test", "", LocalDate.of(2026, 11, 13))
    @Test fun modifiersAreAnExplicitOverrideAndNonNumericEntriesStayExcluded() {
        val entries = listOf(grade("4+", 4.5, 2), grade("5-", 4.75), grade("np", null, 4), grade("6", 6.0, 0))
        assertEquals((9.0 + 4.75) / 3, entries.weightedAverage(GradePolicy())!!, 0.0001)
        assertEquals((8.5 + 4.5) / 3, entries.weightedAverage(GradePolicy(true, 0.25, 0.5))!!, 0.0001)
    }
    @Test fun modifiersCannotCreateAnOutOfRangeGradeOrANumberFromUnknownText() {
        assertNull(listOf(grade("6+", null), grade("1-", null), grade("nb", null)).weightedAverage(GradePolicy(true)))
        assertTrue(runCatching { GradePolicy(true, -1.0, 0.25) }.isFailure)
    }
}
