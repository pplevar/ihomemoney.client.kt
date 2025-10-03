package ru.levar.integration

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.levar.HomemoneyApiClient

/**
 * Integration tests simulating full user workflows
 * SAFETY: All tests use MockWebServer - NO production endpoints are contacted
 */
class ApiIntegrationTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiClient: HomemoneyApiClient
    private lateinit var baseUrl: String

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString()
        apiClient = HomemoneyApiClient(baseUrl)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `complete user workflow - login and fetch all data`() =
        runTest {
            // Arrange - Mock login response
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "workflow-token",
                            "refresh_token": "workflow-refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Mock account groups response
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
                                    "name": "Personal",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 1,
                                    "ListAccountInfo": [
                                        {
                                            "id": "acc1",
                                            "name": "Cash",
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

            // Arrange - Mock categories response
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListCategory": [
                                {
                                    "id": "cat1",
                                    "type": 0,
                                    "Name": "Food",
                                    "FullName": "Expenses:Food",
                                    "isArchive": false,
                                    "isPinned": true
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Mock transactions response
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [
                                {
                                    "TransactionId": "trans1",
                                    "Date": "2024-01-15T10:00:00",
                                    "DateUnix": "1705316400",
                                    "CategoryId": 1,
                                    "CategoryFullName": "Food:Groceries",
                                    "Description": "Grocery shopping",
                                    "isPlan": false,
                                    "type": 0,
                                    "Total": 120.00,
                                    "AccountId": "acc1",
                                    "CurrencyId": 980,
                                    "TransTotal": 0.0,
                                    "TransAccountId": "",
                                    "TransCurrencyId": 0,
                                    "GUID": "",
                                    "CreateDate": "2024-01-15T10:00:00",
                                    "CreateDateUnix": "1705316400"
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )

            // Act - Execute complete workflow
            val loginSuccess = apiClient.login("user", "pass", "5", "demo")
            val accounts = apiClient.getAccounts()
            val categories = apiClient.getCategories()
            val transactions = apiClient.getTransactions(10)

            // Assert
            assertThat(loginSuccess).isTrue()
            assertThat(apiClient.token).isEqualTo("workflow-token")

            assertThat(accounts).hasSize(1)
            assertThat(accounts[0].name).isEqualTo("Cash")

            assertThat(categories.listCategory).hasSize(1)
            assertThat(categories.listCategory[0].name).isEqualTo("Food")

            assertThat(transactions.listTransaction).hasSize(1)
            assertThat(transactions.listTransaction[0].total).isEqualTo(120.00)

            // Verify all requests were made
            assertThat(mockWebServer.requestCount).isEqualTo(4)
        }

    @Test
    fun `workflow should fail gracefully on login failure`() =
        runTest {
            // Arrange - Mock failed login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 401, "message": "Invalid credentials"},
                            "access_token": "",
                            "refresh_token": ""
                        }
                        """.trimIndent(),
                    ),
            )

            // Act
            val loginSuccess = apiClient.login("baduser", "badpass", "5", "demo")

            // Assert
            assertThat(loginSuccess).isFalse()
            assertThat(apiClient.token).isEmpty()
            assertThat(mockWebServer.requestCount).isEqualTo(1)
        }

    @Test
    fun `should handle server errors in middle of workflow`() =
        runTest {
            // Arrange - Successful login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "valid-token",
                            "refresh_token": "valid-refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Server error on accounts request
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"),
            )

            // Act & Assert
            val loginSuccess = apiClient.login("user", "pass", "5", "demo")
            assertThat(loginSuccess).isTrue()

            try {
                apiClient.getAccounts()
                // Should throw exception
            } catch (e: Exception) {
                assertThat(e.message).contains("500")
            }
        }

    @Test
    fun `multiple sequential requests should maintain token`() =
        runTest {
            // Arrange - Login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "persistent-token",
                            "refresh_token": "persistent-refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Multiple successful responses
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

            // Act
            apiClient.login("user", "pass", "5", "demo")
            apiClient.getCategories()
            apiClient.getCategories()
            apiClient.getCategories()

            // Assert - All requests should use same token
            mockWebServer.takeRequest() // Skip login request
            val request1 = mockWebServer.takeRequest()
            val request2 = mockWebServer.takeRequest()
            val request3 = mockWebServer.takeRequest()

            assertThat(request1.path).contains("Token=persistent-token")
            assertThat(request2.path).contains("Token=persistent-token")
            assertThat(request3.path).contains("Token=persistent-token")
        }

    @Test
    fun `should handle empty response lists`() =
        runTest {
            // Arrange - Login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "token",
                            "refresh_token": "refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Empty account groups
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "defaultcurrency": "980",
                            "name": "UAH",
                            "ListGroupInfo": [],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Empty categories
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

            // Arrange - Empty transactions
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

            // Act
            apiClient.login("user", "pass", "5", "demo")
            val accounts = apiClient.getAccounts()
            val categories = apiClient.getCategories()
            val transactions = apiClient.getTransactions(10)

            // Assert - Should handle empty lists gracefully
            assertThat(accounts).isEmpty()
            assertThat(categories.listCategory).isEmpty()
            assertThat(transactions.listTransaction).isEmpty()
        }

    @Test
    fun `should handle complex nested account structure`() =
        runTest {
            // Arrange - Login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "token",
                            "refresh_token": "refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Complex nested structure
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "defaultcurrency": "840",
                            "name": "USD",
                            "ListGroupInfo": [
                                {
                                    "id": "g1",
                                    "name": "Group 1",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 1,
                                    "ListAccountInfo": [
                                        {
                                            "id": "acc1",
                                            "name": "Account 1",
                                            "isDefault": true,
                                            "display": true,
                                            "includeBalance": true,
                                            "hasOpenCurrencies": false,
                                            "ListCurrencyInfo": [],
                                            "isShowInGroup": false
                                        },
                                        {
                                            "id": "acc2",
                                            "name": "Account 2",
                                            "isDefault": false,
                                            "display": true,
                                            "includeBalance": true,
                                            "hasOpenCurrencies": false,
                                            "ListCurrencyInfo": [],
                                            "isShowInGroup": false
                                        }
                                    ]
                                },
                                {
                                    "id": "g2",
                                    "name": "Group 2",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 2,
                                    "ListAccountInfo": [
                                        {
                                            "id": "acc3",
                                            "name": "Account 3",
                                            "isDefault": false,
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

            // Act
            apiClient.login("user", "pass", "5", "demo")
            val accounts = apiClient.getAccounts()

            // Assert - Should flatten all accounts from all groups
            assertThat(accounts).hasSize(3)
            assertThat(accounts.map { it.id }).containsExactly("acc1", "acc2", "acc3")
        }

    @Test
    fun `should handle transaction filtering with topCount`() =
        runTest {
            // Arrange - Login
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "token",
                            "refresh_token": "refresh"
                        }
                        """.trimIndent(),
                    ),
            )

            // Arrange - Transactions with topCount
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [
                                {
                                    "TransactionId": "t1",
                                    "Date": "2024-01-01T00:00:00",
                                    "DateUnix": "1704067200",
                                    "CategoryId": 1,
                                    "CategoryFullName": "Cat1",
                                    "Description": "Trans 1",
                                    "isPlan": false,
                                    "type": 0,
                                    "Total": 100.0,
                                    "AccountId": "acc1",
                                    "CurrencyId": 840,
                                    "TransTotal": 0.0,
                                    "TransAccountId": "",
                                    "TransCurrencyId": 0,
                                    "GUID": "",
                                    "CreateDate": "2024-01-01T00:00:00",
                                    "CreateDateUnix": "1704067200"
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    ),
            )

            // Act
            apiClient.login("user", "pass", "5", "demo")
            val transactions = apiClient.getTransactions(5)

            // Assert
            assertThat(transactions.listTransaction).hasSize(1)

            val request = mockWebServer.takeRequest() // Login
            val transRequest = mockWebServer.takeRequest() // Transactions
            assertThat(transRequest.path).contains("TopCount=5")
        }
}
