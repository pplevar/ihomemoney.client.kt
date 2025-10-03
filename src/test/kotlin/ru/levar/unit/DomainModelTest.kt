package ru.levar.unit

import com.google.gson.Gson
import ru.levar.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit tests for domain model classes
 * Tests JSON serialization/deserialization and data class properties
 */
class DomainModelTest {

    private val gson = Gson()

    @Test
    fun `Account should deserialize from JSON correctly`() {
        // Arrange
        val json = """
            {
                "id": "acc123",
                "name": "Checking Account",
                "isDefault": true,
                "display": true,
                "includeBalance": true,
                "hasOpenCurrencies": false,
                "ListCurrencyInfo": [],
                "isShowInGroup": false
            }
        """.trimIndent()

        // Act
        val account = gson.fromJson(json, Account::class.java)

        // Assert
        assertThat(account.id).isEqualTo("acc123")
        assertThat(account.name).isEqualTo("Checking Account")
        assertThat(account.isDefault).isTrue()
        assertThat(account.display).isTrue()
        assertThat(account.includeBalance).isTrue()
        assertThat(account.hasOpenCurrencies).isFalse()
        assertThat(account.listCurrencyInfo).isEmpty()
        assertThat(account.isDeleted).isFalse()
    }

    @Test
    fun `AccountGroup should deserialize with nested accounts`() {
        // Arrange
        val json = """
            {
                "id": "group1",
                "name": "Main Group",
                "hasAccounts": true,
                "hasShowAccounts": true,
                "order": 1,
                "ListAccountInfo": [
                    {
                        "id": "acc1",
                        "name": "Account 1",
                        "isDefault": false,
                        "display": true,
                        "includeBalance": true,
                        "hasOpenCurrencies": false,
                        "ListCurrencyInfo": [],
                        "isShowInGroup": false
                    }
                ]
            }
        """.trimIndent()

        // Act
        val group = gson.fromJson(json, AccountGroup::class.java)

        // Assert
        assertThat(group.id).isEqualTo("group1")
        assertThat(group.name).isEqualTo("Main Group")
        assertThat(group.hasAccounts).isTrue()
        assertThat(group.hasShowAccounts).isTrue()
        assertThat(group.order).isEqualTo(1)
        assertThat(group.listAccountInfo).hasSize(1)
        assertThat(group.listAccountInfo[0].id).isEqualTo("acc1")
    }

    @Test
    fun `Category should deserialize with correct type mapping`() {
        // Arrange - Expense category
        val expenseJson = """
            {
                "id": "cat1",
                "type": 0,
                "Name": "Food",
                "FullName": "Expenses:Food",
                "isArchive": false,
                "isPinned": true
            }
        """.trimIndent()

        // Act
        val expense = gson.fromJson(expenseJson, Category::class.java)

        // Assert
        assertThat(expense.id).isEqualTo("cat1")
        assertThat(expense.type).isEqualTo(0) // Expense
        assertThat(expense.name).isEqualTo("Food")
        assertThat(expense.fullName).isEqualTo("Expenses:Food")
        assertThat(expense.isArchive).isFalse()
        assertThat(expense.isPinned).isTrue()
    }

    @Test
    fun `Category should handle income type`() {
        // Arrange - Income category
        val incomeJson = """
            {
                "id": "cat2",
                "type": 1,
                "Name": "Salary",
                "FullName": "Income:Salary",
                "isArchive": false,
                "isPinned": false
            }
        """.trimIndent()

        // Act
        val income = gson.fromJson(incomeJson, Category::class.java)

        // Assert
        assertThat(income.type).isEqualTo(1) // Income
        assertThat(income.name).isEqualTo("Salary")
    }

    @Test
    fun `Transaction should deserialize with all fields`() {
        // Arrange
        val json = """
            {
                "TransactionId": "trans123",
                "Date": "2024-01-15T10:30:00",
                "DateUnix": "1705318200",
                "CategoryId": 5,
                "CategoryFullName": "Food:Restaurants",
                "Description": "Lunch at cafe",
                "isPlan": false,
                "type": 0,
                "Total": 45.50,
                "AccountId": "acc1",
                "CurrencyId": 840,
                "TransTotal": 0.0,
                "TransAccountId": "",
                "TransCurrencyId": 0,
                "GUID": "Additional notes",
                "CreateDate": "2024-01-15T10:35:00",
                "CreateDateUnix": "1705318500"
            }
        """.trimIndent()

        // Act
        val transaction = gson.fromJson(json, Transaction::class.java)

        // Assert
        assertThat(transaction.id).isEqualTo("trans123")
        assertThat(transaction.date).isEqualTo("2024-01-15T10:30:00")
        assertThat(transaction.dateUnix).isEqualTo("1705318200")
        assertThat(transaction.categoryId).isEqualTo(5)
        assertThat(transaction.categoryFullName).isEqualTo("Food:Restaurants")
        assertThat(transaction.description).isEqualTo("Lunch at cafe")
        assertThat(transaction.isPlan).isFalse()
        assertThat(transaction.type).isEqualTo(0)
        assertThat(transaction.total).isEqualTo(45.50)
        assertThat(transaction.accountId).isEqualTo("acc1")
        assertThat(transaction.currencyId).isEqualTo(840)
        assertThat(transaction.comment).isEqualTo("Additional notes")
    }

