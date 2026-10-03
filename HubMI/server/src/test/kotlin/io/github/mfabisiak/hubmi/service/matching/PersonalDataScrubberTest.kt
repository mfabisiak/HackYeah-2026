package io.github.mfabisiak.hubmi.service.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalDataScrubberTest {
    @Test
    fun peselWithValidChecksumIsReplaced() {
        assertEquals(
            "Mój PESEL to [PESEL], proszę o pomoc",
            PersonalDataScrubber.scrub("Mój PESEL to 44051401359, proszę o pomoc"),
        )
    }

    @Test
    fun elevenDigitsWithWrongChecksumAreNotAPesel() {
        assertEquals("numer 44051401358 w opisie", PersonalDataScrubber.scrub("numer 44051401358 w opisie"))
        assertFalse(PersonalDataScrubber.isValidPesel("44051401358"))
        assertTrue(PersonalDataScrubber.isValidPesel("44051401359"))
    }

    @Test
    fun phoneNumbersInCommonFormatsAreReplaced() {
        listOf(
            "501234567",
            "501 234 567",
            "501-234-567",
            "+48 501 234 567",
            "+48501234567",
            "0048 501 234 567",
            "48 501 234 567",
            "12 345 67 89",
        ).forEach { phone ->
            assertEquals(
                "zadzwoń pod [TELEFON] po południu",
                PersonalDataScrubber.scrub("zadzwoń pod $phone po południu"),
                phone,
            )
        }
    }

    @Test
    fun emailAddressesAreReplaced() {
        assertEquals(
            "piszcie na [EMAIL] albo na [EMAIL].",
            PersonalDataScrubber.scrub("piszcie na jan.kowalski+hub@gmail.com albo na ola_k@urzad.krakow.pl."),
        )
    }

    @Test
    fun everyKindIsReplacedInOneText() {
        val scrubbed =
            PersonalDataScrubber.scrub(
                "Jan, PESEL 44051401359, tel. 501 234 567, jan@example.com, mama jest chora",
            )

        assertEquals("Jan, PESEL [PESEL], tel. [TELEFON], [EMAIL], mama jest chora", scrubbed)
    }

    @Test
    fun ordinaryNumbersAndTextAreLeftAlone() {
        listOf(
            "Mama ma 85 lat i mieszka na ulicy Długiej 12/3",
            "W 2024 roku było 1500 zł zasiłku",
            "Do przychodni jest 45 km, autobus jedzie 2 razy dziennie",
            "Telefon zaufania 116 123 nie działa w nocy",
        ).forEach { assertEquals(it, PersonalDataScrubber.scrub(it)) }
    }
}
