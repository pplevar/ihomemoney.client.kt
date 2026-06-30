package ru.levar.unit

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.levar.ApiFailure
import ru.levar.ApiResult
import ru.levar.HomemoneyApiClient
import ru.levar.domain.Category
import ru.levar.domain.Transaction
import ru.levar.testutils.expectErr
import ru.levar.testutils.expectOk
import java.util.concurrent.TimeUnit

/**
 * Edge case and error scenario tests
 * SAFETY: All tests use MockWebServer - NO production endpoints
 */
class EdgeCaseTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiClient: HomemoneyApiClient
    private val gson = Gson()

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiClient = HomemoneyApiClient(mockWebServer.url("/").toString())
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should handle malformed JSON response`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{invalid json"),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert - an unparseable body surfaces as the Malformed case
            val failure = session.categories().expectErr()
            assertThat(failure).isInstanceOf(ApiFailure.Malformed::class.java)
        }

    @Test
    fun `should return Malformed when response body is empty`() =
        runTest {
            // Arrange - HTTP 200 with a literal empty body.
            // NOTE: Gson's converter rejects empty input ("End of input ...") before the
            // client's own null-body guard can run, so an empty body surfaces as the
            // Malformed case (carrying the parse cause) rather than EmptyBody. This test
            // pins that actual behavior so a future converter/seam change is caught.
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(""),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isInstanceOf(ApiFailure.Malformed::class.java)
            assertThat((failure as ApiFailure.Malformed).cause.message).contains("End of input")
        }

    @Test
    fun `should return EmptyBody when body deserializes to null`() =
        runTest {
            // Arrange - HTTP 204 No Content: Retrofit returns a successful response
            // with a null body without invoking the Gson converter. This is the path
            // that actually reaches the seam's null-body guard.
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert - seam interprets the null body as the EmptyBody case
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.EmptyBody)
        }

//     @Test
    // NOTE: This test is commented out due to runTest incompatibility with NO_RESPONSE socket policy
    // The timeout behavior is covered by other network error tests
//     fun `should handle network timeout`() = runTest {
//         // Arrange - Configure slow response beyond client timeout
//         mockWebServer.enqueue(MockResponse()
//             .setSocketPolicy(SocketPolicy.NO_RESPONSE))
//
//         // Act - login catches exceptions and returns false
//         val result = apiClient.login("user", "pass", "5", "demo")
//
//         // Assert - Should return false on network error
//         assertThat(result).isFalse()
//         assertThat(apiClient.token).isEmpty()
//     }

    @Test
    fun `should propagate transport error on connection refused`() =
        runTest {
            // Arrange - Shutdown server to simulate connection refused
            mockWebServer.shutdown()

            // Act & Assert - transport failures are not modelled by ApiFailure, so they
            // propagate as thrown exceptions rather than crossing the typed seam.
            assertThrows<Exception> {
                apiClient.authenticate("user", "pass", "5", "demo")
            }
        }

    @Test
    fun `should handle very long response delay`() =
        runTest {
            // Arrange - Delay response but within timeout
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "delayed-token",
                            "refresh_token": "delayed-refresh"
                        }
                        """.trimIndent(),
                    )
                    .setBodyDelay(1, TimeUnit.SECONDS),
            )

            // Act
            val result = apiClient.authenticate("user", "pass", "5", "demo")

            // Assert - Should succeed despite delay, yielding a Session
            assertThat(result).isInstanceOf(ApiResult.Ok::class.java)
        }

    @Test
    fun `should handle special characters in credentials`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "special-token",
                            "refresh_token": "special-refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Act
            val specialUsername = "user@email.com"
            val specialPassword = "p@ssw0rd!#$%"
            val result = apiClient.authenticate(specialUsername, specialPassword, "5", "demo")

            // Assert
            assertThat(result).isInstanceOf(ApiResult.Ok::class.java)

            val request = mockWebServer.takeRequest()
            // URL encoding should handle special characters
            assertThat(request.path).contains("username=")
            assertThat(request.path).contains("password=")
        }

    @Test
    fun `should handle very large transaction list`() =
        runTest {
            // Arrange - Generate large transaction list
            val transactions =
                (1..1000).map { i ->
                    """
                    {
                        "TransactionId": "trans$i",
                        "Date": "2024-01-01T00:00:00",
                        "DateUnix": "1704067200",
                        "CategoryId": $i,
                        "CategoryFullName": "Category$i",
                        "Description": "Transaction $i",
                        "isPlan": false,
                        "type": 0,
                        "Total": ${i * 10.0},
                        "AccountId": "acc1",
                        "CurrencyId": 840,
                        "TransTotal": 0.0,
                        "TransAccountId": "",
                        "TransCurrencyId": 0,
                        "GUID": "",
                        "CreateDate": "2024-01-01T00:00:00",
                        "CreateDateUnix": "1704067200"
                    }
                    """.trimIndent()
                }.joinToString(",")

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [$transactions],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )
            val session = apiClient.sessionWith("token")

            // Act
            val result = session.transactions(null).expectOk()

            // Assert
            assertThat(result).hasSize(1000)
            assertThat(result.first().id).isEqualTo("trans1")
            assertThat(result.last().id).isEqualTo("trans1000")
        }

    @Test
    fun `should handle Unicode characters in names`() {
        // Arrange
        val json =
            """
            {
                "id": "cat1",
                "type": 0,
                "Name": "Еда 食品 🍔",
                "FullName": "Расходы:Еда",
                "isArchive": false,
                "isPinned": false
            }
            """.trimIndent()

        // Act
        val category = gson.fromJson(json, Category::class.java)

        // Assert
        assertThat(category.name).isEqualTo("Еда 食品 🍔")
        assertThat(category.fullName).isEqualTo("Расходы:Еда")
    }

    @Test
    fun `should handle negative transaction amounts`() {
        // Arrange
        val json =
            """
            {
                "TransactionId": "trans1",
                "Date": "2024-01-01T00:00:00",
                "DateUnix": "1704067200",
                "CategoryId": 1,
                "CategoryFullName": "Refund",
                "Description": "Refund transaction",
                "isPlan": false,
                "type": 0,
                "Total": -50.00,
                "AccountId": "acc1",
                "CurrencyId": 840,
                "TransTotal": 0.0,
                "TransAccountId": "",
                "TransCurrencyId": 0,
                "GUID": "",
                "CreateDate": "2024-01-01T00:00:00",
                "CreateDateUnix": "1704067200"
            }
            """.trimIndent()

        // Act
        val transaction = gson.fromJson(json, Transaction::class.java)

        // Assert
        assertThat(transaction.total).isEqualTo(-50.00)
    }

    @Test
    fun `should handle very large decimal amounts`() {
        // Arrange
        val json =
            """
            {
                "TransactionId": "trans1",
                "Date": "2024-01-01T00:00:00",
                "DateUnix": "1704067200",
                "CategoryId": 1,
                "CategoryFullName": "Large",
                "Description": "Large amount",
                "isPlan": false,
                "type": 0,
                "Total": 999999999.99,
                "AccountId": "acc1",
                "CurrencyId": 840,
                "TransTotal": 0.0,
                "TransAccountId": "",
                "TransCurrencyId": 0,
                "GUID": "",
                "CreateDate": "2024-01-01T00:00:00",
                "CreateDateUnix": "1704067200"
            }
            """.trimIndent()

        // Act
        val transaction = gson.fromJson(json, Transaction::class.java)

        // Assert
        assertThat(transaction.total).isEqualTo(999999999.99)
    }

    @Test
    fun `should handle empty string fields`() {
        // Arrange
        val json =
            """
            {
                "id": "",
                "type": 0,
                "Name": "",
                "FullName": "",
                "isArchive": false,
                "isPinned": false
            }
            """.trimIndent()

        // Act
        val category = gson.fromJson(json, Category::class.java)

        // Assert
        assertThat(category.id).isEmpty()
        assertThat(category.name).isEmpty()
        assertThat(category.fullName).isEmpty()
    }

    // NOTE: The former `should handle null token in requests` test is gone by design.
    // Calling a data method without authenticating is no longer a runtime throw to assert
    // on — it is unreachable code (there is no data method outside a Session), so the misuse
    // is now a compile error rather than an IllegalArgumentException.

    @Test
    fun `should handle HTTP 401 Unauthorized`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("Unauthorized"),
            )
            val session = apiClient.sessionWith("invalid-token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Http(401))
        }

    @Test
    fun `should handle HTTP 403 Forbidden`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody("Forbidden"),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Http(403))
        }

    @Test
    fun `should handle HTTP 500 Internal Server Error`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Http(500))
        }

    @Test
    fun `should handle HTTP 503 Service Unavailable`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(503)
                    .setBody("Service Unavailable"),
            )
            val session = apiClient.sessionWith("token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Http(503))
        }

    @Test
    fun `should handle missing required JSON fields`() {
        // Arrange - Missing 'Name' field
        val json =
            """
            {
                "id": "cat1",
                "type": 0,
                "FullName": "Full",
                "isArchive": false,
                "isPinned": false
            }
            """.trimIndent()

        // Act & Assert - Gson should handle with null
        val category = gson.fromJson(json, Category::class.java)
        assertThat(category.id).isEqualTo("cat1")
    }

    @Test
    fun `should handle extra unexpected JSON fields`() {
        // Arrange - Extra fields not in model
        val json =
            """
            {
                "id": "cat1",
                "type": 0,
                "Name": "Food",
                "FullName": "Food",
                "isArchive": false,
                "isPinned": false,
                "extraField": "should be ignored",
                "anotherExtra": 123
            }
            """.trimIndent()

        // Act
        val category = gson.fromJson(json, Category::class.java)

        // Assert - Extra fields should be ignored
        assertThat(category.name).isEqualTo("Food")
    }

    @Test
    fun `should handle account with empty currency list`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "defaultcurrency": "980",
                            "name": "UAH",
                            "ListGroupInfo": [
                                {
                                    "id": "g1",
                                    "name": "Group",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 1,
                                    "ListAccountInfo": [
                                        {
                                            "id": "acc1",
                                            "name": "Account",
                                            "isDefault": true,
                                            "display": true,
                                            "includeBalance": true,
                                            "hasOpenCurrencies": false,
                                            "ListCurrencyInfo": [],
                                            "isShowInGroup": false
                                        }
                                    ]
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )
            val session = apiClient.sessionWith("token")

            // Act
            val result = session.accountGroups().expectOk()

            // Assert
            assertThat(result[0].listAccountInfo[0].listCurrencyInfo).isEmpty()
        }

    @Test
    fun `should handle concurrent requests with same client`() =
        runTest {
            // Arrange - Queue multiple responses
            repeat(3) {
                mockWebServer.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setBody(
                            """
                            {
                                "ListCategory": [],
                                "Error": {"code": 0, "message": ""}
                            }
                            """.trimIndent(),
                        ),
                )
            }
            val session = apiClient.sessionWith("token")

            // Act - Make concurrent requests
            val result1 = session.categories().expectOk()
            val result2 = session.categories().expectOk()
            val result3 = session.categories().expectOk()

            // Assert - All should succeed
            assertThat(result1).isEmpty()
            assertThat(result2).isEmpty()
            assertThat(result3).isEmpty()
            assertThat(mockWebServer.requestCount).isEqualTo(3)
        }

    @Test
    fun `should handle response with BOM (Byte Order Mark)`() =
        runTest {
            // Arrange - Response with UTF-8 BOM
            val responseWithBOM =
                "\uFEFF" +
                    """
                    {
                        "ListCategory": [],
                        "Error": {"code": 0, "message": ""}
                    }
                    """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseWithBOM),
            )
            val session = apiClient.sessionWith("token")

            // Act
            val result = session.categories().expectOk()

            // Assert
            assertThat(result).isEmpty()
        }

    @Test
    fun `should handle zero topCount parameter`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )
            val session = apiClient.sessionWith("token")

            // Act
            val result = session.transactions(0).expectOk()

            // Assert
            assertThat(result).isEmpty()

            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TopCount=0")
        }

    @Test
    fun `should handle negative topCount parameter`() =
        runTest {
            // Arrange
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )
            val session = apiClient.sessionWith("token")

            // Act
            val result = session.transactions(-1).expectOk()

            // Assert
            assertThat(result).isEmpty()

            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TopCount=-1")
        }
}
