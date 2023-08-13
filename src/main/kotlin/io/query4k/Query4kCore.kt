package io.query4k

import arrow.core.Either
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.result.ResultIterable
import org.jetbrains.annotations.ApiStatus
import kotlin.jvm.optionals.getOrNull

class Query4kCore(internal val jdbi: Jdbi) {

    internal fun execute(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Int =
        handle.createUpdate(sql)
            .bindMap(params)
            .execute()

    internal fun executeAndReturnAutoGeneratedKeys(
        handle: Handle, sql: String, params: Map<String, Any>?
    ): List<Map<String, Any>>  = handle.createUpdate(sql)
        .bindMap(params)
        .executeAndReturnGeneratedKeys()
        .mapToMap()
        .list()


    fun throwKeyMappingError(key: String, className: String): Nothing =
        throw IllegalArgumentException("Key '$key' cannot be mapped to $className")

    fun throwKeyNotFoundError(key: String): Nothing =
        throw IllegalArgumentException("There is no auto-generated key '$key' associated with this table")

    @ApiStatus.Experimental
    inline fun <reified A> executeGetKey(
        handle: Handle,
        sql: String,
        key: String,
        params: Map<String, Any>? = null
    ): A = handle.createUpdate(sql)
        .bindMap(params)
        .executeAndReturnGeneratedKeys()
        .mapToMap()
        .findOnly()[key]
        ?.let {
            runCatching {
                it.singleToType<A>()
            }.fold(
                { it },
                { throwKeyMappingError(key, A::class.toString()) }
            )
        } ?: throwKeyNotFoundError(key)

    @ApiStatus.Experimental
    inline fun <reified A> executeGetKeys(
        handle: Handle,
        sql: String,
        key: String,
        params: Map<String, Any>? = null
    ): List<A> = handle.createUpdate(sql)
        .bindMap(params)
        .executeAndReturnGeneratedKeys()
        .mapToMap()
        .list()
        .map {
            runCatching {
                it[key]?.singleToType<A>() ?: throwKeyNotFoundError(key)
            }.fold(
                { it },
                { throwKeyMappingError(key, A::class.toString()) }
            )
        }

    fun query(
        handle: Handle,
        sql: String,
        params: Map<String, Any>? = null
    ): List<Map<String, Any>> = handle
        .createQuery(sql)
        .bindMap(params)
        .mapToMap().list()

    fun queryFirst(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Map<String, Any>? = handle
        .createQuery(sql)
        .bindMap(params)
        .mapToMap()
        .findFirst()
        .getOrNull()

    private fun ResultIterable<Map<String, Any>>.safeFindOnly() = Either.catchOrThrow<IllegalStateException, Map<String, Any>> {
        this.findOnly()
    }.mapLeft { QueryOnlyException }

    fun queryOnly(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Either<QueryOnlyException, Map<String, Any>> = handle.createQuery(sql)
        .bindMap(params)
        .mapToMap()
        .safeFindOnly()
}