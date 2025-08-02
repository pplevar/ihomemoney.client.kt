# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin API client for the iHomemoney financial service (https://ihomemoney.com/api/api2.asmx). It provides a simple interface to interact with the iHomemoney REST API for managing accounts, categories, and financial transactions.

## Essential Commands

### Building and Testing
- `./gradlew build` - Compile and run all tests
- `./gradlew clean build` - Clean build directory and rebuild
- `./gradlew test` - Run unit tests only
- `./gradlew compileKotlin` - Compile main Kotlin sources without tests
- `./gradlew compileTestKotlin` - Compile test sources

### Running the Application
There is no dedicated run task. The main entry point is `MainTest.kt` which can be executed directly from an IDE or compiled and run manually.

## Architecture

### Core Components
- **HomemoneyApiClient** - Main client class that orchestrates API calls and manages authentication token
- **HomemoneyApiService** - Retrofit interface defining all HTTP endpoints with proper annotations
- **AppConfig** - Configuration loader for external properties (loads from `config.properties`)

### Domain Model Structure
Located in `src/main/kotlin/domain/`:
- **Account** - Financial account with currency information
- **AccountGroup** - Groups of related accounts  
- **Category** - Transaction categorization
- **Transaction** - Financial transaction record
- **Currency** - Currency information
- **ErrorType** - API error response structure
- **model.kt** - Additional domain models like AuthRequest

### Key Dependencies
- **Retrofit 3.0.0** - HTTP client with Gson converter for JSON serialization
- **OkHttp 4.10.0** - Underlying HTTP client with logging interceptor
- **Kotlin Coroutines 1.6.4** - All API methods are suspend functions

### Authentication Flow
1. Call `login()` with credentials and client parameters
2. API returns access token stored in client instance
3. Token is automatically passed to subsequent API calls via query parameter

### API Service Structure
The `HomemoneyApiService` interface defines:
- **TokenPassword** endpoint for authentication
- **BalanceList** endpoint for account groups/accounts  
- **CategoryList** endpoint for transaction categories
- **TransactionList** endpoint for transaction history

Response wrapper classes are defined as nested data classes within the service interface.

### Configuration
- Base URL is externalized to `src/main/resources/config/config.properties`
- Default service URI: `https://ihomemoney.com/api/api2.asmx/`
- HTTP timeouts configured to 30 seconds for connect/read/write operations

### Testing Approach
- No formal test framework is currently configured beyond basic JUnit platform
- `MainTest.kt` serves as an integration test example showing typical usage flow
- Contains hardcoded credentials for testing (should be externalized for production use)