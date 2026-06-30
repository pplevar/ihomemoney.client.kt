package ru.levar.unit

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.levar.ApiFailure
import ru.levar.ApiResult
import ru.levar.HomemoneyApiClient
import ru.levar.testutils.expectErr
import ru.levar.testutils.expectOk

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
    fun `authenticate should yield a Session that fetches data with the issued token`() =
        runTest {
            // Arrange - login issues a token, then a data response for the Session call
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": ""},
                            "access_token": "session-token",
                            "refresh_token": "refresh"
                        }
                        """.trimIndent(),
                    ),
            )
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

            // Act - obtain a Session, then reach a data call only through it
            val session = apiClient.authenticate("testuser", "testpass", "5", "demo").expectOk()
            session.categories().expectOk()

            // Assert - the data request carried the token issued by authenticate
            mockWebServer.takeRequest() // login request
            val dataRequest = mockWebServer.takeRequest()
            assertThat(dataRequest.path).contains("CategoryList")
            assertThat(dataRequest.path).contains("Token=session-token")
        }

    @Test
    fun `authenticate should send credentials and succeed with valid login`() =
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
            val result = apiClient.authenticate("testuser", "testpass", "5", "demo")

            // Assert - success yields a Session (not a bare boolean)
            assertThat(result).isInstanceOf(ApiResult.Ok::class.java)

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TokenPassword")
            assertThat(request.path).contains("username=testuser")
            assertThat(request.path).contains("password=testpass")
            assertThat(request.path).contains("client_id=5")
            assertThat(request.path).contains("client_secret=demo")
        }

    @Test
    fun `authenticate should fail with the API cause on invalid credentials`() =
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
            val result = apiClient.authenticate("wronguser", "wrongpass", "5", "demo")

            // Assert - the failure carries why, instead of collapsing to false
            assertThat(result).isEqualTo(ApiResult.Err(ApiFailure.Api(1, "Invalid credentials")))
        }

    @Test
    fun `authenticate should return Http failure on server error`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            mockWebServer.enqueue(mockResponse)

            // Act
            val result = apiClient.authenticate("testuser", "testpass", "5", "demo")

            // Assert
            assertThat(result).isEqualTo(ApiResult.Err(ApiFailure.Http(500)))
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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.accountGroups().expectOk()

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("Main Accounts")
            assertThat(result[0].listAccountInfo).hasSize(1)

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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.accounts().expectOk()

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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.categories().expectOk()

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result[0].name).isEqualTo("Food")
            assertThat(result[0].type).isEqualTo(0)
            assertThat(result[1].name).isEqualTo("Salary")
            assertThat(result[1].type).isEqualTo(1)

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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.transactions(10).expectOk()

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo("trans1")
            assertThat(result[0].total).isEqualTo(150.50)
            assertThat(result[0].description).isEqualTo("Weekly shopping")

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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.transactions(null).expectOk()

            // Assert
            assertThat(result).isEmpty()

            // Verify request doesn't include TopCount parameter
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TransactionList")
            assertThat(request.path).doesNotContain("TopCount")
        }

    @Test
    fun `getCategories should return Api failure when API returns error code on HTTP 200`() =
        runTest {
            // Arrange: HTTP 200 but API-level error in the Error envelope
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "ListCategory": [],
                            "Error": {"code": 5, "message": "Session expired"}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)
            val session = apiClient.sessionWith("test-token")

            // Act & Assert - the unified seam surfaces the API code + message as one typed case
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Api(5, "Session expired"))
        }

    @Test
    fun `interpret should return Http failure on unsuccessful response`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Not Found")
            mockWebServer.enqueue(mockResponse)
            val session = apiClient.sessionWith("test-token")

            // Act & Assert
            val failure = session.categories().expectErr()
            assertThat(failure).isEqualTo(ApiFailure.Http(404))
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
            val session = apiClient.sessionWith("test-token")

            // Act
            val result = session.accounts().expectOk()

            // Assert
            assertThat(result).isEmpty()
        }
}
