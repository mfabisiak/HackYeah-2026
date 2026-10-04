package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.AdaptationStatus

/** A plan is reviewed once: `PENDING_REVIEW` becomes `APPROVED` or `REJECTED`, and the decision is final. */
fun AdaptationStatus.canMoveTo(next: AdaptationStatus): Boolean =
    when (this) {
        AdaptationStatus.PENDING_REVIEW -> next == AdaptationStatus.APPROVED || next == AdaptationStatus.REJECTED
        AdaptationStatus.APPROVED, AdaptationStatus.REJECTED -> false
    }
