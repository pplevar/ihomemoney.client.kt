# iHomemoney Kotlin API Client

A Kotlin-based REST API client for the [iHomemoney](https://ihomemoney.com) personal finance service. This library provides a type-safe, coroutine-based interface for managing accounts, categories, and transactions.

## Features

- **Authentication**: Secure token-based authentication with automatic token management
- **Account Management**: Retrieve account groups and individual accounts with currency information
- **Category Operations**: Fetch income and expense categories
- **Transaction Retrieval**: Access financial transactions with optional filtering
- **Async/Await**: Coroutine-based API for non-blocking operations
- **Type Safety**: Strongly-typed domain models with proper JSON serialization
- **HTTP Logging**: Built-in request/response logging for debugging
- **Comprehensive Testing**: 68 tests across 6 test classes with 100% MockWebServer coverage

## Technology Stack

- **Language**: Kotlin 2.1.20
- **HTTP Client**: Retrofit 3.0.0 + OkHttp 4.10.0
- **JSON Serialization**: Gson
- **Async Operations**: Kotlin Coroutines
- **Testing**: JUnit 5, MockWebServer, MockK, AssertJ
- **Build Tool**: Gradle

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("ru.levar:ihomemoney-client-kt:1.0-SNAPSHOT")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'ru.levar:ihomemoney-client-kt:1.0-SNAPSHOT'
}
```

## Quick Start

### Basic Usage

```kotlin
import kotlinx.coroutines.runBlocking
import ru.levar.HomemoneyApiClient

