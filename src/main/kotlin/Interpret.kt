package ru.levar

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.Response
import ru.levar.api.dto.ApiEnvelope
import java.io.EOFException

/**
 * The single point where a raw HTTP response is interpreted into an [ApiResult].
 *
 * Every failure mode is decided here once: a non-2xx status, a null/empty body,
 * an API-level error on HTTP 200, and a Gson parse failure (including the empty-body
 * "End of input"). Transport errors (e.g. connection refused/timeout) are not modelled
 * by [ApiFailure] and propagate as thrown exceptions.
 *
 * Shared by the unauthenticated [HomemoneyApiClient.authenticate] call and every
 * authenticated [Session] call, so the seam stays in one place regardless of who crosses it.
 */
internal suspend fun <T : ApiEnvelope> interpret(call: suspend () -> Response<T>): ApiResult<T> {
    val response =
        try {
            call()
        } catch (e: Exception) {
            if (e is JsonParseException || e is MalformedJsonException || e is EOFException) {
                return ApiResult.Err(ApiFailure.Malformed(e))
            }
            throw e
        }

    if (!response.isSuccessful) {
        return ApiResult.Err(ApiFailure.Http(response.code()))
    }

    val body = response.body() ?: return ApiResult.Err(ApiFailure.EmptyBody)

    if (body.error.code != 0) {
        return ApiResult.Err(ApiFailure.Api(body.error.code, body.error.message))
    }

    return ApiResult.Ok(body)
}
