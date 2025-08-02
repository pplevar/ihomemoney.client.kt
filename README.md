# iHomemoney Kotlin API Client
Это простой Kotlin клиент для сервиса iHomemoney.

## Использование
Ниже приведен типовой способ использования:

```kotlin
fun main() {
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

## Ссылки
- https://ihomemoney.com/api/api2.asmx - Документация по API.