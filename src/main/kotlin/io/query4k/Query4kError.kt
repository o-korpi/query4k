package io.query4k

interface Query4kError

sealed class QueryOnlyError : Query4kError {
    data object ConnectionError : QueryOnlyError()
    data object IllegalStateError : QueryOnlyError()
}
