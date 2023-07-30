package io.query4k

import arrow.core.Either
import arrow.core.flatten
import arrow.core.raise.either
import kotlinx.coroutines.*
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.result.ResultIterable
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.jvm.optionals.getOrNull


/** Main class for query4k. Create using the `Query4k.create(...)` methods.
 * Does single queries. Use the `.transaction { ... }` method to create transactions. */
class Query4k private constructor(private val jdbi: Jdbi) {
    companion object {
        /** Create an instance from a JDBI instance. */
        @Suppress("unused")
        fun create(jdbi: Jdbi): Query4k = Query4k(jdbi)

        /** Create an instance from a data source, such as a Hikari CP data source. */
        @Suppress("unused")
        fun create(dataSource: DataSource): Query4k = Query4k(Jdbi.create(dataSource))

        /** Create an instance from a URL. */
        @Suppress("unused")
        fun create(
            url: String,
            username: String? = null,
            password: String? = null
        ): Query4k = Query4k(
            if (username == null || password == null)
                Jdbi.create(url)
            else
                Jdbi.create(url, username, password)
        )
    }

    internal fun execute(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Either<SQLException, Int> = Either.catchOrThrow<SQLException, Int> {
        handle.createUpdate(sql)
            .bindMap(params)
            .execute()
    }

    internal fun executeAndReturnAutoGeneratedKeys(
        handle: Handle, sql: String, params: Map<String, Any>?
    ) = Either.catchOrThrow<SQLException, List<Map<String, Any>>> {
        handle.createUpdate(sql)
            .bindMap(params)
            .executeAndReturnGeneratedKeys()
            .mapToMap()
            .list()
    }

    /** Executes a single SQL statement.
     * Example use:
     *
     * ```
     *q4k.execute(
     *   "UPDATE users SET email=:email WHERE id=:id",
     *   mapOf("id" to 0, "email" to "example@email.com")
     *)
     * ```
     *
     * @return Number of affected rows */
    fun execute(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, Int> = either {
        handle().use {
            execute(it, sql, params).bind()
        }
    }

    /** Executes, and retrieves auto-generated keys */
    fun executeAndReturnAutoGeneratedKeys(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, List<Map<String, Any>>> = either {
        handle().use {
            executeAndReturnAutoGeneratedKeys(it, sql, params).bind()
        }
    }

    fun query(
        handle: Handle,
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, List<Map<String, Any>>> = Either.catchOrThrow {
        handle.createQuery(sql)
            .bindMap(params)
            .mapToMap().list()
    }

    fun queryFirst(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Either<SQLException, Map<String, Any>?> = Either.catchOrThrow {
        handle.createQuery(sql)
            .bindMap(params)
            .mapToMap()
            .findFirst()
            .getOrNull()
    }

    private fun ResultIterable<Map<String, Any>>.safeFindOnly() = Either.catchOrThrow<IllegalStateException, Map<String, Any>> {
        this.findOnly()
    }.mapLeft { QueryOnlyError.IllegalStateError }

    fun queryOnly(
        handle: Handle,
        sql: String,
        params: Map<String, Any>?
    ): Either<QueryOnlyError, Map<String, Any>> = Either.catchOrThrow<SQLException, Either<QueryOnlyError.IllegalStateError, Map<String, Any>>> {
        handle.createQuery(sql)
            .bindMap(params)
            .mapToMap()
            .safeFindOnly()
    }.mapLeft {
        QueryOnlyError.ConnectionError
    }.flatten()

    /** Gets _all_ results from a query, and maps them to the target model. Will cause raised exceptions on
     * invalid target model. Example use:
     *```kotlin
     * @Serializable
     * data class User(val email: String)
     *
     * q4k.query<User>("SELECT * FROM users")
     * // or...
     * q4k.query<User>(
     *   "SELECT * FROM users WHERE email=:email",
     *   mapOf("email" to "email")
     * )
     *```
     * Always use the second variation when dealing with variable inputs! This prevents SQL injection.
     *
     * @param sql SQL query. Do not use string interpolation or concatenation.
     * @param params SQL injection safe parameter inputs. Mapped to in `sql` by the use of `:myparam`.
     * @return All results mapped to target data class `A`, or wrapped `SQLException`.
     * */
    inline fun <reified A> query(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, List<A>> = either {
        handle().use { handle ->
            val results = query(handle, sql, params).bind()
            results.map { row -> row.toType<A>() }
        }
    }

    /** Gets the first result from a query, and maps it to `A`. Remaining results are ignored.
     * Similar to `query`, see documentation there. */
    inline fun <reified A> queryFirst(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, A?> = either {
        handle().use { handle ->
            queryFirst(handle, sql, params).bind()
                ?.toType<A>()
        }
    }

    /** Gets one, and only one result from the query. If there are less or more `QueryOnlyError` is returned.
     * Other than that, similar to `query`. See documentation there. */
    inline fun <reified A> queryOnly(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<QueryOnlyError, A> = either {
        handle().use {  handle ->
            queryOnly(handle, sql, params)
                .bind()
                .toType<A>()
        }
    }

    fun handle(): Handle = jdbi.open()

    fun transaction(operations: suspend Transaction.() -> Unit) {
        jdbi.open().useTransaction<SQLException> {
            val transaction = Transaction(this, it)
            runBlocking {
                transaction.operations()
            }
        }
    }
}

