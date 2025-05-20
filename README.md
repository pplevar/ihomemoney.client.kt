# iHomemoney Kotlin API Client
This is the simple Kotlin API Client for iHomemoney service.

## Usage
The simple usage is:

```kotlin
package ru.levar

suspend fun main() {
    val apiClient = HomemoneyApiClient()

    try {
        apiClient.login("login", "password", "5", "demo")

        val accounts = apiClient.getAccounts()
        println("Счета: $accounts")

        val categories = apiClient.getCategories()
        println("Категории: $categories")

        val transactions = apiClient.getTransactions(10)
        println("Транзакции: $transactions")
    } catch (e: Exception) {
        println("Ошибка: ${e.message}")
    }
}
```
