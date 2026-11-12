# Luma · Android

Natywny, otwartoźródłowy prototyp dziennika dla uczniów ZSE Rzeszów.
Inspiracja lekkością Wulkanowego; niezależny projekt, bez afiliacji ze szkołą,
VULCAN ani autorami Wulkanowego. Kod napisany od podstaw, licencja MIT.
Zielony akcent jest propozycją projektu, nie potwierdzoną identyfikacją szkoły.

## Wersja demonstracyjna 0.1.3

Kompilowalna aplikacja **wyłącznie demonstracyjna**: Pulpit Compose Material 3,
Room, Hilt, coroutines/StateFlow, DataStore, Material You, dark/AMOLED.
Pięć zakładek zawiera działające podglądy fikcyjnych danych. Oceny mają prosty
symulator średniej, plan HorizontalPager, frekwencja zestawienie, Więcej ustawienia
motywu i link do strony szkoły. Przed pierwszym zasileniem bazy pokazuje pusty stan.
Nie ma produkcyjnego logowania, pobierania danych, wiadomości ani wysyłania usprawiedliwień.

## Konta i logowanie demo

Pierwszy start otwiera ekran logowania z dwoma fikcyjnymi profilami uczniów.
Przełącznik kont znajduje się na Pulpicie oraz w sekcji Więcej → Konta i profile.
Można dodać drugi profil, przełączyć aktywny oraz usunąć konto z urządzenia.
Lista kont i aktywny profil przetrwają ponowne uruchomienie. Dane Room są rozdzielone
według ucznia; usunięcie konta czyści jego cache także po trwającym odświeżaniu.

`AccountRepository` i `AuthenticationProvider` oddzielają UI od sposobu logowania.
Obecnie Hilt rejestruje tylko `DemoAuthenticationProvider`; przycisk eduVULCAN jest
nieaktywny. Produkcyjny dostawca może zwrócić kilka profili, a ekran przyjmuje
callback uruchamiający właściwą autoryzację. Należy również podmienić `SnapshotSource`
i dodać zweryfikowaną obsługę przekierowań oraz sesji per konto.
DataStore zapisuje wyłącznie metadane profili, bez haseł i tokenów.
Demo nie nawiązuje połączenia z eduVULCAN i nie prosi o prawdziwe dane logowania.

## Struktura i kierunek zależności

```text
:app                     # Composition root Hilt, Activity, demo NavigationBar
:core:model              # Czysty Kotlin: modele, reguły, SchoolRepository/SnapshotSource
:core:database           # Room Entity, DAO, mapowanie, wersjonowane schematy
:core:data               # CachedSchoolRepository, MockDataRepository, DataStore
:core:network            # Ktor/OkHttp, kontrakty VULCAN, AES-GCM/Android Keystore
:core:designsystem       # Material 3, Material You, AMOLED
:feature:dashboard       # Stateless Compose + ViewModel/StateFlow
```

UI → kontrakt `SchoolRepository` z `core:model`; implementacja z `core:data` → Room
i `SnapshotSource`. `app` wiąże zależności w Hilt. `core:model` nie zna Androida.
W następnym etapie proste podglądy `DemoSection` zostaną przeniesione do osobnych
modułów `:feature:grades`, `:feature:timetable`, `:feature:attendance`,
`:feature:messages`, `:feature:settings`; `:core:sync` otrzyma WorkManager.
Te przyszłe moduły nie są jeszcze zadeklarowane w settings.gradle.kts.

## Offline-first

`Room Flow → StateFlow → collectAsStateWithLifecycle → Compose`.
Odczyt cache i odświeżanie startują niezależnie. UI nigdy nie czeka na żądanie
sieciowe przed pokazaniem istniejących danych. Udany **pełny** snapshot jednego
ucznia zastępuje dane w transakcji, włącznie z usuniętymi ocenami i lekcjami.
Błąd pobierania pozostawia cache. Produkcyjny adapter musi najpierw złożyć pełną
odpowiedź ze wszystkich stron/zakresów; nie wolno podać tu częściowego zestawu.
Schematy Room są eksportowane do `core/database/schemas` i powinny być commitowane.
Zmiany schematu wymagają migracji; brak destructive migration.

`MockDataRepository` jest alternatywnym źródłem snapshotów, nie omija warstwy cache.
Testowanie offline: uruchom raz, zamknij aplikację, włącz tryb samolotowy i uruchom ponownie.
Wszystkie identyfikatory oraz rekordy są fikcyjne. Plan demo generuje się na bieżący dzień,
również w weekendy; to dane prezentacyjne, nie oficjalny plan ani kalendarz szkoły.
Godziny i daty obliczane są w Europe/Warsaw, timestampy przechowywane jako UTC.

## Budowanie

Wymagane JDK 21 i Android SDK 37.0 / build-tools 36.0.0. compileSdk = 37,
targetSdk = 36, minSdk = 26. Otwórz projekt w aktualnym
Android Studio albo ustaw `ANDROID_HOME`. Lokalne `local.properties` nie trafia do Git.

