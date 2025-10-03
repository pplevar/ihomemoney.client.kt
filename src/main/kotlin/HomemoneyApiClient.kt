package ru.levar

import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.levar.api.HomemoneyApiService
import ru.levar.domain.Account
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class HomemoneyApiClient(baseUrl: String = AppConfig.serviceUri) {
    private val apiService: HomemoneyApiService
    private var _token: String = ""

    var token: String
        get() = _token
        set(value) {
            require(value.isNotBlank()) { "Token cannot be blank" }
            _token = value
        }

    init {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

        val client =
            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        apiService = retrofit.create(HomemoneyApiService::class.java)
    }

    suspend fun login(
        login: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ): Boolean {
        try {
            val response = apiService.login(login, password, clientId, clientSecret)
            if (!response.isSuccessful || response.body()?.error?.code != 0) {
                throw Exception("Authentication failed: ${response.body()?.error ?: response.message()}")
            }
            token = response.body()!!.token
            return true
        } catch (e: Exception) {
            logger.error(e) { "Authentication failed: ${e.message}" }
            return false
        }
    }

    private fun requireAuthentication() {
        require(_token.isNotBlank()) { "Authentication required. Call login() first." }
    }

    suspend fun getAccountGroups(): ru.levar.api.dto.BalanceListResponse {
        requireAuthentication()
        val response = apiService.getAccountGroups(token)
        return handleResponse(response)
    }

    suspend fun getAccounts(): List<Account> {
        val accGroups = getAccountGroups()
        val accounts = mutableListOf<Account>()
        for (accGroup in accGroups.listAccountGroupInfo) {
            for (acc in accGroup.listAccountInfo) {
                accounts.add(acc)
            }
        }
        return accounts
    }

    suspend fun getCategories(): ru.levar.api.dto.CategoryListResponse {
        requireAuthentication()
        val response = apiService.getCategories(token)
        return handleResponse(response)
    }

    suspend fun getTransactions(topCount: Int?): ru.levar.api.dto.TransactionListResponse {
        requireAuthentication()
        val response = apiService.getTransactions(token, topCount)
        return handleResponse(response)
    }

//    suspend fun getTransactions(
//        startDate: String? = null,
//        endDate: String? = null,
//        accountId: String? = null,
//        categoryId: String? = null,
//        page: Int? = null,
//        pageSize: Int? = null
//    ): List<Transaction> {
//        val response = apiService.getTransactions(
//            token,
//            startDate,
//            endDate,
//            accountId,
//            categoryId,
//            page,
//            pageSize
//        )
//        return handleResponse(response)
//    }

//    suspend fun createTransaction(transaction: Transaction): String {
//        val response = apiService.createTransaction(token, transaction)
//        return handleResponse(response)
//    }

//    suspend fun getCurrencies(): List<Currency> {
//        val response = apiService.getCurrencies(token)
//        return handleResponse(response)
//    }

    private fun <T> handleResponse(response: Response<T>): T {
        if (!response.isSuccessful) {
            throw Exception("Request failed with code ${response.code()}: ${response.message()}")
        }

        val body = response.body()!!
//        if (body == null || body.error?.code != 0) {
//            throw Exception("API error: ${body?.error?.message ?: "Unknown error"}")
//        }

        return body
    }
}
