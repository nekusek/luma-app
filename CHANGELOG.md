# Luma

## 0.1.5 — 2026-11-13

- Zakładka Wiadomości: odebrane, wysłane, odczyt, odpowiedzi i lokalna wysyłka do profili demo.
- Formularz z wyborem odbiorcy, walidacją i potwierdzeniem odrzucenia tekstu.
- Konto nauczyciela Piotr Wiśniewski z własnym Pulpitem, Dziennikiem i przykładowym Planem.
- Wybór klasy, obecności, oceny z wagami, tematy lekcji oraz zadania z terminem.
- Oceny, obecności i zadania nauczyciela widoczne w odpowiednim profilu ucznia.
- Trwały zapis wiadomości i wpisów w osobnej bazie Room.
- Frekwencja dostępna z Pulpitu oraz sekcji Więcej.
- Testy uprawnień demo, izolacji profili, trwałości i pełnego przepływu uczeń–nauczyciel.

## 0.1.4 — 2026-11-12

- Nowa lista ocen: przedmioty, kolorowe wartości i średnie po prawej.
- Dolny panel szczegółów z liczbą ocen, wagami, kategoriami, opisami i datami.
- Symulator średniej przeniesiony do szczegółów przedmiotu.
- Wybór roku szkolnego oraz osobna zakładka ocen śródrocznych z komunikatem o braku danych demo.
- Przycisk profilu w nagłówku ocen oraz obsługa jasnego, ciemnego i AMOLED motywu.
- Testy UI szczegółów ocen, symulatora i filtrowania roku szkolnego.

## 0.1.3 — 2026-11-12

- Ekran logowania demo z wyborem jednego z dwóch fikcyjnych profili.
- Dodawanie i przełączanie kont z Pulpitu oraz sekcji Więcej.
- Zapamiętywanie aktywnego profilu i oddzielny cache uczniów.
- Usuwanie konta z potwierdzeniem i czyszczeniem lokalnych danych.
- Kontrakty dostawców logowania przygotowane do przyszłego adaptera eduVULCAN.
- Nowe ikony logowania, kont, bezpieczeństwa i ustawień motywu.
- Testy repozytorium kont i przepływu logowania w UI, dodane do CI.

## 0.1.2 — 2026-11-12

- Usunięcie nagłówka ZSE / RZESZÓW z Pulpitu.
- Klikalne kafelki prowadzące do planu, ocen, frekwencji i sprawdzianów.
- Nowy widok frekwencji: podsumowanie, liczniki statusów i szczegóły przedmiotów.
- Historia obecności z filtrami statusu oraz zakresy wszystkich danych i bieżącego miesiąca.
- Lista sprawdzianów dostępna z Pulpitu i sekcji Więcej.
- Testy UI nawigacji, filtrów i braku danych, uruchamiane również w CI.

## 0.1.1 — 2026-11-12

- Ponawianie odczytu Room po błędzie z zachowaniem ostatnich danych.
- Ponawianie synchronizacji po błędzie i blokowanie równoległych odświeżeń.
- Odświeżanie danych po powrocie z tła.
- Odliczanie aktualizuje wyłącznie Pulpit, zamiast pozostałych zakładek.
- Zachowanie przewinięcia, wybranego dnia i symulatora przy zmianie zakładek.
- Przycisk Wstecz wraca do Pulpitu z innych zakładek.
- Obsługa błędów zapisu motywu i otwierania strony bez przeglądarki.
- Czytelny brak danych frekwencji i pusty widok ocen.
- Symulator zachowuje wybrany przedmiot po zmianie kolejności danych.
- Daty przyszłych lekcji i poprawny wybór najbliższego sprawdzianu.
- Ikona launchera i wektorowe ikony nawigacji, poprawione etykiety.
- Cztery testy regresji ViewModelu dodane do CI.
- Usunięcie komentarzy opisowych z kodu aplikacji.

Aplikacja korzysta z fikcyjnych danych i nie loguje się do VULCAN.
