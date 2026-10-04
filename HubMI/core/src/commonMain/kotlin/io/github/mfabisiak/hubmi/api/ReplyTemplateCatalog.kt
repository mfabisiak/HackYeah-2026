package io.github.mfabisiak.hubmi.api

/** Replies ROPS officials reach for most often; shared by the server and the demo, which has no server to ask. */
object ReplyTemplateCatalog {
    val all: List<ReplyTemplateDto> =
        listOf(
            ReplyTemplateDto(
                id = "potwierdzenie",
                title = "Potwierdzenie przyjęcia zgłoszenia",
                text =
                    "Dzień dobry,\n\ndziękujemy za zgłoszenie. Przyjęliśmy je i zajmiemy się nim w ciągu 5 dni " +
                        "roboczych. W razie potrzeby wrócimy z dodatkowymi pytaniami.\n\nZ poważaniem",
            ),
            ReplyTemplateDto(
                id = "uzupelnienie",
                title = "Prośba o uzupełnienie informacji",
                text =
                    "Dzień dobry,\n\naby ocenić Państwa pomysł, potrzebujemy kilku dodatkowych informacji: " +
                        "do kogo jest skierowany, na jakim jest etapie i jakie zasoby są już zapewnione. " +
                        "Prosimy o odpowiedź w tym wątku.\n\nZ poważaniem",
            ),
            ReplyTemplateDto(
                id = "nabor",
                title = "Informacja o naborze wniosków",
                text =
                    "Dzień dobry,\n\naktualne nabory wniosków znajdą Państwo w zakładce »Nabory«. Przy każdym " +
                        "naborze jest opis wymagań i terminów, a kreator pomoże przygotować wniosek krok po " +
                        "kroku.\n\nZ poważaniem",
            ),
            ReplyTemplateDto(
                id = "testowanie",
                title = "Zaproszenie do testowania innowacji",
                text =
                    "Dzień dobry,\n\nzapraszamy do przetestowania wybranej innowacji. Na jej stronie można " +
                        "zgłosić chęć udziału w testach, a po nich ocenić rozwiązanie i podzielić się uwagami.\n\n" +
                        "Z poważaniem",
            ),
            ReplyTemplateDto(
                id = "przekazanie",
                title = "Przekazanie sprawy do właściwej jednostki",
                text =
                    "Dzień dobry,\n\nsprawa wykracza poza zakres działania ROPS, dlatego przekazujemy ją do " +
                        "właściwej jednostki. Poinformujemy Państwa, gdy otrzymamy odpowiedź.\n\nZ poważaniem",
            ),
            ReplyTemplateDto(
                id = "zamkniecie",
                title = "Podziękowanie i zamknięcie sprawy",
                text =
                    "Dzień dobry,\n\ndziękujemy za rozmowę. Uznajemy sprawę za zakończoną. Jeśli pojawią się " +
                        "nowe pytania, prosimy o wiadomość w nowym wątku.\n\nZ poważaniem",
            ),
        )
}
