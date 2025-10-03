# iHomemoney Kotlin Client - Test Suite Documentation

## Overview

This comprehensive test suite ensures the reliability, safety, and correctness of the iHomemoney Kotlin API client. All tests are designed with safety as the top priority - **NO tests contact production endpoints**.

## Safety Guarantees

### Production Endpoint Protection
- All tests use `MockWebServer` from OkHttp for HTTP simulation
- Test configuration (`src/test/resources/config/config.properties`) points to `localhost`
- `AppConfigTest` includes explicit safety checks to prevent production usage
- No hardcoded production URLs in any test file

### Isolation
- Each test is self-contained with proper setup/teardown
- MockWebServer instances are created and destroyed per test class
- No shared state between tests
- Tests can run in parallel without conflicts

## Test Structure

### Test Categories

#### 1. Unit Tests
**Location**: `src/test/kotlin/ru/levar/`

- **HomemoneyApiClientTest.kt** (18 tests)
  - Client initialization and configuration
  - Authentication flow (login success/failure)
  - API method functionality (getAccounts, getCategories, getTransactions)
  - Token management
  - Error handling
  - Response parsing

- **AppConfigTest.kt** (3 tests)
  - Configuration loading from properties
  - Safety validation (production endpoint detection)
  - URI format validation

#### 2. API Service Tests
**Location**: `src/test/kotlin/ru/levar/api/`

- **HomemoneyApiServiceTest.kt** (8 tests)
  - Retrofit interface behavior
  - Request parameter encoding
  - Response deserialization
  - HTTP error handling
  - Network failure scenarios

#### 3. Domain Model Tests
**Location**: `src/test/kotlin/ru/levar/domain/`

- **DomainModelTest.kt** (15 tests)
  - JSON serialization/deserialization
  - Data class properties and behavior
  - Complex nested structures
  - Field mapping correctness
  - Copy functionality

#### 4. Integration Tests
**Location**: `src/test/kotlin/ru/levar/integration/`

- **ApiIntegrationTest.kt** (7 tests)
  - Complete user workflows (login → fetch data)
  - Multi-step operations
  - Token persistence across requests
  - Error propagation in workflows
  - Complex data structure handling

#### 5. Edge Cases and Error Scenarios
**Location**: `src/test/kotlin/ru/levar/`

- **EdgeCaseTest.kt** (26 tests)
  - Malformed JSON handling
  - Network timeouts and failures
  - Special characters in input
  - Large data sets
  - Unicode support
  - HTTP error codes (401, 403, 500, 503)
  - Empty/null values
  - Concurrent requests
  - Boundary conditions

## Test Coverage

### Functional Coverage
- ✅ Authentication (login with various scenarios)
- ✅ Account management (getAccounts, getAccountGroups)
- ✅ Category retrieval (getCategories)
- ✅ Transaction queries (getTransactions with filtering)
- ✅ Error handling and recovery
- ✅ Configuration management

### Non-Functional Coverage
- ✅ Network failures and timeouts
- ✅ Malformed data handling
- ✅ Large dataset processing
- ✅ Concurrent request handling
- ✅ Special characters and Unicode
- ✅ HTTP error codes
- ✅ Empty response handling

### Domain Coverage
- ✅ Account
- ✅ AccountGroup
- ✅ AccountCurrencyInfo
- ✅ Category
- ✅ Transaction
- ✅ Currency
- ✅ ErrorType
- ✅ AuthRequest

## Running Tests

