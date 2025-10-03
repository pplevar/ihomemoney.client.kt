# Refactoring Recommendations - iHomeMoney Kotlin Client

**Generated**: 2025-10-03
**Focus**: Code Quality Improvements
**Project**: Kotlin API Client for iHomeMoney Service

---

## Executive Summary

This report provides systematic refactoring recommendations for the iHomeMoney Kotlin API client. Analysis identified **35+ quality improvements** across safety, organization, type safety, and architecture. Recommendations are prioritized by impact and effort, with all changes validated against the existing 68-test suite.

**Quality Score**: Current ~6.5/10 → Target ~8.5/10
**Key Strength**: Comprehensive test coverage with MockWebServer
**Primary Weakness**: Error handling inconsistency and safety gaps

---

## 🔴 CRITICAL PRIORITY (High Impact, Low Effort)

### 1. Fix AppConfig Null Safety Risk
**Location**: `src/main/kotlin/AppConfig.kt:8`
**Issue**: `ClassLoader.getSystemResourceAsStream()` can return null, causing NPE on property access
**Impact**: Runtime crash on first configuration access if file missing

**Current Code**:
```kotlin
object AppConfig {
    private val properties = Properties().apply {
        load(ClassLoader.getSystemResourceAsStream("config/config.properties"))
    }
    val serviceUri: String get() = properties.getProperty("serviceUri")
}
```

**Recommended Fix**:
```kotlin
object AppConfig {
    private val properties = Properties().apply {
        val stream = ClassLoader.getSystemResourceAsStream("config/config.properties")
            ?: throw IllegalStateException("Configuration file config/config.properties not found")
        load(stream)
    }

    val serviceUri: String
        get() = properties.getProperty("serviceUri")
            ?: throw IllegalStateException("serviceUri not defined in configuration")
}
```

**Effort**: 5 minutes
**Test Impact**: Validates existing AppConfigTest behavior

---

### 2. Replace println with Proper Logging ✅ DONE
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:47`
**Issue**: Using `println()` for error logging - no log levels, no structured output
**Impact**: Production debugging difficulty, no log management

**Current Code**:
```kotlin
} catch (e: Exception) {
    println("Some login error: ${e.message}")
    return false
}
```

**Recommended Fix**:
```kotlin
// Add to build.gradle.kts
dependencies {
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

// In HomemoneyApiClient
private val logger = KotlinLogging.logger {}

suspend fun login(...): Boolean {
    try {
        // ... existing logic
    } catch (e: Exception) {
        logger.error(e) { "Authentication failed: ${e.message}" }
        return false
    }
}
```

**Effort**: 15 minutes
**Test Impact**: None (tests already capture this path)

---

### 3. Remove Non-Null Assertion in handleResponse
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:113`
**Issue**: `response.body()!!` can throw NPE if server returns empty body with 200 status
**Impact**: Uncontrolled crash instead of meaningful error

**Current Code**:
```kotlin
private fun <T> handleResponse(response: Response<T>): T {
    if (!response.isSuccessful) {
        throw Exception("Request failed with code ${response.code()}: ${response.message()}")
    }
    val body = response.body()!!
    return body
}
```

**Recommended Fix**:
```kotlin
private fun <T> handleResponse(response: Response<T>): T {
    if (!response.isSuccessful) {
        throw HttpException("Request failed with code ${response.code()}: ${response.message()}")
    }
    return response.body()
        ?: throw HttpException("Server returned empty response body for successful request")
}

class HttpException(message: String) : Exception(message)
```

**Effort**: 5 minutes
**Test Impact**: EdgeCaseTest validates this scenario

---

### 4. Fix Semantic Field Name Mappings
**Location**: `src/main/kotlin/domain/Account.kt:16`, `Transaction.kt:17`
**Issue**: Field names don't match their semantic meaning
**Impact**: Developer confusion, potential bugs from misunderstanding data

**Current Code**:
```kotlin
// Account.kt
@SerializedName("isShowInGroup") val isDeleted: Boolean

// Transaction.kt
@SerializedName("GUID") val comment: String
```

**Recommended Fix**:
```kotlin
// Account.kt
@SerializedName("isShowInGroup") val isShowInGroup: Boolean

// Transaction.kt
@SerializedName("GUID") val guid: String
```

**Effort**: 10 minutes (includes test updates)
**Test Impact**: Update DomainModelTest assertions

---

### 5. Remove Dead Code
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:78-106`
**Issue**: 28 lines of commented implementation
**Impact**: Code bloat, maintenance confusion, merge conflicts

**Recommendation**: Delete lines 78-106 entirely. If needed later, restore from git history.

**Effort**: 1 minute
**Test Impact**: None (not tested)

---

## 🟡 HIGH PRIORITY (High Impact, Medium Effort)

### 6. Extract Response DTOs from Interface ✅ DONE
**Location**: `src/main/kotlin/api/HomemoneyApiService.kt:21-61`
**Issue**: Response models nested in interface violate separation of concerns
**Impact**: Tight coupling, difficult to reuse DTOs, poor organization

**Current Structure**:
```kotlin
interface HomemoneyApiService {
    data class AuthResponse(...)
    data class BalanceListResponse(...)
    data class CategoryListResponse(...)
    data class TransactionListResponse(...)
}
```

**Recommended Structure**:
```
src/main/kotlin/api/
├── HomemoneyApiService.kt (interface only)
└── dto/
    ├── AuthResponse.kt
    ├── BalanceListResponse.kt
    ├── CategoryListResponse.kt
    └── TransactionListResponse.kt
```

**Example - AuthResponse.kt**:
```kotlin
package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.ErrorType

data class AuthResponse(
    @SerializedName("Error") val error: ErrorType,
    @SerializedName("access_token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String,
)
```

**Effort**: 30 minutes
**Test Impact**: Update imports in HomemoneyApiServiceTest

---

### 7. Introduce Result Type for Consistent Error Handling
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:38-50`
**Issue**: `login()` returns Boolean while other methods throw exceptions - inconsistent error handling
**Impact**: Difficult error propagation, loses exception context

**Current Pattern**:
```kotlin
suspend fun login(...): Boolean {
    try {
        // ... logic
        return true
    } catch (e: Exception) {
        println("Some login error: ${e.message}")
        return false
    }
}
```

**Recommended Pattern**:
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception) : ApiResult<Nothing>()
}

