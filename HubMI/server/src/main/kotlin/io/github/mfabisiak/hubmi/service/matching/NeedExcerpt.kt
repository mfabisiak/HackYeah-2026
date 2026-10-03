package io.github.mfabisiak.hubmi.service.matching

private const val MAX_EXCERPT_LENGTH = 120
private const val ELLIPSIS = "…"

/** At most 120 characters of an (already cleaned) need, cut at a word boundary. */
fun excerptOf(text: String): String =
    when {
        text.length <= MAX_EXCERPT_LENGTH -> {
            text
        }

        else -> {
            text
                .take(MAX_EXCERPT_LENGTH)
                .substringBeforeLast(' ', missingDelimiterValue = text.take(MAX_EXCERPT_LENGTH))
                .trimEnd(',', '.', ';', ':', ' ') + ELLIPSIS
        }
    }
