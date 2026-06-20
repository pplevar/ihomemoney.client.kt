package ru.levar.api.dto

import ru.levar.domain.ErrorType

/**
 * Common contract for every API response envelope.
 *
 * Every endpoint wraps its payload alongside an [ErrorType]. Exposing it through
 * a single interface lets the client interpret HTTP status and API error code at
 * one point, regardless of the concrete envelope.
 */
internal interface ApiEnvelope {
    val error: ErrorType
}