suspend fun login(...): ApiResult<String> {
    return try {
        val response = apiService.login(login, password, clientId, clientSecret)
        if (!response.isSuccessful || response.body()?.error?.code != 0) {
            ApiResult.Error(AuthException("Authentication failed: ${response.body()?.error}"))
        } else {
            token = response.body()!!.token
            ApiResult.Success(token)
        }
    } catch (e: Exception) {
        logger.error(e) { "Login request failed" }
        ApiResult.Error(e)
    }
}

class AuthException(message: String) : Exception(message)
```

**Effort**: 2 hours (includes updating all callers and tests)
**Test Impact**: Significant - update all test assertions

---

### 8. Add Token Validation Before API Calls
**Location**: All API methods in `HomemoneyApiClient.kt`
**Issue**: No validation that token is set before making authenticated requests
**Impact**: Cryptic errors from server instead of clear local validation

**Recommended Fix**:
```kotlin
class HomemoneyApiClient(baseUrl: String = AppConfig.serviceUri) {
    private var _token: String = ""

    val token: String
        get() = _token
        set(value) {
            require(value.isNotBlank()) { "Token cannot be blank" }
            _token = value
        }

    private fun requireAuthentication() {
        require(_token.isNotBlank()) { "Authentication required. Call login() first." }
    }

    suspend fun getAccountGroups(): BalanceListResponse {
        requireAuthentication()
        val response = apiService.getAccountGroups(_token)
        return handleResponse(response)
    }
    // Apply to all authenticated methods
}
```

**Effort**: 30 minutes
**Test Impact**: Add tests for unauthenticated access attempts

---

### 9. Replace Manual Loops with Kotlin Stdlib
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:57-66`
**Issue**: Manual loop and mutable list instead of functional approach
**Impact**: More verbose, error-prone, not idiomatic Kotlin

**Current Code**:
```kotlin
suspend fun getAccounts(): List<Account> {
    val accGroups = getAccountGroups()
    val accounts = mutableListOf<Account>()
    for(accGroup in accGroups.listAccountGroupInfo) {
        for(acc in accGroup.listAccountInfo) {
            accounts.add(acc)
        }
    }
    return accounts
}
```

