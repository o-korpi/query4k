package io.query4k

import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

// Functions for converting Any to JsonElement. Would've been private if it was possible with inline functions.


fun Any?.toJsonElement(): JsonElement = when (this) {
    is Number -> JsonPrimitive(this.toString())  // Convert all numbers to strings to make BigDecimal work
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> this.toJsonArray()
    is List<*> -> this.toJsonArray()
    is Map<*, *> -> this.toJsonObject()
    is JsonElement -> this
    else -> JsonPrimitive(this?.toString())
}

fun Array<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Iterable<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Map<*, *>.toJsonObject() = JsonObject(mapKeys { it.key.toString() }.mapValues { it.value.toJsonElement() })

//fun Json.encodeToString(vararg pairs: Pair<*, *>) = encodeToString(pairs.toMap().toJsonElement())
inline fun <reified A> Map<String, Any>.toType(): A = this
    .mapValues { it.value.toJsonElement() }
    .let { Json.encodeToString(serializer(), it) }
    .let { Json.decodeFromString<A>(it) }

// TODO: this probably can't handle BigDecimal?
inline fun <reified A> Any.singleToType(): A = this.let { Json.decodeFromString<A>(this.toString()) }
