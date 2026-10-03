package io.github.mfabisiak.hubmi.service.matching

import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup

fun SocialArea.polishLabel(): String =
    when (this) {
        SocialArea.AGING -> "starzenie się i seniorzy"
        SocialArea.MENTAL_HEALTH -> "zdrowie psychiczne"
        SocialArea.LONELINESS -> "samotność i izolacja"
        SocialArea.DIGITAL_EXCLUSION -> "wykluczenie cyfrowe"
        SocialArea.SERVICE_ACCESS -> "dostęp do usług"
        SocialArea.COORDINATION -> "koordynacja i współpraca"
        SocialArea.DEPOPULATION -> "wyludnienie i rozwój wsi"
        SocialArea.OTHER -> "inne wyzwania społeczne"
    }

fun TargetGroup.polishLabel(): String =
    when (this) {
        TargetGroup.SENIORS -> "seniorzy"
        TargetGroup.YOUTH -> "młodzież"
        TargetGroup.PEOPLE_WITH_DISABILITIES -> "osoby z niepełnosprawnościami"
        TargetGroup.FAMILIES -> "rodziny"
        TargetGroup.RESIDENTS -> "mieszkańcy"
        TargetGroup.NGOS -> "organizacje pozarządowe"
        TargetGroup.LOCAL_GOVERNMENTS -> "samorządy"
    }
