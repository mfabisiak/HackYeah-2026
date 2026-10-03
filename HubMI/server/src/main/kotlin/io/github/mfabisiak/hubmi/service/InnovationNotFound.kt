package io.github.mfabisiak.hubmi.service

import io.github.mfabisiak.hubmi.domain.InnovationId

fun innovationNotFound(id: InnovationId): DomainError.NotFound =
    DomainError.NotFound("Nie znaleziono innowacji o ID: ${id.value.toHexString()}")
