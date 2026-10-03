package io.github.mfabisiak.hubmi.innovations

import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.innovations.InnovationId

fun innovationNotFound(id: InnovationId): DomainError.NotFound =
    DomainError.NotFound("Nie znaleziono innowacji o ID: ${id.value.toHexString()}")