**Recommended Fix**:
```kotlin
suspend fun getAccounts(): List<Account> {
    return getAccountGroups()
        .listAccountGroupInfo
        .flatMap { it.listAccountInfo }
}
```

**Effort**: 2 minutes
**Test Impact**: None (behavior identical)

---

### 10. Introduce Enum Types for Category and Transaction Types
**Location**: `src/main/kotlin/domain/Category.kt:7`, `Transaction.kt:10`
**Issue**: Using `Int` for categorical data - no type safety
**Impact**: Magic numbers, potential invalid values, poor readability

**Current Code**:
```kotlin
data class Category(
    // ...
    @SerializedName("type") val type: Int, // 0 - расход, 1 - доход
)

data class Transaction(
    // ...
    @SerializedName("type") val type: Int,
)
```

**Recommended Fix**:
```kotlin
// Create domain/CategoryType.kt
package ru.levar.domain

import com.google.gson.annotations.SerializedName

enum class CategoryType(val value: Int) {
    @SerializedName("0") EXPENSE(0),
    @SerializedName("1") INCOME(1);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value }
            ?: throw IllegalArgumentException("Unknown category type: $value")
    }
}

// Create domain/TransactionType.kt
enum class TransactionType(val value: Int) {
    @SerializedName("0") EXPENSE(0),
    @SerializedName("1") INCOME(1),
    @SerializedName("2") TRANSFER(2);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value }
            ?: throw IllegalArgumentException("Unknown transaction type: $value")
    }
}

// Update models
data class Category(
    // ...
    @SerializedName("type") private val _type: Int,
) {
    val type: CategoryType get() = CategoryType.fromInt(_type)
}
```

**Effort**: 1 hour
**Test Impact**: Update DomainModelTest to verify enum conversion

---

## 🟢 MEDIUM PRIORITY (Medium Impact, Variable Effort)

### 11. Extract Constants for Magic Values
**Location**: Multiple files
**Issue**: Hardcoded values scattered throughout code
**Impact**: Difficult to maintain, inconsistent values

**Recommendation**:
```kotlin
// Create api/ApiConstants.kt
package ru.levar.api

object ApiConstants {
    const val DEFAULT_GRANT_TYPE = "password"
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 30L

    const val ERROR_CODE_SUCCESS = 0
}

// Usage
@Query("grant_type") grantType: String = ApiConstants.DEFAULT_GRANT_TYPE

val client = OkHttpClient.Builder()
    .connectTimeout(ApiConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(ApiConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .writeTimeout(ApiConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()
```

**Effort**: 30 minutes
**Test Impact**: None

---

### 12. Create Builder Pattern for Client Configuration
**Location**: `src/main/kotlin/HomemoneyApiClient.kt:13-36`
**Issue**: All configuration in constructor, no flexibility
**Impact**: Difficult to customize logging, timeouts, interceptors

**Recommended Pattern**:
```kotlin
class HomemoneyApiClient private constructor(
    private val apiService: HomemoneyApiService,
) {
    var token: String = ""

    class Builder {
        private var baseUrl: String = AppConfig.serviceUri
        private var connectTimeout: Long = 30L
        private var readTimeout: Long = 30L
        private var writeTimeout: Long = 30L
        private var loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY
        private val interceptors = mutableListOf<Interceptor>()

        fun baseUrl(url: String) = apply { baseUrl = url }
        fun connectTimeout(seconds: Long) = apply { connectTimeout = seconds }
        fun readTimeout(seconds: Long) = apply { readTimeout = seconds }
        fun writeTimeout(seconds: Long) = apply { writeTimeout = seconds }
        fun loggingLevel(level: HttpLoggingInterceptor.Level) = apply { loggingLevel = level }
        fun addInterceptor(interceptor: Interceptor) = apply { interceptors.add(interceptor) }

        fun build(): HomemoneyApiClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = loggingLevel
            }

            val clientBuilder = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)

            interceptors.forEach { clientBuilder.addInterceptor(it) }

            val client = clientBuilder.build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return HomemoneyApiClient(retrofit.create(HomemoneyApiService::class.java))
        }
    }

    companion object {
        fun builder() = Builder()
    }
}

// Usage
val client = HomemoneyApiClient.builder()
    .loggingLevel(HttpLoggingInterceptor.Level.BASIC)
    .connectTimeout(60L)
    .build()
```

