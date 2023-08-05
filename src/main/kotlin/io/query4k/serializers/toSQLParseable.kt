package io.query4k.serializers

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.uuid.UUID

fun LocalTime.toSQLParseable() = this.toString()
fun LocalDateTime.toSQLParseable() = this.toString()
fun UUID.toSQLParseable(): String = "'${this}'"
