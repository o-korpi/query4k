package io.query4k.serializers

import kotlinx.uuid.UUID

fun UUID.toSQLParseable(): String = "'${this}'"