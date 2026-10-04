# Kontrakt API (wstępny)

Źródłem prawdy jest kod: trasy to `@Resource` w [core/.../api](../core/src/commonMain/kotlin/io/github/mfabisiak/hubmi/api),
każdy plik grupuje zasoby i DTO jednego modułu. Zasoby oznaczone w tabeli jako *(zaimplementowane)* są gotowe,
pozostałe to plan.

Konwencje: identyfikatory to `String` (hex), czas to ISO-8601 w `String`, listy paginowane (`page`, `size`),
błąd to zawsze `ErrorResponse(code, message)`. Dostęp: 🌐 publiczny · 🔑 zalogowany · 🛡️ rola `admin` · 👔 pracownik ROPS (rola `admin` lub `expert`).

| Moduł | Metoda i ścieżka | Dostęp | Żądanie → odpowiedź |
|:--|:--|:-:|:--|
| – | `GET /health`, `GET /health/ready` | 🌐 | → `HealthResponse` *(zaimplementowane)* |
| – | `GET /api/me` | 🔑 | → `MeResponse` *(zaimplementowane)* |
| – | `GET /api/admin` | 🛡️ | → `MessageResponse` *(zaimplementowane)* |
| 2 | `GET /api/innovations?q&area&targetGroup&page&size` | 🌐 | → `Page<InnovationSummary>` *(zaimplementowane)* |
| 2 | `GET /api/innovations/{id}` | 🌐 | → `InnovationDto` *(zaimplementowane)* |
| 2 | `POST /api/innovations` | 🛡️ | `UpsertInnovationRequest` → `InnovationDto` *(zaimplementowane)* |
| 2 | `PUT /api/innovations/{id}` | 🛡️ | `UpsertInnovationRequest` → `InnovationDto` *(zaimplementowane)* |
| 2 | `DELETE /api/innovations/{id}` | 🛡️ | → `204` *(zaimplementowane)* |
| 2 | `GET /api/challenges?area&page&size`, `GET /api/challenges/{id}` | 🌐 | → `Page<ChallengeDto>`, `ChallengeDto` *(zaimplementowane)* |
| 2 | `POST /api/challenges`, `PUT`/`DELETE /api/challenges/{id}` | 🛡️ | `UpsertChallengeRequest` → `ChallengeDto` *(zaimplementowane)* |
| 2 | `GET /api/materials?q&area&type&page&size`, `GET /api/materials/{id}` | 🌐 | → `Page<MaterialDto>`, `MaterialDto` *(zaimplementowane)* |
| 2 | `POST /api/materials`, `PUT`/`DELETE /api/materials/{id}` | 🛡️ | `UpsertMaterialRequest` → `MaterialDto` *(zaimplementowane)* |
| 1 | `POST /api/matches` | 🌐/🔑 | `MatchRequest` → `MatchResult` *(zaimplementowane; `200`; login opcjonalny – zalogowany zgłaszający zostaje właścicielem potrzeby, nieprawidłowy token → `401`)* |
| 1 | `PUT /api/matches/{needId}/feedback` | 🌐/🔑 | `MatchFeedbackRequest` → `204` *(zaimplementowane; idempotentne – zastępuje poprzednią odpowiedź; potrzebę z właścicielem może ocenić tylko on: bez tokena `401`, ktoś inny `403`)* |
| 3 | `POST /api/ideas` | 🔑 | `CreateIdeaRequest` → `IdeaDto` *(zaimplementowane)* |
| 3 | `GET /api/ideas/mine?page&size`, `GET /api/ideas/{id}` | 🔑 | → `Page<IdeaDto>`, `IdeaDto` *(zaimplementowane)* |
| 3 | `POST /api/ideas/assist` | 🔑 | `AssistDraftRequest` → `AssistResponse` *(zaimplementowane; asystent AI dla pomysłu jeszcze niezapisanego, walidacja jak `POST /api/ideas`; JSON albo strumień zdarzeń, zob. niżej)* |
| 3 | `POST /api/ideas/{id}/assist` | 🔑 | `AssistRequest` → `AssistResponse` *(zaimplementowane; dla zapisanego pomysłu, dostęp jak `GET /api/ideas/{id}`: autor, admin lub ekspert)* |
| 7 | `POST /api/innovations/{id}/adaptations` | 🔑 | `InstitutionProfile` → `AdaptationResponse` *(zaimplementowane; Middleman: plan pilotażu innowacji dla instytucji, zapisany do przeglądu admina; `201` z `Location` gdy plan zapisano, `200` z samym `aiStatus` gdy model go nie dał; JSON albo SSE)* |
| 7 | `GET /api/adaptations/mine?page&size`, `GET /api/adaptations/{id}` | 🔑 | → `Page<AdaptationDto>`, `AdaptationDto` *(zaimplementowane; plan widzi autor albo admin)* |
| 7 | `GET /api/admin/adaptations?status&page&size` | 👔 | → `Page<AdaptationDto>` *(zaimplementowane; kolejka przeglądu, najnowsze pierwsze)* |
| 7 | `PATCH /api/admin/adaptations/{id}/status` | 👔 | `UpdateAdaptationStatusRequest` → `AdaptationDto` *(zaimplementowane; `PENDING_REVIEW → APPROVED \| REJECTED`, odrzucenie wymaga `comment` (`400`), inne przejścia `409`)* |
| 3 | `GET /api/calls?status`, `GET /api/calls/active`, `GET /api/calls/{id}` | 🌐 | → `List<GrantCallDto>`, `GrantCallDto` *(zaimplementowane)* |
| 3 | `POST /api/calls`, `PUT`/`DELETE /api/calls/{id}` | 🛡️ | `UpsertCallRequest` → `GrantCallDto` *(zaimplementowane)* |
| 3 | `GET /api/calls/{id}/declarations?applicantType` | 🌐 | → `DeclarationsResponse` *(zaimplementowane)* |
| 3 | `POST /api/calls/{id}/applications` | 🔑 | `CreateApplicationDraftRequest?` → `ApplicationDto` *(zaimplementowane)* |
| 3 | `GET /api/applications/mine?page&size` | 🔑 | → `Page<ApplicationDto>` *(zaimplementowane)* |
| 3 | `GET /api/applications/{id}` | 🔑 | → `ApplicationDto` *(autor lub admin, zaimplementowane)* |
| 3 | `PUT /api/applications/{id}` | 🔑 | `SaveApplicationDraftRequest` → `ApplicationDto` *(tylko autor, draft, zaimplementowane)* |
| 3 | `POST /api/applications/{id}/submit` | 🔑 | → `ApplicationDto` *(tylko autor, zaimplementowane)* |
| 4 | `GET /api/innovations/{id}/test-request` | 🔑 | → `TestRequestDto` *(zaimplementowane; własne zgłoszenie ze statusem, `404` gdy brak)* |
| 4 | `PUT /api/innovations/{id}/test-request` | 🔑 | `CreateTestRequest` → `TestRequestDto` *(zaimplementowane; `200`, idempotentne: jedno zgłoszenie na użytkownika, powtórzenie podmienia notatkę, dopóki admin nie rozpatrzy zgłoszenia – potem `409`)* |
| 4 | `GET /api/innovations/{id}/feedback` | 🔑 | → `FeedbackDto` *(zaimplementowane; własna ocena, `404` gdy brak)* |
| 4 | `PUT /api/innovations/{id}/feedback` | 🔑 | `CreateFeedbackRequest` → `FeedbackDto` *(zaimplementowane; `200`, jedna ocena na użytkownika – powtórzenie podmienia ocenę)* |
| 4 | `GET /api/admin/feedback?innovationId&page&size` | 👔 | → `Page<AdminFeedbackDto>` *(zaimplementowane; oceny z komentarzami, najnowsze pierwsze)* |
| 4 | `GET /api/admin/test-requests?innovationId&status&page&size` | 👔 | → `Page<AdminTestRequestDto>` *(zaimplementowane)* |
| 4 | `PATCH /api/admin/test-requests/{id}/status` | 👔 | `UpdateTestRequestStatusRequest` → `AdminTestRequestDto` *(zaimplementowane; `NEW → ACCEPTED \| DECLINED`, inne przejścia `409`)* |
| 5 | `GET`/`POST /api/threads` | 🔑 | → `Page<ThreadDto>`; `CreateThreadRequest` → `ThreadDto` *(zaimplementowane; pracownik ROPS widzi wszystkie wątki, reszta własne)* |
| 5 | `GET`/`POST /api/threads/{id}/messages` | 🔑 | → `List<MessageDto>`; `PostMessageRequest` → `MessageDto` *(zaimplementowane; pracownik ROPS odpowiada w każdym wątku w imieniu ROPS, autor wiadomości to jego imię i nazwisko z claimu `name`)* |
| 5 | `PUT`/`DELETE /api/threads/{id}/assignment` | 👔 | → `ThreadDto` *(zaimplementowane; `PUT` przejmuje wątek, idempotentnie dla opiekuna, `409` gdy obsługuje go ktoś inny; `DELETE` oddaje go: opiekun albo admin, inaczej `403`; `ThreadDto` ma `assigneeName` i `assignedToMe`, a nazwę opiekuna widzi też autor)* |
| 5 | `GET /api/reply-templates` | 👔 | → `List<ReplyTemplateDto>` *(zaimplementowane; gotowe odpowiedzi do wstawienia w wiadomość, lista stała z `:core`, ta sama w demo)* |
| 5 | `GET /api/notifications?unreadOnly&page&size`, `GET /api/notifications/stream` | 🔑 | → `Page<NotificationDto>`; SSE stream *(zaimplementowane)* |
| 5 | `POST /api/notifications/{id}/read` | 🔑 | → `204` *(zaimplementowane)* |
| 6 | `GET /api/admin/trends?months` | 🛡️ | → `TrendsDto` *(zaimplementowane; `months` 1..60, domyślnie 6, poza zakresem `400`; gminy i frazy poniżej `privacyThreshold` zgłoszeń są pomijane)* |
| 6 | `GET /api/admin/summary` | 👔 | → `AdminSummaryDto` *(zaimplementowane)* |
| 6 | `GET /api/admin/ideas?status&page&size` | 👔 | → `Page<IdeaDto>` *(zaimplementowane)* |
| 6 | `PATCH /api/admin/ideas/{id}/status` | 👔 | `UpdateIdeaStatusRequest` → `IdeaDto` *(zaimplementowane)* |
| 6 | `GET /api/admin/applications?callId&status&page&size` | 🛡️ | → `Page<ApplicationDto>` *(zaimplementowane)* |