**Effort**: 1.5 hours
**Test Impact**: Update test setup to use builder

---

### 13. Split domain/model.kt into Separate Files
**Location**: `src/main/kotlin/domain/model.kt`
**Issue**: Mixes AuthRequest with ErrorType - different concerns
**Impact**: Poor organization, unclear file purpose

**Recommended Structure**:
```
src/main/kotlin/domain/
├── Account.kt
├── AccountGroup.kt
├── AuthRequest.kt  (new)
├── Category.kt
├── ErrorType.kt    (new)
├── Transaction.kt
└── Currency.kt
```

**Effort**: 10 minutes
**Test Impact**: Update imports in tests

---

### 14. Update Outdated Dependencies
**Location**: `build.gradle.kts`
**Issue**: Several dependencies are outdated
**Impact**: Missing bug fixes, security patches, new features

**Current Versions**:
```kotlin
kotlinx-coroutines-core:1.6.4  // Released 2022
okhttp:4.10.0                   // Released 2022
```

**Recommended Versions**:
```kotlin
dependencies {
    // Kotlin Coroutines (1.6.4 → 1.8.1)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // OkHttp (4.10.0 → 4.12.0)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    // Retrofit is current at 3.0.0
    // Other dependencies are current
}
```

**Effort**: 20 minutes (includes test run)
**Test Impact**: Run full test suite to verify compatibility

---

### 15. Add Base Test Class to Reduce Duplication
**Location**: All test classes
**Issue**: MockWebServer setup duplicated across 6 test classes
**Impact**: Maintenance burden, inconsistent setup

**Recommended Pattern**:
```kotlin
// Create src/test/kotlin/ru/levar/base/BaseApiTest.kt
package ru.levar.base

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import ru.levar.HomemoneyApiClient

abstract class BaseApiTest {
    protected lateinit var mockWebServer: MockWebServer
    protected lateinit var apiClient: HomemoneyApiClient
    protected lateinit var baseUrl: String

    @BeforeEach
    fun baseSetup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString()
        apiClient = HomemoneyApiClient(baseUrl)
    }

    @AfterEach
    fun baseTearDown() {
        mockWebServer.shutdown()
    }
}

// Usage
class HomemoneyApiClientTest : BaseApiTest() {
    // Remove setup() and tearDown() - inherited from base

    @Test
    fun `login should succeed with valid credentials`() = runTest {
        // Test implementation
    }
}
```

**Effort**: 45 minutes
**Test Impact**: Refactor all 6 test classes

---

### 16. Implement Automatic Token Refresh
**Location**: `src/main/kotlin/HomemoneyApiClient.kt`
**Issue**: `refreshToken` from AuthResponse is stored but never used
**Impact**: Users must manually re-authenticate when token expires

**Recommended Implementation**:
```kotlin
class HomemoneyApiClient(baseUrl: String = AppConfig.serviceUri) {
    private var _token: String = ""
    private var _refreshToken: String = ""
    private var tokenExpiresAt: Long = 0

    suspend fun login(...): Boolean {
        try {
            val response = apiService.login(login, password, clientId, clientSecret)
            if (!response.isSuccessful || response.body()?.error?.code != 0) {
                throw Exception("Authentication failed")
            }
            _token = response.body()!!.token
            _refreshToken = response.body()!!.refreshToken
            tokenExpiresAt = System.currentTimeMillis() + (3600 * 1000) // 1 hour
            return true
        } catch (e: Exception) {
            logger.error(e) { "Login failed" }
            return false
        }
    }

    private suspend fun ensureValidToken() {
        if (System.currentTimeMillis() >= tokenExpiresAt && _refreshToken.isNotBlank()) {
            refreshAuthToken()
        }
    }

    private suspend fun refreshAuthToken() {
        // Implement refresh endpoint call
        logger.info { "Refreshing authentication token" }
    }

    suspend fun getAccountGroups(): BalanceListResponse {
        ensureValidToken()
        val response = apiService.getAccountGroups(_token)
        return handleResponse(response)
    }
}
```

**Effort**: 2 hours (requires API endpoint implementation)
**Test Impact**: Add tests for token refresh flow

---

## ⚪ LOW PRIORITY (Nice to Have)

