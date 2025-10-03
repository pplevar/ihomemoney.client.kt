package ru.levar.unit

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import ru.levar.HomemoneyApiClient
import ru.levar.testutils.TestDataFactory
import ru.levar.testutils.enqueueSuccess
import ru.levar.testutils.enqueueError
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

    beforeTest {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        apiClient = HomemoneyApiClient(mockWebServer.url("/").toString())
    }

    afterTest {
        mockWebServer.shutdown()
    }

    describe("Authentication") {

        it("should successfully login with valid credentials") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createSuccessfulAuthResponse())

                // Act
                val result = apiClient.login(
                    TestDataFactory.DEFAULT_USERNAME,
                    TestDataFactory.DEFAULT_PASSWORD,
                    TestDataFactory.DEFAULT_CLIENT_ID,
                    TestDataFactory.DEFAULT_CLIENT_SECRET
                )

                // Assert
                result shouldBe true
                apiClient.token shouldBe TestDataFactory.DEFAULT_TOKEN

                // Verify request
                val request = mockWebServer.takeRequest()
                request.verifyPath("TokenPassword")
                    .verifyQueryParam("username", TestDataFactory.DEFAULT_USERNAME)
                    .verifyQueryParam("password", TestDataFactory.DEFAULT_PASSWORD)
                    .verifyQueryParam("client_id", TestDataFactory.DEFAULT_CLIENT_ID)
                    .verifyQueryParam("client_secret", TestDataFactory.DEFAULT_CLIENT_SECRET)
            }
        }

        it("should fail login with invalid credentials") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createFailedAuthResponse(401, "Invalid credentials")
                )

                // Act
                val result = apiClient.login("wrong", "credentials", "5", "demo")

                // Assert
                result shouldBe false
                apiClient.token shouldBe ""
            }
        }

        it("should handle HTTP error during login") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(500, "Internal Server Error")

                // Act
                val result = apiClient.login("user", "pass", "5", "demo")

                // Assert
                result shouldBe false
                apiClient.token shouldBe ""
            }
        }
    }

    describe("Account Management") {

        beforeTest {
            apiClient.token = TestDataFactory.DEFAULT_TOKEN
        }

        it("should retrieve account groups") {
            runTest {
                // Arrange
                val testGroup = TestDataFactory.createAccountGroup(
                    id = "group1",
                    name = "Personal Accounts"
                )
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createBalanceListResponse(listOf(testGroup))
                )

                // Act
                val result = apiClient.getAccountGroups()

                // Assert
                result.listAccountGroupInfo shouldHaveSize 1
                result.listAccountGroupInfo[0].name shouldBe "Personal Accounts"
                result.defaultCurrencyId shouldBe TestDataFactory.DEFAULT_CURRENCY_ID
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
                    TestDataFactory.createBalanceListResponse(listOf(group1, group2))
                )

                // Act
                val accounts = apiClient.getAccounts()

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
                val accounts = apiClient.getAccounts()

                // Assert
                accounts.shouldBeEmpty()
            }
        }
    }

    describe("Category Operations") {

        beforeTest {
            apiClient.token = TestDataFactory.DEFAULT_TOKEN
        }

        it("should retrieve categories") {
            runTest {
                // Arrange
                val expenseCategory = TestDataFactory.createCategory(
                    id = "cat1",
                    type = 0,
                    name = "Food"
                )
                val incomeCategory = TestDataFactory.createCategory(
                    id = "cat2",
                    type = 1,
                    name = "Salary"
                )

                mockWebServer.enqueueSuccess(
                    TestDataFactory.createCategoryListResponse(listOf(expenseCategory, incomeCategory))
                )

                // Act
                val result = apiClient.getCategories()

                // Assert
                result.listCategory shouldHaveSize 2
                result.listCategory[0].type shouldBe 0 // Expense
                result.listCategory[1].type shouldBe 1 // Income
            }
        }

        it("should handle empty category list") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createEmptyCategoryListResponse())

                // Act
                val result = apiClient.getCategories()

                // Assert
                result.listCategory.shouldBeEmpty()
            }
        }
    }

    describe("Transaction Operations") {

        beforeTest {
            apiClient.token = TestDataFactory.DEFAULT_TOKEN
        }

        it("should retrieve transactions with topCount parameter") {
            runTest {
                // Arrange
                val transaction = TestDataFactory.createTransaction(
                    id = "trans1",
                    description = "Grocery shopping",
                    total = 125.50
                )

                mockWebServer.enqueueSuccess(
                    TestDataFactory.createTransactionListResponse(listOf(transaction))
                )

                // Act
                val result = apiClient.getTransactions(10)

                // Assert
                result.listTransaction shouldHaveSize 1
                result.listTransaction[0].id shouldBe "trans1"
                result.listTransaction[0].total shouldBe 125.50

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
                val result = apiClient.getTransactions(null)

                // Assert
                result.listTransaction.shouldBeEmpty()

                // Verify request doesn't include TopCount
                val request = mockWebServer.takeRequest()
                request.path shouldNotContain "TopCount"
            }
        }
    }

    describe("Error Handling") {

        beforeTest {
            apiClient.token = TestDataFactory.DEFAULT_TOKEN
        }

        it("should throw exception on 404 Not Found") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(404, "Not Found")

                // Act & Assert
                val exception = shouldThrow<Exception> {
                    apiClient.getCategories()
                }
                exception.message shouldContain "404"
            }
        }

        it("should throw exception on 401 Unauthorized") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(401, "Unauthorized")

                // Act & Assert
                val exception = shouldThrow<Exception> {
                    apiClient.getCategories()
                }
                exception.message shouldContain "401"
            }
        }

        it("should throw exception on 500 Internal Server Error") {
            runTest {
                // Arrange
                mockWebServer.enqueueError(500, "Internal Server Error")

                // Act & Assert
                val exception = shouldThrow<Exception> {
                    apiClient.getCategories()
                }
                exception.message shouldContain "500"
            }
        }
    }

    describe("Token Management") {

        it("should have empty token initially") {
            apiClient.token shouldBe ""
        }

        it("should store token after successful login") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(
                    TestDataFactory.createSuccessfulAuthResponse(
                        token = "new-access-token",
                        refreshToken = "new-refresh-token"
                    )
                )

                // Act
                apiClient.login("user", "pass", "5", "demo")

                // Assert
                apiClient.token shouldBe "new-access-token"
            }
        }

        it("should not store token after failed login") {
            runTest {
                // Arrange
                mockWebServer.enqueueSuccess(TestDataFactory.createFailedAuthResponse())

                // Act
                apiClient.login("user", "pass", "5", "demo")

                // Assert
                apiClient.token shouldBe ""
            }
        }
    }
})