```bash
./gradlew :core:model:test :core:data:testDebugUnitTest :feature:dashboard:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Zależności są przypięte w
`gradle/libs.versions.toml`. Gradle Wrapper sprawdza SHA-256 dystrybucji.
AGP 9.5.1 korzysta z wbudowanej obsługi Kotlina i nowego DSL. Współdzielona
wersja pluginów Kotlin JVM, Compose i Serialization to 2.4.21; nie stosujemy kapt.

## Kontrakt VULCAN — granica wdrożonej funkcjonalności

`VulcanTenant`, `LoginMethod`, `VulcanRequestSigner`, `sha256`, klient Ktor i
`KeystoreSessionStore` przygotowują miejsce na produkcyjny adapter. Nie zawierają
zgadywanych endpointów, danych klienta OAuth ani fikcyjnego algorytmu `v-request-*`.
SHA-256 treści nie jest podpisem żądania. Wymagane są zweryfikowane reguły
canonicalizacji, zakres nagłówków, nonce/timestamp, algorytm podpisu oraz rejestracja
certyfikatu właściwa dla konkretnej wersji API. Nie zakładamy, że OAuth/ADFS i API
certyfikatów urządzenia są jednym protokołem.

Kolejny etap: rozpoznanie aktualnego routingu jednostki Rzeszowa na autoryzowanym
koncie testowym, adapter wersji API, contract tests z zanonimizowanymi fixture'ami,
PKCE/state/nonce i walidacja przekierowań tam, gdzie dostawca wspiera OAuth.
ADFS wymaga osobnego adaptera. Klucze podpisujące powinny być nieeksportowalne
w Android Keystore; tokeny/certyfikaty szyfrowane AES-GCM. Gotowy session store
obsługuje szyfrowanie bajtów, a błędy unieważnienia klucza przekazuje do obsługi ponownego logowania.
Baza ucznia jest prywatną bazą Room w sandboxie Androida, nie bazą SQLCipher.
Backup i transfer danych są wyłączone. Brak logowania sekretów i PII.

Średnia wymaga jawnego mapowania wartości przez adapter; nie zgadujemy znaczenia
„5+”. Reguła frekwencji demo: obecność i spóźnienie w liczniku; nieobecności także
usprawiedliwione w mianowniku; zwolnienia wyłączone. Produkcja musi odzwierciedlać
reguły szkoły lub zwracać oficjalne agregaty. Średnia obliczona w aplikacji nie jest
prognozą oceny klasyfikacyjnej.

## GitHub i kolejne wersje

Repozytorium projektu: [nekusek/luma-app](https://github.com/nekusek/luma-app).
Nie commituj danych uczniów, sesji, certyfikatów, kluczy ani local.properties.

`Android CI` sprawdza PR i main, publikuje debug APK jako artefakt.
`Publish demo APK` uruchamiany ręcznie z ID pomyślnego buildu Android CI
publikuje sprawdzony APK w wydaniu `demo-VERSION`. Wersja i commit są odczytywane
z tego buildu; workflow przyjmuje wyłącznie pomyślne buildy push z main.
Wydania demo używają testowego podpisu debug i nie wymagają klucza produkcyjnego.
Tag `vMAJOR.MINOR.PATCH` uruchamia workflow wydania: testy, lint, podpisany,
zmniejszony APK, SHA-256 i GitHub Release. CI nie wymaga sekretów do debug build.
Twórz tag dopiero na sprawdzonym commicie. Workflow release wymaga czterech
sekretów w środowisku GitHub `release`:

* `ZSE_SIGNING_STORE_BASE64` — plik keystore zakodowany Base64;
* `ZSE_SIGNING_STORE_PASSWORD`;
* `ZSE_SIGNING_KEY_ALIAS`;
* `ZSE_SIGNING_KEY_PASSWORD`.

Utwórz jeden klucz dystrybucyjny lokalnie i przechowuj jego bezpieczną kopię.
Każda wersja musi być podpisana tym samym kluczem, aby Android przyjął aktualizację.
Brak sekretów zatrzymuje wydanie. Workflow nie publikuje niepodpisanego APK.
versionName pochodzi z taga; versionCode = 100000 + numer uruchomienia workflow
release. Nie resetuj historii/licznika workflow; po migracji repo zwiększ bazę,
żeby versionCode zawsze rósł. Wydania tego startera nadal zawierają wyłącznie demo.

```bash
git tag v0.1.0
git push origin main v0.1.0
```

W kolejnych iteracjach: pełne moduły funkcji, WorkManager z ograniczeniami sieci
i backoff, synchronizacja delta per konto i semestr, wiadomości i uwagi, testy
migracji/instrumentacyjne, testy dostępności i pomiary startupu na urządzeniu.

## Weryfikacja

Weryfikacja 0.1.3 obejmuje `:core:model:test` (3 testy),
`:core:data:testDebugUnitTest` (6 testów),
`:feature:dashboard:testDebugUnitTest` (4 testy), `:app:testDebugUnitTest` (7 testów),
`:app:assembleDebug`, `:app:assembleRelease` z R8 oraz `:app:lintDebug`.
Testy obejmują pełny przepływ logowania, dodawania, przełączania i usuwania kont,
trwałość wyboru profilu, izolację cache i usuwanie podczas odświeżania.
Lint nie zgłasza błędów; ostrzeżenia dotyczą targetSdk 36 wobec API 37
oraz dostępności nowszej wersji biblioteki testowej Roborazzi.
Testy na emulatorze/urządzeniu i kontraktowe testy produkcyjnego VULCAN pozostają
do wykonania. Lokalne `assembleRelease` bez konfiguracji klucza generuje unsigned
APK; workflow GitHub wymaga klucza i publikuje dopiero podpisane wydanie.
