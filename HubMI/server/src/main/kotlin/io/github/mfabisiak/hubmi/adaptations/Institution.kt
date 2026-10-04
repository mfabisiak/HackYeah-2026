package io.github.mfabisiak.hubmi.adaptations

import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType
import kotlinx.serialization.Serializable

/** The institution a plan was written for, as stored with it. */
@Serializable
data class Institution(
    val type: InstitutionType,
    val staffCount: Int,
    val budgetPln: Int,
    val context: String,
)

fun InstitutionDraft.toInstitution(): Institution =
    Institution(type = type, staffCount = staffCount.value, budgetPln = budget.pln, context = context.value)

fun Institution.toDto(): InstitutionProfile =
    InstitutionProfile(type = type, staffCount = staffCount, budgetPln = budgetPln, context = context)
