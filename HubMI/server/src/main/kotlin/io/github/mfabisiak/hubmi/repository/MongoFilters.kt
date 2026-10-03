package io.github.mfabisiak.hubmi.repository

import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.path
import io.github.mfabisiak.hubmi.domain.SearchQuery
import org.bson.conversions.Bson
import java.util.regex.Pattern
import kotlin.reflect.KProperty

/** Matches documents whose array field contains [value] (Mongo compares a scalar with each array element). */
fun <T : Any> KProperty<Iterable<T>>.hasElement(value: T): Bson =
    com.mongodb.client.model.Filters
        .eq(path(), value)

/** Case-insensitive "contains" match of the query (taken literally, not as a regex) on a text field. */
fun SearchQuery.matches(field: KProperty<String?>): Bson = Filters.regex(field, pattern())

/** Same as [matches], for a field holding a list of strings. */
fun SearchQuery.matchesAny(field: KProperty<Iterable<String?>>): Bson = Filters.regex(field, pattern())

private fun SearchQuery.pattern(): Pattern = Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE)
