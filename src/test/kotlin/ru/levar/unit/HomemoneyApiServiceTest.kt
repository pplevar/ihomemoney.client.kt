package ru.levar.unit

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.levar.api.HomemoneyApiService

/**
 * Unit tests for HomemoneyApiService Retrofit interface
 * SAFETY: All tests use MockWebServer - NO production endpoints are contacted
 */
class HomemoneyApiServiceTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: HomemoneyApiService
    private lateinit var baseUrl: String

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        apiService = retrofit.create(HomemoneyApiService::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login should send correct parameters`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 0, "message": "Success"},
                            "access_token": "token123",
                            "refresh_token": "refresh123"
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val response =
                apiService.login(
                    login = "testuser",
                    password = "testpass",
                    clientId = "5",
                    clientSecret = "demo",
                )

            // Assert
            assertThat(response.isSuccessful).isTrue()
            assertThat(response.body()?.token).isEqualTo("token123")
            assertThat(response.body()?.refreshToken).isEqualTo("refresh123")
            assertThat(response.body()?.error?.code).isEqualTo(0)

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TokenPassword")
            assertThat(request.path).contains("username=testuser")
            assertThat(request.path).contains("password=testpass")
            assertThat(request.path).contains("client_id=5")
            assertThat(request.path).contains("client_secret=demo")
            assertThat(request.path).contains("grant_type=password")
        }

    @Test
    fun `getAccountGroups should parse response correctly`() =
        runTest {
            // Arrange
            val mockResponse =
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
                                    "name": "Group One",
                                    "hasAccounts": true,
                                    "hasShowAccounts": true,
                                    "order": 1,
                                    "ListAccountInfo": []
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val response = apiService.getAccountGroups("test-token")

            // Assert
            assertThat(response.isSuccessful).isTrue()
            val body = response.body()!!
            assertThat(body.defaultCurrencyId).isEqualTo("840")
            assertThat(body.currencyShortName).isEqualTo("USD")
            assertThat(body.listAccountGroupInfo).hasSize(1)
            assertThat(body.listAccountGroupInfo[0].id).isEqualTo("g1")
            assertThat(body.error.code).isEqualTo(0)

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("BalanceList")
            assertThat(request.path).contains("Token=test-token")
        }

    @Test
    fun `getCategories should parse response correctly`() =
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
                                    "id": "c1",
                                    "type": 0,
                                    "Name": "Transport",
                                    "FullName": "Expenses:Transport",
                                    "isArchive": false,
                                    "isPinned": true
                                }
                            ],
                            "Error": {"code": 0, "message": ""}
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val response = apiService.getCategories("test-token")

            // Assert
            assertThat(response.isSuccessful).isTrue()
            val body = response.body()!!
            assertThat(body.listCategory).hasSize(1)
            assertThat(body.listCategory[0].id).isEqualTo("c1")
            assertThat(body.listCategory[0].name).isEqualTo("Transport")
            assertThat(body.listCategory[0].type).isEqualTo(0)
            assertThat(body.listCategory[0].isPinned).isTrue()

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("CategoryList")
        }

    @Test
    fun `getTransactions should send topCount parameter`() =
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

            // Act
            val response = apiService.getTransactions("test-token", 25)

            // Assert
            assertThat(response.isSuccessful).isTrue()

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TransactionList")
            assertThat(request.path).contains("TopCount=25")
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

            // Act
            val response = apiService.getTransactions("test-token", null)

            // Assert
            assertThat(response.isSuccessful).isTrue()

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).contains("TransactionList")
        }

    @Test
    fun `login should handle error response`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                            "Error": {"code": 401, "message": "Unauthorized"},
                            "access_token": "",
                            "refresh_token": ""
                        }
                        """.trimIndent(),
                    )
            mockWebServer.enqueue(mockResponse)

            // Act
            val response = apiService.login("baduser", "badpass", "5", "demo")

            // Assert
            assertThat(response.isSuccessful).isTrue()
            assertThat(response.body()?.error?.code).isEqualTo(401)
            assertThat(response.body()?.error?.message).isEqualTo("Unauthorized")
        }

    @Test
    fun `should handle HTTP error responses`() =
        runTest {
            // Arrange
            val mockResponse =
                MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error")
            mockWebServer.enqueue(mockResponse)

            // Act
            val response = apiService.getCategories("test-token")

            // Assert
            assertThat(response.isSuccessful).isFalse()
            assertThat(response.code()).isEqualTo(500)
        }

    @Test
    fun `should handle network timeout gracefully`() =
        runTest {
            // Arrange
            mockWebServer.shutdown() // Simulate unreachable server

            // Act & Assert - Expect connection exception
            try {
                apiService.getCategories("test-token")
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(java.net.ConnectException::class.java)
            }
        }
}
