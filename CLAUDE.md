# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin-based API client for iHomemoney service (https://ihomemoney.com/api/api2.asmx). The client handles authentication, account management, categories, and transaction retrieval through a REST API interface.

## Development Commands

### Build & Run
```bash
# Clean build
./gradlew clean build

# Run the client (MainTest.kt)
./gradlew run
```

### Testing
```bash
# Run all tests (68 tests across 6 test classes)
./gradlew test

# Run specific test class
./gradlew test --tests ru.levar.HomemoneyApiClientTest
./gradlew test --tests ru.levar.api.HomemoneyApiServiceTest
./gradlew test --tests ru.levar.domain.DomainModelTest
./gradlew test --tests ru.levar.integration.ApiIntegrationTest
./gradlew test --tests ru.levar.EdgeCaseTest
./gradlew test --tests ru.levar.AppConfigTest

# Run specific test method (use backtick syntax)
./gradlew test --tests "ru.levar.HomemoneyApiClientTest.login should succeed with valid credentials"

# Verbose test output
./gradlew test --info

# Continuous testing
./gradlew test --continuous

# Test report generated at: build/reports/tests/test/index.html
```

## Architecture

### Core Components

**HomemoneyApiClient** (`src/main/kotlin/HomemoneyApiClient.kt`)
- Main entry point for API interactions
- Manages authentication token lifecycle
- Wraps HomemoneyApiService with convenience methods
- Provides `getAccounts()` which flattens account groups into account list
- Error handling and response processing
- Configurable base URL (defaults to AppConfig.serviceUri)

**HomemoneyApiService** (`src/main/kotlin/api/HomemoneyApiService.kt`)
- Retrofit interface defining API endpoints
- Three response types: AuthResponse, BalanceListResponse, CategoryListResponse, TransactionListResponse
- All endpoints require token except `login()`
- Uses query parameters for all requests

**AppConfig** (`src/main/kotlin/AppConfig.kt`)
- Loads configuration from `src/main/resources/config/config.properties`
- Provides `serviceUri` property
- Test configuration in `src/test/resources/config/config.properties` uses localhost

### Domain Models (`src/main/kotlin/domain/`)
- **Account**: Represents user account with currency info
- **AccountGroup**: Groups accounts (BalanceList endpoint returns groups)
- **AccountCurrencyInfo**: Currency-specific account balance
- **Category**: Income/expense categories
- **Transaction**: Financial transaction with date, amount, category
- **Currency**: Currency definitions
- **ErrorType**: API error responses
- **AuthRequest**: Authentication request model

All domain models use Gson `@SerializedName` for JSON mapping.

### Technology Stack
- **HTTP Client**: Retrofit 3.0.0 + OkHttp 4.10.0
- **JSON Serialization**: Gson
- **Async**: Kotlin Coroutines (all API methods are `suspend fun`)
- **Logging**: HttpLoggingInterceptor with BODY level
- **Timeouts**: 30 seconds for connect/read/write

## Critical Safety Rules

### Testing Safety
**ALL TESTS USE MOCKWEBSERVER - NO PRODUCTION ENDPOINTS ARE CONTACTED**
- Test config points to localhost only
- MockWebServer simulates HTTP responses
- AppConfigTest validates production endpoint protection
- Never modify test config to use production URLs

### Authentication Flow
1. Call `login(username, password, clientId, clientSecret)` first
2. Client stores token in `token` property
3. All subsequent API calls use stored token
4. Token is passed as query parameter `@Query("Token")`

## Code Patterns

### Suspend Functions
All API operations are async using Kotlin coroutines:
```kotlin
suspend fun getAccounts(): List<Account> {
    // Coroutine-based API call
}
```

### Response Handling
- `handleResponse<T>()` validates HTTP status and extracts body
- Login has custom error handling (checks error.code == 0)
- Throws exceptions on API errors with descriptive messages

### JSON Mapping
Domain classes use `@SerializedName` for field mapping:
```kotlin
data class AuthResponse(
    @SerializedName("Error") val error: ErrorType,
    @SerializedName("access_token") val token: String,
)
```

### Account Flattening
`getAccounts()` convenience method flattens account groups:
```kotlin
suspend fun getAccounts(): List<Account> {
    val accGroups = getAccountGroups()
    return accGroups.listAccountGroupInfo.flatMap { it.listAccountInfo }
}
```

## Commented-Out Functionality

Several features are commented out in HomemoneyApiClient:
- Alternative `getTransactions()` with filtering (startDate, endDate, accountId, categoryId, pagination)
- `createTransaction(transaction: Transaction)`
- `getCurrencies()`

These are NOT implemented or tested. Do not assume they work.

## Test Structure

Tests follow Arrange-Act-Assert pattern with descriptive backtick names:
```kotlin
@Test
fun `should succeed when condition met`() = runTest {
    // Arrange
    mockWebServer.enqueue(MockResponse().setBody(...))

    // Act
    val result = apiClient.method()

    // Assert
    assertThat(result).isEqualTo(expected)
}
```

### Test Categories
1. **Unit Tests**: Client initialization, authentication, API methods
2. **API Service Tests**: Retrofit interface behavior
3. **Domain Model Tests**: JSON serialization/deserialization
4. **Integration Tests**: Multi-step workflows (login → fetch data)
5. **Edge Cases**: Error scenarios, malformed JSON, network failures

### MockWebServer Pattern
```kotlin
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
```

## Configuration

### Main Config (`src/main/resources/config/config.properties`)
```properties
serviceUri=https://ihomemoney.com/api/api2.asmx/
```

### Test Config (`src/test/resources/config/config.properties`)
```properties
serviceUri=http://localhost:8080/
```

Configuration loaded via `AppConfig` singleton object.

## Adding New Endpoints

1. Add method to `HomemoneyApiService` interface with Retrofit annotations
2. Create response data class with `@SerializedName` annotations
3. Add convenience method to `HomemoneyApiClient` using `handleResponse()`
4. Create unit tests in `HomemoneyApiServiceTest`
5. Create integration tests in `ApiIntegrationTest`
6. Add edge case tests in `EdgeCaseTest`
7. Update test documentation

## Gradle Configuration

- Kotlin JVM plugin 2.1.20
- JUnit 5 platform for tests
- Dependencies managed in `build.gradle.kts`
- Test task configured to use JUnit Platform

## Documentation Files

- **TESTING_QUICKSTART.md**: Quick reference for running tests
- **TEST_DOCUMENTATION.md**: Comprehensive test suite documentation
- **TEST_REPORT.md**: Detailed test execution report
- **README.md**: Basic usage examples in Russian
