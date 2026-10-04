package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType

object AdaptationFixtures {
    val institution =
        InstitutionProfile(
            type = InstitutionType.LOCAL_GOVERNMENT,
            staffCount = 6,
            budgetPln = 60_000,
            context = "Gmina wiejska, 8 tys. mieszkańców, GOPS bez własnego transportu.",
        )

    /** The shape of an answer of Bielik 4.5B to a ride service for seniors, with the wording of a recording. */
    fun planAnswer(costPln: Int = 45_000): String =
        listOf(
            """{"serviceForm":"Gminna usługa przewozu seniorów do przychodni, koordynowana przez GOPS","steps":[""",
            """{"title":"Rozeznanie potrzeb","description":"GOPS zbiera zgłoszenia seniorów, którzy nie mają dojazdu.","startMonth":0},""",
            """{"title":"Rekrutacja wolontariuszy","description":"Nabór kierowców z organizacji lokalnych.","startMonth":1},""",
            """{"title":"Szkolenie i ubezpieczenie","description":"Pierwsza pomoc i ubezpieczenie przejazdów.","startMonth":2},""",
            """{"title":"Start pilotażu","description":"Linia telefoniczna i pierwsze przejazdy.","startMonth":3}],""",
            """"requiredResources":["Koordynator w GOPS","Wolontariusze z samochodami","Ubezpieczenie przejazdów"],""",
            """"risks":["Za mało kierowców","Koszty paliwa"],"estimatedCostPln":$costPln}""",
        ).joinToString("")
}
