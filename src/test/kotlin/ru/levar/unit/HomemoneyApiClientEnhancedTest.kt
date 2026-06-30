package ru.levar.unit

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import ru.levar.ApiFailure
import ru.levar.ApiResult
import ru.levar.HomemoneyApiClient
import ru.levar.Session
import ru.levar.testutils.TestDataFactory
import ru.levar.testutils.enqueueError
import ru.levar.testutils.enqueueSuccess
import ru.levar.testutils.expectErr
import ru.levar.testutils.expectOk
import ru.levar.testutils.verifyPath
import ru.levar.testutils.verifyQueryParam

/**
 * Enhanced unit tests for HomemoneyApiClient using Kotest framework
 * Demonstrates modern Kotlin testing patterns with better assertions and BDD-style structure
 *
 * SAFETY: All tests use MockWebServer - NO production endpoints are contacted
 */
class HomemoneyApiClientEnhancedTest : DescribeSpec({

    lateinit var mockWebServer: MockWebServer
    lateinit var apiClient: HomemoneyApiClient
    lateinit var session: Session

    beforeTest {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiClient = HomemoneyApiClient(mockWebServer.url("/").toString())
    }

    afterTest {
        mockWebServer.shutdown()
    }

    describe("Authentication") {

        it("should successfully authenticate with valid credentials") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createSuccessfulAuthResponse())

                // Act
                val result =
                    apiClient.authenticate(
                        TestDataFactory.DEFAULT_USERNAME,
                        TestDataFactory.DEFAULT_PASSWORD,
                        TestDataFactory.DEFAULT_CLIENT_ID,
                        TestDataFactory.DEFAULT_CLIENT_SECRET,
                    )

                // Assert - success yields a Session
                result.expectOk()

                // Verify request
                val request = mockWebServer.takeRequest()
                request.verifyPath("TokenPassword")
                    .verifyQueryParam("username", TestDataFactory.DEFAULT_USERNAME)
                    .verifyQueryParam("password", TestDataFactory.DEFAULT_PASSWORD)
                    .verifyQueryParam("client_id", TestDataFactory.DEFAULT_CLIENT_ID)
                    .verifyQueryParam("client_secret", TestDataFactory.DEFAULT_CLIENT_SECRET)
            }
        }

        it("should fail authentication with invalid credentials") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createFailedAuthResponse(401, "Invalid credentials"),
                )

                // Act
                val result = apiClient.authenticate("wrong", "credentials", "5", "demo")

                // Assert - no Session; the typed cause is carried instead of a bare false
                result shouldBe ApiResult.Err(ApiFailure.Api(401, "Invalid credentials"))
            }
        }

        it("should handle HTTP error during authentication") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(500, "Internal Server Error")

                // Act
                val result = apiClient.authenticate("user", "pass", "5", "demo")

                // Assert
                result shouldBe ApiResult.Err(ApiFailure.Http(500))
            }
        }
    }

    describe("Account Management") {

        beforeTest {
            session = apiClient.sessionWith(TestDataFactory.DEFAULT_TOKEN)
        }

        it("should retrieve account groups") {
            runTest {
                // Arrange
                val testGroup =
                    TestDataFactory.createAccountGroup(
                        id = "group1",
                        name = "Personal Accounts",
                    )
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createBalanceListResponse(listOf(testGroup)),
                )

                // Act
                val result = session.accountGroups().expectOk()

                // Assert
                result shouldHaveSize 1
                result[0].name shouldBe "Personal Accounts"
            }
        }

        it("should flatten account groups into account list") {
            runTest {
                // Arrange
                val account1 = TestDataFactory.createAccount(id = "acc1", name = "Savings")
                val account2 = TestDataFactory.createAccount(id = "acc2", name = "Checking")
                val group1 = TestDataFactory.createAccountGroup(accounts = listOf(account1))
                val group2 = TestDataFactory.createAccountGroup(accounts = listOf(account2))

                mockWebServer.enqueueSuccess(
                    TestDataFactory.createBalanceListResponse(listOf(group1, group2)),
                )

                // Act
                val accounts = session.accounts().expectOk()

                // Assert
                accounts shouldHaveSize 2
                accounts.map { it.name } shouldContain "Savings"
                accounts.map { it.name } shouldContain "Checking"
            }
        }

        it("should handle empty account groups") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createEmptyBalanceListResponse())

                // Act
                val accounts = session.accounts().expectOk()

                // Assert
                accounts.shouldBeEmpty()
            }
        }
    }

    describe("Category Operations") {

        beforeTest {
            session = apiClient.sessionWith(TestDataFactory.DEFAULT_TOKEN)
        }

        it("should retrieve categories") {
            runTest {
                // Arrange
                val expenseCategory =
                    TestDataFactory.createCategory(
                        id = "cat1",
                        type = 0,
                        name = "Food",
                    )
                val incomeCategory =
                    TestDataFactory.createCategory(
                        id = "cat2",
                        type = 1,
                        name = "Salary",
                    )

                mockWebServer.enqueueSuccess(
                    TestDataFactory.createCategoryListResponse(listOf(expenseCategory, incomeCategory)),
                )

                // Act
                val result = session.categories().expectOk()

                // Assert
                result shouldHaveSize 2
                result[0].type shouldBe 0 // Expense
                result[1].type shouldBe 1 // Income
            }
        }

        it("should handle empty category list") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createEmptyCategoryListResponse())

                // Act
                val result = session.categories().expectOk()

                // Assert
                result.shouldBeEmpty()
            }
        }
    }

    describe("Transaction Operations") {

        beforeTest {
            session = apiClient.sessionWith(TestDataFactory.DEFAULT_TOKEN)
        }

        it("should retrieve transactions with topCount parameter") {
            runTest {
                // Arrange
                val transaction =
                    TestDataFactory.createTransaction(
                        id = "trans1",
                        description = "Grocery shopping",
                        total = 125.50,
                    )

                mockWebServer.enqueueSuccess(
                    TestDataFactory.createTransactionListResponse(listOf(transaction)),
                )

                // Act
                val result = session.transactions(10).expectOk()

                // Assert
                result shouldHaveSize 1
                result[0].id shouldBe "trans1"
                result[0].total shouldBe 125.50

                // Verify request
                val request = mockWebServer.takeRequest()
                request.path shouldContain "TopCount=10"
            }
        }

        it("should handle null topCount parameter") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createEmptyTransactionListResponse())

                // Act
                val result = session.transactions(null).expectOk()

                // Assert
                result.shouldBeEmpty()

                // Verify request doesn't include TopCount
                val request = mockWebServer.takeRequest()
                request.path shouldNotContain "TopCount"
            }
        }
    }

    describe("Error Handling") {

        beforeTest {
            session = apiClient.sessionWith(TestDataFactory.DEFAULT_TOKEN)
        }

        it("should return Http failure on 404 Not Found") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(404, "Not Found")

                // Act & Assert
                session.categories().expectErr() shouldBe ApiFailure.Http(404)
            }
        }

        it("should return Http failure on 401 Unauthorized") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(401, "Unauthorized")

                // Act & Assert
                session.categories().expectErr() shouldBe ApiFailure.Http(401)
            }
        }

        it("should return Http failure on 500 Internal Server Error") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(500, "Internal Server Error")

                // Act & Assert
                session.categories().expectErr() shouldBe ApiFailure.Http(500)
            }
        }

        it("should return Api failure when getTransactions returns API error on HTTP 200") {
            runTest {
                // Arrange: HTTP 200 with a non-zero API error code
                mockWebServer.enqueueSuccess(
                    """
                    {
                        "ListTransaction": [],
                        "Error": {"code": 7, "message": "Token expired"}
                    }
                    """.trimIndent(),
                )

                // Act & Assert - uniform interpretation surfaces code + message as one typed case
                session.transactions(10).expectErr() shouldBe ApiFailure.Api(7, "Token expired")
            }
        }

        it("should return Api failure when getAccountGroups returns API error on HTTP 200") {
            runTest {
                // Arrange: HTTP 200 with a non-zero API error code
                mockWebServer.enqueueSuccess(
                    """
                    {
                        "defaultcurrency": "980",
                        "name": "UAH",
                        "ListGroupInfo": [],
                        "Error": {"code": 7, "message": "Token expired"}
                    }
                    """.trimIndent(),
                )

                // Act & Assert - uniform interpretation surfaces code + message as one typed case
                session.accountGroups().expectErr() shouldBe ApiFailure.Api(7, "Token expired")
            }
        }
    }

    describe("Session lifecycle") {

        it("should yield a Session on successful authentication") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createSuccessfulAuthResponse(
                        token = "new-access-token",
                        refreshToken = "new-refresh-token",
                    ),
                )

                // Act
                val result = apiClient.authenticate("user", "pass", "5", "demo")

                // Assert - authentication concentrates auth state in the returned Session
                result.expectOk()
            }
        }

        it("should yield no Session on failed authentication") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createFailedAuthResponse())

                // Act
                val result = apiClient.authenticate("user", "pass", "5", "demo")

                // Assert - a failed auth produces an Err, not a usable handle
                result.expectErr()
            }
        }
    }
})
