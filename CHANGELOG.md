# Luma

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
