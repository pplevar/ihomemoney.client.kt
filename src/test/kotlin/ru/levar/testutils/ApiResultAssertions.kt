package ru.levar.testutils

import ru.levar.ApiFailure
import ru.levar.ApiResult

/**
 * Unwraps a successful [ApiResult], failing the test with a descriptive message when
 * it is an [ApiResult.Err]. Keeps success-path assertions reading like before.
 */
fun <T> ApiResult<T>.expectOk(): T =
    when (this) {
        is ApiResult.Ok -> value
        is ApiResult.Err -> throw AssertionError("Expected ApiResult.Ok but was Err($failure)")
    }

/**
 * Unwraps a failed [ApiResult], returning the [ApiFailure] to branch on, and failing the
 * test with a descriptive message when it is an [ApiResult.Ok].
 */
fun ApiResult<*>.expectErr(): ApiFailure =
    when (this) {
        is ApiResult.Ok -> throw AssertionError("Expected ApiResult.Err but was Ok($value)")
        is ApiResult.Err -> failure
    }