`InnovationDto` i `UpsertInnovationRequest` mają opcjonalne sekcje narracyjne zgodne z formularzem aplikacyjnym ROPS
(`innovativeness` – pkt 4, `problemDiagnosis` – pkt 5, `audienceDescription` – pkt 6, `expectedChange` – pkt 7,
`futureVision` – pkt 8; pkt 3 to `description`). Dane pomysłodawcy z formularza nie są częścią biblioteki.

### Asystent kreatora (strumień zdarzeń)

Obie trasy `…/assist` odpowiadają w jednej z dwóch postaci, zależnie od nagłówka `Accept`:

- domyślnie **JSON**: `AssistResponse` po zakończeniu generowania (typowo 9–14 s na lokalnym modelu);
- `Accept: text/event-stream`: **SSE**, jedno zdarzenie na część odpowiedzi (`event:` to nazwa zdarzenia, `data:` to JSON
  `AssistEvent` z polem `type`): `similar` (natychmiast, to część deterministyczna), `suggestion` (po jednej na gotową
  podpowiedź) albo `flow`, na końcu zawsze `done` z całą `AssistResponse`. `EventSource` w przeglądarce nie wysyła
  nagłówka `Authorization`, więc klient `hubmi-client` czyta strumień sam: `hubApi.assistant.assistDraft(idea, mode, listener)`
  zwraca `AssistStreamJs` z `result` (`Promise<ApiResult<AssistResponseJs>>`) i `cancel()`.

