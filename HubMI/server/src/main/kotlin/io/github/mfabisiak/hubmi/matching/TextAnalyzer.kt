package io.github.mfabisiak.hubmi.matching

import java.text.Normalizer
import java.util.Locale

/**
 * Polish text normalisation shared by the index and the queries: lower-casing, tokenising, stop-word removal,
 * stemming and diacritics folding (so "samotność" and "samotnosc" meet in the same stem).
 */
class TextAnalyzer(
    private val stemmer: Stemmer,
) {
    fun analyze(text: String): List<Token> =
        text
            .lowercase(POLISH)
            .split(NON_WORD)
            .filter { it.length >= MIN_WORD_LENGTH && it !in STOP_WORDS }
            .map { Token(surface = it, stem = fold(stemmer.stem(it))) }

    fun stems(text: String): List<String> = analyze(text).map(Token::stem)

    private companion object {
        const val MIN_WORD_LENGTH = 2
        val POLISH: Locale = Locale.forLanguageTag("pl")
        val NON_WORD = Regex("""[^\p{L}\p{N}]+""")
        val DIACRITICS = Regex("""\p{Mn}+""")

        val STOP_WORDS =
            setOf(
                "i",
                "w",
                "z",
                "na",
                "do",
                "od",
                "po",
                "za",
                "o",
                "u",
                "ze",
                "we",
                "nad",
                "pod",
                "przy",
                "bez",
                "dla",
                "oraz",
                "ale",
                "lub",
                "albo",
                "czy",
                "że",
                "to",
                "ten",
                "ta",
                "te",
                "tym",
                "tego",
                "się",
                "nie",
                "jest",
                "są",
                "był",
                "była",
                "było",
                "być",
                "ma",
                "mam",
                "mają",
                "mamy",
                "mój",
                "moja",
                "moje",
                "moi",
                "nasz",
                "nasza",
                "nasze",
                "jak",
                "jako",
                "co",
                "który",
                "która",
                "które",
                "tak",
                "też",
                "tylko",
                "bardzo",
                "już",
                "jeszcze",
                "może",
                "można",
                "trzeba",
                "chcę",
                "chce",
                "chcemy",
                "będzie",
                "gdy",
                "gdzie",
                "kiedy",
                "przez",
                "ich",
                "jej",
                "jego",
                "mu",
                "mi",
                "go",
                "ją",
                "im",
                "nam",
                "nas",
                "mnie",
                "sam",
                "sama",
                "sami",
                "oni",
                "ona",
                "on",
                "my",
                "ja",
                "ty",
                "wy",
                "bo",
                "więc",
                "żeby",
                "aby",
                "by",
                "tu",
                "tam",
                "każdy",
                "wszystko",
                "wszyscy",
            )

        fun fold(word: String): String =
            Normalizer
                .normalize(word, Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .replace('ł', 'l')
    }
}
