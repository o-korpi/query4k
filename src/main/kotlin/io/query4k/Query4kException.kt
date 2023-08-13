package io.query4k

interface Query4kException

sealed class QueryOnlyException : Query4kException {
    data object ConnectionException : QueryOnlyException()
    data object IllegalStateException : QueryOnlyException()
}