**Middleman** działa tak samo: `Accept: text/event-stream` daje zdarzenia `step` (po jednym na krok planu, gdy model go
napisze) i na końcu `done` z zapisanym planem albo `failed` z `ErrorResponse`, gdy plan powstał, ale nie udało się go
zapisać (błąd bazy; w JSON to `500`). Plan zapisuje się dopiero, gdy przejdzie walidację, więc zapisany jest zawsze
kompletny; `aiGenerated` jest zawsze `true`, a `plan.exceedsBudget` oznacza koszt wyższy niż budżet instytucji. Strumień
co kilka sekund wysyła linię komentarza (`: keep-alive`), bo między zdarzeniami bywa dużo ciszy (model zajęty albo
ładowany), a Netty zamyka milczące połączenie po 10 s; klienci ją pomijają.

Błędy znane przed generowaniem (401, 400, 403, 404) to zwykłe odpowiedzi `ErrorResponse` w obu postaciach. Niedostępny
albo wolny model **nie** jest błędem: odpowiedź ma `aiStatus` (`OK`, `UNAVAILABLE`, `INVALID_OUTPUT`, `NOT_REQUESTED`), a
`similar` i `noveltyHint` są w niej zawsze. `aiGenerated` jest zawsze `true`: wszystko spod `suggestions` i `flow` trzeba
pokazać jako wygenerowane przez AI.

`expert` to urzędnik ROPS z węższymi uprawnieniami niż `admin`: moderuje pomysły, zgłoszenia testów, opinie i plany Middlemana, widzi wskaźniki „wymaga uwagi” i odpowiada mieszkańcom, ale nie widzi trendów (`/api/admin/trends`) i nie zarządza treścią (innowacje, wyzwania, materiały, nabory). W froncie to „Panel eksperta” pod `/ekspert`.

Do ustalenia przy implementacji: autoryzacja „autor lub admin" dla `GET /api/ideas/{id}` i wątków (wymaga sprawdzenia
właściciela, nie tylko roli), limity (rate limiting) dla `POST /api/matches`.
