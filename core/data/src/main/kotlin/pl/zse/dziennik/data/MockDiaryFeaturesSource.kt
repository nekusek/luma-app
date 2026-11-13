package pl.zse.dziennik.data

import java.time.Clock
import java.time.LocalDate
import pl.zse.dziennik.model.*

class MockDiaryFeaturesSource(private val clock: Clock) : DiaryFeaturesSource {
    override suspend fun fetch(studentId: String): DiaryFeatures {
        require(DemoProfiles.accounts.any { it.studentId == studentId && it.role == AccountRole.STUDENT })
        val today = LocalDate.now(clock.withZone(SchoolZone))
        val year = today.year - if (today.monthValue < 9) 1 else 0
        return DiaryFeatures(
            periods = (year - 1..year).flatMap { y -> listOf(
                SchoolPeriod("$y:1", y, 1, LocalDate.of(y, 9, 1), LocalDate.of(y + 1, 1, 31)),
                SchoolPeriod("$y:2", y, 2, LocalDate.of(y + 1, 2, 1), LocalDate.of(y + 1, 8, 31)),
            ) },
            tasks = listOf(
                SchoolTask("e1", SchoolTaskKind.EXAM, "Systemy operacyjne", "Procesy i wątki", "Sprawdzian demo: planowanie procesów, synchronizacja i podstawy wątków.", today.plusDays(2), "Piotr Wiśniewski"),
                SchoolTask("exam:math", SchoolTaskKind.EXAM, "Matematyka", "Funkcje kwadratowe", "Kartkówka demo. Przygotuj postać kanoniczną i miejsca zerowe.", today.plusDays(5), "Nauczyciel demo"),
                SchoolTask("homework:room", SchoolTaskKind.HOMEWORK, "Programowanie", "Przygotuj projekt Room", "Utwórz encję i DAO. Oznaczenie wykonania zapisuje się tylko w Lumie, nie jest oddaniem pracy nauczycielowi.", today.plusDays(1), "Piotr Wiśniewski", "instrukcja-demo.txt", "Luma · materiał demo\n1. Utwórz encję Task.\n2. Dodaj DAO z Flow.\n3. Przygotuj ekran listy zadań."),
                SchoolTask("homework:past", SchoolTaskKind.HOMEWORK, "Elektronika", "Opracuj wyniki pomiarów", "Przykładowe zadanie z minionym terminem. Dołącz jednostki i wnioski.", today.minusDays(2), "Nauczyciel demo"),
            ),
            notes = listOf(
                SchoolNote("note:positive", true, "Aktywność", "Zaangażowanie w przygotowanie projektu laboratoryjnego. Przykładowa pochwała demo.", 5, today.minusDays(1), "Piotr Wiśniewski"),
                SchoolNote("note:neutral", null, "Informacja", "Przypomnienie o zasadach BHP w pracowni elektronicznej. Wpis demo.", null, today.minusDays(3), "Nauczyciel demo"),
                SchoolNote("note:negative", false, "Organizacja", "Brak przygotowanych materiałów na zajęcia. Przykładowa uwaga demo.", -2, today.minusDays(5), "Nauczyciel demo"),
            ),
            classification = listOf(
                ClassificationGrade("classification:it", "Programowanie", year, "5", null, null),
                ClassificationGrade("classification:math", "Matematyka", year, "4", null, null),
                ClassificationGrade("classification:it:previous", "Programowanie", year - 1, "5", "4", "5"),
                ClassificationGrade("classification:math:previous", "Matematyka", year - 1, "4", "4", "4"),
            ),
            luckyNumber = 17, luckyDate = today, pupilNumber = if (studentId == MockDataRepository.DEMO_STUDENT_ID) 17 else 9,
        )
    }
}
