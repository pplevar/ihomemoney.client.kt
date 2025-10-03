package ru.levar.testutils

import ru.levar.domain.*

/**
 * Test Data Factory for creating consistent test data across test suites
 * Provides builder methods for domain objects with sensible defaults
 */
object TestDataFactory {

    // Default test values
    const val DEFAULT_TOKEN = "test-token-123"
    const val DEFAULT_REFRESH_TOKEN = "refresh-token-456"
    const val DEFAULT_USERNAME = "testuser"
    const val DEFAULT_PASSWORD = "testpass"
    const val DEFAULT_CLIENT_ID = "5"
    const val DEFAULT_CLIENT_SECRET = "demo"
    const val DEFAULT_CURRENCY_ID = "980"
    const val DEFAULT_CURRENCY_NAME = "UAH"

    /**
     * Creates a valid AuthResponse for successful authentication
     */
    fun createSuccessfulAuthResponse(
        token: String = DEFAULT_TOKEN,
        refreshToken: String = DEFAULT_REFRESH_TOKEN
    ): String = """
        {
            "Error": {"code": 0, "message": ""},
            "access_token": "$token",
            "refresh_token": "$refreshToken"
        }
    """.trimIndent()

    /**
     * Creates a failed AuthResponse with error code
     */
    fun createFailedAuthResponse(
        errorCode: Int = 401,
        errorMessage: String = "Authentication failed"
    ): String = """
        {
            "Error": {"code": $errorCode, "message": "$errorMessage"},
            "access_token": "",
            "refresh_token": ""
        }
    """.trimIndent()

    /**
     * Creates an Account with default or custom values
     */
    fun createAccount(
        id: String = "acc1",
        name: String = "Test Account",
        isDefault: Boolean = true,
        display: Boolean = true,
        includeBalance: Boolean = true,
        hasOpenCurrencies: Boolean = false,
        currencyInfo: List<AccountCurrencyInfo> = emptyList(),
        isDeleted: Boolean = false
    ): Account = Account(
        id = id,
        name = name,
        isDefault = isDefault,
        display = display,
        includeBalance = includeBalance,
        hasOpenCurrencies = hasOpenCurrencies,
        listCurrencyInfo = currencyInfo,
        isDeleted = isDeleted
    )

    /**
     * Creates an AccountGroup with accounts
     */
    fun createAccountGroup(
        id: String = "group1",
        name: String = "Test Group",
        accounts: List<Account> = listOf(createAccount()),
        hasAccounts: Boolean = true,
        hasShowAccounts: Boolean = true,
        order: Long = 1
    ): AccountGroup = AccountGroup(
        id = id,
        name = name,
        hasAccounts = hasAccounts,
        hasShowAccounts = hasShowAccounts,
        order = order,
        listAccountInfo = accounts
    )

    /**
     * Creates a Category (expense or income)
     */
    fun createCategory(
        id: String = "cat1",
        type: Int = 0, // 0=expense, 1=income
        name: String = "Test Category",
        fullName: String = "Test:Category",
        isArchive: Boolean = false,
        isPinned: Boolean = false
    ): Category = Category(
        id = id,
        type = type,
        name = name,
        fullName = fullName,
        isArchive = isArchive,
        isPinned = isPinned
    )

    /**
     * Creates a Transaction with default or custom values
     */
    fun createTransaction(
        id: String = "trans1",
        date: String = "2024-01-15T10:30:00",
        dateUnix: String = "1705318200",
        categoryId: Int = 1,
        categoryFullName: String = "Test:Category",
        description: String = "Test transaction",
        isPlan: Boolean = false,
        type: Int = 0,
        total: Double = 100.0,
        accountId: String = "acc1",
        currencyId: Int = 980,
        transTotal: Double = 0.0,
        transAccountId: String = "",
        transCurrencyId: Int = 0,
        comment: String = "",
        createDate: String = "2024-01-15T10:35:00",
        createDateUnix: String = "1705318500"
    ): Transaction = Transaction(
        id = id,
        date = date,
        dateUnix = dateUnix,
        categoryId = categoryId,
        categoryFullName = categoryFullName,
        description = description,
        isPlan = isPlan,
        type = type,
        total = total,
        accountId = accountId,
        currencyId = currencyId,
        transTotal = transTotal,
        transAccountId = transAccountId,
        transCurrencyId = transCurrencyId,
        comment = comment,
        createDate = createDate,
        createDateUnix = createDateUnix
    )

    /**
     * Creates a Currency with default or custom values
     */
    fun createCurrency(
        id: Int = 980,
        shortName: String = "UAH",
        rate: Double = 1.0,
        balance: Double = 1000.0,
        display: Boolean = true
    ): Currency = Currency(
        id = id,
        shortName = shortName,
        rate = rate,
        balance = balance,
        display = display
    )

