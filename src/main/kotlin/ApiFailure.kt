package ru.levar

/**
 * The single, typed vocabulary of ways an API call can fail.
 *
 * Failures are produced at one interpretation point (the response seam in
 * [HomemoneyApiClient]) and carried across it inside [ApiResult.Err], so callers
 * branch on the case — e.g. retry only on [Http] 503 — instead of grepping an
 * exception message.
 */
sealed interface ApiFailure {
    /** Transport succeeded but the server returned a non-2xx HTTP status. */
    data class Http(val code: Int) : ApiFailure

    /** HTTP 200 whose envelope carried a non-zero API error code. */
    data class Api(val code: Int, val message: String) : ApiFailure

    /** A successful response with no body to interpret (e.g. HTTP 204 / null body). */
    data object EmptyBody : ApiFailure

    /** The body could not be parsed as the expected JSON envelope (incl. empty-body "End of input"). */
    data class Malformed(val cause: Throwable) : ApiFailure
}
