package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType
import io.github.mfabisiak.hubmi.common.asNel

/** An [InstitutionProfile] whose numbers are in range and whose text has been trimmed. */
data class InstitutionDraft(
    val type: InstitutionType,
    val staffCount: StaffCount,
    val budget: Budget,
    val context: LocalContext,
) {
    companion object {
        fun parse(profile: InstitutionProfile): EitherNel<FieldError, InstitutionDraft> =
            Either.zipOrAccumulate(
                StaffCount.parse(profile.staffCount).asNel(),
                Budget.parse(profile.budgetPln).asNel(),
                LocalContext.parse(profile.context).asNel(),
            ) { staffCount, budget, context -> InstitutionDraft(profile.type, staffCount, budget, context) }
    }
}
