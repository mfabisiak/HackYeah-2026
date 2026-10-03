package io.github.mfabisiak.hubmi.web

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull

/** Parses an enum passed from JS as its name; an unknown name short-circuits the surrounding `either` with `INVALID_ARGUMENT`. */
internal inline fun <reified E : Enum<E>> Raise<ApiErrorJs>.enumOf(
    value: String,
    argument: String,
): E = ensureNotNull(enumValues<E>().firstOrNull { it.name == value }) { invalidArgument(argument, value) }

internal inline fun <reified E : Enum<E>> Raise<ApiErrorJs>.enumOrNull(
    value: String?,
    argument: String,
): E? = value?.let { enumOf<E>(it, argument) }

internal inline fun <reified E : Enum<E>> Raise<ApiErrorJs>.enumsOf(
    values: Array<String>,
    argument: String,
): List<E> = values.map { enumOf<E>(it, argument) }

internal fun <E : Enum<E>> List<E>.names(): Array<String> = map { it.name }.toTypedArray()

internal fun invalidArgument(
    argument: String,
    value: String,
): ApiErrorJs =
    ApiErrorJs(CLIENT_ERROR_STATUS, "INVALID_ARGUMENT", "Nieprawidłowa wartość '$value' argumentu '$argument'")