    /**
     * Creates an AccountCurrencyInfo with default or custom values
     */
    fun createAccountCurrencyInfo(
        id: String = "curr1",
        shortName: String = "USD",
        rate: Double = 1.0,
        balance: Double = 1000.0,
        display: Boolean = true
    ): AccountCurrencyInfo = AccountCurrencyInfo(
        id = id,
        shortName = shortName,
        rate = rate,
        balance = balance,
        display = display
    )

    /**
     * Creates JSON response for BalanceList endpoint
     */
    fun createBalanceListResponse(
        groups: List<AccountGroup> = listOf(createAccountGroup()),
        defaultCurrency: String = DEFAULT_CURRENCY_ID,
        currencyName: String = DEFAULT_CURRENCY_NAME
    ): String {
        val groupsJson = groups.joinToString(",") { group ->
            val accountsJson = group.listAccountInfo.joinToString(",") { account ->
                """
                {
                    "id": "${account.id}",
                    "name": "${account.name}",
                    "isDefault": ${account.isDefault},
                    "display": ${account.display},
                    "includeBalance": ${account.includeBalance},
                    "hasOpenCurrencies": ${account.hasOpenCurrencies},
                    "ListCurrencyInfo": [],
                    "isShowInGroup": false
                }
                """.trimIndent()
            }

            """
            {
                "id": "${group.id}",
                "name": "${group.name}",
                "hasAccounts": ${group.hasAccounts},
                "hasShowAccounts": ${group.hasShowAccounts},
                "order": ${group.order},
                "ListAccountInfo": [$accountsJson]
            }
            """.trimIndent()
        }

        return """
        {
            "defaultcurrency": "$defaultCurrency",
            "name": "$currencyName",
            "ListGroupInfo": [$groupsJson],
            "Error": {"code": 0, "message": ""}
        }
        """.trimIndent()
    }

    /**
     * Creates JSON response for CategoryList endpoint
     */
    fun createCategoryListResponse(
        categories: List<Category> = listOf(createCategory())
    ): String {
        val categoriesJson = categories.joinToString(",") { category ->
            """
            {
                "id": "${category.id}",
                "type": ${category.type},
                "Name": "${category.name}",
                "FullName": "${category.fullName}",
                "isArchive": ${category.isArchive},
                "isPinned": ${category.isPinned}
            }
            """.trimIndent()
        }

        return """
        {
            "ListCategory": [$categoriesJson],
            "Error": {"code": 0, "message": ""}
        }
        """.trimIndent()
    }

    /**
     * Creates JSON response for TransactionList endpoint
     */
    fun createTransactionListResponse(
        transactions: List<Transaction> = listOf(createTransaction())
    ): String {
        val transactionsJson = transactions.joinToString(",") { transaction ->
            """
            {
                "TransactionId": "${transaction.id}",
                "Date": "${transaction.date}",
                "DateUnix": "${transaction.dateUnix}",
                "CategoryId": ${transaction.categoryId},
                "CategoryFullName": "${transaction.categoryFullName}",
                "Description": "${transaction.description}",
                "isPlan": ${transaction.isPlan},
                "type": ${transaction.type},
                "Total": ${transaction.total},
                "AccountId": "${transaction.accountId}",
                "CurrencyId": ${transaction.currencyId},
                "TransTotal": ${transaction.transTotal},
                "TransAccountId": "${transaction.transAccountId}",
                "TransCurrencyId": ${transaction.transCurrencyId},
                "GUID": "${transaction.comment}",
                "CreateDate": "${transaction.createDate}",
                "CreateDateUnix": "${transaction.createDateUnix}"
            }
            """.trimIndent()
        }

        return """
        {
            "ListTransaction": [$transactionsJson],
            "Error": {"code": 0, "message": ""}
        }
        """.trimIndent()
    }

    /**
     * Creates an empty BalanceList response
     */
    fun createEmptyBalanceListResponse(): String = """
        {
            "defaultcurrency": "$DEFAULT_CURRENCY_ID",
            "name": "$DEFAULT_CURRENCY_NAME",
            "ListGroupInfo": [],
            "Error": {"code": 0, "message": ""}
        }
    """.trimIndent()

    /**
     * Creates an empty CategoryList response
     */
    fun createEmptyCategoryListResponse(): String = """
        {
            "ListCategory": [],
            "Error": {"code": 0, "message": ""}
        }
    """.trimIndent()

    /**
     * Creates an empty TransactionList response
     */
    fun createEmptyTransactionListResponse(): String = """
        {
            "ListTransaction": [],
            "Error": {"code": 0, "message": ""}
        }
    """.trimIndent()
}