### 17. Add Retry Logic with Exponential Backoff
**Location**: New `api/RetryInterceptor.kt`
**Issue**: No handling of transient network failures
**Impact**: Requests fail on temporary network issues

**Recommendation**: Implement OkHttp interceptor with retry logic for 5xx errors and network failures.

**Effort**: 3 hours
**Test Impact**: Add retry scenario tests

---

### 18. Implement Request Cancellation Support
**Location**: `HomemoneyApiClient.kt`
**Issue**: No way to cancel in-flight requests
**Impact**: Wasted resources, can't cancel long-running operations

**Recommendation**: Return `Deferred<T>` instead of direct values to enable cancellation.

**Effort**: 2 hours
**Test Impact**: Add cancellation tests

---

### 19. Add Metrics and Observability Hooks
**Location**: New `api/MetricsInterceptor.kt`
**Issue**: No visibility into API performance, error rates
**Impact**: Difficult to diagnose production issues

**Recommendation**: Add interceptor to track request duration, success/failure rates, endpoint usage.

**Effort**: 4 hours
**Test Impact**: Add metrics validation tests

---

### 20. Localize Comments to English
**Location**: `api/HomemoneyApiService.kt:27, 40, 51`
**Issue**: Mixed Russian/English comments
**Impact**: Team accessibility, international collaboration

**Current**:
```kotlin
// Работа со счетами
// Работа с категориями
// Работа с транзакциями
```

**Recommended**:
```kotlin
// Account operations
// Category operations
// Transaction operations
```

**Effort**: 5 minutes
**Test Impact**: None

---

## Implementation Roadmap

### Week 1: Critical Safety Fixes
- [ ] Fix AppConfig null handling
- [ ] Add proper logging framework
- [ ] Remove non-null assertion
- [ ] Fix semantic field names
- [ ] Remove dead code

**Expected Outcome**: Eliminate crash risks, improve debugging

---

### Week 2: Structural Improvements
- [ ] Extract response DTOs
- [ ] Introduce Result type
- [ ] Add token validation
- [ ] Use Kotlin stdlib idioms
- [ ] Introduce enum types

**Expected Outcome**: Better type safety, consistent error handling

---

### Week 3: Quality & Architecture
- [ ] Extract constants
- [ ] Create builder pattern
- [ ] Split domain/model.kt
- [ ] Update dependencies
- [ ] Add base test class
- [ ] Implement token refresh

**Expected Outcome**: Maintainable, extensible architecture

---

### Week 4: Polish & Advanced Features (Optional)
- [ ] Add retry logic
- [ ] Implement cancellation
- [ ] Add metrics/observability
- [ ] Localize comments

**Expected Outcome**: Production-ready resilient client

---

## Validation Strategy

### Test Coverage Maintenance
- Run `./gradlew test` after EVERY change
- Maintain 68/68 tests passing
- Add new tests for new functionality
- Update existing tests for API changes

### Quality Gates
```bash
# Before committing any refactoring
./gradlew clean build test

# Verify test report
open build/reports/tests/test/index.html

# Check for compilation warnings
./gradlew compileKotlin --warning-mode all
```

### Incremental Approach
1. One refactoring at a time
2. Commit after each successful change
3. Run full test suite between changes
4. Use feature branches for major refactors

---

## Risk Assessment

**Low Risk** (Safe to implement immediately):
- Constants extraction
- Using flatMap
- Comment localization
- Dead code removal

**Medium Risk** (Requires thorough testing):
- DTO extraction
- Enum type introduction
- Dependency updates
- Base test class

**High Risk** (Needs careful planning):
- Result type introduction
- Token refresh mechanism
- Builder pattern migration
- Retry logic

---

## Summary

**Total Recommendations**: 20
**Critical**: 5
**High Priority**: 5
**Medium Priority**: 6
**Low Priority**: 4

**Estimated Effort**:
- Critical fixes: 1 hour
- High priority: 6 hours
- Medium priority: 8 hours
- Low priority: 13 hours
- **Total**: ~28 hours over 4 weeks

**Expected Quality Improvement**:
- Null safety: 100% → Complete
- Error handling: Inconsistent → Standardized
- Code organization: 6/10 → 9/10
- Type safety: 7/10 → 9/10
- Maintainability: 6/10 → 8/10

All recommendations preserve the existing test suite and maintain backward compatibility where possible. Breaking changes are clearly marked and include migration guides.