fun main() = runBlocking {
    // Initialize the client
    val apiClient = HomemoneyApiClient()

    try {
        // Authenticate
        apiClient.login(
            login = "your_username",
            password = "your_password",
            clientId = "5",
            clientSecret = "demo"
        )

        // Fetch accounts
        val accounts = apiClient.getAccounts()
        println("Accounts: $accounts")

        // Fetch categories
        val categories = apiClient.getCategories()
        println("Categories: ${categories.listCategoryInfo}")

        // Fetch recent transactions
        val transactions = apiClient.getTransactions(topCount = 10)
        println("Recent transactions: ${transactions.listTransactionInfo}")

    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
```

### Custom Base URL

```kotlin
val apiClient = HomemoneyApiClient(
    baseUrl = "https://custom.ihomemoney.com/api/api2.asmx/"
)
```

## API Reference

### Authentication

#### `login(login: String, password: String, clientId: String, clientSecret: String): Boolean`

Authenticates with the iHomemoney service and stores the access token for subsequent requests.

**Parameters:**
- `login` - User login/username
- `password` - User password
- `clientId` - Application client ID
- `clientSecret` - Application client secret

**Returns:** `true` on successful authentication, `false` on failure

**Throws:** `Exception` if authentication fails

```kotlin
val success = apiClient.login("username", "password", "5", "demo")
if (success) {
    println("Authentication successful")
}
```

### Account Operations

#### `getAccountGroups(): BalanceListResponse`

Retrieves all account groups with their accounts.

**Returns:** `BalanceListResponse` containing list of account groups

```kotlin
val accountGroups = apiClient.getAccountGroups()
for (group in accountGroups.listAccountGroupInfo) {
    println("Group: ${group.name}")
    for (account in group.listAccountInfo) {
        println("  - ${account.name}: ${account.balance}")
    }
}
```

#### `getAccounts(): List<Account>`

Convenience method that flattens all accounts from all groups into a single list.

**Returns:** `List<Account>` - flat list of all accounts

```kotlin
val accounts = apiClient.getAccounts()
accounts.forEach { account ->
    println("${account.name}: ${account.balance} ${account.listAccountCurrencyInfo.firstOrNull()?.currencyCode}")
}
```

### Category Operations

#### `getCategories(): CategoryListResponse`

Retrieves all income and expense categories.

**Returns:** `CategoryListResponse` containing list of categories

```kotlin
val categories = apiClient.getCategories()
for (category in categories.listCategoryInfo) {
    println("${category.name} (${category.typeString})")
}
```

### Transaction Operations

#### `getTransactions(topCount: Int?): TransactionListResponse`

Retrieves recent transactions with optional limit.

**Parameters:**
- `topCount` - Maximum number of transactions to retrieve (nullable)

**Returns:** `TransactionListResponse` containing list of transactions

```kotlin
// Get last 20 transactions
val transactions = apiClient.getTransactions(topCount = 20)
for (transaction in transactions.listTransactionInfo) {
    println("${transaction.date}: ${transaction.amount} - ${transaction.comment}")
}
```

## Domain Models

### Account

```kotlin
data class Account(
    val id: Int,
    val name: String,
    val balance: Double,
    val listAccountCurrencyInfo: List<AccountCurrencyInfo>
)
```

### Category

```kotlin
data class Category(
    val id: Int,
    val name: String,
    val typeString: String,  // "Income" or "Expense"
    val parentId: Int?
)
```

### Transaction

```kotlin
data class Transaction(
    val id: Int,
    val amount: Double,
    val date: String,
    val categoryId: Int,
    val accountId: Int,
    val comment: String?
)
```

## Configuration

The client uses a configuration file for default settings.

### Main Configuration

Create `src/main/resources/config/config.properties`:

```properties
serviceUri=https://ihomemoney.com/api/api2.asmx/
```

### Test Configuration

Create `src/test/resources/config/config.properties`:

```properties
serviceUri=http://localhost:8080/
```

Configuration is loaded via the `AppConfig` singleton object:

```kotlin
val apiClient = HomemoneyApiClient(AppConfig.serviceUri)
```

## Development

### Building the Project

```bash
# Clean build
./gradlew clean build

# Run the main test
./gradlew run
```

### Running Tests

```bash
# Run all tests (68 tests across 6 test classes)
./gradlew test

# Run specific test class
./gradlew test --tests ru.levar.HomemoneyApiClientTest

# Run with verbose output
./gradlew test --info

# Continuous testing
./gradlew test --continuous

# View test report
open build/reports/tests/test/index.html
```

### Test Structure

The project includes comprehensive test coverage:

1. **Unit Tests** (`HomemoneyApiClientTest`) - Client initialization and API methods
2. **API Service Tests** (`HomemoneyApiServiceTest`) - Retrofit interface behavior
3. **Domain Model Tests** (`DomainModelTest`) - JSON serialization/deserialization
4. **Integration Tests** (`ApiIntegrationTest`) - Multi-step workflows
5. **Edge Case Tests** (`EdgeCaseTest`) - Error scenarios and network failures
6. **Configuration Tests** (`AppConfigTest`) - Configuration loading and validation

All tests use MockWebServer to simulate API responses without contacting production endpoints.

## Architecture

### Core Components

**HomemoneyApiClient**
- Main entry point for all API interactions
- Manages authentication token lifecycle
- Wraps `HomemoneyApiService` with convenience methods
- Handles errors and response validation

**HomemoneyApiService**
- Retrofit interface defining API endpoints
- Uses suspend functions for coroutine support
- Query parameter-based API calls
- Token-based authentication (except login)

**Domain Models**
- Type-safe data classes with Gson annotations
- Located in `src/main/kotlin/domain/`
- Includes: Account, Category, Transaction, Currency, ErrorType

### HTTP Configuration

- **Timeouts**: 30 seconds for connect/read/write operations
- **Logging**: Full request/response body logging in debug mode
- **JSON**: Gson serialization with `@SerializedName` annotations
- **Error Handling**: Automatic HTTP status validation with descriptive exceptions

## Error Handling

The client throws exceptions for error conditions:

```kotlin
try {
    val accounts = apiClient.getAccounts()
} catch (e: Exception) {
    when {
        e.message?.contains("Authentication failed") == true -> {
            // Handle authentication error
        }
        e.message?.contains("Request failed") == true -> {
            // Handle HTTP error
        }
        else -> {
            // Handle other errors
        }
    }
}
```

## API Documentation

Official iHomemoney API documentation: [https://ihomemoney.com/api/api2.asmx](https://ihomemoney.com/api/api2.asmx)

## Project Structure

```
ihomemoney.client.kt/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── HomemoneyApiClient.kt
│   │   │   ├── AppConfig.kt
│   │   │   ├── api/
│   │   │   │   └── HomemoneyApiService.kt
│   │   │   └── domain/
│   │   │       ├── Account.kt
│   │   │       ├── Category.kt
│   │   │       ├── Transaction.kt
│   │   │       └── ... (other models)
│   │   └── resources/
│   │       └── config/
│   │           └── config.properties
│   └── test/
│       ├── kotlin/
│       │   ├── HomemoneyApiClientTest.kt
│       │   ├── HomemoneyApiServiceTest.kt
│       │   ├── DomainModelTest.kt
│       │   ├── ApiIntegrationTest.kt
│       │   ├── EdgeCaseTest.kt
│       │   └── AppConfigTest.kt
│       └── resources/
│           └── config/
│               └── config.properties
├── build.gradle.kts
├── README.md
├── CLAUDE.md
└── TEST_DOCUMENTATION.md
```

## Contributing

### Adding New Endpoints

1. Add method to `HomemoneyApiService` interface with Retrofit annotations
2. Create response data class with `@SerializedName` annotations
3. Add convenience method to `HomemoneyApiClient` using `handleResponse()`
4. Create unit tests in `HomemoneyApiServiceTest`
5. Create integration tests in `ApiIntegrationTest`
6. Add edge case tests in `EdgeCaseTest`
7. Update documentation

### Code Style

- Follow Kotlin coding conventions
- Use descriptive function names with backtick syntax for tests
- Maintain Arrange-Act-Assert pattern in tests
- All API methods must be suspend functions
- Use `@SerializedName` for JSON field mapping

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or contributions, please open an issue on the project repository.

## Related Documentation

- **CLAUDE.md** - Developer guidance for working with this codebase
- **TEST_DOCUMENTATION.md** - Comprehensive test suite documentation
---

**Note**: This is an unofficial client library and is not affiliated with or endorsed by iHomemoney.
