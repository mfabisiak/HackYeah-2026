# Backlog frontendu

Powiązane: [ROADMAP.md](ROADMAP.md) (sekcja Web) · [API.md](API.md) · [BACKEND_TICKETS.md](BACKEND_TICKETS.md) · [CLAUDE.md](../CLAUDE.md).
Rozmiary: **S** ≤ 2 h, **M** ≤ pół dnia, **L** ≈ dzień. Numery `#N` w „Zależy od" to issues backendowe z GitHuba.
Założone na GitHubie jako issues [#12–#21](https://github.com/mfabisiak/HackYeah-2026/issues) (FE-01 → #12 … FE-10 → #21);
źródłem prawdy o zakresie są issues, ten plik to skrót. Frontend nie czeka na backend: pracuje na mockach HTTP (FE-02), a po gotowości endpointu przełącza się na prawdziwy.

**Stan obecny `web/`:** React 19 + TypeScript + Vite 8, Mantine 9, React Router 7, `keycloak-js` (PKCE, `check-sso`),
klient `HubApi` z `:web-client` (`health()`, `me()` oraz moduły `innovations`, `challenges`, `materials`, `matches`, `ideas`, `calls`, `threads`, `notifications`, `admin` pokrywające cały kontrakt z [API.md](API.md); backend odpowiada na nie na razie `501`). Strony: Start, Status systemu, Moje konto. Powłoka z linkiem
„Przejdź do treści", landmarkami i przełącznikiem motywu; ESLint z `jsx-a11y`; bundle ~1,2 MB (~337 KB gzip). Brak testów,
brak ochrony tras, teksty wpisane na sztywno po polsku.

| ID    | Tytuł                                                          | Rozm. | Moduł | Zależy od |
|:------|:---------------------------------------------------------------|:-----:|:-----:|:----------|
| FE-01 | Makiety UX/UI i design system (deliverable)                    |   L   |   –   | –         |
| FE-02 | Fundament techniczny: klient, mocki, ochrona tras, testy       |   L   |   –   | –         |
| FE-03 | Matchmaking: „Opisz problem" i wyniki                          |   L   |   1   | FE-02, #3 |
| FE-04 | Zasobnik wiedzy: innowacje, wyzwania, materiały                |   L   |   2   | FE-02, #2 |
| FE-05 | Kreator pomysłów i generator wniosków                          |   L   |   3   | FE-02, #5 |
| FE-06 | Tester innowacji: zgłoszenia i oceny                           |   S   |   4   | FE-04, #6 |
| FE-07 | Komunikacja i powiadomienia                                    |   M   |   5   | FE-02, #7 |
| FE-08 | Panel administratora: moderacja i zarządzanie treścią          |   L   |   6   | FE-02, #2, #5 |
| FE-09 | Trendy i dashboard admina                                      |   M   |   6   | FE-08, #8 |
| FE-10 | Dostępność, wydajność i dostawa (audyt, e2e, deklaracja)       |   M   |   –   | wszystkie |

---

## FE-01 Makiety UX/UI i design system
**Rozmiar:** L · wymagany deliverable („wizualizacja rozwiązania: minimum makiety UX/UI") i kryteria *Atrakcyjność, pomysłowość i jakość interfejsu* (10%) oraz *Dostępność i intuicyjność* (20%).

### Kontekst
Użytkownicy to mieszkańcy w każdym wieku i o różnych umiejętnościach cyfrowych, NGO, samorządy, pracownicy ROPS i eksperci. Intuicyjność jest oceniana wprost („czy mieszkaniec bez przygotowania technicznego potrafi wypełnić moduły i znaleźć informacje"). Makiety są też materiałem do prezentacji — warto je zrobić zanim ruszy implementacja widoków.

### Stan obecny
Domyślny motyw Mantine (`web/src/theme.ts`), trzy proste strony; brak makiet, brak zdefiniowanych tokenów i wzorców (formularze, błędy, karty, puste stany).

### Zakres
- Makiety kluczowych ścieżek (Figma lub równoważne): matchmaking, przeglądanie innowacji, zgłoszenie pomysłu, wątek z ROPS, panel admina, trendy — w wersji desktop i telefon.
- Design system w kodzie: tokeny kolorów, typografia, odstępy, komponenty bazowe i ich stany.

### Propozycje do zwalidowania
1. **Najpierw ścieżka matchmakingu** (obligatoryjna): jeden ekran z jednym wyraźnym polem i jednym przyciskiem; przykładowe problemy jako klikalne podpowiedzi obniżają próg wejścia dla osób, które „nie wiedzą, co wpisać".
2. **Tryby czytelności ❓:** przełączniki *duża czcionka* i *wysoki kontrast* oraz **język prosty (ETR)** jako osobna wersja streszczenia innowacji. Do ustalenia z backendem, czy kontrakt dostaje pole `easyReadSummary` (zmiana w #2).
3. **Paleta zweryfikowana narzędziem:** wszystkie pary kolor/tło ≥ 4,5:1 (tekst) i ≥ 3:1 (elementy interfejsu); status zawsze ikoną **i** tekstem, nigdy samym kolorem.
4. **Marka i nazwa ❓:** zadanie prosi o „nazwanie" rozwiązania; roboczo „HubMI". Decyzja zespołu, bo wpływa na prezentację i makiety.
5. **Wzorce wielokrotnego użytku:** formularz z błędami (podsumowanie na górze + komunikaty przy polach), karta innowacji, pusty stan, stan ładowania, błąd sieci, potwierdzenie destrukcyjnej akcji.

### Kryteria akceptacji
- [ ] Makiety kluczowych ścieżek (min. 6 ekranów × 2 rozdzielczości) dostępne pod linkiem do wklejenia do zgłoszenia.
- [ ] Tokeny i komponenty bazowe w `web/src/theme.ts` i `web/src/components/`, z opisanymi stanami (fokus, błąd, wyłączony).
- [ ] Tabela kontrastów dla wszystkich par kolorów dołączona do dokumentacji.

### Ryzyka
Dopracowywanie makiet kosztem implementacji — ograniczyć do ścieżek z demo i dać limit czasu.

---

## FE-02 Fundament techniczny
**Rozmiar:** L · pozwala pracować równolegle z backendem.

### Kontekst
Backend dostarcza endpointy etapami, a frontend ma zbudować wszystkie widoki. Bez wspólnego fundamentu każdy widok wymyśli własną obsługę ładowania, błędów i uprawnień, a testy dostępności nie będą powtarzalne.

### Stan obecny
`HubApi` ma tylko `health()` i `me()`. Każda strona sama woła `hubApi` w `useEffect`. Brak ochrony tras, mocków i testów.

### Zakres
1. **Dokończenie klienta Kotlin/JS dla całego kontraktu** (`:web-client`) — metody i typy `...Js` dla zasobów z atrap (`docs/API.md`).
2. **Warstwa danych** w React (pobieranie, cache, odświeżanie, stany ładowania/błędu).
3. **Mocki HTTP**, ochrona tras, wspólne komponenty, infrastruktura testów.

### Propozycje do zwalidowania
1. **Skala boilerplate'u klienta ❓ (spike ≈ 2 h):** ręczne klasy `...Js` dla ~30 endpointów i ich DTO to sporo mechanicznej pracy. Porównać: (A) ręczne typy `...Js` (bezpieczne, rozwlekłe — obecny wzorzec); (B) zwracanie przez klienta czystych obiektów JS (`Json.encodeToDynamic`) + ręcznie utrzymywane interfejsy TypeScript (mniej Kotlina, ryzyko rozjazdu kontraktu); (C) generowanie typów TS z serializatorów Kotlina (narzędzie do oceny: dojrzałość, utrzymanie). Wynik spike'a decyduje, czy wzorzec A zostaje. **Kto pisze metody klienta ❓** — proponuję autora endpointu po stronie backendu, a frontend startuje na mockach.
2. **Pobieranie danych ❓:** TanStack Query (cache, ponowienia, `invalidate`) vs własne hooki. Rekomendacja: TanStack Query — mniej własnego kodu na stany ładowania/błędów; działa z `ApiResult` przez cienki adapter.
3. **Mocki:** MSW (tylko dev/test) na poziomie HTTP — klient Kotlin pozostaje bez zmian, a handlery zwracają dane zgodne z kontraktem. Włączane flagą `VITE_MOCK=true`. Dane mocków wspólne z fixture'ami testów.
4. **Ochrona tras:** komponenty `RequireAuth` i `RequireRole("admin")` (z `keycloak.tokenParsed`), przekierowanie do logowania i powrót na żądaną stronę; `403` z czytelną stroną.
5. **Obsługa błędów:** jedna funkcja zamieniająca `ApiError` na komunikat po polsku (kody z `ErrorResponse`), wspólny `ErrorAlert`; błędy walidacji per pole (zależne od decyzji w #1 o `details` w `ErrorResponse`).
6. **Wspólne komponenty:** `PageHeader`, `LoadingState`, `EmptyState`, `ErrorAlert`, `FormField` (etykieta, podpowiedź, błąd z `aria-describedby`), `ErrorSummary` (focus po nieudanym wysłaniu), `Pagination` z `aria-label`.
7. **Podział kodu:** trasy ładowane leniwie (`React.lazy`) — dziś jeden bundle ~337 KB gzip; ustalić budżet (np. ≤ 250 KB gzip dla pierwszej strony).
8. **Testy:** Vitest + Testing Library + `jest-axe`/`vitest-axe`; każdy widok ma test renderu i podstawowy test dostępności.
9. **Teksty ❓:** na MVP polski wpisany w komponentach; wydzielić stałe tekstowe tylko tam, gdzie powtarzają się komunikaty błędów.

### Kryteria akceptacji
- [ ] Pełny `HubApi` dla kontraktu (lub decyzja ze spike'a udokumentowana) i `docs/API.md` zgodne z kodem.
- [ ] Aplikacja działa w całości na mockach (`VITE_MOCK=true`) bez backendu.
- [ ] Trasa admina niedostępna dla roli `user` (test).
- [ ] `npm run lint`, typecheck, testy i build przechodzą w CI.

### Ryzyka
Za duża „platforma" przed pierwszym widokiem — robić fundament na pierwszym prawdziwym widoku (FE-03), nie na sucho.

---

## FE-03 Matchmaking: „Opisz problem" i wyniki
**Rozmiar:** L · moduł obligatoryjny; kryteria: *łatwość zgłoszenia problemu*, *trafność proponowanych rozwiązań*, *intuicyjność*. **Zależy od:** FE-02, #3.

### Kontekst
To główny ekran produktu: osoba opisuje problem swoimi słowami i dostaje sprawdzone rozwiązania z uzasadnieniem. Musi działać dla seniora i dla urzędnika, z klawiatury i z czytnikiem ekranu.

### Stan obecny
Strona Start ma tylko kafelek „Wkrótce". Kontrakt (`Matching.kt`) i klient: brak metod `HubApi` dla `matches`.

### Zakres
Strona „Opisz problem", wyniki dopasowań, stan „brak dobrego dopasowania", „podobne zgłoszenia" i pytanie „czy to pomogło?".

### Propozycje do zwalidowania
1. **Formularz:** jedno pole `textarea` z widoczną etykietą (nie sam placeholder), podpowiedzią i licznikiem znaków; opcjonalny wybór gminy (słownik z API); przykładowe problemy jako przyciski wstawiające tekst. Wysyłanie klawiszem Enter nie jest potrzebne (wielolinijkowe pole).
2. **Ostrzeżenie o danych osobowych:** stały komunikat nad polem („Nie wpisuj imion, adresów ani numerów") — backend usuwa PESEL/telefon/e-mail, ale imion nie wykryje (#3).
3. **Opis nie trafia do URL ani do `localStorage`:** zapytanie żyje tylko w stanie komponentu (dane potencjalnie wrażliwe).
4. **Stan ładowania:** `aria-live="polite"` z komunikatem „Szukam rozwiązań…"; przy trybie AI odpowiedź może trwać kilka sekund, więc przycisk wyłączony i widoczny postęp.
5. **Wyniki:** lista kart (`<ul>`): tytuł jako link, jedno zdanie streszczenia, **uzasadnienie** („Dlaczego to pasuje"), dopasowane frazy wyróżnione elementem `<mark>` (z tekstem dla czytnika), etap innowacji ikoną **i** słowem. Po załadowaniu fokus przechodzi na nagłówek wyników.
6. **Ocena dopasowania ❓:** zamiast procentów etykiety jakościowe („bardzo dobre dopasowanie", „możliwe dopasowanie") wyliczane z `score` — proste do zrozumienia, mniej mylące (zob. #3).
7. **Brak dopasowania:** życzliwy komunikat, sugestia przeformułowania i przycisk „Zgłoś swój pomysł" prowadzący do FE-05 (z przeniesionym opisem, jeśli użytkownik się zgodzi).
8. **Podobne zgłoszenia:** pokazywane tak, jak ustali #3 (fragment tekstu albo tylko obszar i liczba).
9. **Feedback „Czy to pomogło?":** dwa przyciski (tak/nie) z potwierdzeniem w regionie `aria-live`; wysyłany raz, możliwa zmiana.
10. **Dyktowanie głosem:** **poza zakresem** (Web Speech API wysyła audio do zewnętrznej usługi — zakaz w `CLAUDE.md`).

### Kryteria akceptacji
- [ ] Ścieżka kompletna z klawiatury, bez myszy; test `vitest-axe` bez naruszeń.
- [ ] Stany: ładowanie, wyniki, brak dopasowania, błąd sieci (z ponowieniem), `429` (zbyt wiele zapytań) po polsku.
- [ ] Działa na mocku i na prawdziwym backendzie bez zmian w komponentach.
- [ ] Test z czytnikiem ekranu (VoiceOver) opisany w PR: zapowiedź wyników, fokus, uzasadnienia.

### Ryzyka
Kryterium „trafność" zależy od backendu i seedu, nie od UI — uzgodnić z BE scenariusze demo wcześnie.

---

## FE-04 Zasobnik wiedzy
**Rozmiar:** L · moduł 2. **Zależy od:** FE-02, #2.

### Kontekst
Biblioteka innowacji, wyzwania Małopolski i materiały edukacyjne mają być przedstawione w ciekawej i **dostępnej** formie (m.in. filmy), a treści łatwo aktualizowane (część admina — FE-08).

### Stan obecny
Brak widoków; kontrakt (`Innovations.kt`, `Knowledge.kt`) gotowy, backend to atrapy.

### Zakres
Lista i wyszukiwarka innowacji, szczegóły innowacji, wyzwania, materiały.

### Propozycje do zwalidowania
1. **Lista innowacji:** pole szukania z etykietą, filtry (obszar, grupa docelowa) jako grupa pól wyboru, paginacja; stan filtrów **w adresie URL** (można udostępnić link) — tu jest to bezpieczne, bo nie zawiera danych osobowych.
2. **Szczegóły:** tytuł, streszczenie, opis, etap, obszary, ocena tekstem („4,2 na 5, 12 ocen"), media. **Wideo wyłącznie z napisami lub transkrypcją** (WCAG 1.2); osadzanie plików własnych/self-hosted, nie zewnętrznych odtwarzaczy (prywatność, zakaz CDN).
3. **Wyzwania Małopolski:** lista wg obszaru z opisami; „mapa" jako **dostępna lista gmin** z opcjonalną prostą wizualizacją — nie mapa bez alternatywy tekstowej.
4. **Materiały:** filtr typu (raport, wideo, poradnik, canva), link do pobrania z opisem formatu i rozmiaru.
5. **Tryb prostego języka ❓:** jeśli backend doda `easyReadSummary` (FE-01/#2), przełącznik „Pokaż prostym językiem" na karcie i w szczegółach.
6. **Puste stany i szkielety ładowania** zamiast pustych ekranów; komunikat o liczbie wyników w `aria-live`.
7. **Tytuły stron** (`document.title`) i `h1` unikalne dla każdego widoku; ścieżka okruszkowa w szczegółach.

### Kryteria akceptacji
- [ ] Wyszukiwanie i filtry działają z klawiatury; liczba wyników ogłaszana czytnikowi.
- [ ] Film bez napisów/transkrypcji nie jest wyświetlany bez ostrzeżenia (lub nie przechodzi walidacji w panelu admina — FE-08).
- [ ] Test dostępności bez naruszeń; filtry odtwarzalne z URL.

---

## FE-05 Kreator pomysłów i generator wniosków
**Rozmiar:** L · moduł 3. **Zależy od:** FE-02, #5.

### Kontekst
Fiszka pomysłu jest dostępna zawsze (istota, komu dedykowany, etap), a w czasie naboru dochodzi **generator wniosków** dopasowany do konkretnego naboru. Autor ma też widzieć, co się dzieje z jego pomysłem (ścieżka odpowiedzi — kryterium *szybkość komunikacji*).

### Stan obecny
Zaimplementowane w `web/src/features/ideas` (fiszka, „Moje pomysły”, szczegóły) i `web/src/features/applications` (nabory, kreator wniosku ROPS, „Moje wnioski”); trasy `/pomysly`, `/pomysly/nowy`, `/nabory`, `/wnioski/:id` (krok kreatora w `?krok=N`). Decyzje:
- Kreator wniosku to stałe komponenty wg wzoru ROPS (osiem kroków), a nie render z `fields` naboru.
- Klient `:web-client` oddaje treść wniosku jako `ApplicationJs.contentJson` (JSON `SaveApplicationDraftRequest`, bo wnioskodawca jest typem polimorficznym) i przyjmuje go w `saveDraft`; mapowanie na formularz jest w `features/applications/form.ts`. Doszły `CallsApi.get` i `CallsApi.declarations`.
- Szkic wniosku jest tylko na serwerze (autozapis po 2,5 s, zapis przy zmianie kroku); w `localStorage` trzymamy wyłącznie szkic fiszki i ostatnio widziane statusy pomysłów, czyszczone przy wylogowaniu.
- Walidacja po stronie klienta odwzorowuje `RopsApplicationValidator`; błędy z `details` serwera trafiają do tych samych pól.
- `VITE_MOCK=true` działa bez Keycloaka (użytkownik testowy), a mocki ideas/calls/applications trzymają stan w pamięci (`mocks/applications.ts`).
- Kontrakt: `CreateIdeaRequest`, `IdeaDto`, `GrantCallDto`, `ApplicationDto`.

### Zakres
Formularz fiszki, „Moje pomysły" ze statusami, lista naborów, formularz wniosku generowany z szablonu.

### Propozycje do zwalidowania
1. **Fiszka krok po kroku:** istota → adresaci → etap → podsumowanie. Każdy krok to osobna grupa pól (`<fieldset>` z `<legend>`), wskaźnik postępu tekstowy („Krok 2 z 3").
2. **Walidacja i błędy:** podsumowanie błędów na górze (z linkami do pól, fokus po nieudanym wysłaniu) oraz komunikaty przy polach (`aria-describedby`); zależne od `details` w `ErrorResponse` (#1).
3. **Szkic ❓:** zapis lokalny w `localStorage` na wypadek zamknięcia karty, **czyszczony przy wylogowaniu** (wspólne komputery w bibliotekach). Zależy od decyzji w #5, czy backend ma status `DRAFT` i edycję.
4. **Moje pomysły:** lista z statusem **tekstem i ikoną**, komentarzem admina i datą (`<time datetime>`), oznaczenie „zmiana od ostatniej wizyty".
5. **Nabory:** lista aktywnych naborów z terminami (jak liczone: z dat, `Intl` w `pl-PL`), strona naboru z opisem i przyciskiem „Złóż wniosek" tylko gdy `OPEN`.
6. **Generator wniosków:** formularz renderowany z `fields` naboru; typy pól (tekst, długi tekst, liczba, wybór) i **prefill z fiszki** — wymaga rozszerzenia `CallField` (typ, limit znaków, podpowiedź, mapowanie na pole fiszki; zob. #5). Do czasu zmiany kontraktu obsłużyć tylko tekst.
7. **Canvy innowacji:** link do materiałów typu `CANVAS` w kreatorze („Potrzebujesz pomocy? Pobierz canvę").
8. **Asystent AI (bonus):** miejsce na panel „Podpowiedzi" (FE-10 poza zakresem), włączane po BE-10.
   *Stan:* panel jest gotowy jako `AssistantPanel` (`web/src/features/assistant`), na razie na własnej stronie `/asystent`; w kreatorze wystarczy go osadzić, podając `resolveIdea` z formularza fiszki.

### Kryteria akceptacji
- [ ] Wysłanie fiszki z samego klawiatury; błędy ogłaszane i powiązane z polami.
- [ ] Zamknięty nabór nie pozwala złożyć wniosku (komunikat, nie martwy przycisk).
- [ ] Test przejścia całej ścieżki na mocku (fiszka → lista → wniosek).

---

## FE-06 Tester innowacji
**Rozmiar:** S · moduł 4. **Zależy od:** FE-04, #6.

### Kontekst
Użytkownik może zgłosić chęć udziału w testach, ocenić rozwiązanie i zaproponować usprawnienia. To tani moduł (+5%), który dokłada wiarygodności (oceny widać w bibliotece).

### Zakres
Przyciski i formularze na stronie szczegółów innowacji: „Chcę przetestować" i „Oceń".

### Propozycje do zwalidowania
1. **Zgłoszenie do testów:** okno dialogowe (Mantine `Modal` — pułapka fokusu i `Esc` w komplecie) z opcjonalną notatką; po wysłaniu komunikat potwierdzający i wyłączony przycisk.
2. **Ocena:** grupa przycisków radiowych 1–5 z etykietami słownymi („1 – słabo … 5 – bardzo dobrze") zamiast samych gwiazdek; komentarz i „co usprawnić" jako osobne pola.
3. **Nadpisanie oceny:** jeśli użytkownik już ocenił, formularz startuje z poprzednią wartością (backend robi upsert, #6).
4. **Wymóg logowania:** niezalogowany widzi zachętę „Zaloguj się, aby ocenić", a po zalogowaniu wraca do tej samej innowacji.
5. **Prezentacja agregatu:** „4,2 na 5 (12 ocen)" jako tekst; gwiazdki wyłącznie dekoracyjne (`aria-hidden`).

### Kryteria akceptacji
- [ ] Dialog i formularz oceny obsłużone z klawiatury; fokus wraca do przycisku otwierającego.
- [ ] Błąd `401` przekierowuje do logowania i zachowuje kontekst.

---

## FE-07 Komunikacja i powiadomienia
**Rozmiar:** M · moduł 5. **Zależy od:** FE-02, #7.

### Kontekst
Dialog ROPS ↔ użytkownicy i mentorzy oraz powiadomienia o nowych pomysłach i zmianach statusu. W ocenie wprost: *jak admin dowiaduje się o nowym pomyśle i jak wygląda ścieżka odpowiedzi do autora* — to scenariusz do dopracowania w demo.

### Zakres
Dzwonek powiadomień, skrzynka, lista wątków, widok wątku z wiadomościami, nowy wątek.

### Propozycje do zwalidowania
1. **Powiadomienia w nagłówku:** przycisk „Powiadomienia (3 nieprzeczytane)" z licznikiem w etykiecie dla czytnika; lista w oknie z listą `<ul>`; „oznacz jako przeczytane".
2. **Odświeżanie ❓:** odpytywanie co ~15–30 s (proste) vs **SSE** (powiadomienie pojawia się natychmiast — lepszy efekt w demo, wymaga rozszerzenia kontraktu w #7). Rekomendacja: zacząć od odpytywania, SSE jako rozszerzenie.
3. **Nowe powiadomienie** ogłaszane w `aria-live="polite"` bez kradzieży fokusu.
4. **Wątki:** lista z oznaczeniem nieprzeczytanych tekstem; dla admina filtr „wymaga odpowiedzi". Widok wątku jako lista wiadomości (`role="log"`), pole odpowiedzi na dole, wysyłanie przyciskiem (nie tylko skrótem).
5. **Nowy wątek z pomysłu:** przycisk „Napisz do ROPS" na stronie pomysłu (FE-05) wstawia powiązanie `relatedIdeaId`.
6. **Czas:** `<time datetime>` z formatem względnym i pełną datą w `title`/dostępnej etykiecie.
7. **Role nadawców:** wyraźne etykiety (Autor, ROPS, Ekspert) tekstem, nie samym kolorem dymka.

### Kryteria akceptacji
- [ ] Nowa wiadomość/powiadomienie pojawia się bez przeładowania i jest zapowiedziana czytnikowi.
- [ ] Obcy wątek zwraca `404` obsłużone czytelną stroną.
- [ ] Test ścieżki: autor wysyła pomysł → admin widzi powiadomienie → odpowiada → autor widzi odpowiedź (na mocku).

---

## FE-08 Panel administratora: moderacja i zarządzanie treścią
**Rozmiar:** L · moduł 6. **Zależy od:** FE-02, #2, #5.

### Kontekst
Pracownicy ROPS muszą szybko modyfikować, weryfikować i udostępniać wiedzę, a także odpowiadać na pomysły. Panel to też miejsce, które pokazuje „szybkość komunikacji" jurorom.

### Zakres
Trasa `/admin` (rola `admin`): kolejka pomysłów, zmiana statusów, CRUD innowacji, wyzwań i materiałów.

### Propozycje do zwalidowania
1. **Układ:** osobna powłoka admina z nawigacją boczną; trasa chroniona `RequireRole("admin")`; widoczny wskaźnik „jesteś w panelu administratora".
2. **Kolejka pomysłów:** tabela z poprawnymi nagłówkami (`<th scope>`), filtr statusu, sortowanie z `aria-sort`; szczegóły w panelu bocznym; zmiana statusu z wymaganym komentarzem przy odrzuceniu; potwierdzenie przy akcjach nieodwracalnych.
3. **Formularze treści:** wspólny `FormField`, walidacja po stronie klienta zgodna z regułami serwera (długości, obszary); **pole transkrypcji/napisów wymagane dla wideo** (spójność z FE-04); podgląd „tak zobaczy to użytkownik".
4. **Archiwizacja zamiast usuwania** (zgodnie z miękkim usuwaniem w #2) z jasnym komunikatem.
5. **Szybkie edycje ❓:** edycja inline tylko tam, gdzie da się to zrobić dostępnie; w razie wątpliwości osobna strona edycji.
6. **Dwa tryby listy:** „Wymaga uwagi" (nowe pomysły, wątki bez odpowiedzi) jako domyślny widok startowy panelu.

### Kryteria akceptacji
- [ ] Użytkownik z rolą `user` nie widzi linków do panelu i dostaje `403` po wejściu pod adres.
- [ ] Cała obsługa (kolejka, edycja, zmiana statusu) z klawiatury; testy dostępności tabel.
- [ ] Błędy `409` (równoległa zmiana statusu, zob. #5) obsłużone komunikatem „ktoś już zmienił ten status — odśwież".

---

## FE-09 Trendy i dashboard admina
**Rozmiar:** M · moduł 6. **Zależy od:** FE-08, #8.

### Kontekst
System ma agregować potrzeby w obszarach i wyznaczać trendy widoczne **tylko** dla administratora. To efektowny element prezentacji, ale wykresy są częstą pułapką dostępności.

### Zakres
Dashboard z licznikami i trendami potrzeb po obszarze i gminie.

### Propozycje do zwalidowania
1. **Liczniki:** karty („Nowe pomysły: 4", „Wątki bez odpowiedzi: 2") z linkami do odpowiednich list.
2. **Wykresy ❓:** biblioteka do oceny (np. wykresy Mantine oparte na Recharts) pod kątem dostępności i rozmiaru bundle'a (ładowanie leniwe). Każdy wykres ma **tabelę z tymi samymi danymi** i krótki opis tekstowy („Najwięcej zgłoszeń dotyczy samotności, +12% względem poprzedniego okresu").
3. **Kodowanie:** wzorce/etykiety zamiast samych kolorów; kontrast linii i słupków ≥ 3:1.
4. **Okres:** wybór liczby miesięcy (parametr `months`) z podanym zakresem dat; porównanie z poprzednim okresem tekstem i strzałką z `aria-label`.
5. **Luki w bibliotece:** lista najczęstszych fraz z zapytań bez dopasowania (jeśli backend doda, zob. #8) jako „czego szukają, a jeszcze nie mamy".
6. **Małe liczby:** dla gmin z liczbą poniżej progu prywatności pokazać „mniej niż N" (zgodnie z #8), nie zero.
7. **Serie czasowe ❓:** wykres liniowy wymaga rozszerzenia `TrendsDto` o `series` (zob. #8).

### Kryteria akceptacji
- [ ] Każdy wykres ma równoważną tabelę i opis; test dostępności bez naruszeń.
- [ ] Widok niedostępny dla `user` i `expert`.
- [ ] Koszt bundle'a wykresów nie wchodzi do pierwszej strony (ładowanie leniwe).

---

## FE-10 Dostępność, wydajność i dostawa
**Rozmiar:** M · kryteria *Dostępność i intuicyjność* (20%), *Potencjał wdrożeniowy* (20%), *Jakość materiałów i MVP* (10%). **Zależy od:** wszystkich widoków.

### Kontekst
WCAG 2.1 AA jest wymaganiem, a sprawdzą je ludzie, nie tylko narzędzia. Ostatni ticket zamyka jakość: audyt, testy end-to-end, wydajność i artefakty do oceny.

### Zakres
Audyt dostępności, testy e2e kluczowych ścieżek, budżet wydajności, nagłówki bezpieczeństwa, deklaracja dostępności.

### Propozycje do zwalidowania
1. **Audyt automatyczny w CI:** axe (przez Playwright) na każdej trasie; Lighthouse CI z progami (dostępność ≥ 95, wydajność ≥ 85 — do dostrojenia).
2. **Audyt ręczny:** lista kontrolna (tylko klawiatura, VoiceOver, 200% zoom i reflow na 320 px, wysoki kontrast, `prefers-reduced-motion`), wynik w dokumencie `docs/ACCESSIBILITY.md`.
3. **Test użyteczności:** 3–5 osób w różnym wieku na ścieżce matchmakingu i zgłoszenia pomysłu; wnioski do prezentacji.
4. **Deklaracja dostępności ❓:** podmioty publiczne w Polsce zwykle muszą ją publikować — strona `/dostepnosc` ze stanem zgodności, ograniczeniami i kontaktem; wymóg prawny do potwierdzenia z ROPS, ale dla oceny to mocny sygnał.
5. **E2E (Playwright) na stosie z compose:** ścieżki: matchmaking, zgłoszenie pomysłu i moderacja, wątek ROPS ↔ autor. Dane z seeda (BE-02/BE-09).
6. **Wydajność:** podział na trasy, analiza zawartości bundle'a, ładowanie ikon/wykresów leniwie; budżet w CI (próg rozmiaru).
7. **Bezpieczeństwo przeglądarki:** `Content-Security-Policy` w nginx dopuszczająca wyłącznie własny origin i Keycloak (bez zewnętrznych CDN), `X-Content-Type-Options`, `Referrer-Policy`; sprawdzić działanie `keycloak-js` pod CSP.
8. **Dostawa:** `VITE_KEYCLOAK_URL` jako argument budowania obrazu, README frontendu, instrukcja nagrania demo (scenariusze zgodne z backendem).

### Kryteria akceptacji
- [ ] CI blokuje merge przy naruszeniach axe i spadku poniżej progów Lighthouse.
- [ ] `docs/ACCESSIBILITY.md` z wynikami audytu ręcznego i listą znanych ograniczeń.
- [ ] Trzy scenariusze e2e zielone na świeżym `docker compose up`.
- [ ] Strona deklaracji dostępności opublikowana i zlinkowana ze stopki.
