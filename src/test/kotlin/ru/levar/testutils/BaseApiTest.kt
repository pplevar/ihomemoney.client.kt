package ru.levar.testutils

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import ru.levar.HomemoneyApiClient

/**
 * Base test class providing common setup for API tests
 * Uses MockWebServer for safe testing without production endpoint contact
 *
 * SAFETY: All tests use MockWebServer - NO production endpoints are contacted
 */
abstract class BaseApiTest {
    protected lateinit var mockWebServer: MockWebServer
    protected lateinit var apiClient: HomemoneyApiClient
    protected lateinit var baseUrl: String

    @BeforeEach
    open fun setup() {
        // Initialize mock server on random available port
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString()

        // Create client pointing to mock server
        apiClient = HomemoneyApiClient(baseUrl)
    }

    @AfterEach
    open fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Helper to set token on API client for authenticated requests
     */
    protected fun authenticateClient(token: String = TestDataFactory.DEFAULT_TOKEN) {
        apiClient.token = token
    }

    /**
     * Helper to enqueue a successful login response
     */
    protected fun enqueueSuccessfulLogin(
        token: String = TestDataFactory.DEFAULT_TOKEN,
        refreshToken: String = TestDataFactory.DEFAULT_REFRESH_TOKEN,
    ) {
        mockWebServer.enqueueSuccess(
            TestDataFactory.createSuccessfulAuthResponse(token, refreshToken),
        )
    }

    /**
     * Helper to enqueue a failed login response
     */
    protected fun enqueueFailedLogin(
        errorCode: Int = 401,
        errorMessage: String = "Authentication failed",
    ) {
        mockWebServer.enqueueSuccess(
            TestDataFactory.createFailedAuthResponse(errorCode, errorMessage),
        )
    }
}
