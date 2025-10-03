# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Packages publishing configuration
- Automated release workflow with GitHub Actions
- Snapshot publishing workflow for continuous delivery
- Complete POM metadata (licenses, developers, SCM)
- Source and Javadoc JAR generation with Dokka

### Changed
- Project artifact name from `iHomemoney.clien.kt` to `ihomemoney-client-kt`

## [1.0.0] - 2025-10-03

### Added
- Initial release of iHomemoney Kotlin API Client
- Authentication support with token-based API access
- Account management operations
  - Retrieve account groups with `getAccountGroups()`
  - Retrieve flattened account list with `getAccounts()`
- Category operations
  - Retrieve income and expense categories with `getCategories()`
- Transaction retrieval
  - Retrieve transactions with optional count limit via `getTransactions(topCount)`
- Comprehensive test suite
  - 68 tests across 6 test classes
  - Unit tests for API client and service
  - Domain model serialization tests
  - Integration tests for multi-step workflows
  - Edge case and error scenario testing
  - Configuration validation tests
- Full MockWebServer test coverage (no production endpoints contacted)
- Kotlin Coroutines support for async operations
- HTTP logging with OkHttp interceptor
- Complete API documentation in README.md

### Dependencies
- Kotlin 2.1.20
- Retrofit 3.0.0 for HTTP client
- OkHttp 4.10.0 for networking
- Gson for JSON serialization
- Kotlin Coroutines 1.6.4 for async support
- JUnit 5 + Kotest for testing
- MockWebServer for API testing
- MockK for mocking
- AssertJ for assertions

### Documentation
- Comprehensive README.md with usage examples
- CLAUDE.md for development guidelines
- TEST_DOCUMENTATION.md for test suite details
- PUBLISHING_PLAN.md for GitHub Packages publishing guide
- MIT License

---

## Version History

- **1.0.0** (2025-10-03) - Initial release with authentication, accounts, categories, and transactions

[Unreleased]: https://github.com/pplevar/ihomemoney.client.kt/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/pplevar/ihomemoney.client.kt/releases/tag/v1.0.0
