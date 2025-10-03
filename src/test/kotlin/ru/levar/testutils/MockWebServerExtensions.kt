package ru.levar.testutils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.TimeUnit

/**
 * Extension functions for MockWebServer to simplify test setup
 * Provides fluent API for common test scenarios
 *
 * Enqueues a successful response with JSON body
 */
fun MockWebServer.enqueueSuccess(
    body: String,
    code: Int = 200,
    contentType: String = "application/json",
): MockWebServer {
    this.enqueue(
        MockResponse()
            .setResponseCode(code)
            .setBody(body)
            .setHeader("Content-Type", contentType),
    )
    return this
}

/**
 * Enqueues an error response with specified HTTP code
 */
fun MockWebServer.enqueueError(
    code: Int = 500,
    body: String = "Server Error",
    contentType: String = "text/plain",
): MockWebServer {
    this.enqueue(
        MockResponse()
            .setResponseCode(code)
            .setBody(body)
            .setHeader("Content-Type", contentType),
    )
    return this
}

/**
 * Enqueues a delayed response to test timeout scenarios
 */
fun MockWebServer.enqueueDelayed(
    body: String,
    delaySeconds: Long = 1,
    code: Int = 200,
): MockWebServer {
    this.enqueue(
        MockResponse()
            .setResponseCode(code)
            .setBody(body)
            .setBodyDelay(delaySeconds, TimeUnit.SECONDS),
    )
    return this
}

/**
 * Enqueues multiple responses with the same body
 */
fun MockWebServer.enqueueMultiple(
    count: Int,
    body: String,
    code: Int = 200,
): MockWebServer {
    repeat(count) {
        this.enqueueSuccess(body, code)
    }
    return this
}

/**
 * Takes a request and verifies it contains expected path and parameters
 */
fun RecordedRequest.verifyPath(
    expectedPath: String,
    expectedParams: Map<String, String> = emptyMap(),
): RecordedRequest {
    assert(this.path?.contains(expectedPath) == true) {
        "Expected path to contain '$expectedPath' but was '${this.path}'"
    }

    expectedParams.forEach { (key, value) ->
        assert(this.path?.contains("$key=$value") == true) {
            "Expected parameter '$key=$value' in path but was '${this.path}'"
        }
    }

    return this
}

/**
 * Verifies the request was a GET request
 */
fun RecordedRequest.verifyGet(): RecordedRequest {
    assert(this.method == "GET") {
        "Expected GET request but was ${this.method}"
    }
    return this
}

/**
 * Verifies the request was a POST request
 */
fun RecordedRequest.verifyPost(): RecordedRequest {
    assert(this.method == "POST") {
        "Expected POST request but was ${this.method}"
    }
    return this
}

/**
 * Verifies the request contains a header with expected value
 */
fun RecordedRequest.verifyHeader(
    headerName: String,
    expectedValue: String,
): RecordedRequest {
    val actualValue = this.getHeader(headerName)
    assert(actualValue == expectedValue) {
        "Expected header '$headerName' to be '$expectedValue' but was '$actualValue'"
    }
    return this
}

/**
 * Verifies the request contains a query parameter
 */
fun RecordedRequest.verifyQueryParam(
    paramName: String,
    expectedValue: String? = null,
): RecordedRequest {
    val path = this.path ?: ""
    assert(path.contains("$paramName=")) {
        "Expected query parameter '$paramName' in path but was not found"
    }

    if (expectedValue != null) {
        assert(path.contains("$paramName=$expectedValue")) {
            "Expected query parameter '$paramName=$expectedValue' but value differs"
        }
    }

    return this
}

/**
 * Verifies the request does not contain a query parameter
 */
fun RecordedRequest.verifyNoQueryParam(paramName: String): RecordedRequest {
    val path = this.path ?: ""
    assert(!path.contains("$paramName=")) {
        "Expected no query parameter '$paramName' but found it in path"
    }
    return this
}
