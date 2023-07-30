package io.query4k

import arrow.core.Either
import arrow.core.raise.either
import org.jdbi.v3.core.Handle
import java.sql.SQLException

class Transaction internal constructor(val query4k: Query4k, val handle: Handle) {
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
    ): Either<SQLException, Int> = query4k.execute(handle, sql, params)

    /** Executes, and retrieves auto-generated keys */
    fun executeAndReturnAutoGeneratedKeys(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, List<Map<String, Any>>> =
        query4k.executeAndReturnAutoGeneratedKeys(handle, sql, params)

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
        query4k.query(handle, sql, params).bind()  // todo: could be refactored
            .map { row -> row.toType<A>() }
    }

    /** Gets the first result from a query, and maps it to `A`. Remaining results are ignored.
     * Similar to `query`, see documentation there. */
    inline fun <reified A> queryFirst(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<SQLException, A?> = either {
        query4k.queryFirst(handle, sql, params).bind()
            ?.toType<A>()
    }

    /** Gets one, and only one result from the query. If there are less or more `QueryOnlyError` is returned.
     * Other than that, similar to `query`. See documentation there. */
    inline fun <reified A> queryOnly(
        sql: String,
        params: Map<String, Any>? = null
    ): Either<QueryOnlyError, A> = either {
        query4k.queryOnly(handle, sql, params)
            .bind()
            .toType<A>()
    }
}