    @Test
    fun `Currency should deserialize correctly`() {
        // Arrange
        val json = """
            {
                "id": 980,
                "shortName": "UAH",
                "rate": 1.0,
                "balance": 5000.75,
                "display": true
            }
        """.trimIndent()

        // Act
        val currency = gson.fromJson(json, Currency::class.java)

        // Assert
        assertThat(currency.id).isEqualTo(980)
        assertThat(currency.shortName).isEqualTo("UAH")
        assertThat(currency.rate).isEqualTo(1.0)
        assertThat(currency.balance).isEqualTo(5000.75)
        assertThat(currency.display).isTrue()
    }

    @Test
    fun `AccountCurrencyInfo should deserialize correctly`() {
        // Arrange
        val json = """
            {
                "id": "curr1",
                "shortName": "EUR",
                "rate": 0.92,
                "balance": 1000.00,
                "display": true
            }
        """.trimIndent()

        // Act
        val currencyInfo = gson.fromJson(json, AccountCurrencyInfo::class.java)

        // Assert
        assertThat(currencyInfo.id).isEqualTo("curr1")
        assertThat(currencyInfo.shortName).isEqualTo("EUR")
        assertThat(currencyInfo.rate).isEqualTo(0.92)
        assertThat(currencyInfo.balance).isEqualTo(1000.00)
        assertThat(currencyInfo.display).isTrue()
    }

    @Test
    fun `ErrorType should deserialize correctly`() {
        // Arrange
        val json = """
            {
                "code": 404,
                "message": "Not found"
            }
        """.trimIndent()

        // Act
        val error = gson.fromJson(json, ErrorType::class.java)

        // Assert
        assertThat(error.code).isEqualTo(404)
        assertThat(error.message).isEqualTo("Not found")
    }

    @Test
    fun `AuthRequest should serialize correctly`() {
        // Arrange
        val authRequest = AuthRequest(
            login = "testuser",
            password = "testpass",
            deviceId = "5",
            appKey = "demo"
        )

        // Act
        val json = gson.toJson(authRequest)

        // Assert
        assertThat(json).contains("\"username\":\"testuser\"")
        assertThat(json).contains("\"password\":\"testpass\"")
        assertThat(json).contains("\"client_id\":\"5\"")
        assertThat(json).contains("\"client_secret\":\"demo\"")
    }

    @Test
    fun `Account data class should support copy`() {
        // Arrange
        val account = Account(
            id = "1",
            name = "Original",
            isDefault = false,
            display = true,
            includeBalance = true,
            hasOpenCurrencies = false,
            listCurrencyInfo = emptyList(),
            isDeleted = false
        )

        // Act
        val copied = account.copy(name = "Modified")

        // Assert
        assertThat(copied.name).isEqualTo("Modified")
        assertThat(copied.id).isEqualTo("1") // Other fields unchanged
        assertThat(account.name).isEqualTo("Original") // Original unchanged
    }

    @Test
    fun `Category archived status should work correctly`() {
        // Arrange
        val activeCategory = Category("1", 0, "Active", "Active", false, false)
        val archivedCategory = Category("2", 0, "Archived", "Archived", true, false)

        // Assert
        assertThat(activeCategory.isArchive).isFalse()
        assertThat(archivedCategory.isArchive).isTrue()
    }

    @Test
    fun `Transaction with zero amount should be valid`() {
        // Arrange
        val json = """
            {
                "TransactionId": "trans0",
                "Date": "2024-01-01T00:00:00",
                "DateUnix": "1704067200",
                "CategoryId": 1,
                "CategoryFullName": "Test",
                "Description": "Zero transaction",
                "isPlan": false,
                "type": 0,
                "Total": 0.0,
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
        assertThat(transaction.total).isEqualTo(0.0)
        assertThat(transaction.transTotal).isEqualTo(0.0)
    }

    @Test
    fun `Account with multiple currencies should deserialize correctly`() {
        // Arrange
        val json = """
            {
                "id": "multi-curr-acc",
                "name": "Multi Currency",
                "isDefault": false,
                "display": true,
                "includeBalance": true,
                "hasOpenCurrencies": true,
                "ListCurrencyInfo": [
                    {
                        "id": "curr1",
                        "shortName": "USD",
                        "rate": 1.0,
                        "balance": 1000.0,
                        "display": true
                    },
                    {
                        "id": "curr2",
                        "shortName": "EUR",
                        "rate": 0.92,
                        "balance": 500.0,
                        "display": true
                    }
                ],
                "isShowInGroup": false
            }
        """.trimIndent()

        // Act
        val account = gson.fromJson(json, Account::class.java)

        // Assert
        assertThat(account.hasOpenCurrencies).isTrue()
        assertThat(account.listCurrencyInfo).hasSize(2)
        assertThat(account.listCurrencyInfo[0].shortName).isEqualTo("USD")
        assertThat(account.listCurrencyInfo[1].shortName).isEqualTo("EUR")
    }
}
