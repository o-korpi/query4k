package io.query4k

import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.jetbrains.annotations.ApiStatus


@ApiStatus.Internal
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

@ApiStatus.Internal
fun Array<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })

@ApiStatus.Internal
fun Iterable<*>.toJsonArray() = JsonArray(map { it.toJsonElement() })

@ApiStatus.Internal
fun Map<*, *>.toJsonObject() = JsonObject(mapKeys { it.key.toString() }.mapValues { it.value.toJsonElement() })

//fun Json.encodeToString(vararg pairs: Pair<*, *>) = encodeToString(pairs.toMap().toJsonElement())
@ApiStatus.Internal
inline fun <reified A> Map<String, Any>.toType(): A = this
    .mapValues { it.value.toJsonElement() }
    .let { Json.encodeToString(serializer(), it) }
    .let { Json.decodeFromString<A>(it) }

// TODO: this probably can't handle UUID? Test when possible
@ApiStatus.Internal
inline fun <reified A> Any.singleToType(): A = this.let { Json.decodeFromString<A>(this.toString()) }
