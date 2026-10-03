# Zadanie: Małopolski Hub Innowacji Społecznych (HubMI)

Wyzwanie HackYeah 2026 od **ROPS Kraków** (Regionalny Ośrodek Polityki Społecznej).
Źródło: `CRITERIA Wojewodztwo Malopolskie HUBMI.pdf`.

## Cel

Zaprojektować, nazwać i zbudować **prototyp (działające MVP) platformy z AI** – „cyfrowego serca" Hubu. Platforma ma:
łączyć zgłaszane problemy społeczne z istniejącymi innowacjami, wspierać zarządzanie wiedzą, zbierać pomysły
i budować współpracę mieszkańców, samorządów, NGO i ekspertów.

Kontekst: ROPS ma ~200 innowacji społecznych w portfolio. Wyzwania regionu: starzenie się społeczeństwa, zdrowie
psychiczne, samotność, wykluczenie cyfrowe, dostępność usług, koordynacja działań, depopulacja i zmiana struktury
osadniczej. Brakuje miejsca łączącego: diagnozę problemów → rozwój pomysłów → testy → upowszechnianie → partnerstwa.

## Użytkownicy (role)

| Rola              | Kim jest                                  | Czego oczekuje                                                  |
|:------------------|:------------------------------------------|:----------------------------------------------------------------|
| Mieszkaniec / NGO | zgłasza problemy i pomysły                | prosty interfejs, przejrzysty proces, sprawna komunikacja       |
| JST               | diagnozuje wyzwania, szuka rozwiązań      | katalog gotowych innowacji do wdrożenia w politykach lokalnych  |
| Admin (ROPS)      | administrator / koordynator               | panel: prezentacja wiedzy, monitoring zgłoszeń, dialog          |
| Ekspert           | konsultant innowatorów i doradca JST      | szybka komunikacja, feedback, budowanie współpracy              |

## Moduły

Numeracja zgodna z PDF. **Matchmaking jest obligatoryjny**, każdy dodatkowy moduł jest punktowany.

1. **Matchmaking społeczny (OBLIGATORYJNY)** – użytkownik opisuje problem; system (AI) wyszukuje podobne przypadki
   i proponuje gotowe rozwiązania/innowacje. Oceniana *trafność dopasowania* po słowach kluczowych z opisu potrzeby.
2. **Zasobnik wiedzy** – wyzwania społeczne Małopolski (raporty, Mapa Wyzwań Społecznych), **Biblioteka Innowacji
   Społecznych** (atrakcyjna forma, np. filmy), materiały edukacyjne. Szybka aktualizacja danych. Moduł agreguje
   zgłaszane potrzeby w obszary i wyznacza **trendy – widoczne tylko dla admina**.
3. **Kreator pomysłów** – **fiszka** pomysłu (istota, adresaci, etap realizacji) dostępna zawsze; w czasie naborów
   grantowych dodatkowo **generator wniosków** dopasowany do konkretnego naboru. Materiały: Canvy Innowacji
   Społecznych. Mile widziany **Asystent kreatora innowacji** (AI: rozwija pomysł, podpowiada rozwiązania, wizualizacja).
4. **Tester innowacji** – zgłoszenie chęci udziału w testach, ocena istniejących rozwiązań, feedback, propozycje usprawnień.
5. **Platforma aktywnej komunikacji** – dialog ROPS ↔ użytkownicy, pytania, wsparcie mentorów, partnerstwa międzysektorowe.
6. **Panel administratora** – szybka modyfikacja, weryfikacja i udostępnianie wiedzy.
7. **Middleman Innowacji** – Asystent AI dostosowujący innowację do formy usługi wg potrzeb zgłaszającej instytucji.

## Wymagania

**Dostarczyć:** nazwa i opis rozwiązania, prezentacja PDF (max 10 slajdów) *lub* film (max 3 min), link do dema
i makiet UX/UI, przewidywany koszt utrzymania wraz z zasobami.

**Funkcjonalne:** działający prototyp najważniejszych modułów, makiety UX/UI (minimum).

**Niefunkcjonalne:**
- **WCAG 2.1 AA** – seniorzy i osoby z niepełnosprawnościami muszą móc korzystać z narzędzia,
- **skalowalność** – dane z całego województwa, wielu użytkowników jednocześnie,
- **integracje i automatyzacja** – docelowo z innymi systemami Hubu (np. baza grantowa), automatyczne powiadomienia
  o nowych pomysłach i zmianach w naborach,
- **bezpieczeństwo danych**, możliwość dalszej rozbudowy.

**Ograniczenie:** nie używamy prawdziwych danych osobowych ani wrażliwych z materiałów ROPS – tylko dane przykładowe / seed.

## Kryteria oceny

| Kryterium                                             | Waga | Uwagi                                                                  |
|:------------------------------------------------------|-----:|:-----------------------------------------------------------------------|
| Stopień spełnienia wyzwania                           |  40% | Matchmaking 10%, każdy kolejny moduł +5%                               |
| Potencjał wdrożeniowy                                 |  20% | skalowalność, elastyczność, koszt, prostota utrzymania                 |
| Dostępność i intuicyjność prototypu                   |  20% | WCAG 2.1 AA, czytelność dla każdej grupy wiekowej                      |
| Atrakcyjność, pomysłowość, jakość interfejsu          |  10% | nowa jakość, a nie odtwórcza integracja istniejących portali           |
| Jakość materiałów i MVP                               |  10% | sposób komunikacji koncepcji                                           |

Dodatkowo patrzą na: szybkość komunikacji (jak admin dowiaduje się o nowym pomyśle i jak odpowiada autor), łatwość
zgłoszenia problemu, trafność propozycji.

## Dostępne zasoby od ROPS

Mapa wyzwań społecznych + linki do raportów, Biblioteka Innowacji Społecznych, materiały o doświadczeniach ROPS,
plansze Canw Innowacji Społecznych, mentorzy na miejscu, przykładowe dane do MVP.

## Zakres backendu (REST API)

Backend jest Ktor + MongoDB + Keycloak (zob. [CLAUDE.md](../CLAUDE.md)). Zasoby wynikające z modułów:

| Zasób                | Moduł | Dostęp                                         |
|:---------------------|:-----:|:-----------------------------------------------|
| `/api/matches`       |   1   | publiczny/zalogowany – opis problemu → dopasowania |
| `/api/needs`         |  1,2  | zgłaszanie potrzeb; agregacja i trendy – admin |
| `/api/innovations`   |  2,7  | katalog (publiczny), edycja – admin            |
| `/api/challenges`, `/api/materials` | 2 | katalog publiczny, edycja – admin     |
| `/api/ideas`         |   3   | fiszki (zalogowany), moderacja – admin         |
| `/api/calls`, `/api/applications` | 3 | nabory i wnioski                     |
| `/api/tests`, `/api/feedback` | 4 | zgłoszenia do testów, oceny            |
| `/api/threads`, `/api/messages` | 5 | komunikacja, mentorzy                 |
| `/api/admin/*`       |   6   | rola `admin`                                   |
| `/api/notifications` |  5    | powiadomienia o nowych pomysłach / naborach    |

Kolejność prac: (1) szkielet + Mongo, (2) innowacje + seed, (3) **matchmaking**, (4) potrzeby + trendy,
(5) pomysły/fiszki, (6) pozostałe moduły w kolejności opłacalności punktowej.
