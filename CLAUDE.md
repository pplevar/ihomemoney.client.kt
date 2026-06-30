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
- Unauthenticated entry point; its only operation is `authenticate(...)`, which returns `ApiResult<Session>`
- Holds no token and no mutable auth state — authenticated operations live on `Session`
- Configurable base URL (defaults to AppConfig.serviceUri)

**Session** (`src/main/kotlin/Session.kt`)
- Authenticated handle obtained from `HomemoneyApiClient.authenticate(...)`; its constructor is module-internal, so it cannot be fabricated without authenticating
- Carries the issued token privately and exposes the authenticated calls: `accountGroups()`, `accounts()` (flattens groups), `categories()`, `transactions(topCount)`
- "Log in before calling anything" is enforced by the type system rather than a runtime guard

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
1. Call `authenticate(username, password, clientId, clientSecret)` — returns `ApiResult<Session>`
2. On `Ok`, the `Session` holds the issued token privately; on `Err`, the typed `ApiFailure` carries why
3. All authenticated calls hang off the `Session`, so data methods are unreachable without one (compile-time)
4. Token is attached by `Session` as query parameter `@Query("Token")`

## Code Patterns

### Suspend Functions
All API operations are async using Kotlin coroutines:
```kotlin
suspend fun getAccounts(): List<Account> {
    // Coroutine-based API call
}
```

### Response Handling
- `interpret<T>()` (top-level `internal` in `Interpret.kt`) is the single seam: it validates HTTP status, the response body, and the API `Error.code` at one point, returning an `ApiResult<T>` (`Ok`/`Err`). It is shared by `authenticate` and every `Session` call.
- Failures cross the seam as a typed `ApiFailure` value (`Http` / `Api` / `EmptyBody` / `Malformed`), not as a thrown `Exception` — callers branch on the case
- `HomemoneyApiClient.authenticate(...)` returns `ApiResult<Session>`; `Session.accountGroups()`, `accounts()`, `categories()`, `transactions(...)` each return `ApiResult<…>`
- Transport errors (connection refused/timeout) are not modelled by `ApiFailure` and propagate as thrown exceptions

### JSON Mapping
Domain classes use `@SerializedName` for field mapping:
```kotlin
data class AuthResponse(
    @SerializedName("Error") val error: ErrorType,
    @SerializedName("access_token") val token: String,
)
```

### Account Flattening
`Session.accounts()` convenience method flattens account groups, mapping over the `ApiResult`:
```kotlin
suspend fun accounts(): ApiResult<List<Account>> =
    accountGroups().map { groups -> groups.flatMap { it.listAccountInfo } }
```

## Not Yet Implemented

Filtered `transactions(...)` (startDate, endDate, accountId, categoryId, pagination), `createTransaction(...)`, and `getCurrencies()` are not implemented or tested. Do not assume they work.

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
3. Add the authenticated call to `Session` returning `ApiResult<…>` via `interpret()` (or `authenticate`-style on the client if it is an unauthenticated endpoint)
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

## Agent skills

### Issue tracker

Issues and PRDs live as GitHub issues (`pplevar/ihomemoney.client.kt`), managed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles using default label names (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
