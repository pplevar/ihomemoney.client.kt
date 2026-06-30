package ru.levar

/**
 * A two-arm result carrying either a success payload [T] or a typed [ApiFailure].
 *
 * Kotlin's stdlib `Result` only carries a `Throwable`, so the client uses this small
 * sealed type to surface failures as values without throwing.
 */
sealed interface ApiResult<out T> {
    data class Ok<out T>(val value: T) : ApiResult<T>

    data class Err(val failure: ApiFailure) : ApiResult<Nothing>
}

/** Transforms the success payload, leaving an [ApiResult.Err] untouched. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Ok -> ApiResult.Ok(transform(value))
        is ApiResult.Err -> this
    }