### Prerequisites
```bash
# Ensure dependencies are installed
./gradlew clean build
```

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests ru.levar.HomemoneyApiClientTest
./gradlew test --tests ru.levar.api.HomemoneyApiServiceTest
./gradlew test --tests ru.levar.domain.DomainModelTest
./gradlew test --tests ru.levar.integration.ApiIntegrationTest
./gradlew test --tests ru.levar.EdgeCaseTest
```

### Run Tests with Coverage Report
```bash
./gradlew test jacocoTestReport
# Report will be at: build/reports/jacoco/test/html/index.html
```

### Run Tests in Verbose Mode
```bash
./gradlew test --info
```

### Run Tests in Watch Mode (Continuous)
```bash
./gradlew test --continuous
```

## Test Statistics

- **Total Test Classes**: 6
- **Total Test Methods**: 77+
- **Test Execution Time**: ~5-10 seconds (all tests)
- **Code Coverage Target**: >80% line coverage

## Test Dependencies

All testing dependencies are managed in `build.gradle.kts`:

```kotlin
testImplementation(kotlin("test"))                              // Kotlin test framework
testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")     // JUnit 5
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4") // Coroutine testing
testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0") // HTTP mocking
testImplementation("io.mockk:mockk:1.13.5")                     // Kotlin mocking library
testImplementation("org.assertj:assertj-core:3.24.2")           // Fluent assertions
```

## Test Patterns and Best Practices

### Naming Conventions
- Test methods use backticks with descriptive names: `` `should do something when condition` ``
- Test classes follow `[ClassName]Test.kt` pattern
- Clear Given-When-Then structure in test comments

### Arrange-Act-Assert Pattern
```kotlin
@Test
fun `example test pattern`() = runTest {
    // Arrange - Setup test data and mocks
    mockWebServer.enqueue(MockResponse()...)

    // Act - Execute the operation
    val result = apiClient.someMethod()

    // Assert - Verify expectations
    assertThat(result).isEqualTo(expected)
}
```

### MockWebServer Usage
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

### Coroutine Testing
```kotlin
@Test
fun `test with coroutines`() = runTest {
    // Test code using suspend functions
}
```

## Quality Gates

### Test Failures
Tests will fail if:
- Production endpoints are detected in configuration
- Network calls are made to non-mocked servers
- Expected data structures don't match actual
- Error handling is insufficient
- Timeout thresholds are exceeded

### CI/CD Integration
Tests are designed to run in CI/CD pipelines:
- Fast execution (<10 seconds total)
- No external dependencies
- Deterministic results
- Clear failure messages

## Coverage Gaps and Future Improvements

### Current Gaps
1. Performance/load testing not included
2. Security testing (authentication edge cases) could be expanded
3. Commented-out functionality not tested (createTransaction, getCurrencies)
4. Real network integration tests (optional, with flag)

### Recommended Additions
1. **Property-Based Testing**: Use Kotest or similar for generative testing
2. **Contract Testing**: Add Pact or similar for API contract verification
3. **Mutation Testing**: Use Pitest to verify test effectiveness
4. **Performance Benchmarks**: Add JMH benchmarks for critical paths
5. **Security Tests**: Add tests for SQL injection, XSS in data fields

### Future Test Coverage
```kotlin
// Planned tests (commented functionality)
- createTransaction() tests
- getCurrencies() tests
- Refresh token flow
- Token expiration handling
- Rate limiting scenarios
```

## Troubleshooting

### Common Issues

#### Tests fail with "Connection refused"
- Ensure MockWebServer is properly initialized in `@BeforeEach`
- Check that `tearDown()` is called in `@AfterEach`

#### Tests fail with timeout
- Check coroutine test scope usage
- Verify MockWebServer response queue is not empty
- Increase timeout in client configuration if needed

#### JSON parsing errors
- Verify test JSON matches actual API response format
- Check for missing/extra fields in domain models
- Validate SerializedName annotations

#### Configuration not found
- Ensure `src/test/resources/config/config.properties` exists
- Check file is included in test classpath
- Verify properties file syntax

## Continuous Improvement

### Adding New Tests
1. Follow existing patterns and structure
2. Use MockWebServer for HTTP testing
3. Include safety checks for production endpoints
4. Add clear documentation in test names
5. Update this documentation

### Maintaining Tests
1. Keep tests isolated and independent
2. Update tests when API changes
3. Refactor common patterns into helper methods
4. Monitor test execution time
5. Review and update coverage metrics

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Kotlin Coroutines Testing](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [MockWebServer Guide](https://github.com/square/okhttp/tree/master/mockwebserver)
- [AssertJ Documentation](https://assertj.github.io/doc/)

## Contact and Support

For questions or issues with tests:
1. Review this documentation
2. Check test failure messages
3. Examine test implementation for examples
4. Consult project maintainers

---

**Last Updated**: 2025-10-03
**Test Suite Version**: 1.0
**Total Tests**: 77+
**Safety Level**: Production-isolated ✅
