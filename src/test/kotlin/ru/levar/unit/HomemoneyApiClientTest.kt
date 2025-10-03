package ru.levar.unit

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.levar.HomemoneyApiClient
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for HomemoneyApiClient
 * SAFETY: All tests use MockWebServer - NO production endpoints are contacted
 */
class HomemoneyApiClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiClient: HomemoneyApiClient
    private lateinit var baseUrl: String

    @BeforeEach
    fun setup() {
        // Initialize mock server on random available port
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString()

        // Create client pointing to mock server
        apiClient = HomemoneyApiClient(baseUrl)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login should succeed with valid credentials`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "test-token-123",
                            "refresh_token": "refresh-token-456"
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val result = apiClient.login("testuser", "testpass", "5", "demo")

            // Assert
            assertTrue(result, "Login should succeed")
            assertThat(apiClient.token).isEqualTo("test-token-123")

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TokenPassword")
            assertThat(request.path).contains("username=testuser")
            assertThat(request.path).contains("password=testpass")
            assertThat(request.path).contains("client_id=5")
            assertThat(request.path).contains("client_secret=demo")
        }

    @Test
    fun `login should fail with invalid credentials`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 1, "message": "Invalid credentials"},
                            "access_token": "",
                            "refresh_token": ""
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val result = apiClient.login("wronguser", "wrongpass", "5", "demo")

            // Assert
            assertFalse(result, "Login should fail")
            assertThat(apiClient.token).isEmpty()
        }

    @Test
    fun `login should handle network error gracefully`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            mockWebServer.enqueue(mockResponse)

            // Act
            val result = apiClient.login("testuser", "testpass", "5", "demo")

            // Assert
            assertFalse(result, "Login should fail on network error")
        }

    @Test
    fun `getAccountGroups should return account groups`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "defaultcurrency": "980",
                            "name": "UAH",
                            "ListGroupInfo": [
                                {
                                    "id": "group1",
                                    "name": "Main Accounts",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 1,
                                    "ListAccountInfo": [
                                        {
                                            "id": "acc1",
                                            "name": "Savings",
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
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getAccountGroups()

            // Assert
            assertThat(result.defaultCurrencyId).isEqualTo("980")
            assertThat(result.currencyShortName).isEqualTo("UAH")
            assertThat(result.listAccountGroupInfo).hasSize(1)
            assertThat(result.listAccountGroupInfo[0].name).isEqualTo("Main Accounts")
            assertThat(result.listAccountGroupInfo[0].listAccountInfo).hasSize(1)

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("BalanceList")
            assertThat(request.path).contains("Token=test-token")
        }

    @Test
    fun `getAccounts should flatten account groups into list`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "defaultcurrency": "980",
                            "name": "UAH",
                            "ListGroupInfo": [
                                {
                                    "id": "group1",
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
                                    "id": "group2",
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
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getAccounts()

            // Assert
            assertThat(result).hasSize(3)
            assertThat(result.map { it.id }).containsExactly("acc1", "acc2", "acc3")
            assertThat(result.map { it.name }).containsExactly("Account 1", "Account 2", "Account 3")
        }

    @Test
    fun `getCategories should return categories list`() =
        runTest {
            // Arrange
            val mockResponse =
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
                                },
                                {
                                    "id": "cat2",
                                    "type": 1,
                                    "Name": "Salary",
                                    "FullName": "Income:Salary",
                                    "isArchive": false,
                                    "isPinned": false
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getCategories()

            // Assert
            assertThat(result.listCategory).hasSize(2)
            assertThat(result.listCategory[0].name).isEqualTo("Food")
            assertThat(result.listCategory[0].type).isEqualTo(0)
            assertThat(result.listCategory[1].name).isEqualTo("Salary")
            assertThat(result.listCategory[1].type).isEqualTo(1)

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("CategoryList")
            assertThat(request.path).contains("Token=test-token")
        }

    @Test
    fun `getTransactions should return transactions with topCount parameter`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [
                                {
                                    "TransactionId": "trans1",
                                    "Date": "2024-01-15T10:30:00",
                                    "DateUnix": "1705318200",
                                    "CategoryId": 1,
                                    "CategoryFullName": "Food:Groceries",
                                    "Description": "Weekly shopping",
                                    "isPlan": false,
                                    "type": 0,
                                    "Total": 150.50,
                                    "AccountId": "acc1",
                                    "CurrencyId": 980,
                                    "TransTotal": 0.0,
                                    "TransAccountId": "",
                                    "TransCurrencyId": 0,
                                    "GUID": "Comment here",
                                    "CreateDate": "2024-01-15T10:35:00",
                                    "CreateDateUnix": "1705318500"
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getTransactions(10)

            // Assert
            assertThat(result.listTransaction).hasSize(1)
            assertThat(result.listTransaction[0].id).isEqualTo("trans1")
            assertThat(result.listTransaction[0].total).isEqualTo(150.50)
            assertThat(result.listTransaction[0].description).isEqualTo("Weekly shopping")

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TransactionList")
            assertThat(request.path).contains("Token=test-token")
            assertThat(request.path).contains("TopCount=10")
        }

    @Test
    fun `getTransactions should handle null topCount`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListTransaction": [],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getTransactions(null)

            // Assert
            assertThat(result.listTransaction).isEmpty()

            // Verify request doesn't include TopCount parameter
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TransactionList")
            assertThat(request.path).doesNotContain("TopCount")
        }

    @Test
    fun `handleResponse should throw exception on unsuccessful response`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found")
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act & Assert
            val exception =
                assertThrows<Exception> {
                    apiClient.getCategories()
                }
            assertThat(exception.message).contains("404")
        }

    @Test
    fun `token should be empty initially`() {
        // Assert
        assertThat(apiClient.token).isEmpty()
    }

    @Test
    fun `token should be set after successful login`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "new-token",
                            "refresh_token": "new-refresh"
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            apiClient.login("user", "pass", "5", "demo")

            // Assert
            assertThat(apiClient.token).isEqualTo("new-token")
        }

    @Test
    fun `token setter should reject blank values`() {
        // Act & Assert
        val exception =
            assertThrows<IllegalArgumentException> {
                apiClient.token = ""
            }
        assertThat(exception.message).contains("Token cannot be blank")

        val exception2 =
            assertThrows<IllegalArgumentException> {
                apiClient.token = "   "
            }
        assertThat(exception2.message).contains("Token cannot be blank")
    }

    @Test
    fun `token setter should accept valid values`() {
        // Act
        apiClient.token = "valid-token-123"

        // Assert
        assertThat(apiClient.token).isEqualTo("valid-token-123")
    }

    @Test
    fun `should handle empty account groups`() =
        runTest {
            // Arrange
            val mockResponse =
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
                    )
            mockWebServer.enqueue(mockResponse)
            apiClient.token = "test-token"

            // Act
            val result = apiClient.getAccounts()

            // Assert
            assertThat(result).isEmpty()
        }
}
