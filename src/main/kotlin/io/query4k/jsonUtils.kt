package io.query4k

import kotlinx.serialization.json.*

/** Functions for converting Any to JsonElement */



fun Any?.toJsonElement(): JsonElement = when (this) {
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> this.toJsonArray()
    is List<*> -> this.toJsonArray()
    is Map<*, *> -> this.toJsonObject()
    is JsonElement -> this
    else -> JsonNull
}

fun Array<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Iterable<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })
fun Map<*, *>.toJsonObject() = JsonObject(mapKeys { it.key.toString() }.mapValues { it.value.toJsonElement() })

//fun Json.encodeToString(vararg pairs: Pair<*, *>) = encodeToString(pairs.toMap().toJsonElement())