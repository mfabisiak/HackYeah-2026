package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.innovations.InnovationItem

/**
 * The text that stands for an innovation in the vector space: the fields users write about, from the most to the
 * least telling ones. Equal text means an equal vector, which is how [VectorIndex] avoids embedding it again.
 */
fun InnovationItem.embeddingText(): String =
    listOfNotNull(
        title,
        summary,
        keywords.takeIf { it.isNotEmpty() }?.joinToString(", "),
        problemDiagnosis,
        audienceDescription,
        description,
        expectedChange,
    ).joinToString("\n")
