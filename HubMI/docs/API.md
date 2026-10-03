# Kontrakt API (wstępny)

Źródłem prawdy jest kod: trasy to `@Resource` w [core/.../api](../core/src/commonMain/kotlin/io/github/mfabisiak/hubmi/api),
każdy plik grupuje zasoby i DTO jednego modułu. Serwer na razie odpowiada na nie `501 Not Implemented`
([ContractStubs.kt](../server/src/main/kotlin/io/github/mfabisiak/hubmi/contract/ContractStubs.kt)), ale **polityka dostępu
jest już egzekwowana** (brak tokena → `401`, brak roli → `403`). Realizując moduł, zastępujemy atrapę prawdziwą trasą.

Konwencje: identyfikatory to `String` (hex), czas to ISO-8601 w `String`, listy paginowane (`page`, `size`),
błąd to zawsze `ErrorResponse(code, message)`. Dostęp: 🌐 publiczny · 🔑 zalogowany · 🛡️ rola `admin`.

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
| 3 | `GET /api/calls?status`, `GET /api/calls/active`, `GET /api/calls/{id}` | 🌐 | → `List<GrantCallDto>`, `GrantCallDto` *(zaimplementowane)* |
| 3 | `POST /api/calls`, `PUT`/`DELETE /api/calls/{id}` | 🛡️ | `UpsertCallRequest` → `GrantCallDto` *(zaimplementowane)* |
| 3 | `GET /api/calls/{id}/declarations?applicantType` | 🌐 | → `DeclarationsResponse` *(zaimplementowane)* |
| 3 | `POST /api/calls/{id}/applications` | 🔑 | `CreateApplicationDraftRequest?` → `ApplicationDto` *(zaimplementowane)* |
| 3 | `GET /api/applications/mine?page&size` | 🔑 | → `Page<ApplicationDto>` *(zaimplementowane)* |
| 3 | `GET /api/applications/{id}` | 🔑 | → `ApplicationDto` *(autor lub admin, zaimplementowane)* |
| 3 | `PUT /api/applications/{id}` | 🔑 | `SaveApplicationDraftRequest` → `ApplicationDto` *(tylko autor, draft, zaimplementowane)* |
| 3 | `POST /api/applications/{id}/submit` | 🔑 | → `ApplicationDto` *(tylko autor, zaimplementowane)* |
| 4 | `POST /api/innovations/{id}/test-requests` | 🔑 | `CreateTestRequest` → `TestRequestDto` |
| 4 | `POST /api/innovations/{id}/feedback` | 🔑 | `CreateFeedbackRequest` → `FeedbackDto` |
| 5 | `GET`/`POST /api/threads` | 🔑 | → `Page<ThreadDto>`; `CreateThreadRequest` → `ThreadDto` |
| 5 | `GET`/`POST /api/threads/{id}/messages` | 🔑 | → `List<MessageDto>`; `PostMessageRequest` → `MessageDto` |
| 5 | `GET /api/notifications?unreadOnly&page&size` | 🔑 | → `Page<NotificationDto>` |
| 5 | `POST /api/notifications/{id}/read` | 🔑 | → `204` |
| 6 | `GET /api/admin/trends?months` | 🛡️ | → `TrendsDto` |
| 6 | `GET /api/admin/ideas?status&page&size` | 🛡️ | → `Page<IdeaDto>` *(zaimplementowane)* |
| 6 | `PATCH /api/admin/ideas/{id}/status` | 🛡️ | `UpdateIdeaStatusRequest` → `IdeaDto` *(zaimplementowane)* |
| 6 | `GET /api/admin/applications?callId&status&page&size` | 🛡️ | → `Page<ApplicationDto>` *(zaimplementowane)* |

`InnovationDto` i `UpsertInnovationRequest` mają opcjonalne sekcje narracyjne zgodne z formularzem aplikacyjnym ROPS
(`innovativeness` – pkt 4, `problemDiagnosis` – pkt 5, `audienceDescription` – pkt 6, `expectedChange` – pkt 7,
`futureVision` – pkt 8; pkt 3 to `description`). Dane pomysłodawcy z formularza nie są częścią biblioteki.

Do ustalenia przy implementacji: autoryzacja „autor lub admin" dla `GET /api/ideas/{id}` i wątków (wymaga sprawdzenia
właściciela, nie tylko roli), rola `expert` w Keycloaku dla modułu 5, limity (rate limiting) dla `POST /api/matches`.
