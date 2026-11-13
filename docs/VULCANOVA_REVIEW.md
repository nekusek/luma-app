# Porównanie z Vulcanovą

Punkt odniesienia: [Vulcanova/Vulcanova](https://github.com/Vulcanova/Vulcanova),
commit `f08622362b87d6a9a543e19d725de9b53e116a58`, sprawdzenie 2026-11-13.
Repozytorium jest zarchiwizowane, napisane w C#/Xamarin.Forms i udostępnione na
Apache-2.0. Luma zachowuje własną implementację Kotlin/Compose i licencję MIT.
Nie przeniesiono kodu, ikon ani zasobów Vulcanovy.

## Główne funkcje

Zakres pochodzi z README oraz implementacji `Vulcanova/Vulcanova/Features`.
Wszystkie wskazane niżej dane Lumy są demonstracyjne, a operacje zapisywane
na jednym urządzeniu. Ta tabela nie oznacza zgodności z produkcyjnym API.

| Funkcja Vulcanovy | Luma 0.2.0 | Gdzie |
| --- | --- | --- |
| Pulpit | Następna lekcja, ostatnie oceny, frekwencja, terminy i skróty | Pulpit |
| Szczęśliwy numerek | Liczba, data i porównanie z numerem ucznia | Pulpit |
| Oceny | Szczegóły, wagi, średnia, symulator i udostępnianie tekstu | Oceny |
| Okresy i klasyfikacja | Jawne semestry demo, przewidywane/śródroczne/roczne wartości ze źródła | Oceny |
| Plan lekcji | Przewijanie dni, szczegóły, sale, grupy, okienka, odwołania i zastępstwa | Plan |
| Frekwencja | Historia, statusy, raport ogólny i według przedmiotu | Pulpit / Więcej |
| Sprawdziany | Kalendarz oraz lista i szczegóły | Więcej → Kalendarz i zadania |
| Zadania domowe | Terminy, opis, lokalny podgląd materiału tekstowego, oznaczenie wykonania | Kalendarz i zadania |
| Uwagi | Pochwały, informacje, uwagi, punkty i szczegóły | Więcej → Uwagi i pochwały |
| Usprawiedliwienia | Wybór nieobecności, powód, potwierdzenie, historia i decyzja nauczyciela | Więcej → Usprawiedliwienia |
| Wiadomości | Skrzynki, odpowiedzi, odbiorcy, walidacja, lokalny odczyt i wysyłka | Wiadomości |
| Konta | Dwóch uczniów, rodzic Anny i nauczyciel, trwały wybór konta | Przełącznik / Więcej |
| Ustawienia | Motyw, modyfikatory ocen, odczyty, okres i warunki odświeżania | Więcej → Ustawienia dziennika |
| Widgety | Widget Androida: najbliższa zapisana lekcja i numerek | Launcher Androida |

Vulcanova ma osobne implementacje widoków, repozytoriów i usług m.in. w
`Features/Exams`, `Homework`, `Notes`, `LuckyNumber`, `Attendance/Justification`,
`Grades/Final`, `Grades/Summary`, `Messages` oraz `Timetable/Changes`.
Luma rozdziela odpowiadające dane kontraktami `SchoolRepository`,
`WorkspaceRepository` i `DiaryFeaturesRepository`; UI nie wywołuje API bezpośrednio.

## Działanie demo

Profil Maria Kowalska dotyczy tej samej Anny, co konto uczniowskie. Rodzic może
złożyć wniosek wyłącznie o istniejące nieusprawiedliwione nieobecności tego ucznia.
Wniosek nie zmienia obecności przed zatwierdzeniem. Nauczyciel Piotr Wiśniewski
rozpatruje wnioski w swoim profilu; odrzucenie pozwala złożyć nowy wniosek,
a akceptacja zmienia widok odpowiednich wpisów u rodzica i ucznia. Ponowna decyzja
jest blokowana. Rola demo nie potwierdza żadnych uprawnień w szkole.

Baza WorkspaceDatabase przechodzi automatyczną migrację 1 → 2. Zachowuje
wiadomości i dziennik nauczyciela; dodaje cache nowych modułów i lokalne akcje.
Nie ma destructive migration. Ustawienia są w DataStore. WorkManager jest
domyślnie wyłączony; jego harmonogram dotyczy źródła demo, nie synchronizacji
z dziennikiem szkoły. Widget czyta cache, pokazuje datę numerka i nie loguje się
w tle do usług VULCAN.

## Granica produkcyjnego wdrożenia

`Features/Auth/AuthenticationService.cs` Vulcanovy rejestruje urządzenie na
podstawie tokenu, PIN-u i certyfikatu X509. `Features/Auth/ScanningQrCode`
dekoduje kod rejestracyjny. Niskopoziomowy klient jest osobnym submodułem
`Vulcanova.Uonet` wskazanym w `.gitmodules`. Przeczytanie tego historycznego
kodu nie weryfikuje współczesnego logowania eduVULCAN, routingu Rzeszowa,
dostępności API ani praw kont nauczycielskich.

Nadal wymagane przed produkcją:

- zweryfikowany adapter aktualnej usługi, logowanie i rejestracja sesji;
- kontraktowe testy na zanonimizowanych danych i autoryzowanym koncie testowym;
- pobieranie danych i oficjalnych uprawnień, okresów, kolorów i reguł szkoły;
- prawdziwe wysyłanie wiadomości i usprawiedliwień oraz operacje nauczyciela;
- pobieranie dowolnych załączników i oddawanie odpowiedzi do zadań;
- pomiary, testy instrumentacyjne i weryfikacja widgetu/harmonogramu na urządzeniu.

Udostępnianie ocen w Lumie wysyła tekst przez systemowy chooser, nie obraz taki
jak w Vulcanovie. Podgląd załącznika obejmuje lokalny materiał tekstowy demo.
Nie włączono rejestrowania surowego ruchu HTTP. Te różnice są celowe i nie są
przedstawiane jako pełna zgodność ani gotowa integracja produkcyjna.
